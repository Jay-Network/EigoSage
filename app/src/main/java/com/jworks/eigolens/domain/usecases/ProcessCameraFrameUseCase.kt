package com.jworks.eigolens.domain.usecases

import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.jworks.eigolens.domain.models.DetectedText
import com.jworks.eigolens.domain.models.OCRResult
import com.jworks.eigolens.domain.models.TextElement
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class ProcessCameraFrameUseCase @Inject constructor(
    private val textRecognizer: TextRecognizer
) {
    companion object {
        private const val TAG = "OCR"
        private const val MIN_WORD_LENGTH = 1
    }

    suspend fun processStaticImage(bitmap: Bitmap): OCRResult {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val imageSize = Size(bitmap.width, bitmap.height)
        return executeStatic(inputImage, imageSize)
    }

    /** For static captures — no timeout, wait for full result */
    private suspend fun executeStatic(inputImage: InputImage, imageSize: Size): OCRResult {
        val startTime = System.currentTimeMillis()

        val visionText = try {
            textRecognizer.process(inputImage).await()
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit processing failed", e)
            return OCRResult(emptyList(), System.currentTimeMillis(), imageSize, 0L)
        }

        return buildResult(visionText, imageSize, startTime)
    }

    /** For real-time camera frames — short timeout, OK to drop frames */
    suspend fun execute(inputImage: InputImage, imageSize: Size): OCRResult {
        val startTime = System.currentTimeMillis()

        val visionText = withTimeoutOrNull(500L) {
            textRecognizer.process(inputImage).await()
        } ?: return OCRResult(emptyList(), System.currentTimeMillis(), imageSize, 500L)

        return buildResult(visionText, imageSize, startTime)
    }

    private fun buildResult(
        visionText: com.google.mlkit.vision.text.Text,
        imageSize: Size,
        startTime: Long
    ): OCRResult {
        val detectedTexts = visionText.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                val lineText = line.text.trim()
                if (lineText.isEmpty()) return@mapNotNull null

                val elements = line.elements.mapNotNull { element ->
                    val word = element.text.trim()
                    if (word.length < MIN_WORD_LENGTH) return@mapNotNull null
                    if (!word.any { it.isLetter() }) return@mapNotNull null

                    TextElement(
                        text = word,
                        bounds = element.boundingBox,
                        isWord = word.all { it.isLetter() || it == '\'' || it == '-' }
                    )
                }

                if (elements.isEmpty()) return@mapNotNull null

                DetectedText(
                    text = lineText,
                    bounds = line.boundingBox,
                    confidence = line.confidence,
                    language = line.recognizedLanguage ?: "en",
                    elements = elements
                )
            }
        }

        val processingTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "OCR: ${detectedTexts.size} lines, ${processingTime}ms, " +
                "words: ${detectedTexts.sumOf { it.elements.size }}")

        return OCRResult(
            texts = detectedTexts,
            timestamp = System.currentTimeMillis(),
            imageSize = imageSize,
            processingTimeMs = processingTime
        )
    }
}
