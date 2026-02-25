import SwiftUI

struct StyledMarkdownText: View {
    let text: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            ForEach(Array(text.components(separatedBy: .newlines).enumerated()), id: \.offset) { _, line in
                let trimmed = line.trimmingCharacters(in: .whitespaces)
                if !trimmed.isEmpty {
                    if trimmed.hasPrefix("- ") || trimmed.hasPrefix("* ") {
                        bulletLine(String(trimmed.dropFirst(2)))
                    } else if let match = trimmed.range(of: #"^\d+\.\s+"#, options: .regularExpression) {
                        numberedLine(number: String(trimmed[trimmed.startIndex..<match.upperBound]),
                                     content: String(trimmed[match.upperBound...]))
                    } else {
                        parseBoldText(trimmed)
                    }
                }
            }
        }
    }

    private func bulletLine(_ content: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text("\u{2022}")
                .foregroundStyle(.secondary)
            parseBoldText(content)
        }
    }

    private func numberedLine(number: String, content: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 4) {
            Text(number)
                .font(EigoLensTheme.bodyMedium)
                .foregroundStyle(.secondary)
            parseBoldText(content)
        }
    }

    @ViewBuilder
    private func parseBoldText(_ text: String) -> some View {
        let parts = splitBoldSegments(text)
        let attributed = parts.reduce(AttributedString()) { result, part in
            var segment = AttributedString(part.text)
            if part.isBold {
                segment.font = EigoLensTheme.bodyMedium.bold()
            } else {
                segment.font = EigoLensTheme.bodyMedium
            }
            return result + segment
        }
        Text(attributed)
    }

    private struct TextSegment {
        let text: String
        let isBold: Bool
    }

    private func splitBoldSegments(_ text: String) -> [TextSegment] {
        var segments: [TextSegment] = []
        var remaining = text
        while let start = remaining.range(of: "**") {
            // Add text before the bold marker
            let before = String(remaining[remaining.startIndex..<start.lowerBound])
            if !before.isEmpty {
                segments.append(TextSegment(text: before, isBold: false))
            }
            remaining = String(remaining[start.upperBound...])

            // Find closing **
            if let end = remaining.range(of: "**") {
                let boldText = String(remaining[remaining.startIndex..<end.lowerBound])
                segments.append(TextSegment(text: boldText, isBold: true))
                remaining = String(remaining[end.upperBound...])
            } else {
                // No closing **, treat rest as normal
                segments.append(TextSegment(text: remaining, isBold: false))
                remaining = ""
            }
        }
        if !remaining.isEmpty {
            segments.append(TextSegment(text: remaining, isBold: false))
        }
        return segments
    }
}
