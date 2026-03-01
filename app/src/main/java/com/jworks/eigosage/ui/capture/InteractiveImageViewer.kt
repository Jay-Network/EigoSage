package com.jworks.eigosage.ui.capture

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import android.graphics.Paint
import android.graphics.Typeface
import com.jworks.eigosage.R
import com.jworks.eigosage.domain.models.CefrLevel
import com.jworks.eigosage.domain.models.EnrichedWord
import com.jworks.eigosage.domain.models.color

@Composable
fun InteractiveImageViewer(
    capturedImage: CapturedImage,
    interactionMode: InteractionMode,
    onInteractionModeChange: (InteractionMode) -> Unit,
    onWordsSelected: (List<String>, android.graphics.Rect?) -> Unit,
    onWordTapped: (TapResult) -> Unit,
    onWordLongPressed: (TapResult) -> Unit = {},
    tappedWord: TapResult?,
    enrichedWords: List<EnrichedWord> = emptyList(),
    cefrThreshold: CefrLevel = CefrLevel.B2,
    showIpa: Boolean = true,
    ipaFontScale: Float = 0.6f,
    modifier: Modifier = Modifier
) {
    val bitmap = capturedImage.bitmap
    val ocrResult = capturedImage.ocrResult

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rectStart by remember { mutableStateOf<Offset?>(null) }
    var rectEnd by remember { mutableStateOf<Offset?>(null) }
    var completedRectStart by remember { mutableStateOf<Offset?>(null) }
    var completedRectEnd by remember { mutableStateOf<Offset?>(null) }
    var selectedWordBounds by remember { mutableStateOf<List<android.graphics.Rect>>(emptyList()) }

    // Pulse animation for tapped word highlight
    val infiniteTransition = rememberInfiniteTransition(label = "tap_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tap_pulse_alpha"
    )

    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val containerSize = Size(containerWidth, containerHeight)

        val initialScale = remember(bitmap, containerWidth, containerHeight) {
            val scaleX = containerWidth / bitmap.width
            val scaleY = containerHeight / bitmap.height
            minOf(scaleX, scaleY)
        }

        LaunchedEffect(initialScale) {
            scale = initialScale
        }

        // Two-finger zoom/pan — works in both TAP and CIRCLE modes
        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(initialScale, initialScale * 5f)

            val maxOffsetX = ((bitmap.width * scale - containerWidth) / 2f).coerceAtLeast(0f)
            val maxOffsetY = ((bitmap.height * scale - containerHeight) / 2f).coerceAtLeast(0f)

            offset = Offset(
                x = (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                y = (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // Two-finger zoom/pan always active
                .transformable(state = transformableState)
                .then(
                    when (interactionMode) {
                        InteractionMode.TAP -> {
                            // Tap = word lookup, long-press = sentence analysis
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { tapOffset ->
                                        val result = findTappedWord(
                                            tapOffset, ocrResult, bitmap,
                                            scale, offset, containerSize
                                        )
                                        if (result != null) {
                                            selectedWordBounds = emptyList()
                                            completedRectStart = null
                                            completedRectEnd = null
                                            onWordTapped(result)
                                        }
                                    },
                                    onLongPress = { tapOffset ->
                                        val result = findTappedWord(
                                            tapOffset, ocrResult, bitmap,
                                            scale, offset, containerSize
                                        )
                                        if (result != null) {
                                            selectedWordBounds = emptyList()
                                            completedRectStart = null
                                            completedRectEnd = null
                                            onWordLongPressed(result)
                                        }
                                    }
                                )
                            }
                        }
                        InteractionMode.CIRCLE -> {
                            // Single-finger drag to draw rectangle
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { startPoint ->
                                        rectStart = startPoint
                                        rectEnd = startPoint
                                        selectedWordBounds = emptyList()
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        rectEnd = change.position
                                    },
                                    onDragEnd = {
                                        val start = rectStart
                                        val end = rectEnd
                                        if (start != null && end != null) {
                                            val words = findWordsInRect(
                                                start, end, ocrResult, bitmap,
                                                scale, offset, containerSize
                                            )
                                            if (words.isNotEmpty()) {
                                                selectedWordBounds = findSelectedBoundsInRect(
                                                    start, end, ocrResult, bitmap,
                                                    scale, offset, containerSize
                                                )
                                                // Persist the selection rectangle
                                                completedRectStart = start
                                                completedRectEnd = end
                                                val imageRect = screenRectToImageRect(
                                                    start, end, bitmap,
                                                    scale, offset, containerSize
                                                )
                                                onWordsSelected(words, imageRect)
                                            }
                                        }
                                        rectStart = null
                                        rectEnd = null
                                        // Auto-switch back to TAP after selection
                                        onInteractionModeChange(InteractionMode.TAP)
                                    }
                                )
                            }
                        }
                    }
                )
        ) {
            // Layer 1: Bitmap image with zoom/pan transforms
            val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val imageWidth = bitmap.width * scale
                val imageHeight = bitmap.height * scale
                val imageLeft = (containerWidth - imageWidth) / 2f + offset.x
                val imageTop = (containerHeight - imageHeight) / 2f + offset.y

                drawImage(
                    image = imageBitmap,
                    dstOffset = IntOffset(imageLeft.toInt(), imageTop.toInt()),
                    dstSize = IntSize(imageWidth.toInt(), imageHeight.toInt())
                )
            }

            // Build enrichment lookup for fast matching
            val enrichmentMap = remember(enrichedWords) {
                enrichedWords.filter { it.bounds != null }
                    .groupBy { "${it.bounds!!.left},${it.bounds.top}" }
            }

            // IPA text paint (reused across draw calls)
            val ipaPaint = remember {
                Paint().apply {
                    color = android.graphics.Color.rgb(0, 230, 255) // cyan
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    isAntiAlias = true
                }
            }

            // Layer 2: OCR bounding boxes + CEFR coloring + IPA + highlights + lasso
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw OCR word bounding boxes with CEFR coloring (always visible)
                for (detected in ocrResult.texts) {
                    for (element in detected.elements) {
                        val bounds = element.bounds ?: continue
                        val screenRect = transformBoundsToScreen(
                            bounds, bitmap, scale, offset, containerSize
                        )

                        // Find enrichment data for this word
                        val key = "${bounds.left},${bounds.top}"
                        val enriched = enrichmentMap[key]?.firstOrNull()
                        val wordCefr = enriched?.cefr

                        if (wordCefr != null && wordCefr.ordinalIndex >= cefrThreshold.ordinalIndex) {
                            // CEFR-colored box for difficult words
                            val cefrColor = wordCefr.color()
                            if (cefrColor != Color.Transparent) {
                                drawRoundRect(
                                    color = cefrColor.copy(alpha = 0.25f),
                                    topLeft = Offset(screenRect.left, screenRect.top),
                                    size = Size(screenRect.width, screenRect.height),
                                    cornerRadius = CornerRadius(3f, 3f)
                                )
                                drawRoundRect(
                                    color = cefrColor.copy(alpha = 0.6f),
                                    topLeft = Offset(screenRect.left, screenRect.top),
                                    size = Size(screenRect.width, screenRect.height),
                                    cornerRadius = CornerRadius(3f, 3f),
                                    style = Stroke(width = 1.5f)
                                )
                            }

                            // Draw IPA above word (respects toggle)
                            if (showIpa) {
                                enriched.ipa?.let { ipa ->
                                    val baseSizeDp = 6f + (ipaFontScale * 8f) // 8.4dp – 14dp
                                    ipaPaint.textSize = baseSizeDp * density.density
                                    drawContext.canvas.nativeCanvas.drawText(
                                        ipa,
                                        (screenRect.left + screenRect.right) / 2f,
                                        screenRect.top - 2f * density.density,
                                        ipaPaint
                                    )
                                }
                            }
                        } else {
                            // Default blue outline for easy words
                            drawRect(
                                color = Color(0xFF2196F3).copy(alpha = 0.25f),
                                topLeft = Offset(screenRect.left, screenRect.top),
                                size = Size(screenRect.width, screenRect.height),
                                style = Stroke(width = 1.5f)
                            )
                        }
                    }
                }

                // Draw tapped word highlight (blue rounded rect with pulse)
                tappedWord?.let { tap ->
                    val bounds = tap.screenBounds
                    val padding = 4f
                    drawRoundRect(
                        color = Color(0xFF2196F3).copy(alpha = pulseAlpha),
                        topLeft = Offset(bounds.left - padding, bounds.top - padding),
                        size = Size(bounds.width + padding * 2, bounds.height + padding * 2),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color(0xFF2196F3),
                        topLeft = Offset(bounds.left - padding, bounds.top - padding),
                        size = Size(bounds.width + padding * 2, bounds.height + padding * 2),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 2.5f)
                    )
                }

                // Draw lasso-selected word highlights (orange)
                for (bounds in selectedWordBounds) {
                    val screenRect = transformBoundsToScreen(
                        bounds, bitmap, scale, offset, containerSize
                    )
                    drawRect(
                        color = Color(0xFFFF9800).copy(alpha = 0.3f),
                        topLeft = Offset(screenRect.left, screenRect.top),
                        size = Size(screenRect.width, screenRect.height)
                    )
                    drawRect(
                        color = Color(0xFFFF9800),
                        topLeft = Offset(screenRect.left, screenRect.top),
                        size = Size(screenRect.width, screenRect.height),
                        style = Stroke(width = 2.5f)
                    )
                }

                // Draw completed selection rectangle (persists after finger release)
                val cStart = completedRectStart
                val cEnd = completedRectEnd
                if (cStart != null && cEnd != null) {
                    val left = minOf(cStart.x, cEnd.x)
                    val top = minOf(cStart.y, cEnd.y)
                    val width = kotlin.math.abs(cEnd.x - cStart.x)
                    val height = kotlin.math.abs(cEnd.y - cStart.y)
                    drawRect(
                        color = Color(0xFFFFEB3B).copy(alpha = 0.10f),
                        topLeft = Offset(left, top),
                        size = Size(width, height)
                    )
                    drawRect(
                        color = Color(0xFFFFEB3B).copy(alpha = 0.7f),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        style = Stroke(width = 2f)
                    )
                }

                // Draw selection rectangle while dragging
                val start = rectStart
                val end = rectEnd
                if (start != null && end != null) {
                    val left = minOf(start.x, end.x)
                    val top = minOf(start.y, end.y)
                    val width = kotlin.math.abs(end.x - start.x)
                    val height = kotlin.math.abs(end.y - start.y)
                    // Fill
                    drawRect(
                        color = Color(0xFFFFEB3B).copy(alpha = 0.15f),
                        topLeft = Offset(left, top),
                        size = Size(width, height)
                    )
                    // Border
                    drawRect(
                        color = Color(0xFFFFEB3B),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        style = Stroke(width = 2.5f)
                    )
                }
            }
        }

        // Mode indicator
        if (interactionMode == InteractionMode.CIRCLE) {
            Text(
                text = "Drag to select words",
                color = Color(0xFFFFEB3B),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }
    }
}

/** Collect the bounding boxes of words inside the selection rectangle */
private fun findSelectedBoundsInRect(
    start: Offset,
    end: Offset,
    ocrResult: com.jworks.eigosage.domain.models.OCRResult,
    bitmap: android.graphics.Bitmap,
    scale: Float,
    offset: Offset,
    containerSize: Size
): List<android.graphics.Rect> {
    val left = minOf(start.x, end.x)
    val top = minOf(start.y, end.y)
    val right = maxOf(start.x, end.x)
    val bottom = maxOf(start.y, end.y)

    val result = mutableListOf<android.graphics.Rect>()
    for (detected in ocrResult.texts) {
        for (element in detected.elements) {
            val bounds = element.bounds ?: continue
            if (!element.isWord) continue

            val center = transformPointToScreen(
                bounds.exactCenterX(), bounds.exactCenterY(),
                bitmap, scale, offset, containerSize
            )
            if (center.x in left..right && center.y in top..bottom) {
                result.add(bounds)
            }
        }
    }
    return result
}
