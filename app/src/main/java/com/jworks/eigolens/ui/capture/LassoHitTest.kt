package com.jworks.eigolens.ui.capture

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.jworks.eigolens.domain.models.OCRResult

/**
 * Coordinate transform chain:
 * 1. OCR bounding box: coordinates in original bitmap (imageWidth x imageHeight)
 * 2. Image is centered in container, then scaled and offset by user zoom/pan
 * 3. Lasso path: screen-space coordinates
 * 4. Hit-test: check if each word's bounding box center is inside the lasso path
 */

fun findWordsInRect(
    start: Offset,
    end: Offset,
    ocrResult: OCRResult,
    bitmap: Bitmap,
    scale: Float,
    offset: Offset,
    containerSize: Size
): List<String> {
    val left = minOf(start.x, end.x)
    val top = minOf(start.y, end.y)
    val right = maxOf(start.x, end.x)
    val bottom = maxOf(start.y, end.y)

    data class WordHit(val text: String, val top: Int, val left: Int)
    val hits = mutableListOf<WordHit>()

    for (detected in ocrResult.texts) {
        for (element in detected.elements) {
            val bounds = element.bounds ?: continue
            if (!element.isWord) continue

            val center = transformPointToScreen(
                x = bounds.exactCenterX(),
                y = bounds.exactCenterY(),
                bitmap = bitmap,
                scale = scale,
                offset = offset,
                containerSize = containerSize
            )

            if (center.x in left..right && center.y in top..bottom) {
                hits.add(WordHit(element.text, bounds.top, bounds.left))
            }
        }
    }

    // Sort in reading order: top-to-bottom, left-to-right
    return hits
        .sortedWith(compareBy({ it.top }, { it.left }))
        .map { it.text }
}

fun transformPointToScreen(
    x: Float,
    y: Float,
    bitmap: Bitmap,
    scale: Float,
    offset: Offset,
    containerSize: Size
): Offset {
    val imageLeft = (containerSize.width - bitmap.width * scale) / 2f + offset.x
    val imageTop = (containerSize.height - bitmap.height * scale) / 2f + offset.y

    return Offset(
        x = imageLeft + x * scale,
        y = imageTop + y * scale
    )
}

fun transformBoundsToScreen(
    bounds: android.graphics.Rect,
    bitmap: Bitmap,
    scale: Float,
    offset: Offset,
    containerSize: Size
): Rect {
    val topLeft = transformPointToScreen(
        bounds.left.toFloat(), bounds.top.toFloat(),
        bitmap, scale, offset, containerSize
    )
    val bottomRight = transformPointToScreen(
        bounds.right.toFloat(), bounds.bottom.toFloat(),
        bitmap, scale, offset, containerSize
    )
    return Rect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
}
