import Foundation

enum ScopeLevel: Equatable {
    case word(String)
    case phrase(text: String, words: [String])
    case sentence(String)
    case paragraph(String)
    case fullSnapshot

    var displayName: String {
        switch self {
        case .word: return "Word"
        case .phrase: return "Phrase"
        case .sentence: return "Sentence"
        case .paragraph: return "Paragraph"
        case .fullSnapshot: return "Full Text"
        }
    }

    var scopeName: String {
        switch self {
        case .word: return "word"
        case .phrase: return "phrase"
        case .sentence: return "sentence"
        case .paragraph: return "paragraph"
        case .fullSnapshot: return "full"
        }
    }
}
