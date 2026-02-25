import Foundation
import SwiftUI

@MainActor
final class AppContainer: ObservableObject {
    @Published var initError: String?

    // Transient capture storage (avoids Hashable requirement on CapturedImage)
    private var pendingCaptures: [UUID: CapturedImage] = [:]

    // -- Phase 2: OCR --
    private(set) var ocrService: OCRService!

    // -- Phase 4: Definition --
    private(set) var wordNetDatabase: WordNetDatabase!
    private(set) var lemmatizer: EnglishLemmatizer!
    private(set) var definitionRepository: DefinitionRepository!

    // -- Phase 5: AI --
    private(set) var aiProviderManager: AiProviderManager!
    private(set) var geminiOcrCorrector: GeminiOcrCorrector!
    private(set) var readabilityCalculator: ReadabilityCalculator!

    // -- Phase 6: History + Settings --
    private(set) var historyRepository: HistoryRepository!
    private(set) var keychainStore: KeychainStore!

    init() {
        do {
            try initializeServices()
        } catch {
            initError = "Failed to initialize: \(error.localizedDescription)"
        }
    }

    private func initializeServices() throws {
        // Phase 2
        ocrService = OCRService()

        // Phase 4: WordNet + Definitions
        wordNetDatabase = try WordNetDatabase.openBundled()
        lemmatizer = EnglishLemmatizer(db: wordNetDatabase)
        definitionRepository = DefinitionRepository(db: wordNetDatabase, lemmatizer: lemmatizer)

        // Phase 5: AI
        keychainStore = KeychainStore()
        readabilityCalculator = ReadabilityCalculator()

        let claudeKey = keychainStore.load(key: KeychainStore.Keys.claudeApiKey) ?? ""
        let geminiKey = keychainStore.load(key: KeychainStore.Keys.geminiApiKey) ?? ""
        let preferredProvider = UserDefaults.standard.string(forKey: "active_provider")

        aiProviderManager = AiProviderManager()
        aiProviderManager.configure(claudeKey: claudeKey, geminiKey: geminiKey, preferred: preferredProvider)
        geminiOcrCorrector = GeminiOcrCorrector(apiKey: geminiKey)

        // Phase 6: History
        historyRepository = HistoryRepository()

        // Preload common words
        Task {
            await definitionRepository.preloadCommonWords(count: Configuration.preloadWordCount)
        }
    }

    // MARK: - Pending Captures

    func setPendingCapture(_ image: CapturedImage) -> UUID {
        let id = UUID()
        pendingCaptures[id] = image
        return id
    }

    func getCapturedImage(id: UUID) -> CapturedImage? {
        pendingCaptures[id]
    }

    func clearPendingCapture(id: UUID) {
        pendingCaptures.removeValue(forKey: id)
    }
}
