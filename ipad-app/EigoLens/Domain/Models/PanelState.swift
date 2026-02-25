import Foundation

enum PanelState: Equatable {
    case idle
    case loading
    case wordDefinition(definition: Definition, contextualInsight: ContextualInsight? = nil)
    case aiLoading(scopeLevel: ScopeLevel, selectedText: String, readability: ReadabilityMetrics? = nil)
    case aiAnalysis(scopeLevel: ScopeLevel, selectedText: String, response: AiResponse, readability: ReadabilityMetrics? = nil)
    case notFound(word: String)
    case error(message: String)

    var isVisible: Bool {
        self != .idle
    }
}

enum InteractionMode: Equatable {
    case tap
    case select
}
