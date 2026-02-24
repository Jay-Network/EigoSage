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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.graphics.Brush
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
import com.jworks.eigolens.domain.ai.AiResponse

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
    val isCorrectingOcr by viewModel.isCorrectingOcr.collectAsState()
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
            onWordLongPressed = { tapResult -> viewModel.analyzeSentenceForWord(tapResult) },
            tappedWord = tappedWord,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )

        // Back button with scrim circle for visibility
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back to camera",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // Top scrim gradient for readability over camera content
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        )

        // Word count badge + OCR correction status
        val wordCount = capturedImage.ocrResult.texts.sumOf { it.elements.size }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCorrectingOcr) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = if (isCorrectingOcr) "$wordCount words · enhancing..."
                       else "$wordCount words detected",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }

        // Analysis FABs column (bottom-start, shifts up when panel open)
        if (!showPanel || (panelState !is PanelState.ReadabilityResult && panelState !is PanelState.AiAnalysis)) {
            Column(
                modifier = Modifier
                    .align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomStart)
                    .padding(16.dp)
                    .then(
                        if (showPanel && !isLandscape) Modifier.padding(bottom = 280.dp)
                        else Modifier
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Full Text AI analysis
                ExtendedFloatingActionButton(
                    onClick = { viewModel.analyzeFullText() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    icon = {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null
                        )
                    },
                    text = { Text("AI Analyze") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Readability analysis
                ExtendedFloatingActionButton(
                    onClick = { viewModel.analyzeReadability() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_read),
                            contentDescription = null
                        )
                    },
                    text = { Text("Reading Level") }
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
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
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
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
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
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
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
                    modifier = Modifier.fillMaxSize(),
                    contextualInsight = state.contextualInsight
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
            is PanelState.AiLoading -> {
                AiLoadingPanel(
                    selectedText = state.selectedText,
                    scopeLevel = state.scopeLevel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is PanelState.AiAnalysis -> {
                AiAnalysisPanel(
                    selectedText = state.selectedText,
                    scopeLevel = state.scopeLevel,
                    response = state.response,
                    onDismiss = onDismiss,
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
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
