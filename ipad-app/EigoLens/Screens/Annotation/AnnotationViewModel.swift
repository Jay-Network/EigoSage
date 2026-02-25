import SwiftUI
import Combine

@MainActor
final class AnnotationViewModel: ObservableObject {
    // MARK: - Published State

    @Published var currentImage: CapturedImage = CapturedImage(image: UIImage(), ocrResult: .empty)
    @Published var panelState: PanelState = .idle
    @Published var interactionMode: InteractionMode = .tap
    @Published var tappedWord: TapResult?
    @Published var isCorrectingOcr = false
    @Published var isCurrentWordBookmarked = false

    // Panel size hints for FAB positioning
    var panelWidthForFab: CGFloat = 0
    var panelHeightForFab: CGFloat = 0

    // MARK: - Dependencies

    private var definitionRepository: DefinitionRepository?
    private var aiProviderManager: AiProviderManager?
    private var geminiOcrCorrector: GeminiOcrCorrector?
    private var historyRepository: HistoryRepository?
    private var readabilityCalculator: ReadabilityCalculator?

    private var currentWord: String?

    // MARK: - Configuration

    func configure(capturedImage: CapturedImage, container: AppContainer) {
        self.currentImage = capturedImage
        self.definitionRepository = container.definitionRepository
        self.aiProviderManager = container.aiProviderManager
        self.geminiOcrCorrector = container.geminiOcrCorrector
        self.historyRepository = container.historyRepository
        self.readabilityCalculator = container.readabilityCalculator

        // Run Gemini OCR correction in background
        if let corrector = geminiOcrCorrector, corrector.isAvailable, !capturedImage.ocrResult.lines.isEmpty {
            correctOcrWithGemini(image: capturedImage.image, mlKitResult: capturedImage.ocrResult)
        }
    }

    // MARK: - Interaction Handlers

    func onWordTapped(_ tapResult: TapResult) {
        tappedWord = tapResult
        let cleanWord = tapResult.word.trimmingPunctuation
        guard !cleanWord.isEmpty else { return }

        // Local WordNet lookup (fast)
        lookupWord(cleanWord)

        // Parallel: contextual insight from Gemini
        if let corrector = geminiOcrCorrector, corrector.isAvailable {
            fetchContextualInsight(word: cleanWord)
        }
    }

    func onWordLongPressed(_ tapResult: TapResult) {
        tappedWord = tapResult
        let cleanWord = tapResult.word.trimmingPunctuation
        guard !cleanWord.isEmpty else { return }

        let fullText = currentImage.ocrResult.fullText
        guard !fullText.isBlank else { return }

        let sentence = extractSentence(from: fullText, containing: cleanWord)

        if let aiManager = aiProviderManager, aiManager.isAiAvailable, sentence.split(separator: " ").count > 1 {
            analyzeWithAi(selectedText: sentence, scopeLevel: .sentence(sentence))
        } else {
            lookupWord(cleanWord)
        }
    }

    func selectWords(_ words: [String]) {
        guard !words.isEmpty else { return }
        tappedWord = nil

        if words.count == 1 {
            lookupWord(words[0].trimmingPunctuation)
        } else {
            let selectedText = words.joined(separator: " ")
            let scopeLevel: ScopeLevel = words.count <= 8
                ? .phrase(text: selectedText, words: words)
                : .paragraph(selectedText)

            if let aiManager = aiProviderManager, aiManager.isAiAvailable {
                analyzeWithAi(selectedText: selectedText, scopeLevel: scopeLevel)
            } else {
                lookupWord(selectedText, fallbackWord: words[0].trimmingPunctuation)
            }
        }
    }

    func analyzeFullText() {
        let fullText = currentImage.ocrResult.fullText
        guard !fullText.isBlank else { return }
        analyzeWithAi(selectedText: fullText, scopeLevel: .fullSnapshot)
    }

    func toggleInteractionMode() {
        interactionMode = interactionMode == .tap ? .select : .tap
    }

    func dismissPanel() {
        panelState = .idle
        currentWord = nil
    }

    func toggleBookmark() {
        guard case .wordDefinition(let def, _) = panelState else { return }

        let word = def.word
        let definition = def.meanings.first?.definition ?? ""
        let snippet = getContextSnippet()

        Task {
            guard let repo = historyRepository else { return }
            if isCurrentWordBookmarked {
                try? repo.removeBookmark(word: word)
                isCurrentWordBookmarked = false
            } else {
                repo.addBookmark(word: word, definition: definition, contextSnippet: snippet)
                isCurrentWordBookmarked = true
            }
        }
    }

