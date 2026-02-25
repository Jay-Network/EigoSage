import CoreGraphics

struct TapResult: Equatable {
    let word: String
    let wordBox: CGRect      // normalized image coordinates (0..1)
    let lineIndex: Int
    let wordIndex: Int
}
