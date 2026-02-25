import SwiftUI

struct AiAnalysisPanelView: View {
    enum ViewState {
        case loading(String, ScopeLevel, ReadabilityMetrics?)
        case loaded(String, ScopeLevel, AiResponse, ReadabilityMetrics?)
    }

    let viewState: ViewState
    var onDismiss: () -> Void

    @State private var selectedTab = 0

    private var scope: ScopeLevel {
        switch viewState {
        case .loading(_, let s, _), .loaded(_, let s, _, _): return s
        }
    }

    private var selectedText: String {
        switch viewState {
        case .loading(let t, _, _), .loaded(let t, _, _, _): return t
        }
    }

    private var readability: ReadabilityMetrics? {
        switch viewState {
        case .loading(_, _, let r), .loaded(_, _, _, let r): return r
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Header
            header

            // Tab picker
            Picker("", selection: $selectedTab) {
                Text("Summary").tag(0)
                Text("Full Text").tag(1)
                Text("Readability").tag(2)
                Text("Statistics").tag(3)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            // Tab content
            ScrollView {
                Group {
                    switch selectedTab {
                    case 0: summaryTab
                    case 1: fullTextTab
                    case 2: readabilityTab
                    case 3: statisticsTab
                    default: EmptyView()
                    }
                }
                .padding()
            }
        }
    }

    // MARK: - Header

    private var header: some View {
        HStack {
            ScopeBadge(scope: scope)

            if case .loaded(_, _, let response, _) = viewState {
                Text(response.provider)
                    .font(EigoLensTheme.labelSmall)
                    .foregroundStyle(.secondary)
                if let tokens = response.tokensUsed {
                    Text("\(tokens) tokens")
                        .font(EigoLensTheme.labelSmall)
                        .foregroundStyle(.secondary)
                }
                Text("\(response.processingTimeMs)ms")
                    .font(EigoLensTheme.labelSmall)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .foregroundStyle(.secondary)
                    .padding(8)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
    }

    // MARK: - Summary Tab

    @ViewBuilder
    private var summaryTab: some View {
        switch viewState {
        case .loading:
            VStack(spacing: 16) {
                ProgressView()
                    .scaleEffect(1.2)
                Text("Analyzing...")
                    .font(EigoLensTheme.bodyMedium)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, minHeight: 200)

        case .loaded(_, _, let response, _):
            let sections = parseAiSections(response.content)
            VStack(alignment: .leading, spacing: 12) {
                ForEach(sections, id: \.title) { section in
                    AiSectionCard(section: section)
                }
            }
        }
    }

    // MARK: - Full Text Tab

    private var fullTextTab: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Captured Text")
                .font(EigoLensTheme.titleMedium)
            Text(selectedText)
                .font(EigoLensTheme.bodyMedium)
                .textSelection(.enabled)
        }
    }

    // MARK: - Readability Tab

    @ViewBuilder
    private var readabilityTab: some View {
        if let metrics = readability {
            VStack(alignment: .leading, spacing: 16) {
                // Difficulty badge
                HStack {
                    Text(metrics.difficulty.rawValue)
                        .font(EigoLensTheme.titleMedium)
                        .foregroundStyle(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(difficultyColor(metrics.difficulty), in: Capsule())

                    Text("Target: \(metrics.targetAudience)")
                        .font(EigoLensTheme.bodySmall)
                        .foregroundStyle(.secondary)
                }

                // Score cards
                readabilityScore(name: "Flesch-Kincaid Grade", value: metrics.fleschKincaidGrade, format: "%.1f")
                readabilityScore(name: "Flesch Reading Ease", value: metrics.fleschReadingEase, format: "%.0f/100")
                readabilityScore(name: "SMOG Index", value: metrics.smogIndex, format: "%.1f")
                readabilityScore(name: "Coleman-Liau Index", value: metrics.colemanLiauIndex, format: "%.1f")
            }
        } else {
            Text("Not enough text for readability analysis")
                .font(EigoLensTheme.bodyMedium)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, minHeight: 100)
        }
    }

    // MARK: - Statistics Tab

    @ViewBuilder
    private var statisticsTab: some View {
        if let metrics = readability {
            let stats = metrics.statistics
            VStack(alignment: .leading, spacing: 8) {
                statRow(label: "Words", value: "\(stats.totalWords)")
                statRow(label: "Sentences", value: "\(stats.totalSentences)")
                statRow(label: "Syllables", value: "\(stats.totalSyllables)")
                statRow(label: "Characters", value: "\(stats.totalCharacters)")
                Divider()
                statRow(label: "Avg words/sentence", value: String(format: "%.1f", stats.averageWordsPerSentence))
                statRow(label: "Avg syllables/word", value: String(format: "%.2f", stats.averageSyllablesPerWord))
                statRow(label: "Polysyllabic words", value: "\(stats.polysyllableCount)")
            }
        } else {
            Text("Not enough text for statistics")
                .font(EigoLensTheme.bodyMedium)
                .foregroundStyle(.secondary)
        }
    }

    // MARK: - Helpers

    private func readabilityScore(name: String, value: Double, format: String) -> some View {
        HStack {
            Text(name)
                .font(EigoLensTheme.bodyMedium)
            Spacer()
            Text(String(format: format, value))
                .font(EigoLensTheme.titleMedium)
                .monospacedDigit()
        }
        .padding(12)
        .background(EigoLensTheme.surfaceVariant, in: RoundedRectangle(cornerRadius: EigoLensTheme.radiusS))
    }

    private func statRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(EigoLensTheme.bodyMedium)
                .foregroundStyle(.secondary)
            Spacer()
            Text(value)
                .font(EigoLensTheme.bodyLarge)
                .monospacedDigit()
        }
    }

    private func difficultyColor(_ level: DifficultyLevel) -> Color {
        switch level {
        case .veryEasy: return EigoLensTheme.success
        case .easy: return Color(hex: 0x66BB6A)
        case .moderate: return EigoLensTheme.warning
        case .difficult: return Color(hex: 0xEF6C00)
        case .veryDifficult: return EigoLensTheme.error
        }
    }
}

