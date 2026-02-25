import Foundation

protocol AiProviderProtocol {
    var name: String { get }
    var isAvailable: Bool { get }
    func analyze(context: AnalysisContext) async throws -> AiResponse
}

struct AnalysisContext: Equatable {
    let selectedText: String
    let fullSnapshotText: String
    let scopeLevel: ScopeLevel
}

struct AiResponse: Equatable {
    let content: String
    let provider: String
    let model: String
    let tokensUsed: Int?
    let processingTimeMs: Int
}

struct ContextualInsight: Equatable {
    let meaning: String
    let partOfSpeech: String
    let note: String?
}

enum AiError: Error, LocalizedError {
    case notConfigured(String)
    case apiError(String, Int)
    case emptyResponse
    case parseError(String)

    var errorDescription: String? {
        switch self {
        case .notConfigured(let provider): return "\(provider) is not configured. Add an API key in Settings."
        case .apiError(let provider, let code): return "\(provider) returned error \(code)"
        case .emptyResponse: return "Empty response from AI provider"
        case .parseError(let msg): return "Parse error: \(msg)"
        }
    }
}
