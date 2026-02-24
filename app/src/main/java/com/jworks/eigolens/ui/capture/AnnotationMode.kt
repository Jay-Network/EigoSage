package com.jworks.eigolens.ui.capture

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jworks.eigolens.R
import com.jworks.eigolens.ui.camera.DefinitionPanel
import com.jworks.eigolens.ui.camera.DefinitionSkeleton
import com.jworks.eigolens.ui.camera.ReadabilityPanel
import kotlin.math.roundToInt

@Composable
fun AnnotationMode(
    capturedImage: CapturedImage,
    viewModel: CaptureFlowViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val panelState by viewModel.panelState.collectAsState()
    val interactionMode by viewModel.interactionMode.collectAsState()
    val tappedWord by viewModel.tappedWord.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val showPanel = panelState !is PanelState.Idle

    Box(modifier = modifier.fillMaxSize()) {
        // Full-screen image viewer (always full size)
        InteractiveImageViewer(
            capturedImage = capturedImage,
            interactionMode = interactionMode,
            onInteractionModeChange = { viewModel.setInteractionMode(it) },
            onWordsSelected = { words -> viewModel.selectWords(words) },
            onWordTapped = { tapResult -> viewModel.onWordTapped(tapResult) },
            tappedWord = tappedWord,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back to camera",
                tint = Color.White
            )
        }

        // Word count badge
        val wordCount = capturedImage.ocrResult.texts.sumOf { it.elements.size }
        Text(
            text = "$wordCount words detected",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Readability FAB (visible when panel is not showing readability)
        if (!showPanel || panelState !is PanelState.ReadabilityResult) {
            FloatingActionButton(
                onClick = { viewModel.analyzeReadability() },
                modifier = Modifier
                    .align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomStart)
                    .padding(16.dp)
                    .then(
                        if (showPanel && !isLandscape) Modifier.padding(bottom = 280.dp)
                        else Modifier
                    ),
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_read),
                    contentDescription = "Readability analysis"
                )
            }
        }

        // Overlay panel — slides in from bottom (portrait) or side (landscape)
        if (isLandscape) {
            LandscapePanel(
                panelState = panelState,
                showPanel = showPanel,
                onDismiss = { viewModel.switchToWordLookup() }
            )
        } else {
            PortraitPanel(
                panelState = panelState,
                showPanel = showPanel,
                onDismiss = { viewModel.switchToWordLookup() }
            )
        }
    }
}

@Composable
private fun PortraitPanel(
    panelState: PanelState,
    showPanel: Boolean,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val minHeightPx = with(density) { 200.dp.toPx() }
    val maxHeightPx = screenHeightPx * 0.85f
    val defaultHeightPx = screenHeightPx * 0.45f

    // Draggable panel height (offset from bottom)
    var panelHeightPx by remember { mutableFloatStateOf(defaultHeightPx) }

    AnimatedVisibility(
        visible = showPanel,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(with(density) { panelHeightPx.toDp() })
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color(0xFFF8F9FA).copy(alpha = 0.97f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Drag handle
                    DragHandle(
                        onDrag = { deltaY ->
                            panelHeightPx = (panelHeightPx - deltaY)
                                .coerceIn(minHeightPx, maxHeightPx)
                        }
                    )

                    // Panel content
                    PanelContent(
                        panelState = panelState,
                        onDismiss = onDismiss,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LandscapePanel(
    panelState: PanelState,
    showPanel: Boolean,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val minWidthPx = with(density) { 200.dp.toPx() }
    val maxWidthPx = screenWidthPx * 0.6f
    val defaultWidthPx = screenWidthPx * 0.4f

    var panelWidthPx by remember { mutableFloatStateOf(defaultWidthPx) }

    AnimatedVisibility(
        visible = showPanel,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(with(density) { panelWidthPx.toDp() })
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .background(Color(0xFFF8F9FA).copy(alpha = 0.97f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Drag handle (horizontal in landscape)
                    DragHandle(
                        onDrag = { deltaY ->
                            // In landscape, vertical drag on handle resizes width
                            panelWidthPx = (panelWidthPx + deltaY)
                                .coerceIn(minWidthPx, maxWidthPx)
                        }
                    )

                    PanelContent(
                        panelState = panelState,
                        onDismiss = onDismiss,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DragHandle(onDrag: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.y)
                }
            }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.Gray.copy(alpha = 0.4f))
        )
    }
}

@Composable
private fun PanelContent(
    panelState: PanelState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (val state = panelState) {
            is PanelState.WordDefinition -> {
                DefinitionPanel(
                    definition = state.definition,
                    onDismiss = onDismiss,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is PanelState.Loading -> {
                DefinitionSkeleton(modifier = Modifier.fillMaxSize())
            }
            is PanelState.ReadabilityResult -> {
                ReadabilityPanel(
                    metrics = state.metrics,
                    onBack = onDismiss,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is PanelState.NotFound -> {
                InstructionsPanel(
                    message = "No definition found for \"${state.word}\"",
                    isError = true
                )
            }
            is PanelState.Error -> {
                InstructionsPanel(
                    message = state.message,
                    isError = true
                )
            }
            is PanelState.Idle -> {
                // Should not be visible, but handle gracefully
            }
        }
    }
}

@Composable
private fun InstructionsPanel(
    message: String,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(
                    if (isError) R.drawable.ic_search else R.drawable.ic_tap
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .padding(bottom = 16.dp),
                tint = if (isError) Color(0xFFE57373) else Color(0xFF90A4AE)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isError) Color(0xFFE57373) else Color(0xFF78909C),
                textAlign = TextAlign.Center
            )
        }
    }
}
