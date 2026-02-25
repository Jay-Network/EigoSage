import SwiftUI

struct PanelContentView: View {
    let state: PanelState
    var onDismiss: () -> Void
    var onToggleBookmark: () -> Void
    var isBookmarked: Bool

    var body: some View {
        switch state {
        case .idle:
            EmptyView()
        case .loading:
            DefinitionSkeletonView()
        case .wordDefinition(let definition, let insight):
            DefinitionPanelView(
                definition: definition,
                insight: insight,
                isBookmarked: isBookmarked,
                onDismiss: onDismiss,
                onToggleBookmark: onToggleBookmark
            )
        case .aiLoading(let scope, let text, let readability):
            AiAnalysisPanelView(
                viewState: .loading(text, scope, readability),
                onDismiss: onDismiss
            )
        case .aiAnalysis(let scope, let text, let response, let readability):
            AiAnalysisPanelView(
                viewState: .loaded(text, scope, response, readability),
                onDismiss: onDismiss
            )
        case .notFound(let word):
            NotFoundPanelView(word: word, onDismiss: onDismiss)
        case .error(let message):
            ErrorPanelView(message: message, onDismiss: onDismiss)
        }
    }
}

// MARK: - Not Found Panel

struct NotFoundPanelView: View {
    let word: String
    var onDismiss: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            HStack {
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .foregroundStyle(.secondary)
                        .padding(8)
                }
            }

            Image(systemName: "book.closed")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)

            Text("Not Found")
                .font(EigoLensTheme.titleLarge)

            Text("No definition found for \"\(word)\"")
                .font(EigoLensTheme.bodyMedium)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            Spacer()
        }
        .padding()
    }
}

// MARK: - Error Panel

struct ErrorPanelView: View {
    let message: String
    var onDismiss: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            HStack {
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .foregroundStyle(.secondary)
                        .padding(8)
                }
            }

            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 48))
                .foregroundStyle(EigoLensTheme.error)

            Text("Error")
                .font(EigoLensTheme.titleLarge)

            Text(message)
                .font(EigoLensTheme.bodyMedium)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            Spacer()
        }
        .padding()
    }
}