// MARK: - AI Section Parsing

struct AiSection: Equatable {
    let title: String
    let content: String
}

func parseAiSections(_ text: String) -> [AiSection] {
    var sections: [AiSection] = []
    var currentTitle = "Analysis"
    var currentContent: [String] = []

    for line in text.components(separatedBy: .newlines) {
        let trimmed = line.trimmingCharacters(in: .whitespaces)
        if trimmed.hasPrefix("###") {
            if !currentContent.isEmpty {
                sections.append(AiSection(title: currentTitle, content: currentContent.joined(separator: "\n")))
                currentContent = []
            }
            currentTitle = trimmed.replacingOccurrences(of: "###", with: "").trimmingCharacters(in: .whitespaces)
        } else if trimmed.hasPrefix("##") {
            if !currentContent.isEmpty {
                sections.append(AiSection(title: currentTitle, content: currentContent.joined(separator: "\n")))
                currentContent = []
            }
            currentTitle = trimmed.replacingOccurrences(of: "##", with: "").trimmingCharacters(in: .whitespaces)
        } else if trimmed.hasPrefix("#") {
            if !currentContent.isEmpty {
                sections.append(AiSection(title: currentTitle, content: currentContent.joined(separator: "\n")))
                currentContent = []
            }
            currentTitle = trimmed.replacingOccurrences(of: "#", with: "").trimmingCharacters(in: .whitespaces)
        } else {
            currentContent.append(line)
        }
    }

    if !currentContent.isEmpty {
        sections.append(AiSection(title: currentTitle, content: currentContent.joined(separator: "\n")))
    }

    return sections
}

// MARK: - Section Card

struct AiSectionCard: View {
    let section: AiSection

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(section.title)
                .font(EigoLensTheme.titleMedium)
                .foregroundStyle(EigoLensTheme.primary)

            StyledMarkdownText(text: section.content)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(EigoLensTheme.surfaceVariant, in: RoundedRectangle(cornerRadius: EigoLensTheme.radiusM))
    }
}

// MARK: - Scope Badge

struct ScopeBadge: View {
    let scope: ScopeLevel

    var body: some View {
        Text(scope.displayName)
            .font(EigoLensTheme.labelMedium)
            .foregroundStyle(.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 4)
            .background(scopeColor, in: Capsule())
    }

    private var scopeColor: Color {
        switch scope {
        case .word: return EigoLensTheme.scopeWord
        case .phrase: return EigoLensTheme.scopePhrase
        case .sentence: return EigoLensTheme.scopeSentence
        case .paragraph: return EigoLensTheme.scopeParagraph
        case .fullSnapshot: return EigoLensTheme.scopeFullText
        }
    }
}
