package com.jworks.eigolens.domain.models

import android.graphics.Rect

data class EnrichedWord(
    val text: String,
    val bounds: Rect?,
    val ipa: String?,
    val cefr: CefrLevel?,
    val briefDefinition: String?
)
