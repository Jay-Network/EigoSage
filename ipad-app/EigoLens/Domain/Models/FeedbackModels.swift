import Foundation

struct Feedback: Identifiable {
    let id: Int64
    let category: FeedbackCategory
    let feedbackText: String
    let status: FeedbackStatus
    let createdAt: String
    let updatedAt: String
    let completionNote: String?
}

enum FeedbackCategory: String, CaseIterable {
    case bug, feature, ui, performance, content, other

    var label: String {
        switch self {
        case .bug: return "Bug"
        case .feature: return "Feature Request"
        case .ui: return "UI/UX"
        case .performance: return "Performance"
        case .content: return "Content"
        case .other: return "Other"
        }
    }

    var icon: String {
        switch self {
        case .bug: return "ladybug"
        case .feature: return "lightbulb"
        case .ui: return "paintbrush"
        case .performance: return "speedometer"
        case .content: return "doc.text"
        case .other: return "ellipsis.circle"
        }
    }
}

enum FeedbackStatus: String {
    case pending, under_review, approved, rejected, assigned
    case in_progress, testing, deployed, on_hold, cancelled

    var label: String {
        switch self {
        case .pending: return "Pending"
        case .under_review: return "Under Review"
        case .approved: return "Approved"
        case .rejected: return "Rejected"
        case .assigned: return "Assigned"
        case .in_progress: return "In Progress"
        case .testing: return "Testing"
        case .deployed: return "Deployed"
        case .on_hold: return "On Hold"
        case .cancelled: return "Cancelled"
        }
    }

    var emoji: String {
        switch self {
        case .pending: return "⏳"
        case .under_review: return "👀"
        case .approved: return "✅"
        case .rejected: return "❌"
        case .assigned: return "👤"
        case .in_progress: return "🔨"
        case .testing: return "🧪"
        case .deployed: return "🚀"
        case .on_hold: return "⏸️"
        case .cancelled: return "🚫"
        }
    }

    static func from(_ value: String) -> FeedbackStatus {
        FeedbackStatus(rawValue: value) ?? .pending
    }
}

enum SubmitFeedbackResult {
    case success(feedbackId: Int64, createdAt: String)
    case rateLimited(message: String)
    case error(message: String)
}
