package com.jworks.eigosage.domain.analysis

import com.jworks.eigosage.domain.models.DifficultyLevel
import com.jworks.eigosage.domain.models.ReadabilityMetrics
import com.jworks.eigosage.domain.models.TextStatistics
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class ReadabilityCalculator @Inject constructor() {

    fun calculate(text: String): ReadabilityMetrics? {
        val stats = analyzeText(text)
        if (stats.totalWords < 2 || stats.totalSentences < 1) return null

        val fkGrade = fleschKincaidGrade(stats)
        val fre = fleschReadingEase(stats)
        val smog = smogIndex(stats)
        val cli = colemanLiauIndex(stats)

        val avgGrade = (fkGrade + smog + cli) / 3.0
        val difficulty = DifficultyLevel.fromGrade(avgGrade)

        return ReadabilityMetrics(
            fleschKincaidGrade = fkGrade,
            fleschReadingEase = fre,
            smogIndex = smog,
            colemanLiauIndex = cli,
            averageGrade = avgGrade,
            difficulty = difficulty,
            targetAudience = targetAudience(avgGrade),
            statistics = stats
        )
    }

    // Flesch-Kincaid Grade Level
    // Grade = 0.39 * (words/sentences) + 11.8 * (syllables/words) - 15.59
    private fun fleschKincaidGrade(stats: TextStatistics): Double {
        return 0.39 * stats.averageWordsPerSentence +
                11.8 * stats.averageSyllablesPerWord - 15.59
    }

    // Flesch Reading Ease Score (higher = easier)
    // Score = 206.835 - 1.015 * (words/sentences) - 84.6 * (syllables/words)
    private fun fleschReadingEase(stats: TextStatistics): Double {
        return (206.835 - 1.015 * stats.averageWordsPerSentence -
                84.6 * stats.averageSyllablesPerWord).coerceIn(0.0, 100.0)
    }

    // SMOG Index
    // Grade = 1.0430 * sqrt(polysyllables * (30 / sentences)) + 3.1291
    private fun smogIndex(stats: TextStatistics): Double {
        if (stats.totalSentences < 1) return 0.0
        return 1.0430 * sqrt(
            stats.polysyllableCount.toDouble() * (30.0 / stats.totalSentences)
        ) + 3.1291
    }

    // Coleman-Liau Index (character-based, no syllable counting needed)
    // Grade = 0.0588 * L - 0.296 * S - 15.8
    // L = avg letters per 100 words, S = avg sentences per 100 words
    private fun colemanLiauIndex(stats: TextStatistics): Double {
        val l = stats.totalCharacters.toDouble() / stats.totalWords * 100.0
        val s = stats.totalSentences.toDouble() / stats.totalWords * 100.0
        return 0.0588 * l - 0.296 * s - 15.8
    }

    fun analyzeText(text: String): TextStatistics {
        val words = tokenizeWords(text)
        val sentences = tokenizeSentences(text)
        val wordCount = words.size.coerceAtLeast(1)
        val sentenceCount = sentences.size.coerceAtLeast(1)
        val syllables = words.sumOf { countSyllables(it) }
        val characters = words.sumOf { it.length }
        val polysyllables = words.count { countSyllables(it) >= 3 }

        return TextStatistics(
            totalWords = wordCount,
            totalSentences = sentenceCount,
            totalSyllables = syllables,
            totalCharacters = characters,
            averageWordsPerSentence = wordCount.toDouble() / sentenceCount,
            averageSyllablesPerWord = syllables.toDouble() / wordCount,
            polysyllableCount = polysyllables
        )
    }

    fun countSyllables(word: String): Int {
        val cleaned = word.lowercase().trim()
        if (cleaned.isEmpty()) return 1

        var count = 0
        var prevVowel = false

        for (c in cleaned) {
            val isVowel = c in "aeiouy"
            if (isVowel && !prevVowel) {
                count++
            }
            prevVowel = isVowel
        }

        // Silent 'e' at end (but not "le" which is a syllable)
        if (cleaned.endsWith("e") && !cleaned.endsWith("le") && count > 1) {
            count--
        }

        // Common suffixes that add syllables
        if (cleaned.endsWith("ed") && count > 1) {
            // "ed" is usually silent unless preceded by t or d
            val beforeEd = cleaned.dropLast(2).lastOrNull()
            if (beforeEd != null && beforeEd != 't' && beforeEd != 'd') {
                count--
            }
        }

        return count.coerceAtLeast(1)
    }

    private fun tokenizeWords(text: String): List<String> {
        return text.split(Regex("[\\s—–]+"))
            .map { it.replace(Regex("[^a-zA-Z']"), "") }
            .filter { it.length >= 1 && it.any { c -> c.isLetter() } }
    }

    private fun tokenizeSentences(text: String): List<String> {
        return text.split(Regex("[.!?]+"))
            .filter { it.trim().isNotBlank() }
    }

    private fun targetAudience(grade: Double): String = when {
        grade <= 1.0 -> "Pre-K to 1st grade"
        grade <= 3.0 -> "1st-3rd grade"
        grade <= 5.0 -> "4th-5th grade"
        grade <= 6.0 -> "6th grade"
        grade <= 8.0 -> "7th-8th grade"
        grade <= 10.0 -> "9th-10th grade"
        grade <= 12.0 -> "11th-12th grade"
        grade <= 14.0 -> "College"
        else -> "Graduate / Professional"
    }
}
