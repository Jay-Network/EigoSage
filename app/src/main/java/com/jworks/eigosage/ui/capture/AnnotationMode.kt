package com.jworks.eigosage.ui.capture

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jworks.eigosage.R
import com.jworks.eigosage.domain.models.EnrichedWord
import com.jworks.eigosage.ui.camera.DefinitionPanel
import com.jworks.eigosage.ui.camera.DefinitionSkeleton

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
    val isBookmarked by viewModel.isCurrentWordBookmarked.collectAsState()
    val enrichedWords by viewModel.enrichedWords.collectAsState()
    val cefrThreshold by viewModel.cefrThreshold.collectAsState()
    val isSendingToEigoQuest by viewModel.isSendingToEigoQuest.collectAsState()
    val eiGoQuestSendResult by viewModel.eiGoQuestSendResult.collectAsState()
    val showIpa by viewModel.showIpaOverlay.collectAsState()
    val ipaFontScale by viewModel.ipaFontScale.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val handleWordClick: (String) -> Unit = { word ->
        val cleanWord = word.replace(Regex("[^a-zA-Z'-]"), "").trim()
        if (cleanWord.isNotEmpty()) {
            viewModel.selectWords(listOf(cleanWord))
        }
    }
    val handleChatClick = { viewModel.startChat() }
    val handleSendChatMessage: (String) -> Unit = { viewModel.sendChatMessage(it) }
    val handleDismissChat = { viewModel.dismissChat() }

    Box(modifier = modifier.fillMaxSize()) {
        // Full-screen image viewer (always full size)
        InteractiveImageViewer(
            capturedImage = capturedImage,
            interactionMode = interactionMode,
            onInteractionModeChange = { viewModel.setInteractionMode(it) },
            onWordsSelected = { words, rect -> viewModel.selectWords(words, rect) },
            onWordTapped = { tapResult -> viewModel.onWordTapped(tapResult) },
            onWordLongPressed = { tapResult -> viewModel.analyzeSentenceForWord(tapResult) },
            tappedWord = tappedWord,
            enrichedWords = enrichedWords,
            cefrThreshold = cefrThreshold,
            showIpa = showIpa,
            ipaFontScale = ipaFontScale,
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

        // Top scrim gradient
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

        // Always-visible draggable panel (portrait: bottom, landscape: right)
        if (isLandscape) {
            LandscapePanel(
                panelState = panelState,
                onDismiss = { viewModel.switchToWordLookup() },
                isBookmarked = isBookmarked,
                onToggleBookmark = { viewModel.toggleBookmark() },
                onWordClick = handleWordClick,
                enrichedWords = enrichedWords,
                onAiAnalyze = { viewModel.analyzeFullText() },
                interactionMode = interactionMode,
                onInteractionModeChange = { viewModel.setInteractionMode(it) },
                onBackToWords = { viewModel.showDifficultWordsPanel() },
                onSendToEigoQuest = { viewModel.sendToEigoQuest() },
                isSendingToEigoQuest = isSendingToEigoQuest,
                eiGoQuestSendResult = eiGoQuestSendResult,
                onChatClick = handleChatClick,
                onSendChatMessage = handleSendChatMessage,
                onDismissChat = handleDismissChat
            )
        } else {
            PortraitPanel(
                panelState = panelState,
                onDismiss = { viewModel.switchToWordLookup() },
                isBookmarked = isBookmarked,
                onToggleBookmark = { viewModel.toggleBookmark() },
                onWordClick = handleWordClick,
                enrichedWords = enrichedWords,
                onAiAnalyze = { viewModel.analyzeFullText() },
                interactionMode = interactionMode,
                onInteractionModeChange = { viewModel.setInteractionMode(it) },
                onBackToWords = { viewModel.showDifficultWordsPanel() },
                onSendToEigoQuest = { viewModel.sendToEigoQuest() },
                isSendingToEigoQuest = isSendingToEigoQuest,
                eiGoQuestSendResult = eiGoQuestSendResult,
                onChatClick = handleChatClick,
                onSendChatMessage = handleSendChatMessage,
                onDismissChat = handleDismissChat
            )
        }
    }
}

