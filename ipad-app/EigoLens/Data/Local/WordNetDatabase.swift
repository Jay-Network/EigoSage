import Foundation
import SQLite3

struct WordRow {
    let wordId: Int
    let word: String
    let lemma: String
    let frequency: Int?
}

struct DefinitionRow {
    let pos: String?
    let meaning: String
    let example: String?
    let synonyms: String?
    let antonyms: String?
}

enum WordNetError: Error, LocalizedError {
    case databaseNotFound
    case openFailed(String)
    case queryFailed(String)

    var errorDescription: String? {
        switch self {
        case .databaseNotFound: return "WordNet database not found in bundle"
        case .openFailed(let msg): return "Failed to open WordNet: \(msg)"
        case .queryFailed(let msg): return "Query failed: \(msg)"
        }
    }
}

final class WordNetDatabase {
    private var db: OpaquePointer?

    static func openBundled() throws -> WordNetDatabase {
        let dest = stagedURL()

        if !FileManager.default.fileExists(atPath: dest.path) {
            guard let src = Bundle.main.url(
                forResource: Configuration.wordNetDbName,
                withExtension: Configuration.wordNetDbExtension
            ) else {
                throw WordNetError.databaseNotFound
            }
            try FileManager.default.createDirectory(
                at: dest.deletingLastPathComponent(),
                withIntermediateDirectories: true
            )
            try FileManager.default.copyItem(at: src, to: dest)
        }

        return try WordNetDatabase(url: dest)
    }

    private static func stagedURL() -> URL {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return docs.appendingPathComponent("\(Configuration.wordNetDbName).\(Configuration.wordNetDbExtension)")
    }

    private init(url: URL) throws {
        let flags = SQLITE_OPEN_READONLY | SQLITE_OPEN_NOMUTEX
        let status = sqlite3_open_v2(url.path, &db, flags, nil)
        guard status == SQLITE_OK else {
            let msg = db.flatMap { String(cString: sqlite3_errmsg($0)) } ?? "unknown"
            throw WordNetError.openFailed(msg)
        }
    }

    deinit {
        sqlite3_close(db)
    }

    // MARK: - Queries

    func getWord(_ word: String) -> WordRow? {
        let sql = "SELECT word_id, word, lemma, frequency FROM words WHERE word = ? LIMIT 1"
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }

        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return nil }
        sqlite3_bind_text(stmt, 1, (word as NSString).utf8String, -1, nil)

        guard sqlite3_step(stmt) == SQLITE_ROW else { return nil }

        return WordRow(
            wordId: Int(sqlite3_column_int(stmt, 0)),
            word: String(cString: sqlite3_column_text(stmt, 1)),
            lemma: String(cString: sqlite3_column_text(stmt, 2)),
            frequency: sqlite3_column_type(stmt, 3) == SQLITE_NULL ? nil : Int(sqlite3_column_int(stmt, 3))
        )
    }

    func lookupWithDefinitions(_ word: String) -> (WordRow, [DefinitionRow])? {
        guard let wordRow = getWord(word) else { return nil }

        let sql = "SELECT pos, meaning, example, synonyms, antonyms FROM definitions WHERE word_id = ? ORDER BY def_id"
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }

        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return nil }
        sqlite3_bind_int(stmt, 1, Int32(wordRow.wordId))

        var defs: [DefinitionRow] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            let pos = sqlite3_column_type(stmt, 0) == SQLITE_NULL ? nil : String(cString: sqlite3_column_text(stmt, 0))
            let meaning = String(cString: sqlite3_column_text(stmt, 1))
            let example = sqlite3_column_type(stmt, 2) == SQLITE_NULL ? nil : String(cString: sqlite3_column_text(stmt, 2))
            let synonyms = sqlite3_column_type(stmt, 3) == SQLITE_NULL ? nil : String(cString: sqlite3_column_text(stmt, 3))
            let antonyms = sqlite3_column_type(stmt, 4) == SQLITE_NULL ? nil : String(cString: sqlite3_column_text(stmt, 4))

            defs.append(DefinitionRow(pos: pos, meaning: meaning, example: example, synonyms: synonyms, antonyms: antonyms))
        }

        return (wordRow, defs)
    }

    func getTopFrequent(limit: Int) -> [WordRow] {
        let sql = "SELECT word_id, word, lemma, frequency FROM words WHERE frequency IS NOT NULL ORDER BY frequency DESC LIMIT ?"
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }

        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return [] }
        sqlite3_bind_int(stmt, 1, Int32(limit))

        var rows: [WordRow] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            rows.append(WordRow(
                wordId: Int(sqlite3_column_int(stmt, 0)),
                word: String(cString: sqlite3_column_text(stmt, 1)),
                lemma: String(cString: sqlite3_column_text(stmt, 2)),
                frequency: sqlite3_column_type(stmt, 3) == SQLITE_NULL ? nil : Int(sqlite3_column_int(stmt, 3))
            ))
        }
        return rows
    }
}
