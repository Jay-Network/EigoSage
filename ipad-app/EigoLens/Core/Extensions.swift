import Foundation
import CoreGraphics
import SwiftUI

extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}

extension String {
    var trimmingPunctuation: String {
        replacingOccurrences(of: "[^a-zA-Z'-]", with: "", options: .regularExpression).trimmingCharacters(in: .whitespaces)
    }

    var isBlank: Bool {
        trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}

extension Color {
    init(hex: UInt, alpha: Double = 1.0) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: alpha
        )
    }
}

extension Bundle {
    func infoPlistString(for key: String) -> String? {
        object(forInfoDictionaryKey: key) as? String
    }
}
