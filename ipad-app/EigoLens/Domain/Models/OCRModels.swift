import Foundation
import CoreGraphics

struct OCRResult: Equatable {
    let lines: [DetectedLine]
    let imageSize: CGSize
    let processingTimeMs: Int
    let timestamp: Date

    var fullText: String {
        lines.map(\.text).joined(separator: " ")
    }

    var wordCount: Int {
        lines.reduce(0) { $0 + $1.words.count }
    }

    static let empty = OCRResult(lines: [], imageSize: .zero, processingTimeMs: 0, timestamp: .now)
}

struct DetectedLine: Equatable {
    let text: String
    let boundingBox: CGRect   // normalized 0..1, top-left origin (flipped from Vision)
    let confidence: Float
    var words: [DetectedWord]
}

struct DetectedWord: Equatable {
    var text: String
    let boundingBox: CGRect   // normalized 0..1, top-left origin
    let confidence: Float
    var isWord: Bool
}
