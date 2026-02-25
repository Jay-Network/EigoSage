import Foundation

final class ReadabilityCalculator {
    func calculate(text: String) -> ReadabilityMetrics? {
        let stats = analyzeText(text)
        guard stats.totalWords >= 2, stats.totalSentences >= 1 else { return nil }

        let fkGrade = fleschKincaidGrade(stats)
        let fre = fleschReadingEase(stats)
        let smog = smogIndex(stats)
        let cli = colemanLiauIndex(stats)

        let avgGrade = (fkGrade + smog + cli) / 3.0
        let difficulty = DifficultyLevel.fromGrade(avgGrade)

        return ReadabilityMetrics(
            fleschKincaidGrade: fkGrade,
            fleschReadingEase: fre,
            smogIndex: smog,
            colemanLiauIndex: cli,
            averageGrade: avgGrade,
            difficulty: difficulty,
            targetAudience: targetAudience(avgGrade),
            statistics: stats
        )
    }

    // Flesch-Kincaid Grade Level
    private func fleschKincaidGrade(_ stats: TextStatistics) -> Double {
        0.39 * stats.averageWordsPerSentence + 11.8 * stats.averageSyllablesPerWord - 15.59
    }

    // Flesch Reading Ease (higher = easier, 0-100)
    private func fleschReadingEase(_ stats: TextStatistics) -> Double {
        (206.835 - 1.015 * stats.averageWordsPerSentence - 84.6 * stats.averageSyllablesPerWord)
            .clamped(to: 0.0...100.0)
    }

    // SMOG Index
    private func smogIndex(_ stats: TextStatistics) -> Double {
        guard stats.totalSentences >= 1 else { return 0.0 }
        return 1.0430 * (Double(stats.polysyllableCount) * (30.0 / Double(stats.totalSentences))).squareRoot() + 3.1291
    }

    // Coleman-Liau Index (character-based)
    private func colemanLiauIndex(_ stats: TextStatistics) -> Double {
        let l = Double(stats.totalCharacters) / Double(stats.totalWords) * 100.0
        let s = Double(stats.totalSentences) / Double(stats.totalWords) * 100.0
        return 0.0588 * l - 0.296 * s - 15.8
    }

    func analyzeText(_ text: String) -> TextStatistics {
        let words = tokenizeWords(text)
        let sentences = tokenizeSentences(text)
        let wordCount = max(words.count, 1)
        let sentenceCount = max(sentences.count, 1)
        let syllables = words.reduce(0) { $0 + countSyllables($1) }
        let characters = words.reduce(0) { $0 + $1.count }
        let polysyllables = words.filter { countSyllables($0) >= 3 }.count

        return TextStatistics(
            totalWords: wordCount,
            totalSentences: sentenceCount,
            totalSyllables: syllables,
            totalCharacters: characters,
            averageWordsPerSentence: Double(wordCount) / Double(sentenceCount),
            averageSyllablesPerWord: Double(syllables) / Double(wordCount),
            polysyllableCount: polysyllables
        )
    }

    func countSyllables(_ word: String) -> Int {
        let cleaned = word.lowercased().trimmingCharacters(in: .whitespaces)
        guard !cleaned.isEmpty else { return 1 }

        let vowels: Set<Character> = ["a", "e", "i", "o", "u", "y"]
        var count = 0
        var prevVowel = false

        for c in cleaned {
            let isVowel = vowels.contains(c)
            if isVowel && !prevVowel {
                count += 1
            }
            prevVowel = isVowel
        }

        // Silent 'e' at end (but not "le")
        if cleaned.hasSuffix("e") && !cleaned.hasSuffix("le") && count > 1 {
            count -= 1
        }

        // "ed" is usually silent unless preceded by t or d
        if cleaned.hasSuffix("ed") && count > 1 {
            let beforeEd = cleaned.dropLast(2).last
            if let c = beforeEd, c != "t" && c != "d" {
                count -= 1
            }
        }

        return max(count, 1)
    }

    private func tokenizeWords(_ text: String) -> [String] {
        text.components(separatedBy: .whitespacesAndNewlines)
            .flatMap { $0.components(separatedBy: CharacterSet(charactersIn: "\u{2014}\u{2013}")) }
            .map { $0.replacingOccurrences(of: "[^a-zA-Z']", with: "", options: .regularExpression) }
            .filter { !$0.isEmpty && $0.contains(where: \.isLetter) }
    }

    private func tokenizeSentences(_ text: String) -> [String] {
        text.components(separatedBy: CharacterSet(charactersIn: ".!?"))
            .filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
    }

    private func targetAudience(_ grade: Double) -> String {
        switch grade {
        case ...1.0: return "Pre-K to 1st grade"
        case ...3.0: return "1st-3rd grade"
        case ...5.0: return "4th-5th grade"
        case ...6.0: return "6th grade"
        case ...8.0: return "7th-8th grade"
        case ...10.0: return "9th-10th grade"
        case ...12.0: return "11th-12th grade"
        case ...14.0: return "College"
        default: return "Graduate / Professional"
        }
    }
}
