import Foundation
import SwiftData

@Model
final class LookupHistoryEntry {
    var id: UUID
    var word: String
    var scopeLevel: String
    var timestamp: Date
    var contextSnippet: String?
    var aiProvider: String?

    init(word: String, scopeLevel: String, contextSnippet: String? = nil, aiProvider: String? = nil) {
        self.id = UUID()
        self.word = word
        self.scopeLevel = scopeLevel
        self.timestamp = .now
        self.contextSnippet = contextSnippet
        self.aiProvider = aiProvider
    }
}

@Model
final class BookmarkedWord {
    var id: UUID
    var word: String
    var definition: String
    var contextSnippet: String?
    var bookmarkedAt: Date

    init(word: String, definition: String, contextSnippet: String? = nil) {
        self.id = UUID()
        self.word = word
        self.definition = definition
        self.contextSnippet = contextSnippet
        self.bookmarkedAt = .now
    }
}
