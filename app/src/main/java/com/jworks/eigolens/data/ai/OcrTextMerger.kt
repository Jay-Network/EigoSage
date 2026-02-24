package com.jworks.eigolens.data.ai

import android.util.Log
import com.jworks.eigolens.domain.models.DetectedText
import com.jworks.eigolens.domain.models.OCRResult
import com.jworks.eigolens.domain.models.TextElement

/**
 * Merges Gemini Vision's corrected text with ML Kit's bounding boxes.
 *
 * Strategy: align lines by index, then align words within each line.
 * ML Kit provides spatial positions; Gemini provides accurate text.
 * When line/word counts differ, we use best-effort alignment.
 */
object OcrTextMerger {

    private const val TAG = "OcrMerger"

    fun merge(mlKitResult: OCRResult, geminiLines: List<String>): OCRResult {
        val mlKitTexts = mlKitResult.texts
        if (mlKitTexts.isEmpty() || geminiLines.isEmpty()) return mlKitResult

        val corrected = mutableListOf<DetectedText>()
        var geminiIdx = 0

        for (mlLine in mlKitTexts) {
            if (geminiIdx >= geminiLines.size) {
                // No more Gemini lines — keep ML Kit's original
                corrected.add(mlLine)
                continue
            }

            val geminiLine = geminiLines[geminiIdx]
            val geminiWords = geminiLine.split(Regex("\\s+")).filter { it.isNotBlank() }
            geminiIdx++

            // If Gemini line is very different length, it might span multiple ML Kit lines
            // or ML Kit split one visual line into multiple. Use simple 1:1 for now.
            val mergedElements = mergeWords(mlLine.elements, geminiWords)

            corrected.add(
                mlLine.copy(
                    text = geminiLine,
                    elements = mergedElements
                )
            )
        }

        val correctedCount = corrected.sumOf { it.elements.size }
        val originalCount = mlKitTexts.sumOf { it.elements.size }
        Log.d(TAG, "Merged: $originalCount ML Kit words -> $correctedCount corrected words, " +
                "${mlKitTexts.size} ML Kit lines / ${geminiLines.size} Gemini lines")

        return mlKitResult.copy(texts = corrected)
    }

    private fun mergeWords(
        mlKitElements: List<TextElement>,
        geminiWords: List<String>
    ): List<TextElement> {
        if (mlKitElements.isEmpty()) return mlKitElements
        if (geminiWords.isEmpty()) return mlKitElements

        val result = mutableListOf<TextElement>()

        when {
            // Same word count — direct 1:1 mapping (most common case)
            mlKitElements.size == geminiWords.size -> {
                for (i in mlKitElements.indices) {
                    result.add(
                        mlKitElements[i].copy(
                            text = geminiWords[i],
                            isWord = geminiWords[i].any { it.isLetter() }
                        )
                    )
                }
            }

            // Gemini has fewer words — ML Kit may have split words
            // Map what we can, keep ML Kit for the rest
            geminiWords.size < mlKitElements.size -> {
                val ratio = mlKitElements.size.toFloat() / geminiWords.size
                for (i in geminiWords.indices) {
                    // Map each Gemini word to the closest ML Kit element
                    val mlIdx = (i * ratio).toInt().coerceIn(0, mlKitElements.lastIndex)
                    result.add(
                        mlKitElements[mlIdx].copy(
                            text = geminiWords[i],
                            isWord = geminiWords[i].any { it.isLetter() }
                        )
                    )
                }
                // Add remaining ML Kit elements that weren't mapped
                val mappedIndices = (geminiWords.indices).map { (it * ratio).toInt().coerceIn(0, mlKitElements.lastIndex) }.toSet()
                for (i in mlKitElements.indices) {
                    if (i !in mappedIndices) {
                        result.add(mlKitElements[i])
                    }
                }
                // Sort by bounds position (left to right)
                result.sortBy { it.bounds?.left ?: 0 }
            }

            // Gemini has more words — ML Kit may have merged words
            // Map 1:1 for available boxes, drop extra Gemini words (no box for them)
            else -> {
                for (i in mlKitElements.indices) {
                    val geminiIdx = (i.toFloat() / mlKitElements.size * geminiWords.size).toInt()
                        .coerceIn(0, geminiWords.lastIndex)
                    result.add(
                        mlKitElements[i].copy(
                            text = geminiWords[geminiIdx],
                            isWord = geminiWords[geminiIdx].any { it.isLetter() }
                        )
                    )
                }
            }
        }

        return result
    }
}