    // MARK: - Private Methods

    private func lookupWord(_ query: String, fallbackWord: String? = nil) {
        Task {
            panelState = .loading

            guard let repo = definitionRepository else {
                panelState = .error(message: "Definition service not available")
                return
            }

            do {
                let definition = try await repo.getDefinition(for: query)
                panelState = .wordDefinition(definition: definition)
                currentWord = definition.word
                updateBookmarkStatus(word: definition.word)
                historyRepository?.recordLookup(word: definition.word, scopeLevel: "word", contextSnippet: getContextSnippet())
            } catch is DefinitionError {
                if let fallback = fallbackWord {
                    do {
                        let definition = try await repo.getDefinition(for: fallback)
                        panelState = .wordDefinition(definition: definition)
                        currentWord = definition.word
                        updateBookmarkStatus(word: definition.word)
                        historyRepository?.recordLookup(word: definition.word, scopeLevel: "word", contextSnippet: getContextSnippet())
                    } catch {
                        panelState = .notFound(word: fallback)
                    }
                } else {
                    panelState = .notFound(word: query)
                }
            } catch {
                panelState = .error(message: error.localizedDescription)
            }
        }
    }

    private func analyzeWithAi(selectedText: String, scopeLevel: ScopeLevel) {
        let fullText = currentImage.ocrResult.fullText

        let metrics = readabilityCalculator?.calculate(text: selectedText)
        panelState = .aiLoading(scopeLevel: scopeLevel, selectedText: selectedText, readability: metrics)

        Task {
            guard let aiManager = aiProviderManager else {
                panelState = .error(message: "AI not configured")
                return
            }

            let context = AnalysisContext(
                selectedText: selectedText,
                fullSnapshotText: fullText,
                scopeLevel: scopeLevel
            )

            do {
                let response = try await aiManager.analyze(context: context)
                panelState = .aiAnalysis(scopeLevel: scopeLevel, selectedText: selectedText, response: response, readability: metrics)
                historyRepository?.recordLookup(
                    word: String(selectedText.prefix(100)),
                    scopeLevel: scopeLevel.scopeName,
                    contextSnippet: nil,
                    aiProvider: response.provider
                )
            } catch {
                panelState = .error(message: error.localizedDescription)
            }
        }
    }

    private func fetchContextualInsight(word: String) {
        let fullText = currentImage.ocrResult.fullText
        guard !fullText.isBlank else { return }

        Task {
            guard let corrector = geminiOcrCorrector else { return }
            if case .success(let insight) = await corrector.getContextualInsight(word: word, surroundingText: fullText) {
                // Only update if still showing this word's definition
                if case .wordDefinition(let def, _) = panelState,
                   def.word.lowercased() == word.lowercased() {
                    panelState = .wordDefinition(definition: def, contextualInsight: insight)
                }
            }
        }
    }

    private func correctOcrWithGemini(image: UIImage, mlKitResult: OCRResult) {
        isCorrectingOcr = true
        Task {
            defer { isCorrectingOcr = false }
            guard let corrector = geminiOcrCorrector else { return }

            if case .success(let geminiLines) = await corrector.extractText(from: image) {
                let corrected = OcrTextMerger.merge(visionResult: mlKitResult, geminiLines: geminiLines)
                currentImage = CapturedImage(
                    id: currentImage.id,
                    image: image,
                    ocrResult: corrected,
                    timestamp: currentImage.timestamp
                )
            }
        }
    }

    private func extractSentence(from fullText: String, containing word: String) -> String {
        // Split on sentence-ending punctuation
        let sentences = fullText.split(omittingEmptySubsequences: true) { ".!?".contains($0) }
        if let match = sentences.first(where: { $0.localizedCaseInsensitiveContains(word) }) {
            return String(match).trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return String(fullText.prefix(200))
    }

    private func getContextSnippet() -> String? {
        let fullText = currentImage.ocrResult.fullText
        return fullText.isBlank ? nil : String(fullText.prefix(200))
    }

    private func updateBookmarkStatus(word: String) {
        Task {
            guard let repo = historyRepository else { return }
            isCurrentWordBookmarked = (try? repo.isBookmarked(word: word)) ?? false
        }
    }
}
