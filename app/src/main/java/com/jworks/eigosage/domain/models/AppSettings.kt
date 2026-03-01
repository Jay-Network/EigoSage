package com.jworks.eigosage.domain.models

data class AppSettings(
    val highlightColor: Long = 0xFF2196F3,
    val strokeWidth: Float = 2f,
    val aiProvider: String = "",
    val aiModel: String = "",
    val cefrThreshold: String = "B2",
    val showIpaOverlay: Boolean = true,
    val ipaFontScale: Float = 0.6f,  // 0.3..1.0 range, default smaller
    val claudeInputTokens: Long = 0,
    val claudeOutputTokens: Long = 0,
    val geminiInputTokens: Long = 0,
    val geminiOutputTokens: Long = 0
)
