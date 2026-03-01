package com.jworks.eigosage.domain.ai

import android.graphics.Bitmap
import com.jworks.eigosage.ui.capture.ScopeLevel

interface AiProvider {
    val name: String
    val isAvailable: Boolean
    suspend fun analyze(context: AnalysisContext): Result<AiResponse>
}

data class AnalysisContext(
    val selectedText: String,
    val fullSnapshotText: String,
    val scopeLevel: ScopeLevel,
    val surroundingLines: List<String> = emptyList(),
    val croppedImage: Bitmap? = null
)

data class AiResponse(
    val content: String,
    val provider: String,
    val model: String,
    val tokensUsed: Int? = null,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val processingTimeMs: Long
)
