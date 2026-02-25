import Foundation

struct Definition: Equatable {
    let word: String
    let lemma: String
    let frequency: Int?
    let meanings: [Meaning]
}

struct Meaning: Equatable {
    let partOfSpeech: PartOfSpeech
    let definition: String
    let examples: [String]
    let synonyms: [String]
    let antonyms: [String]
}

enum PartOfSpeech: String, Equatable, CaseIterable {
    case noun = "noun"
    case verb = "verb"
    case adjective = "adj"
    case adverb = "adv"
    case preposition = "prep"
    case conjunction = "conj"
    case interjection = "intj"
    case unknown = "unknown"

    var label: String { rawValue }

    static func fromString(_ pos: String?) -> PartOfSpeech {
        guard let pos else { return .unknown }
        switch pos.lowercased().trimmingCharacters(in: .whitespaces) {
        case "noun", "n": return .noun
        case "verb", "v": return .verb
        case "adjective", "adj", "a", "satellite", "s": return .adjective
        case "adverb", "adv", "r": return .adverb
        case "preposition", "prep": return .preposition
        case "conjunction", "conj": return .conjunction
        case "interjection", "intj": return .interjection
        default: return .unknown
        }
    }
}
