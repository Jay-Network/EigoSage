package com.jworks.eigolens.domain.ai

import com.jworks.eigolens.ui.capture.ScopeLevel

interface AiProvider {
    val name: String
    val isAvailable: Boolean
    suspend fun analyze(context: AnalysisContext): Result<AiResponse>
}

data class AnalysisContext(
    val selectedText: String,
    val fullSnapshotText: String,
    val scopeLevel: ScopeLevel,
    val surroundingLines: List<String> = emptyList()
)

data class AiResponse(
    val content: String,
    val provider: String,
    val model: String,
    val tokensUsed: Int? = null,
    val processingTimeMs: Long
)
