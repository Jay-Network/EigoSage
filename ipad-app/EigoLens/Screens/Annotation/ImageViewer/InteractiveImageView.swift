import SwiftUI

struct InteractiveImageView: View {
    let capturedImage: CapturedImage
    let interactionMode: InteractionMode
    let tappedWord: TapResult?
    var onWordTapped: (TapResult) -> Void
    var onWordLongPressed: (TapResult) -> Void
    var onWordsSelected: ([String]) -> Void

    @State private var scale: CGFloat = 1.0
    @State private var lastScale: CGFloat = 1.0
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero

    // Selection mode state (active drag)
    @State private var selectStart: CGPoint?
    @State private var selectEnd: CGPoint?
    // Completed selection (persists after finger release)
    @State private var completedSelectStart: CGPoint?
    @State private var completedSelectEnd: CGPoint?

    var body: some View {
        GeometryReader { geometry in
            let imageSize = capturedImage.image.size
            let containerSize = geometry.size

            if imageSize.width > 0 && imageSize.height > 0 {
                imageContent(imageSize: imageSize, containerSize: containerSize)
            } else {
                Color.black
            }
        }
        .background(Color.black)
    }

    // MARK: - Image Content

    private func imageContent(imageSize: CGSize, containerSize: CGSize) -> some View {
        let fitScale = (containerSize.width > 0 && containerSize.height > 0)
            ? min(containerSize.width / imageSize.width, containerSize.height / imageSize.height)
            : 1.0
        let displaySize = CGSize(width: imageSize.width * fitScale, height: imageSize.height * fitScale)

        return ZStack {
            // Image layer
            Image(uiImage: capturedImage.image)
                .resizable()
                .scaledToFit()
                .scaleEffect(scale)
                .offset(offset)

            // OCR overlay
            Canvas { context, size in
                drawOCROverlay(
                    context: &context,
                    size: size,
                    displaySize: displaySize,
                    containerSize: containerSize
                )
            }
            .allowsHitTesting(false)
            .scaleEffect(scale)
            .offset(offset)

            // Completed selection rectangle (persists after finger release)
            if let start = completedSelectStart, let end = completedSelectEnd {
                selectionRectangle(start: start, end: end)
            }

            // Active drag selection rectangle
            if interactionMode == .select, let start = selectStart, let end = selectEnd {
                selectionRectangle(start: start, end: end)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .contentShape(Rectangle())
        .gesture(combinedGesture(containerSize: containerSize, displaySize: displaySize, fitScale: fitScale))
    }

    @ViewBuilder
    private func selectionRectangle(start: CGPoint, end: CGPoint) -> some View {
        let rect = CGRect(
            x: min(start.x, end.x),
            y: min(start.y, end.y),
            width: abs(end.x - start.x),
            height: abs(end.y - start.y)
        )
        ZStack {
            Path { path in path.addRect(rect) }
                .fill(Color(hex: 0xFFEB3B).opacity(0.10))
            Path { path in path.addRect(rect) }
                .stroke(Color(hex: 0xFFEB3B).opacity(0.7), lineWidth: 2)
        }
    }

    // MARK: - OCR Overlay Drawing

    private func drawOCROverlay(
        context: inout GraphicsContext,
        size: CGSize,
        displaySize: CGSize,
        containerSize: CGSize
    ) {
        let ocrResult = capturedImage.ocrResult
        let originX = (containerSize.width - displaySize.width) / 2
        let originY = (containerSize.height - displaySize.height) / 2

        for (lineIndex, line) in ocrResult.lines.enumerated() {
            for (wordIndex, word) in line.words.enumerated() {
                guard word.isWord else { continue }

                let rect = wordScreenRect(word.boundingBox, displaySize: displaySize, origin: CGPoint(x: originX, y: originY))

                let isTapped = tappedWord?.lineIndex == lineIndex && tappedWord?.wordIndex == wordIndex

                if interactionMode == .tap {
                    // Show subtle underline for all words, highlight for tapped
                    if isTapped {
                        context.fill(
                            Path(roundedRect: rect.insetBy(dx: -2, dy: -2), cornerRadius: 4),
                            with: .color(EigoLensTheme.primary.opacity(0.3))
                        )
                        context.stroke(
                            Path(roundedRect: rect.insetBy(dx: -2, dy: -2), cornerRadius: 4),
                            with: .color(EigoLensTheme.primary),
                            lineWidth: 2
                        )
                    }
                } else {
                    // SELECT mode: show all word boxes
                    context.stroke(
                        Path(roundedRect: rect, cornerRadius: 2),
                        with: .color(Color.orange.opacity(isTapped ? 0.8 : 0.3)),
                        lineWidth: 1
                    )
                }
            }
        }
    }

    // MARK: - Gestures

    private func combinedGesture(containerSize: CGSize, displaySize: CGSize, fitScale: CGFloat) -> some Gesture {
        let tapGesture = SpatialTapGesture()
            .onEnded { value in
                guard interactionMode == .tap else { return }
                handleTap(at: value.location, containerSize: containerSize, displaySize: displaySize, fitScale: fitScale)
            }

        let longPressGesture = LongPressGesture(minimumDuration: 0.5)
            .sequenced(before: SpatialTapGesture())
            .onEnded { value in
                guard interactionMode == .tap else { return }
                if case .second(_, let tapValue) = value, let location = tapValue?.location {
                    handleLongPress(at: location, containerSize: containerSize, displaySize: displaySize, fitScale: fitScale)
                }
            }

        let magnificationGesture = MagnificationGesture()
            .onChanged { value in
                scale = (lastScale * value).clamped(to: 0.5...5.0)
            }
            .onEnded { value in
                lastScale = scale
            }

        let dragGesture = DragGesture()
            .onChanged { value in
                if interactionMode == .select {
                    if selectStart == nil {
                        selectStart = value.startLocation
                    }
                    selectEnd = value.location
                } else {
                    offset = CGSize(
                        width: lastOffset.width + value.translation.width,
                        height: lastOffset.height + value.translation.height
                    )
                }
            }
            .onEnded { value in
                if interactionMode == .select {
                    handleSelectionEnd(containerSize: containerSize, displaySize: displaySize, fitScale: fitScale)
                    selectStart = nil
                    selectEnd = nil
                } else {
                    lastOffset = offset
                }
            }

        return tapGesture
            .simultaneously(with: longPressGesture)
            .simultaneously(with: magnificationGesture.simultaneously(with: dragGesture))
    }

    // MARK: - Hit Testing

    private func handleTap(at point: CGPoint, containerSize: CGSize, displaySize: CGSize, fitScale: CGFloat) {
        completedSelectStart = nil
        completedSelectEnd = nil
        if let result = findTappedWord(at: point, containerSize: containerSize, displaySize: displaySize, fitScale: fitScale) {
            onWordTapped(result)
        }
    }

    private func handleLongPress(at point: CGPoint, containerSize: CGSize, displaySize: CGSize, fitScale: CGFloat) {
        completedSelectStart = nil
        completedSelectEnd = nil
        if let result = findTappedWord(at: point, containerSize: containerSize, displaySize: displaySize, fitScale: fitScale) {
            onWordLongPressed(result)
        }
    }

    private func handleSelectionEnd(containerSize: CGSize, displaySize: CGSize, fitScale: CGFloat) {
        guard let start = selectStart, let end = selectEnd else { return }

        let selectionRect = CGRect(
            x: min(start.x, end.x),
            y: min(start.y, end.y),
            width: abs(end.x - start.x),
            height: abs(end.y - start.y)
        )

        let words = findWordsInRect(selectionRect, containerSize: containerSize, displaySize: displaySize)
        if !words.isEmpty {
            completedSelectStart = start
            completedSelectEnd = end
            onWordsSelected(words)
        }
    }

    private func findTappedWord(
        at screenPoint: CGPoint,
        containerSize: CGSize,
        displaySize: CGSize,
        fitScale: CGFloat
    ) -> TapResult? {
        let scaledWidth = displaySize.width * scale
        let scaledHeight = displaySize.height * scale
        guard scaledWidth > 0, scaledHeight > 0 else { return nil }

        let originX = (containerSize.width - scaledWidth) / 2 + offset.width
        let originY = (containerSize.height - scaledHeight) / 2 + offset.height

        // Convert screen point to normalized image coordinates
        let imageX = (screenPoint.x - originX) / scaledWidth
        let imageY = (screenPoint.y - originY) / scaledHeight

        let tolerance = Configuration.tapTolerancePoints / scaledWidth

        var bestResult: TapResult?
        var bestDistance: CGFloat = .greatestFiniteMagnitude

        for (lineIndex, line) in capturedImage.ocrResult.lines.enumerated() {
            for (wordIndex, word) in line.words.enumerated() {
                guard word.isWord else { continue }
                let box = word.boundingBox
                let expandedBox = box.insetBy(dx: -tolerance, dy: -tolerance)

                if expandedBox.contains(CGPoint(x: imageX, y: imageY)) {
                    let centerX = box.midX
                    let centerY = box.midY
                    let dist = hypot(imageX - centerX, imageY - centerY)
                    if dist < bestDistance {
                        bestDistance = dist
                        bestResult = TapResult(
                            word: word.text,
                            wordBox: box,
                            lineIndex: lineIndex,
                            wordIndex: wordIndex
                        )
                    }
                }
            }
        }

        return bestResult
    }

    private func findWordsInRect(_ screenRect: CGRect, containerSize: CGSize, displaySize: CGSize) -> [String] {
        let scaledWidth = displaySize.width * scale
        let scaledHeight = displaySize.height * scale
        guard scaledWidth > 0, scaledHeight > 0 else { return [] }

        let originX = (containerSize.width - scaledWidth) / 2 + offset.width
        let originY = (containerSize.height - scaledHeight) / 2 + offset.height

        // Convert screen rect to normalized image coordinates
        let imgX = (screenRect.minX - originX) / scaledWidth
        let imgY = (screenRect.minY - originY) / scaledHeight
        let imgW = screenRect.width / scaledWidth
        let imgH = screenRect.height / scaledHeight
        let imageRect = CGRect(x: imgX, y: imgY, width: imgW, height: imgH)

        var selectedWords: [(word: String, y: CGFloat, x: CGFloat)] = []

        for line in capturedImage.ocrResult.lines {
            for word in line.words {
                guard word.isWord else { continue }
                if imageRect.intersects(word.boundingBox) {
                    selectedWords.append((word.text, word.boundingBox.midY, word.boundingBox.midX))
                }
            }
        }

        // Sort in reading order: top-to-bottom, then left-to-right
        selectedWords.sort { a, b in
            if abs(a.y - b.y) < 0.01 {
                return a.x < b.x
            }
            return a.y < b.y
        }

        return selectedWords.map(\.word)
    }

    // MARK: - Coordinate Helpers

    private func wordScreenRect(_ normalizedBox: CGRect, displaySize: CGSize, origin: CGPoint) -> CGRect {
        CGRect(
            x: origin.x + normalizedBox.minX * displaySize.width,
            y: origin.y + normalizedBox.minY * displaySize.height,
            width: normalizedBox.width * displaySize.width,
            height: normalizedBox.height * displaySize.height
        )
    }
}
