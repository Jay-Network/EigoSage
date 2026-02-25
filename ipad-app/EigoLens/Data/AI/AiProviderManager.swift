import Foundation

final class AiProviderManager {
    private var providers: [AiProviderProtocol] = []
    private(set) var activeProviderName: String?

    var isAiAvailable: Bool {
        providers.contains(where: \.isAvailable)
    }

    func configure(claudeKey: String, geminiKey: String, preferred: String?) {
        providers = [
            ClaudeClient(apiKey: claudeKey),
            GeminiClient(apiKey: geminiKey)
        ]
        activeProviderName = preferred ?? providers.first(where: \.isAvailable)?.name
    }

    func analyze(context: AnalysisContext) async throws -> AiResponse {
        guard let provider = activeProvider() else {
            throw AiError.notConfigured("any")
        }
        return try await provider.analyze(context: context)
    }

    func setActiveProvider(_ name: String) {
        activeProviderName = name
    }

    var availableProviders: [String] {
        providers.filter(\.isAvailable).map(\.name)
    }

    private func activeProvider() -> AiProviderProtocol? {
        if let name = activeProviderName,
           let provider = providers.first(where: { $0.name == name && $0.isAvailable }) {
            return provider
        }
        return providers.first(where: \.isAvailable)
    }
}
