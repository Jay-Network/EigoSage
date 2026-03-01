package com.jworks.eigosage.domain.models

data class ReadabilityMetrics(
    val fleschKincaidGrade: Double,
    val fleschReadingEase: Double,
    val smogIndex: Double,
    val colemanLiauIndex: Double,
    val averageGrade: Double,
    val difficulty: DifficultyLevel,
    val targetAudience: String,
    val statistics: TextStatistics
)

data class TextStatistics(
    val totalWords: Int,
    val totalSentences: Int,
    val totalSyllables: Int,
    val totalCharacters: Int,
    val averageWordsPerSentence: Double,
    val averageSyllablesPerWord: Double,
    val polysyllableCount: Int
)

enum class DifficultyLevel(val label: String) {
    VERY_EASY("Very Easy"),
    EASY("Easy"),
    MODERATE("Moderate"),
    DIFFICULT("Difficult"),
    VERY_DIFFICULT("Very Difficult");

    companion object {
        fun fromGrade(grade: Double): DifficultyLevel = when {
            grade <= 5.0 -> VERY_EASY
            grade <= 7.0 -> EASY
            grade <= 10.0 -> MODERATE
            grade <= 13.0 -> DIFFICULT
            else -> VERY_DIFFICULT
        }
    }
}
