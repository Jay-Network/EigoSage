import XCTest
@testable import EigoLens

final class EigoLensTests: XCTestCase {
    func testReadabilityCalculator() {
        let calc = ReadabilityCalculator()

        // "The cat sat on the mat." — simple text
        let result = calc.calculate(text: "The cat sat on the mat.")
        XCTAssertNotNil(result)
        XCTAssertEqual(result?.statistics.totalWords, 6)
        XCTAssertEqual(result?.statistics.totalSentences, 1)
    }

    func testSyllableCounter() {
        let calc = ReadabilityCalculator()
        XCTAssertEqual(calc.countSyllables("beautiful"), 3)
        XCTAssertEqual(calc.countSyllables("the"), 1)
        XCTAssertEqual(calc.countSyllables("cat"), 1)
        XCTAssertEqual(calc.countSyllables("understanding"), 4)
    }

    func testOcrTextMerger_matchingCounts() {
        let visionResult = OCRResult(
            lines: [
                DetectedLine(
                    text: "Hello wrold",
                    boundingBox: .init(x: 0, y: 0, width: 1, height: 0.1),
                    confidence: 0.9,
                    words: [
                        DetectedWord(text: "Hello", boundingBox: .init(x: 0, y: 0, width: 0.5, height: 0.1), confidence: 0.9, isWord: true),
                        DetectedWord(text: "wrold", boundingBox: .init(x: 0.5, y: 0, width: 0.5, height: 0.1), confidence: 0.9, isWord: true)
                    ]
                )
            ],
            imageSize: .init(width: 1000, height: 500),
            processingTimeMs: 100,
            timestamp: .now
        )

        let geminiLines = ["Hello world"]
        let merged = OcrTextMerger.merge(visionResult: visionResult, geminiLines: geminiLines)

        XCTAssertEqual(merged.lines[0].text, "Hello world")
        XCTAssertEqual(merged.lines[0].words[1].text, "world")
    }

    func testOcrTextMerger_mismatchedLineCount() {
        let visionResult = OCRResult(
            lines: [
                DetectedLine(text: "Line one", boundingBox: .zero, confidence: 0.9, words: [])
            ],
            imageSize: .init(width: 100, height: 100),
            processingTimeMs: 50,
            timestamp: .now
        )

        let geminiLines = ["Line one", "Line two"]
        let merged = OcrTextMerger.merge(visionResult: visionResult, geminiLines: geminiLines)

        // Should keep original (mismatched counts)
        XCTAssertEqual(merged.lines.count, 1)
        XCTAssertEqual(merged.lines[0].text, "Line one")
    }
}
