import Foundation

enum DefinitionError: Error, LocalizedError {
    case notFound(String)
    case databaseError(String)

    var errorDescription: String? {
        switch self {
        case .notFound(let word): return "No definition found for '\(word)'"
        case .databaseError(let msg): return "Database error: \(msg)"
        }
    }
}

final class DefinitionRepository {
    private let db: WordNetDatabase
    private let lemmatizer: EnglishLemmatizer
    private var cache: [String: Definition] = [:]
    private let cacheLimit: Int

    init(db: WordNetDatabase, lemmatizer: EnglishLemmatizer, cacheLimit: Int = Configuration.definitionCacheSize) {
        self.db = db
        self.lemmatizer = lemmatizer
        self.cacheLimit = cacheLimit
    }

    func getDefinition(for word: String) async throws -> Definition {
        let key = word.lowercased().trimmingCharacters(in: .whitespaces)

        if let cached = cache[key] {
            return cached
        }

        // Direct lookup
        if let result = db.lookupWithDefinitions(key) {
            let def = toDomain(wordRow: result.0, defRows: result.1)
            evictIfNeeded()
            cache[key] = def
            return def
        }

        // Lemmatize and retry
        let lemma = lemmatizer.lemmatize(key)
        if lemma != key, let result = db.lookupWithDefinitions(lemma) {
            let def = toDomain(wordRow: result.0, defRows: result.1)
            cache[key] = def
            return def
        }

        throw DefinitionError.notFound(word)
    }

    func preloadCommonWords(count: Int) async {
        let words = db.getTopFrequent(limit: count)
        for wordRow in words {
            let key = wordRow.word.lowercased()
            guard cache[key] == nil else { continue }
            if let result = db.lookupWithDefinitions(wordRow.word) {
                cache[key] = toDomain(wordRow: result.0, defRows: result.1)
            }
        }
    }

    // MARK: - Mapping

    private func toDomain(wordRow: WordRow, defRows: [DefinitionRow]) -> Definition {
        let meanings = defRows.map { row in
            Meaning(
                partOfSpeech: PartOfSpeech.fromString(row.pos),
                definition: row.meaning,
                examples: row.example.map { $0.components(separatedBy: "; ").filter { !$0.isEmpty } } ?? [],
                synonyms: row.synonyms.map { $0.components(separatedBy: ", ").filter { !$0.isEmpty } } ?? [],
                antonyms: row.antonyms.map { $0.components(separatedBy: ", ").filter { !$0.isEmpty } } ?? []
            )
        }

        return Definition(
            word: wordRow.word,
            lemma: wordRow.lemma,
            frequency: wordRow.frequency,
            meanings: meanings
        )
    }

    private func evictIfNeeded() {
        if cache.count >= cacheLimit {
            // Remove ~25% of entries (simple eviction)
            let keysToRemove = Array(cache.keys.prefix(cacheLimit / 4))
            for key in keysToRemove {
                cache.removeValue(forKey: key)
            }
        }
    }
}
