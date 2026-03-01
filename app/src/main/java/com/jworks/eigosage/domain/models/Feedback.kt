package com.jworks.eigosage.domain.models

data class Feedback(
    val id: Long,
    val category: FeedbackCategory,
    val feedbackText: String,
    val status: FeedbackStatus,
    val createdAt: String,
    val updatedAt: String,
    val completionNote: String?
)

enum class FeedbackCategory(val value: String, val label: String) {
    BUG("bug", "Bug"),
    FEATURE("feature", "Feature Request"),
    UI("ui", "UI/UX"),
    PERFORMANCE("performance", "Performance"),
    CONTENT("content", "Content"),
    OTHER("other", "Other");

    companion object {
        fun fromString(value: String): FeedbackCategory =
            entries.find { it.value == value } ?: OTHER
    }
}

enum class FeedbackStatus(val value: String, val label: String, val emoji: String) {
    PENDING("pending", "Pending", "\u23F3"),
    UNDER_REVIEW("under_review", "Under Review", "\uD83D\uDC40"),
    APPROVED("approved", "Approved", "\u2705"),
    REJECTED("rejected", "Rejected", "\u274C"),
    ASSIGNED("assigned", "Assigned", "\uD83D\uDC64"),
    IN_PROGRESS("in_progress", "In Progress", "\uD83D\uDD28"),
    TESTING("testing", "Testing", "\uD83E\uDDEA"),
    DEPLOYED("deployed", "Deployed", "\uD83D\uDE80"),
    ON_HOLD("on_hold", "On Hold", "\u23F8\uFE0F"),
    CANCELLED("cancelled", "Cancelled", "\uD83D\uDEAB");

    companion object {
        fun fromString(value: String): FeedbackStatus =
            entries.find { it.value == value } ?: PENDING
    }
}

data class FeedbackStatusChange(
    val oldStatus: FeedbackStatus?,
    val newStatus: FeedbackStatus,
    val changedBy: String,
    val changedAt: String,
    val note: String?
)

sealed class SubmitFeedbackResult {
    data class Success(val feedbackId: Long, val createdAt: String) : SubmitFeedbackResult()
    data class RateLimited(val message: String) : SubmitFeedbackResult()
    data class Error(val message: String) : SubmitFeedbackResult()
}

data class FeedbackWithHistory(
    val feedback: Feedback,
    val statusHistory: List<FeedbackStatusChange>
)
