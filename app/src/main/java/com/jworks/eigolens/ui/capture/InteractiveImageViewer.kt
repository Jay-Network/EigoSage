package com.jworks.eigolens.ui.capture

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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.jworks.eigolens.R

@Composable
fun InteractiveImageViewer(
    capturedImage: CapturedImage,
    interactionMode: InteractionMode,
    onInteractionModeChange: (InteractionMode) -> Unit,
    onWordsSelected: (List<String>) -> Unit,
    onWordTapped: (TapResult) -> Unit,
    tappedWord: TapResult?,
    modifier: Modifier = Modifier
) {
    val bitmap = capturedImage.bitmap
    val ocrResult = capturedImage.ocrResult

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var lassoPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
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
                            // Single-finger tap to find words
                            Modifier.pointerInput(Unit) {
                                detectTapGestures { tapOffset ->
                                    val result = findTappedWord(
                                        tapOffset, ocrResult, bitmap,
                                        scale, offset, containerSize
                                    )
                                    if (result != null) {
                                        selectedWordBounds = emptyList()
                                        onWordTapped(result)
                                    }
                                }
                            }
                        }
                        InteractionMode.CIRCLE -> {
                            // Single-finger drag to draw lasso
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { startPoint ->
                                        lassoPoints = listOf(startPoint)
                                        selectedWordBounds = emptyList()
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        lassoPoints = lassoPoints + change.position
                                    },
                                    onDragEnd = {
                                        if (lassoPoints.size >= 3) {
                                            val words = findWordsInLasso(
                                                lassoPoints, ocrResult, bitmap,
                                                scale, offset, containerSize
                                            )
                                            if (words.isNotEmpty()) {
                                                selectedWordBounds = findSelectedBounds(
                                                    lassoPoints, ocrResult, bitmap,
                                                    scale, offset, containerSize
                                                )
                                                onWordsSelected(words)
                                            }
                                        }
                                        lassoPoints = emptyList()
                                        // Auto-switch back to TAP after lasso selection
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

            // Layer 2: OCR bounding boxes + highlights + lasso
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw OCR word bounding boxes
                for (detected in ocrResult.texts) {
                    for (element in detected.elements) {
                        val bounds = element.bounds ?: continue
                        val screenRect = transformBoundsToScreen(
                            bounds, bitmap, scale, offset, containerSize
                        )
                        drawRect(
                            color = Color(0xFF2196F3).copy(alpha = 0.25f),
                            topLeft = Offset(screenRect.left, screenRect.top),
                            size = Size(screenRect.width, screenRect.height),
                            style = Stroke(width = 1.5f)
                        )
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

                // Draw lasso path while drawing
                if (lassoPoints.size >= 2) {
                    val path = Path().apply {
                        moveTo(lassoPoints.first().x, lassoPoints.first().y)
                        for (i in 1 until lassoPoints.size) {
                            lineTo(lassoPoints[i].x, lassoPoints[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFFFEB3B),
                        style = Stroke(width = 3f)
                    )
                }
            }
        }

        // Mode toggle FAB: TAP ↔ CIRCLE
        FloatingActionButton(
            onClick = {
                val newMode = if (interactionMode == InteractionMode.TAP) {
                    InteractionMode.CIRCLE
                } else {
                    InteractionMode.TAP
                }
                onInteractionModeChange(newMode)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(40.dp),
            containerColor = if (interactionMode == InteractionMode.CIRCLE)
                Color(0xFFFFEB3B) else Color.Black.copy(alpha = 0.6f),
            contentColor = if (interactionMode == InteractionMode.CIRCLE)
                Color.Black else Color.White
        ) {
            Icon(
                painter = painterResource(
                    if (interactionMode == InteractionMode.CIRCLE) R.drawable.ic_tap
                    else R.drawable.ic_circle
                ),
                contentDescription = if (interactionMode == InteractionMode.CIRCLE)
                    "Switch to tap mode" else "Switch to circle mode",
                modifier = Modifier.size(20.dp)
            )
        }

        // Mode indicator
        if (interactionMode == InteractionMode.CIRCLE) {
            Text(
                text = "Draw around words",
                color = Color(0xFFFFEB3B),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }
    }
}

/** Collect the bounding boxes of words hit by the lasso for highlighting */
private fun findSelectedBounds(
    lassoPoints: List<Offset>,
    ocrResult: com.jworks.eigolens.domain.models.OCRResult,
    bitmap: android.graphics.Bitmap,
    scale: Float,
    offset: Offset,
    containerSize: Size
): List<android.graphics.Rect> {
    if (lassoPoints.size < 3) return emptyList()

    val path = android.graphics.Path().apply {
        moveTo(lassoPoints.first().x, lassoPoints.first().y)
        for (i in 1 until lassoPoints.size) {
            lineTo(lassoPoints[i].x, lassoPoints[i].y)
        }
        close()
    }

    val clipRegion = android.graphics.Region(
        0, 0, containerSize.width.toInt(), containerSize.height.toInt()
    )
    val lassoRegion = android.graphics.Region()
    lassoRegion.setPath(path, clipRegion)

    val result = mutableListOf<android.graphics.Rect>()
    for (detected in ocrResult.texts) {
        for (element in detected.elements) {
            val bounds = element.bounds ?: continue
            if (!element.isWord) continue

            val center = transformPointToScreen(
                bounds.exactCenterX(), bounds.exactCenterY(),
                bitmap, scale, offset, containerSize
            )
            if (lassoRegion.contains(center.x.toInt(), center.y.toInt())) {
                result.add(bounds)
            }
        }
    }
    return result
}
