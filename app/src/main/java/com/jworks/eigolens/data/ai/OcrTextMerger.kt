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

        // Only attempt line-level merge when line counts match.
        // Mismatched line counts cause cascading word-to-box misalignment.
        if (mlKitTexts.size != geminiLines.size) {
            Log.d(TAG, "Line count mismatch (ML Kit: ${mlKitTexts.size}, Gemini: ${geminiLines.size}), keeping ML Kit result")
            return mlKitResult
        }

        val corrected = mutableListOf<DetectedText>()

        for (i in mlKitTexts.indices) {
            val mlLine = mlKitTexts[i]
            val geminiLine = geminiLines[i]
            val geminiWords = geminiLine.split(Regex("\\s+")).filter { it.isNotBlank() }

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

        // Only replace text when word counts match exactly.
        // Mismatched counts caused words to be assigned to wrong bounding boxes,
        // resulting in taps looking up the wrong word.
        if (mlKitElements.size == geminiWords.size) {
            return mlKitElements.mapIndexed { i, element ->
                element.copy(
                    text = geminiWords[i],
                    isWord = geminiWords[i].any { it.isLetter() }
                )
            }
        }

        // Word count mismatch — keep ML Kit's original text to preserve
        // accurate word-to-bounding-box mapping
        Log.d(TAG, "Word count mismatch (ML Kit: ${mlKitElements.size}, Gemini: ${geminiWords.size}), keeping ML Kit text")
        return mlKitElements
    }
}
