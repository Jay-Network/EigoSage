import Foundation

enum NavRoute: Hashable {
    case capture
    case annotation(capturedImageId: UUID)
    case history
    case settings
}
