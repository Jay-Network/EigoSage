import Vision
import UIKit

enum OCRError: Error, LocalizedError {
    case invalidImage
    case recognitionFailed(String)

    var errorDescription: String? {
        switch self {
        case .invalidImage: return "Invalid image for OCR"
        case .recognitionFailed(let msg): return "OCR failed: \(msg)"
        }
    }
}

final class OCRService {
    func recognizeText(in image: UIImage) async throws -> OCRResult {
        guard let cgImage = image.cgImage else {
            throw OCRError.invalidImage
        }

        let start = Date()

        let request = VNRecognizeTextRequest()
        request.recognitionLevel = .accurate
        request.usesLanguageCorrection = true
        request.recognitionLanguages = ["en-US"]

        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        try handler.perform([request])

        guard let observations = request.results else {
            return OCRResult(lines: [], imageSize: image.size, processingTimeMs: 0, timestamp: start)
        }

        var lines: [DetectedLine] = []

        for observation in observations {
            guard let topCandidate = observation.topCandidates(1).first else { continue }

            // Flip Y: Vision uses bottom-left origin; we use top-left
            let visionBox = observation.boundingBox
            let flippedBox = CGRect(
                x: visionBox.minX,
                y: 1.0 - visionBox.maxY,
                width: visionBox.width,
                height: visionBox.height
            )

            let words = buildWords(from: topCandidate, observation: observation, flippedLineBounds: flippedBox)

            lines.append(DetectedLine(
                text: topCandidate.string,
                boundingBox: flippedBox,
                confidence: topCandidate.confidence,
                words: words
            ))
        }

        let elapsed = Int(Date().timeIntervalSince(start) * 1000)
        return OCRResult(lines: lines, imageSize: image.size, processingTimeMs: elapsed, timestamp: start)
    }

    private func buildWords(
        from candidate: VNRecognizedText,
        observation: VNRecognizedTextObservation,
        flippedLineBounds: CGRect
    ) -> [DetectedWord] {
        let rawText = candidate.string
        let rawWords = rawText.split(separator: " ", omittingEmptySubsequences: true).map(String.init)
        guard !rawWords.isEmpty else { return [] }

        // Try to get per-word bounding boxes from Vision
        var words: [DetectedWord] = []
        let nsString = rawText as NSString

        for word in rawWords {
            let range = nsString.range(of: word)
            guard range.location != NSNotFound else {
                // Fallback: proportional subdivision
                return buildWordsProportional(rawWords: rawWords, flippedLineBounds: flippedLineBounds, confidence: candidate.confidence)
            }

            let stringRange = Range(range, in: rawText)!
            if let wordBox = try? candidate.boundingBox(for: stringRange) {
                let visionRect = wordBox.boundingBox
                let flippedWordBox = CGRect(
                    x: visionRect.minX,
                    y: 1.0 - visionRect.maxY,
                    width: visionRect.width,
                    height: visionRect.height
                )
                words.append(DetectedWord(
                    text: word,
                    boundingBox: flippedWordBox,
                    confidence: candidate.confidence,
                    isWord: word.contains(where: \.isLetter)
                ))
            } else {
                return buildWordsProportional(rawWords: rawWords, flippedLineBounds: flippedLineBounds, confidence: candidate.confidence)
            }
        }

        return words
    }

    private func buildWordsProportional(
        rawWords: [String],
        flippedLineBounds: CGRect,
        confidence: Float
    ) -> [DetectedWord] {
        let totalChars = max(rawWords.reduce(0) { $0 + $1.count }, 1)
        var x = flippedLineBounds.minX

        return rawWords.map { word in
            let frac = CGFloat(word.count) / CGFloat(totalChars)
            let w = flippedLineBounds.width * frac
            let box = CGRect(x: x, y: flippedLineBounds.minY, width: w, height: flippedLineBounds.height)
            x += w
            return DetectedWord(
                text: word,
                boundingBox: box,
                confidence: confidence,
                isWord: word.contains(where: \.isLetter)
            )
        }
    }
}
