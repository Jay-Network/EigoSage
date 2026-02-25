import Foundation
import SwiftData

@MainActor
final class HistoryRepository {
    private var context: ModelContext?

    init() {}

    func configure(context: ModelContext) {
        self.context = context
    }

    // MARK: - Lookup History

    func recordLookup(word: String, scopeLevel: String, contextSnippet: String? = nil, aiProvider: String? = nil) {
        guard let context else { return }
        let entry = LookupHistoryEntry(word: word, scopeLevel: scopeLevel, contextSnippet: contextSnippet, aiProvider: aiProvider)
        context.insert(entry)
        try? context.save()
    }

    func recentHistory(limit: Int = 100) throws -> [LookupHistoryEntry] {
        guard let context else { return [] }
        var descriptor = FetchDescriptor<LookupHistoryEntry>(
            sortBy: [SortDescriptor(\.timestamp, order: .reverse)]
        )
        descriptor.fetchLimit = limit
        return try context.fetch(descriptor)
    }

    func clearHistory() throws {
        guard let context else { return }
        try context.delete(model: LookupHistoryEntry.self)
        try context.save()
    }

    // MARK: - Bookmarks

    func isBookmarked(word: String) throws -> Bool {
        guard let context else { return false }
        let predicate = #Predicate<BookmarkedWord> { $0.word == word }
        let descriptor = FetchDescriptor(predicate: predicate)
        return try !context.fetch(descriptor).isEmpty
    }

    func addBookmark(word: String, definition: String, contextSnippet: String? = nil) {
        guard let context else { return }
        let bookmark = BookmarkedWord(word: word, definition: definition, contextSnippet: contextSnippet)
        context.insert(bookmark)
        try? context.save()
    }

    func removeBookmark(word: String) throws {
        guard let context else { return }
        let predicate = #Predicate<BookmarkedWord> { $0.word == word }
        let results = try context.fetch(FetchDescriptor(predicate: predicate))
        for result in results {
            context.delete(result)
        }
        try context.save()
    }

    func allBookmarks() throws -> [BookmarkedWord] {
        guard let context else { return [] }
        let descriptor = FetchDescriptor<BookmarkedWord>(
            sortBy: [SortDescriptor(\.bookmarkedAt, order: .reverse)]
        )
        return try context.fetch(descriptor)
    }
}
