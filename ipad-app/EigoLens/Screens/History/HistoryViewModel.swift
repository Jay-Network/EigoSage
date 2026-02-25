import SwiftUI

@MainActor
final class HistoryViewModel: ObservableObject {
    @Published var recentHistory: [LookupHistoryEntry] = []
    @Published var bookmarks: [BookmarkedWord] = []

    private var historyRepo: HistoryRepository?

    func configure(container: AppContainer) {
        historyRepo = container.historyRepository
        loadData()
    }

    func loadData() {
        recentHistory = (try? historyRepo?.recentHistory()) ?? []
        bookmarks = (try? historyRepo?.allBookmarks()) ?? []
    }

    func clearHistory() {
        try? historyRepo?.clearHistory()
        recentHistory = []
    }

    func removeBookmark(word: String) {
        try? historyRepo?.removeBookmark(word: word)
        bookmarks.removeAll { $0.word == word }
    }
}
