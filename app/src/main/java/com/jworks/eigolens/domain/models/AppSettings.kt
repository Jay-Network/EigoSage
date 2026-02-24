package com.jworks.eigolens.domain.models

data class AppSettings(
    val highlightColor: Long = 0xFF2196F3,
    val strokeWidth: Float = 2f,
    val aiProvider: String = "",
    val aiModel: String = ""
)