@Composable
private fun PortraitPanel(
    panelState: PanelState,
    onDismiss: () -> Unit,
    isBookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    onWordClick: (String) -> Unit = {},
    enrichedWords: List<EnrichedWord> = emptyList(),
    onAiAnalyze: () -> Unit = {},
    interactionMode: InteractionMode = InteractionMode.TAP,
    onInteractionModeChange: (InteractionMode) -> Unit = {},
    onBackToWords: () -> Unit = {},
    onSendToEigoQuest: () -> Unit = {},
    isSendingToEigoQuest: Boolean = false,
    eiGoQuestSendResult: String? = null,
    onChatClick: () -> Unit = {},
    onSendChatMessage: (String) -> Unit = {},
    onDismissChat: () -> Unit = {}
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val handleHeightPx = with(density) { 28.dp.toPx() }
    val minHeightPx = handleHeightPx // collapsed = just the handle bar
    val maxHeightPx = screenHeightPx * 0.85f
    val defaultHeightPx = if (panelState is PanelState.Idle) handleHeightPx
                          else screenHeightPx * 0.45f

    var panelHeightPx by remember { mutableFloatStateOf(defaultHeightPx) }

    // Expand when content arrives if currently collapsed
    val hasContent = panelState !is PanelState.Idle
    val isChat = panelState is PanelState.Chat
    androidx.compose.runtime.LaunchedEffect(hasContent, isChat) {
        if (isChat) {
            panelHeightPx = screenHeightPx * 0.70f
        } else if (hasContent && panelHeightPx < with(density) { 200.dp.toPx() }) {
            panelHeightPx = screenHeightPx * 0.45f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(with(density) { panelHeightPx.toDp() })
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drag handle — always visible
                DragHandle(
                    onDrag = { deltaY ->
                        panelHeightPx = (panelHeightPx - deltaY)
                            .coerceIn(minHeightPx, maxHeightPx)
                    }
                )

                // Panel content (only if expanded enough)
                if (panelHeightPx > with(density) { 60.dp.toPx() }) {
                    PanelContent(
                        panelState = panelState,
                        onDismiss = onDismiss,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        isBookmarked = isBookmarked,
                        onToggleBookmark = onToggleBookmark,
                        onWordClick = onWordClick,
                        enrichedWords = enrichedWords,
                        onAiAnalyze = onAiAnalyze,
                        interactionMode = interactionMode,
                        onInteractionModeChange = onInteractionModeChange,
                        onBackToWords = onBackToWords,
                        onSendToEigoQuest = onSendToEigoQuest,
                        isSendingToEigoQuest = isSendingToEigoQuest,
                        eiGoQuestSendResult = eiGoQuestSendResult,
                        onChatClick = onChatClick,
                        onSendChatMessage = onSendChatMessage,
                        onDismissChat = onDismissChat
                    )
                }
            }
        }
    }
}

