import SwiftUI

struct DefinitionPanelView: View {
    let definition: Definition
    let insight: ContextualInsight?
    let isBookmarked: Bool
    var onDismiss: () -> Void
    var onToggleBookmark: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                // Header
                headerView

                // Contextual insight card (from AI)
                if let insight {
                    insightCard(insight)
                }

                // POS chips
                posChips

                // Definition cards
                ForEach(Array(definition.meanings.enumerated()), id: \.offset) { index, meaning in
                    definitionCard(meaning, index: index + 1)
                }
            }
            .padding()
        }
    }

    // MARK: - Header

    private var headerView: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                Text(definition.word)
                    .font(.system(.title, design: .serif, weight: .bold))

                if definition.lemma != definition.word {
                    Text("from: \(definition.lemma)")
                        .font(EigoLensTheme.labelMedium)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()

            if let freq = definition.frequency {
                frequencyBadge(freq)
            }

            Button(action: onToggleBookmark) {
                Image(systemName: isBookmarked ? "bookmark.fill" : "bookmark")
                    .font(.title3)
                    .foregroundStyle(isBookmarked ? EigoLensTheme.warning : .secondary)
            }

            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .foregroundStyle(.secondary)
                    .padding(8)
            }
        }
    }

    // MARK: - POS Chips

    private var posChips: some View {
        let uniquePOS = Array(Set(definition.meanings.map(\.partOfSpeech)))
        return ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(uniquePOS, id: \.self) { pos in
                    Text(pos.label)
                        .font(EigoLensTheme.labelMedium)
                        .foregroundStyle(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(posColor(pos), in: Capsule())
                }
            }
        }
    }

    // MARK: - Definition Card

    private func definitionCard(_ meaning: Meaning, index: Int) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline) {
                Text("\(index).")
                    .font(EigoLensTheme.titleMedium)
                    .foregroundStyle(.secondary)
                Text(meaning.definition)
                    .font(EigoLensTheme.bodyLarge)
            }

            // Examples
            ForEach(meaning.examples, id: \.self) { example in
                Text("  \"\(example)\"")
                    .font(EigoLensTheme.bodyMedium)
                    .italic()
                    .foregroundStyle(.secondary)
            }

            // Synonyms
            if !meaning.synonyms.isEmpty {
                chipRow(label: "Synonyms", items: meaning.synonyms, color: EigoLensTheme.secondary)
            }

            // Antonyms
            if !meaning.antonyms.isEmpty {
                chipRow(label: "Antonyms", items: meaning.antonyms, color: EigoLensTheme.tertiary)
            }
        }
        .padding(12)
        .background(EigoLensTheme.surfaceVariant, in: RoundedRectangle(cornerRadius: EigoLensTheme.radiusM))
    }

    // MARK: - Insight Card

    private func insightCard(_ insight: ContextualInsight) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                Image(systemName: "brain")
                    .font(.caption)
                    .foregroundStyle(EigoLensTheme.primary)
                Text("In this context")
                    .font(EigoLensTheme.labelMedium)
                    .foregroundStyle(EigoLensTheme.primary)
            }

            Text(insight.meaning)
                .font(EigoLensTheme.bodyMedium)

            if !insight.partOfSpeech.isEmpty {
                Text(insight.partOfSpeech)
                    .font(EigoLensTheme.labelSmall)
                    .foregroundStyle(.secondary)
            }

            if let note = insight.note {
                Text(note)
                    .font(EigoLensTheme.bodySmall)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(EigoLensTheme.primary.opacity(0.08), in: RoundedRectangle(cornerRadius: EigoLensTheme.radiusM))
    }

    // MARK: - Helpers

    private func frequencyBadge(_ freq: Int) -> some View {
        let label: String
        let color: Color
        switch freq {
        case 5000...: label = "Very Common"; color = EigoLensTheme.success
        case 1000...: label = "Common"; color = EigoLensTheme.secondary
        case 100...: label = "Uncommon"; color = EigoLensTheme.warning
        default: label = "Rare"; color = EigoLensTheme.tertiary
        }
        return Text(label)
            .font(EigoLensTheme.labelSmall)
            .foregroundStyle(color)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(color.opacity(0.12), in: Capsule())
    }

    private func chipRow(label: String, items: [String], color: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(EigoLensTheme.labelSmall)
                .foregroundStyle(.secondary)
            FlowLayout(spacing: 6) {
                ForEach(items, id: \.self) { item in
                    Text(item)
                        .font(EigoLensTheme.labelMedium)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(color.opacity(0.12), in: Capsule())
                }
            }
        }
    }

    private func posColor(_ pos: PartOfSpeech) -> Color {
        switch pos {
        case .noun: return EigoLensTheme.posNoun
        case .verb: return EigoLensTheme.posVerb
        case .adjective: return EigoLensTheme.posAdj
        case .adverb: return EigoLensTheme.posAdv
        default: return .secondary
        }
    }
}

// MARK: - Flow Layout

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = arrangeSubviews(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrangeSubviews(proposal: proposal, subviews: subviews)
        for (index, position) in result.positions.enumerated() {
            subviews[index].place(
                at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y),
                proposal: .unspecified
            )
        }
    }

    private func arrangeSubviews(proposal: ProposedViewSize, subviews: Subviews) -> (positions: [CGPoint], size: CGSize) {
        let maxWidth = proposal.width ?? .greatestFiniteMagnitude
        var positions: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var totalHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth, x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            positions.append(CGPoint(x: x, y: y))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
            totalHeight = y + rowHeight
        }

        return (positions, CGSize(width: maxWidth, height: totalHeight))
    }
}
