package com.jworks.eigolens.ui.capture

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.jworks.eigolens.domain.models.OCRResult
import com.jworks.eigolens.domain.models.TextElement

data class TapResult(
    val word: String,
    val element: TextElement,
    val screenBounds: Rect
)

/** Tolerance in image-space pixels for imprecise taps */
private const val TAP_TOLERANCE_PX = 20f

/**
 * Inverse of [transformPointToScreen] in LassoHitTest.kt.
 * Converts screen-space coordinates back to original image (bitmap) coordinates.
 */
fun transformScreenToImage(
    screenX: Float,
    screenY: Float,
    bitmap: Bitmap,
    scale: Float,
    offset: Offset,
    containerSize: Size
): Offset {
    val imageLeft = (containerSize.width - bitmap.width * scale) / 2f + offset.x
    val imageTop = (containerSize.height - bitmap.height * scale) / 2f + offset.y

    return Offset(
        x = (screenX - imageLeft) / scale,
        y = (screenY - imageTop) / scale
    )
}

/**
 * Finds the word tapped at the given screen-space offset.
 *
 * Algorithm:
 * 1. Convert screen tap to image-space coordinates via [transformScreenToImage]
 * 2. Iterate OCR TextElements, find the one whose bounds contain the image-space point
 *    (with [TAP_TOLERANCE_PX] tolerance for imprecise taps)
 * 3. If multiple elements match, pick the closest by center distance
 * 4. Return [TapResult] with the word text, element, and screen-space bounds for highlighting
 */
fun findTappedWord(
    tapOffset: Offset,
    ocrResult: OCRResult,
    bitmap: Bitmap,
    scale: Float,
    offset: Offset,
    containerSize: Size
): TapResult? {
    val imagePoint = transformScreenToImage(
        tapOffset.x, tapOffset.y,
        bitmap, scale, offset, containerSize
    )

    data class Candidate(
        val element: TextElement,
        val distance: Float,
        val screenBounds: Rect
    )

    val candidates = mutableListOf<Candidate>()

    for (detected in ocrResult.texts) {
        for (element in detected.elements) {
            val bounds = element.bounds ?: continue
            if (!element.isWord) continue

            // Expand bounds by tolerance for imprecise taps
            val expandedLeft = bounds.left - TAP_TOLERANCE_PX
            val expandedTop = bounds.top - TAP_TOLERANCE_PX
            val expandedRight = bounds.right + TAP_TOLERANCE_PX
            val expandedBottom = bounds.bottom + TAP_TOLERANCE_PX

            if (imagePoint.x >= expandedLeft && imagePoint.x <= expandedRight &&
                imagePoint.y >= expandedTop && imagePoint.y <= expandedBottom
            ) {
                val centerX = bounds.exactCenterX()
                val centerY = bounds.exactCenterY()
                val dx = imagePoint.x - centerX
                val dy = imagePoint.y - centerY
                val distance = dx * dx + dy * dy // squared distance is fine for comparison

                val screenBounds = transformBoundsToScreen(
                    bounds, bitmap, scale, offset, containerSize
                )

                candidates.add(Candidate(element, distance, screenBounds))
            }
        }
    }

    if (candidates.isEmpty()) return null

    val best = candidates.minBy { it.distance }
    return TapResult(
        word = best.element.text,
        element = best.element,
        screenBounds = best.screenBounds
    )
}
