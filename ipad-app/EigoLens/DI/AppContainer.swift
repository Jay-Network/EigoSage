import Foundation
import SwiftUI
import Combine

@MainActor
final class AppContainer: ObservableObject {
    @Published var initError: String?
    private var authCancellable: AnyCancellable?

    // Transient capture storage (avoids Hashable requirement on CapturedImage)
    private var pendingCaptures: [UUID: CapturedImage] = [:]

    // -- Phase 2: OCR --
    private(set) var ocrService: OCRService!

    // -- Phase 4: Definition + Enrichment --
    private(set) var wordNetDatabase: WordNetDatabase?
    private(set) var lemmatizer: EnglishLemmatizer?
    private(set) var definitionRepository: DefinitionRepository?
    private(set) var wordEnrichmentRepository: WordEnrichmentRepository?

    // -- Phase 5: AI --
    private(set) var aiProviderManager: AiProviderManager!
    private(set) var geminiOcrCorrector: GeminiOcrCorrector!
    private(set) var readabilityCalculator: ReadabilityCalculator!

    // -- Phase 6: History + Settings --
    private(set) var historyRepository: HistoryRepository!
    private(set) var keychainStore: KeychainStore!

    // -- Feedback --
    private(set) var feedbackRepository: FeedbackRepository!

    // -- Auth --
    private(set) var authManager: AuthManager!

    init() {
        initializeServices()
    }

    private func initializeServices() {
        // Auth (before other services so it starts session restoration early)
        keychainStore = KeychainStore()
        let supabaseClient = SupabaseAuthClient(
            baseURL: Configuration.builtInSupabaseUrl,
            anonKey: Configuration.builtInSupabaseAnonKey
        )
        authManager = AuthManager(client: supabaseClient, keychain: keychainStore)
        authCancellable = authManager.objectWillChange.sink { [weak self] _ in
            self?.objectWillChange.send()
        }

        // Phase 2
        ocrService = OCRService()

        // Phase 4: WordNet + Definitions
        do {
            let db = try WordNetDatabase.openBundled()
            wordNetDatabase = db
            let lem = EnglishLemmatizer(db: db)
            lemmatizer = lem
            definitionRepository = DefinitionRepository(db: db, lemmatizer: lem)
            wordEnrichmentRepository = WordEnrichmentRepository(db: db, lemmatizer: lem)
        } catch {
            print("WordNet init failed (definitions unavailable): \(error.localizedDescription)")
        }

        // Phase 5: AI
        readabilityCalculator = ReadabilityCalculator()

        let claudeKey = keychainStore.load(key: KeychainStore.Keys.claudeApiKey)
            ?? Configuration.builtInClaudeApiKey
        let geminiKey = keychainStore.load(key: KeychainStore.Keys.geminiApiKey)
            ?? Configuration.builtInGeminiApiKey
        let preferredProvider = UserDefaults.standard.string(forKey: "active_provider")

        aiProviderManager = AiProviderManager()
        aiProviderManager.configure(claudeKey: claudeKey, geminiKey: geminiKey, preferred: preferredProvider)
        geminiOcrCorrector = GeminiOcrCorrector(apiKey: geminiKey)

        // Phase 6: History
        historyRepository = HistoryRepository()

        // Feedback
        feedbackRepository = FeedbackRepository(
            baseURL: Configuration.builtInSupabaseUrl,
            anonKey: Configuration.builtInSupabaseAnonKey
        )

        // Preload common words
        if let repo = definitionRepository {
            Task {
                await repo.preloadCommonWords(count: Configuration.preloadWordCount)
            }
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
