package com.jworks.eigosage.domain.models

import androidx.compose.ui.graphics.Color

enum class CefrLevel(val label: String, val ordinalIndex: Int) {
    A1("Beginner", 0),
    A2("Elementary", 1),
    B1("Intermediate", 2),
    B2("Upper-Int", 3),
    C1("Advanced", 4),
    C2("Proficiency", 5);

    companion object {
        fun fromString(value: String?): CefrLevel? {
            if (value == null) return null
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }

        val ALL = entries.toList()
    }
}

fun CefrLevel.color(): Color = when (this) {
    CefrLevel.A1 -> Color.Transparent
    CefrLevel.A2 -> Color.Transparent
    CefrLevel.B1 -> Color(0xFFFFC107)  // amber
    CefrLevel.B2 -> Color(0xFFFF9800)  // orange
    CefrLevel.C1 -> Color(0xFFF44336)  // red
    CefrLevel.C2 -> Color(0xFFB71C1C)  // dark red
}
