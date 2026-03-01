package com.jworks.eigosage.domain.models

import android.graphics.Rect

data class DetectedText(
    val text: String,
    val bounds: Rect?,
    val confidence: Float,
    val language: String = "en",
    val elements: List<TextElement> = emptyList()
)

data class TextElement(
    val text: String,
    val bounds: Rect?,
    val isWord: Boolean = true
)
