import SwiftUI

enum EigoLensTheme {
    // Brand colors — neutral to let the image be the hero
    static let primary = Color(hex: 0x1B6B93)          // Deep teal
    static let primaryDark = Color(hex: 0x145374)
    static let onPrimary = Color.white

    static let secondary = Color(hex: 0x468B97)        // Muted teal
    static let secondaryDark = Color(hex: 0x2D6A72)
    static let onSecondary = Color.white

    static let tertiary = Color(hex: 0xEF6262)         // Warm coral
    static let onTertiary = Color.white

    static let background = Color(hex: 0xF5F5F5)
    static let surface = Color.white
    static let surfaceVariant = Color(hex: 0xF0F0F0)
    static let onSurface = Color(hex: 0x1A1A1A)
    static let onSurfaceVariant = Color(hex: 0x666666)
    static let outline = Color(hex: 0xCCCCCC)

    static let error = Color(hex: 0xB00020)
    static let success = Color(hex: 0x2E7D32)
    static let warning = Color(hex: 0xF9A825)

    // Scope level colors (matching Android)
    static let scopeWord = Color(hex: 0x1B6B93)
    static let scopePhrase = Color(hex: 0x7B2D8E)
    static let scopeSentence = Color(hex: 0xC2571A)
    static let scopeParagraph = Color(hex: 0x2E7D32)
    static let scopeFullText = Color(hex: 0x37474F)

    // POS tag colors
    static let posNoun = Color(hex: 0x1565C0)
    static let posVerb = Color(hex: 0xC62828)
    static let posAdj = Color(hex: 0x2E7D32)
    static let posAdv = Color(hex: 0x6A1B9A)

    // Typography
    static let headlineLarge = Font.system(.title, design: .default, weight: .bold)
    static let headlineMedium = Font.system(.title2, design: .default, weight: .semibold)
    static let titleLarge = Font.system(.title3, design: .default, weight: .semibold)
    static let titleMedium = Font.system(.headline)
    static let bodyLarge = Font.system(.body)
    static let bodyMedium = Font.system(.callout)
    static let bodySmall = Font.system(.footnote)
    static let labelMedium = Font.system(.caption, weight: .medium)
    static let labelSmall = Font.system(.caption2)

    // Spacing
    static let spacingXS: CGFloat = 4
    static let spacingS: CGFloat = 8
    static let spacingM: CGFloat = 16
    static let spacingL: CGFloat = 24
    static let spacingXL: CGFloat = 32

    // Corner radius
    static let radiusS: CGFloat = 8
    static let radiusM: CGFloat = 12
    static let radiusL: CGFloat = 20
    static let radiusXL: CGFloat = 28
}
