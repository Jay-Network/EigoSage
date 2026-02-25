import Foundation

struct ReadabilityMetrics: Equatable {
    let fleschKincaidGrade: Double
    let fleschReadingEase: Double
    let smogIndex: Double
    let colemanLiauIndex: Double
    let averageGrade: Double
    let difficulty: DifficultyLevel
    let targetAudience: String
    let statistics: TextStatistics
}

struct TextStatistics: Equatable {
    let totalWords: Int
    let totalSentences: Int
    let totalSyllables: Int
    let totalCharacters: Int
    let averageWordsPerSentence: Double
    let averageSyllablesPerWord: Double
    let polysyllableCount: Int
}

enum DifficultyLevel: String, Equatable {
    case veryEasy = "Very Easy"
    case easy = "Easy"
    case moderate = "Moderate"
    case difficult = "Difficult"
    case veryDifficult = "Very Difficult"

    static func fromGrade(_ grade: Double) -> DifficultyLevel {
        switch grade {
        case ...5.0: return .veryEasy
        case ...7.0: return .easy
        case ...10.0: return .moderate
        case ...13.0: return .difficult
        default: return .veryDifficult
        }
    }
}
