package com.jworks.eigosage.domain.repository

import com.jworks.eigosage.domain.models.FeedbackCategory
import com.jworks.eigosage.domain.models.FeedbackWithHistory
import com.jworks.eigosage.domain.models.SubmitFeedbackResult

interface FeedbackRepository {
    suspend fun submitFeedback(
        email: String,
        appId: String,
        category: FeedbackCategory,
        feedbackText: String,
        deviceInfo: Map<String, String>? = null
    ): SubmitFeedbackResult

    suspend fun getFeedbackUpdates(
        email: String,
        appId: String,
        sinceId: Long? = null
    ): List<FeedbackWithHistory>

    suspend fun registerFcmToken(
        email: String,
        appId: String,
        fcmToken: String,
        deviceInfo: Map<String, String>? = null
    ): Boolean
}
