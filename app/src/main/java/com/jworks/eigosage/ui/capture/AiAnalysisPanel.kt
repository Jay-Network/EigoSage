package com.jworks.eigosage.ui.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.jworks.eigosage.R
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.eigosage.domain.ai.AiResponse
import com.jworks.eigosage.domain.models.CefrLevel
import com.jworks.eigosage.domain.models.DifficultyLevel
import com.jworks.eigosage.domain.models.EnrichedWord
import com.jworks.eigosage.domain.models.ReadabilityMetrics
import com.jworks.eigosage.ui.theme.GlassBorder
import com.jworks.eigosage.ui.theme.GlassGradient
import com.jworks.eigosage.ui.theme.glassCardColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAnalysisPanel(
    selectedText: String,
    scopeLevel: ScopeLevel,
    response: AiResponse,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    readability: ReadabilityMetrics? = null,
    enrichedWords: List<EnrichedWord> = emptyList(),
    onWordClick: (String) -> Unit = {},
    interactionMode: InteractionMode = InteractionMode.TAP,
    onInteractionModeChange: (InteractionMode) -> Unit = {},
    onChatClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Filter difficult words for the Words tab
    val difficultWords = remember(enrichedWords) {
        enrichedWords
            .filter { it.cefr != null && it.cefr.ordinalIndex >= CefrLevel.B1.ordinalIndex }
            .distinctBy { it.text }
    }
    val hasWords = difficultWords.isNotEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        AiPanelHeader(
            scopeLevel = scopeLevel,
            provider = response.provider,
            onDismiss = onDismiss,
            interactionMode = interactionMode,
            onInteractionModeChange = onInteractionModeChange
        )

        // Tabs
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (hasWords) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Words") }
                )
            }
            Tab(
                selected = selectedTab == (if (hasWords) 1 else 0),
                onClick = { selectedTab = if (hasWords) 1 else 0 },
                text = { Text("Summary") }
            )
            Tab(
                selected = selectedTab == (if (hasWords) 2 else 1),
                onClick = { selectedTab = if (hasWords) 2 else 1 },
                text = { Text("Full Text") }
            )
            Tab(
                selected = selectedTab == (if (hasWords) 3 else 2),
                onClick = { selectedTab = if (hasWords) 3 else 2 },
                text = { Text("Readability") }
            )
            Tab(
                selected = selectedTab == (if (hasWords) 4 else 3),
                onClick = { selectedTab = if (hasWords) 4 else 3 },
                text = { Text("Statistics") }
            )
        }

        // Content — adjust tab indices based on whether Words tab exists
        val summaryIdx = if (hasWords) 1 else 0
        val fullTextIdx = if (hasWords) 2 else 1
        val readabilityIdx = if (hasWords) 3 else 2
        val statisticsIdx = if (hasWords) 4 else 3

        when (selectedTab) {
            0 -> if (hasWords) {
                // Words tab
                DifficultWordsPanel(
                    words = difficultWords,
                    threshold = CefrLevel.B1,
                    onWordClick = onWordClick,
                    onDismiss = onDismiss,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Summary (when no Words tab)
                SummaryContent(response = response, modifier = Modifier.weight(1f))
            }
            summaryIdx -> if (hasWords) {
                SummaryContent(response = response, modifier = Modifier.weight(1f))
            }
            fullTextIdx -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = selectedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            readabilityIdx -> {
                ReadabilityTab(
                    readability = readability,
                    modifier = Modifier.weight(1f)
                )
            }
            statisticsIdx -> {
                StatisticsTab(
                    readability = readability,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Footer
        AiPanelFooter(
            processingTimeMs = response.processingTimeMs,
            tokensUsed = response.tokensUsed,
            onChatClick = onChatClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiLoadingPanel(
    selectedText: String,
    scopeLevel: ScopeLevel,
    modifier: Modifier = Modifier,
    readability: ReadabilityMetrics? = null,
    onDismiss: () -> Unit = {},
    enrichedWords: List<EnrichedWord> = emptyList(),
    onWordClick: (String) -> Unit = {},
    interactionMode: InteractionMode = InteractionMode.TAP,
    onInteractionModeChange: (InteractionMode) -> Unit = {}
) {
    // Start on Words tab (index 0) so user can study while AI loads
    var selectedTab by remember { mutableIntStateOf(0) }

    val difficultWords = remember(enrichedWords) {
        enrichedWords
            .filter { it.cefr != null && it.cefr.ordinalIndex >= CefrLevel.B1.ordinalIndex }
            .distinctBy { it.text }
    }
    val hasWords = difficultWords.isNotEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        AiPanelHeader(
            scopeLevel = scopeLevel,
            provider = "Loading...",
            onDismiss = onDismiss,
            interactionMode = interactionMode,
            onInteractionModeChange = onInteractionModeChange
        )

        // Tabs — Words first (if available), then AI loading tabs
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (hasWords) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Words") }
                )
            }
            Tab(
                selected = selectedTab == (if (hasWords) 1 else 0),
                onClick = { selectedTab = if (hasWords) 1 else 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Summary")
                    }
                }
            )
            Tab(
                selected = selectedTab == (if (hasWords) 2 else 1),
                onClick = { selectedTab = if (hasWords) 2 else 1 },
                text = { Text("Full Text") }
            )
            Tab(
                selected = selectedTab == (if (hasWords) 3 else 2),
                onClick = { selectedTab = if (hasWords) 3 else 2 },
                text = { Text("Readability") }
            )
        }

        val summaryIdx = if (hasWords) 1 else 0
        val fullTextIdx = if (hasWords) 2 else 1
        val readabilityIdx = if (hasWords) 3 else 2

        when (selectedTab) {
            0 -> if (hasWords) {
                DifficultWordsPanel(
                    words = difficultWords,
                    threshold = CefrLevel.B1,
                    onWordClick = onWordClick,
                    onDismiss = onDismiss,
                    modifier = Modifier.weight(1f),
                    isAiLoading = true
                )
            } else {
                // Summary loading placeholder
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Analyzing...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            summaryIdx -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Analyzing...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            fullTextIdx -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = selectedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            readabilityIdx -> {
                ReadabilityTab(
                    readability = readability,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryContent(response: AiResponse, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val sections = parseAiSections(response.content)
        items(sections) { section ->
            AiSectionCard(section)
        }
    }
}

// -- Header --

@Composable
private fun AiPanelHeader(
    scopeLevel: ScopeLevel,
    provider: String,
    onDismiss: () -> Unit,
    interactionMode: InteractionMode = InteractionMode.TAP,
    onInteractionModeChange: (InteractionMode) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScopeBadge(scopeLevel)
            Spacer(Modifier.width(8.dp))
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = provider,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        // Lasso selection toggle
        IconButton(
            onClick = {
                val newMode = if (interactionMode == InteractionMode.TAP) {
                    InteractionMode.CIRCLE
                } else {
                    InteractionMode.TAP
                }
                onInteractionModeChange(newMode)
            },
            modifier = Modifier.size(36.dp)
        ) {
            if (interactionMode == InteractionMode.CIRCLE) {
                Icon(
                    painter = painterResource(R.drawable.ic_tap),
                    contentDescription = "Switch to tap mode",
                    tint = Color(0xFFFFEB3B),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CropFree,
                    contentDescription = "Switch to select mode",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// -- Quote card --

@Composable
private fun SelectedQuoteCard(selectedText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        Text(
            text = "\u201C${selectedText}\u201D",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// -- Section card --

private data class AiSection(val title: String, val bodyLines: List<String>)

private fun parseAiSections(raw: String): List<AiSection> {
    val lines = raw.lines()
    val out = mutableListOf<AiSection>()
    var currentTitle = "Analysis"
    val currentBody = mutableListOf<String>()

    fun flush() {
        if (currentBody.isNotEmpty()) {
            out += AiSection(currentTitle, currentBody.toList())
            currentBody.clear()
        }
    }

    lines.forEach { line ->
        when {
            line.startsWith("### ") -> {
                flush()
                currentTitle = line.removePrefix("### ").trim()
            }
            line.startsWith("## ") -> {
                flush()
                currentTitle = line.removePrefix("## ").trim()
            }
            line.startsWith("# ") -> {
                flush()
                currentTitle = line.removePrefix("# ").trim()
            }
            else -> currentBody += line
        }
    }
    flush()
    return out.ifEmpty { listOf(AiSection("Analysis", lines)) }
}

@Composable
private fun AiSectionCard(section: AiSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = glassCardColors(),
        border = GlassBorder,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.background(GlassGradient).padding(14.dp)) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (line in section.bodyLines) {
                    when {
                        line.isBlank() -> Spacer(modifier = Modifier.height(2.dp))
                        line.startsWith("- ") || line.startsWith("* ") -> {
                            Row(modifier = Modifier.padding(start = 4.dp)) {
                                Text(
                                    text = "\u2022  ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                StyledText(
                                    text = line.removePrefix("- ").removePrefix("* "),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        else -> {
                            StyledText(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

// -- Footer --

@Composable
private fun AiPanelFooter(
    processingTimeMs: Long,
    tokensUsed: Int?,
    onChatClick: () -> Unit = {}
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chat with Claude button
        AssistChip(
            onClick = onChatClick,
            label = {
                Text(
                    text = "Chat",
                    style = MaterialTheme.typography.labelSmall
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = "${processingTimeMs}ms",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            tokensUsed?.let {
                Spacer(Modifier.width(8.dp))
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = "$it tokens",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

// -- Shared components --

@Composable
private fun ScopeBadge(scopeLevel: ScopeLevel) {
    val (label, color) = when (scopeLevel) {
        is ScopeLevel.Word -> "Word" to MaterialTheme.colorScheme.primary
        is ScopeLevel.Phrase -> "Phrase" to MaterialTheme.colorScheme.secondary
        is ScopeLevel.Sentence -> "Sentence" to MaterialTheme.colorScheme.secondary
        is ScopeLevel.Paragraph -> "Paragraph" to MaterialTheme.colorScheme.secondary
        is ScopeLevel.FullSnapshot -> "Full Text" to MaterialTheme.colorScheme.tertiary
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StyledText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    val annotated = buildAnnotatedString {
        var remaining = text
        while (remaining.contains("**")) {
            val start = remaining.indexOf("**")
            val end = remaining.indexOf("**", start + 2)
            if (end == -1) {
                append(remaining)
                remaining = ""
                break
            }
            append(remaining.substring(0, start))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(remaining.substring(start + 2, end))
            }
            remaining = remaining.substring(end + 2)
        }
        append(remaining)
    }

    Text(
        text = annotated,
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

// -- Readability Tab --

@Composable
private fun ReadabilityTab(
    readability: ReadabilityMetrics?,
    modifier: Modifier = Modifier
) {
    if (readability == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Text too short for readability analysis",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Grade level card
        val diffColor = difficultyColor(readability.difficulty)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(diffColor.copy(alpha = 0.15f))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Grade Level",
                    style = MaterialTheme.typography.labelMedium,
                    color = diffColor
                )
                Text(
                    text = "%.1f".format(readability.averageGrade),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = diffColor
                )
                Text(
                    text = readability.difficulty.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = diffColor
                )
                Text(
                    text = readability.targetAudience,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Text(
            text = "Scores",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ScoreRow("Flesch-Kincaid Grade", "%.1f".format(readability.fleschKincaidGrade), "U.S. grade level")
        ScoreRow("Reading Ease", "%.0f".format(readability.fleschReadingEase), readingEaseLabel(readability.fleschReadingEase))
        ScoreRow("SMOG Index", "%.1f".format(readability.smogIndex), "Education years needed")
        ScoreRow("Coleman-Liau", "%.1f".format(readability.colemanLiauIndex), "Character-based grade")
    }
}

// -- Statistics Tab --

@Composable
private fun StatisticsTab(
    readability: ReadabilityMetrics?,
    modifier: Modifier = Modifier
) {
    if (readability == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Text too short for statistics",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Text Statistics",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        StatRow("Words", readability.statistics.totalWords.toString())
        StatRow("Sentences", readability.statistics.totalSentences.toString())
        StatRow("Syllables", readability.statistics.totalSyllables.toString())
        StatRow("Avg words/sentence", "%.1f".format(readability.statistics.averageWordsPerSentence))
        StatRow("Avg syllables/word", "%.2f".format(readability.statistics.averageSyllablesPerWord))
        StatRow("Polysyllable words", readability.statistics.polysyllableCount.toString())
    }
}

@Composable
private fun ScoreRow(name: String, value: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun difficultyColor(level: DifficultyLevel): Color = when (level) {
    DifficultyLevel.VERY_EASY -> Color(0xFF1A73E8)
    DifficultyLevel.EASY -> Color(0xFF4285F4)
    DifficultyLevel.MODERATE -> Color(0xFF5F6368)
    DifficultyLevel.DIFFICULT -> Color(0xFF80868B)
    DifficultyLevel.VERY_DIFFICULT -> Color(0xFFADB0B5)
}

private fun readingEaseLabel(score: Double): String = when {
    score >= 90 -> "Very Easy"
    score >= 80 -> "Easy"
    score >= 70 -> "Fairly Easy"
    score >= 60 -> "Standard"
    score >= 50 -> "Fairly Difficult"
    score >= 30 -> "Difficult"
    else -> "Very Difficult"
}
