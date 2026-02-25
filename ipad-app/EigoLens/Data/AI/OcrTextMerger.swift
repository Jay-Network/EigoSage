import Foundation

enum OcrTextMerger {
    static func merge(visionResult: OCRResult, geminiLines: [String]) -> OCRResult {
        let visionLines = visionResult.lines
        guard !visionLines.isEmpty, !geminiLines.isEmpty else { return visionResult }

        // Only merge when line counts match exactly
        guard visionLines.count == geminiLines.count else {
            return visionResult
        }

        var corrected: [DetectedLine] = []

        for (i, visionLine) in visionLines.enumerated() {
            let geminiLine = geminiLines[i]
            let geminiWords = geminiLine.split(separator: " ", omittingEmptySubsequences: true).map(String.init)

            let mergedWords = mergeWords(visionWords: visionLine.words, geminiWords: geminiWords)

            corrected.append(DetectedLine(
                text: geminiLine,
                boundingBox: visionLine.boundingBox,
                confidence: visionLine.confidence,
                words: mergedWords
            ))
        }

        return OCRResult(
            lines: corrected,
            imageSize: visionResult.imageSize,
            processingTimeMs: visionResult.processingTimeMs,
            timestamp: visionResult.timestamp
        )
    }

    private static func mergeWords(visionWords: [DetectedWord], geminiWords: [String]) -> [DetectedWord] {
        guard !visionWords.isEmpty, !geminiWords.isEmpty else { return visionWords }

        // Only replace when word counts match exactly
        guard visionWords.count == geminiWords.count else {
            return visionWords
        }

        return visionWords.enumerated().map { i, element in
            DetectedWord(
                text: geminiWords[i],
                boundingBox: element.boundingBox,
                confidence: element.confidence,
                isWord: geminiWords[i].contains(where: \.isLetter)
            )
        }
    }
}