@Composable
private fun LandscapePanel(
    panelState: PanelState,
    onDismiss: () -> Unit,
    isBookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    onWordClick: (String) -> Unit = {},
    enrichedWords: List<EnrichedWord> = emptyList(),
    onAiAnalyze: () -> Unit = {},
    interactionMode: InteractionMode = InteractionMode.TAP,
    onInteractionModeChange: (InteractionMode) -> Unit = {},
    onBackToWords: () -> Unit = {},
    onSendToEigoQuest: () -> Unit = {},
    isSendingToEigoQuest: Boolean = false,
    eiGoQuestSendResult: String? = null,
    onChatClick: () -> Unit = {},
    onSendChatMessage: (String) -> Unit = {},
    onDismissChat: () -> Unit = {}
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val handleWidthPx = with(density) { 24.dp.toPx() }
    val minWidthPx = handleWidthPx
    val maxWidthPx = screenWidthPx * 0.65f
    val defaultWidthPx = if (panelState is PanelState.Idle) handleWidthPx
                         else screenWidthPx * 0.45f

    var panelWidthPx by remember { mutableFloatStateOf(defaultWidthPx) }

    val hasContent = panelState !is PanelState.Idle
    androidx.compose.runtime.LaunchedEffect(hasContent) {
        if (hasContent && panelWidthPx < with(density) { 200.dp.toPx() }) {
            panelWidthPx = screenWidthPx * 0.45f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(with(density) { panelWidthPx.toDp() })
        ) {
            // Vertical drag handle — always visible
            VerticalDragHandle(
                onDrag = { deltaX ->
                    panelWidthPx = (panelWidthPx - deltaX)
                        .coerceIn(minWidthPx, maxWidthPx)
                }
            )

            if (panelWidthPx > with(density) { 60.dp.toPx() }) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                ) {
                    PanelContent(
                        panelState = panelState,
                        onDismiss = onDismiss,
                        modifier = Modifier.fillMaxSize(),
                        isBookmarked = isBookmarked,
                        onToggleBookmark = onToggleBookmark,
                        onWordClick = onWordClick,
                        enrichedWords = enrichedWords,
                        onAiAnalyze = onAiAnalyze,
                        interactionMode = interactionMode,
                        onInteractionModeChange = onInteractionModeChange,
                        onBackToWords = onBackToWords,
                        onSendToEigoQuest = onSendToEigoQuest,
                        isSendingToEigoQuest = isSendingToEigoQuest,
                        eiGoQuestSendResult = eiGoQuestSendResult,
                        onChatClick = onChatClick,
                        onSendChatMessage = onSendChatMessage,
                        onDismissChat = onDismissChat
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
private fun VerticalDragHandle(onDrag: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(20.dp)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        )
    }
}

@Composable
private fun PanelContent(
    panelState: PanelState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    onWordClick: (String) -> Unit = {},
    enrichedWords: List<EnrichedWord> = emptyList(),
    onAiAnalyze: () -> Unit = {},
    interactionMode: InteractionMode = InteractionMode.TAP,
    onInteractionModeChange: (InteractionMode) -> Unit = {},
    onBackToWords: () -> Unit = {},
    onSendToEigoQuest: () -> Unit = {},
    isSendingToEigoQuest: Boolean = false,
    eiGoQuestSendResult: String? = null,
    onChatClick: () -> Unit = {},
    onSendChatMessage: (String) -> Unit = {},
    onDismissChat: () -> Unit = {}
) {
    Box(modifier = modifier) {
        when (val state = panelState) {
            is PanelState.WordDefinition -> {
                DefinitionPanel(
                    definition = state.definition,
                    onDismiss = onDismiss,
                    modifier = Modifier.fillMaxSize(),
                    contextualInsight = state.contextualInsight,
                    isBookmarked = isBookmarked,
                    onToggleBookmark = onToggleBookmark,
                    onBackToWords = onBackToWords,
                    onChatClick = onChatClick
                )
            }
            is PanelState.Loading -> {
                DefinitionSkeleton(modifier = Modifier.fillMaxSize())
            }
            is PanelState.ReadabilityResult -> {
                // Readability is now shown as tabs in AiAnalysisPanel
            }
            is PanelState.DifficultWordsList -> {
                DifficultWordsPanel(
                    words = state.words,
                    threshold = state.threshold,
                    onWordClick = onWordClick,
                    onDismiss = onDismiss,
                    modifier = Modifier.fillMaxSize(),
                    onAiAnalyze = onAiAnalyze,
                    interactionMode = interactionMode,
                    onInteractionModeChange = onInteractionModeChange,
                    onSendToEigoQuest = onSendToEigoQuest,
                    isSendingToEigoQuest = isSendingToEigoQuest,
                    eiGoQuestSendResult = eiGoQuestSendResult,
                    onChatClick = onChatClick
                )
            }
            is PanelState.AiLoading -> {
                AiLoadingPanel(
                    selectedText = state.selectedText,
                    scopeLevel = state.scopeLevel,
                    readability = state.readability,
                    onDismiss = onDismiss,
                    modifier = Modifier.fillMaxSize(),
                    enrichedWords = enrichedWords,
                    onWordClick = onWordClick,
                    interactionMode = interactionMode,
                    onInteractionModeChange = onInteractionModeChange
                )
            }
            is PanelState.AiAnalysis -> {
                AiAnalysisPanel(
                    selectedText = state.selectedText,
                    scopeLevel = state.scopeLevel,
                    response = state.response,
                    readability = state.readability,
                    onDismiss = onDismiss,
                    modifier = Modifier.fillMaxSize(),
                    enrichedWords = enrichedWords,
                    onWordClick = onWordClick,
                    interactionMode = interactionMode,
                    onInteractionModeChange = onInteractionModeChange,
                    onChatClick = onChatClick
                )
            }
            is PanelState.Chat -> {
                ChatPanel(
                    messages = state.messages,
                    isLoading = state.isLoading,
                    onSendMessage = onSendChatMessage,
                    onDismiss = onDismissChat,
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
                // Empty — handle bar is still visible
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
