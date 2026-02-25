import SwiftUI

@MainActor
final class SettingsViewModel: ObservableObject {
    @Published var claudeApiKey = ""
    @Published var geminiApiKey = ""
    @Published var activeProvider: String? = nil

    private var keychainStore: KeychainStore?
    private var aiProviderManager: AiProviderManager?

    func configure(container: AppContainer) {
        keychainStore = container.keychainStore
        aiProviderManager = container.aiProviderManager

        claudeApiKey = keychainStore?.load(key: KeychainStore.Keys.claudeApiKey) ?? ""
        geminiApiKey = keychainStore?.load(key: KeychainStore.Keys.geminiApiKey) ?? ""
        activeProvider = UserDefaults.standard.string(forKey: "active_provider") ?? aiProviderManager?.activeProviderName
    }

    func saveClaudeKey() {
        try? keychainStore?.save(key: KeychainStore.Keys.claudeApiKey, value: claudeApiKey)
        reconfigureAi()
    }

    func saveGeminiKey() {
        try? keychainStore?.save(key: KeychainStore.Keys.geminiApiKey, value: geminiApiKey)
        reconfigureAi()
    }

    func setActiveProvider(_ provider: String?) {
        activeProvider = provider
        UserDefaults.standard.set(provider, forKey: "active_provider")
        reconfigureAi()
    }

    private func reconfigureAi() {
        aiProviderManager?.configure(
            claudeKey: claudeApiKey,
            geminiKey: geminiApiKey,
            preferred: activeProvider
        )
    }
}
