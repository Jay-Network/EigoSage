package com.jworks.eigolens.ui.capture

import android.graphics.Paint
import android.graphics.Typeface
import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import com.jworks.eigolens.domain.models.CefrLevel
import com.jworks.eigolens.domain.models.EnrichedWord
import com.jworks.eigolens.domain.models.color

/**
 * Canvas overlay drawn on top of the live camera preview.
 * Maps OCR bounding-box coordinates (in image space) onto the
 * PreviewView using FILL_CENTER scaling — identical to how
 * PreviewView itself scales the camera feed.
 */
@Composable
fun LiveOverlayCanvas(
    enrichedWords: List<EnrichedWord>,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    cefrThreshold: CefrLevel,
    showIpa: Boolean,
    ipaFontScale: Float = 0.6f,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val ipaPaint = remember {
        Paint().apply {
            color = android.graphics.Color.rgb(0, 230, 255) // cyan
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (imageWidth == 0 || imageHeight == 0) return@Canvas

        val canvasW = size.width
        val canvasH = size.height

        // The image dimensions have already been rotated by the ViewModel
        val effectiveW = imageWidth.toFloat()
        val effectiveH = imageHeight.toFloat()

        // FILL_CENTER: scale to fill, then crop overflow
        val scale = maxOf(canvasW / effectiveW, canvasH / effectiveH)
        val cropOffsetX = (effectiveW * scale - canvasW) / 2f
        val cropOffsetY = (effectiveH * scale - canvasH) / 2f

        for (word in enrichedWords) {
            val bounds = word.bounds ?: continue
            val wordCefr = word.cefr ?: continue
            if (wordCefr.ordinalIndex < cefrThreshold.ordinalIndex) continue

            // Map OCR bounds to canvas coordinates
            val left = bounds.left * scale - cropOffsetX
            val top = bounds.top * scale - cropOffsetY
            val right = bounds.right * scale - cropOffsetX
            val bottom = bounds.bottom * scale - cropOffsetY

            val w = right - left
            val h = bottom - top

            // Skip off-screen words
            if (right < 0f || left > canvasW || bottom < 0f || top > canvasH) continue

            val cefrColor = wordCefr.color()
            if (cefrColor != Color.Transparent) {
                // Filled background
                drawRoundRect(
                    color = cefrColor.copy(alpha = 0.25f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(w, h),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // Border
                drawRoundRect(
                    color = cefrColor.copy(alpha = 0.6f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(w, h),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = Stroke(width = 1.5f)
                )
            }

            // IPA text above the word
            if (showIpa) {
                word.ipa?.let { ipa ->
                    val baseSizeDp = 6f + (ipaFontScale * 8f) // 8.4dp – 14dp
                    ipaPaint.textSize = baseSizeDp * density.density
                    drawContext.canvas.nativeCanvas.drawText(
                        ipa,
                        (left + right) / 2f,
                        top - 2f * density.density,
                        ipaPaint
                    )
                }
            }
        }
    }
}
