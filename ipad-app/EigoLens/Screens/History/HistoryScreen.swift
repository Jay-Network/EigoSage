import SwiftUI

struct HistoryScreen: View {
    var onBack: () -> Void
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = HistoryViewModel()
    @State private var selectedTab = 0

    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $selectedTab) {
                Text("Recent (\(viewModel.recentHistory.count))").tag(0)
                Text("Saved (\(viewModel.bookmarks.count))").tag(1)
            }
            .pickerStyle(.segmented)
            .padding()

            if selectedTab == 0 {
                recentTab
            } else {
                bookmarksTab
            }
        }
        .navigationTitle("History")
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                }
            }
            if selectedTab == 0 && !viewModel.recentHistory.isEmpty {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Clear") {
                        viewModel.clearHistory()
                    }
                    .foregroundStyle(EigoLensTheme.error)
                }
            }
        }
        .task {
            viewModel.configure(container: container)
        }
    }

    // MARK: - Recent Tab

    private var recentTab: some View {
        Group {
            if viewModel.recentHistory.isEmpty {
                emptyState(icon: "clock", message: "No recent lookups")
            } else {
                List(viewModel.recentHistory, id: \.id) { entry in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(entry.word)
                                .font(EigoLensTheme.titleMedium)
                            if let snippet = entry.contextSnippet {
                                Text(snippet)
                                    .font(EigoLensTheme.bodySmall)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(1)
                            }
                        }

                        Spacer()

                        VStack(alignment: .trailing, spacing: 4) {
                            scopeBadgeSmall(entry.scopeLevel)
                            Text(entry.timestamp, style: .relative)
                                .font(EigoLensTheme.labelSmall)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                .listStyle(.plain)
            }
        }
    }

    // MARK: - Bookmarks Tab

    private var bookmarksTab: some View {
        Group {
            if viewModel.bookmarks.isEmpty {
                emptyState(icon: "bookmark", message: "No saved words")
            } else {
                List {
                    ForEach(viewModel.bookmarks, id: \.id) { bookmark in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(bookmark.word)
                                .font(EigoLensTheme.titleMedium)
                            Text(bookmark.definition)
                                .font(EigoLensTheme.bodySmall)
                                .foregroundStyle(.secondary)
                                .lineLimit(2)
                        }
                    }
                    .onDelete { indexSet in
                        for index in indexSet {
                            let word = viewModel.bookmarks[index].word
                            viewModel.removeBookmark(word: word)
                        }
                    }
                }
                .listStyle(.plain)
            }
        }
    }

    // MARK: - Helpers

    private func emptyState(icon: String, message: String) -> some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: icon)
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text(message)
                .font(EigoLensTheme.bodyMedium)
                .foregroundStyle(.secondary)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    private func scopeBadgeSmall(_ scope: String) -> some View {
        Text(scope)
            .font(EigoLensTheme.labelSmall)
            .foregroundStyle(.white)
            .padding(.horizontal, 8)
            .padding(.vertical, 2)
            .background(scopeColorForString(scope), in: Capsule())
    }

    private func scopeColorForString(_ scope: String) -> Color {
        switch scope {
        case "word": return EigoLensTheme.scopeWord
        case "phrase": return EigoLensTheme.scopePhrase
        case "sentence": return EigoLensTheme.scopeSentence
        case "paragraph": return EigoLensTheme.scopeParagraph
        case "full": return EigoLensTheme.scopeFullText
        default: return .secondary
        }
    }
}
