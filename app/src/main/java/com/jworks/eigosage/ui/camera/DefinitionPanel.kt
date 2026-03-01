package com.jworks.eigosage.ui.camera

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import com.jworks.eigosage.data.ai.ContextualInsight
import com.jworks.eigosage.domain.models.CefrLevel
import com.jworks.eigosage.domain.models.Definition
import com.jworks.eigosage.domain.models.Meaning
import com.jworks.eigosage.domain.models.PartOfSpeech
import com.jworks.eigosage.domain.models.color
import com.jworks.eigosage.ui.theme.GlassBorder
import com.jworks.eigosage.ui.theme.GlassGradient
import com.jworks.eigosage.ui.theme.glassCardColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DefinitionPanel(
    definition: Definition,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contextualInsight: ContextualInsight? = null,
    isBookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    onBackToWords: () -> Unit = {},
    onChatClick: () -> Unit = {}
) {
    val slideOffset = remember { Animatable(40f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(definition.word) {
        launch {
            slideOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(300)
            )
        }
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .offset(y = slideOffset.value.dp)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 72.dp)
                .alpha(alpha.value)
        ) {
            // Word header with gradient frequency badge
            WordHeader(
                word = definition.word,
                lemma = definition.lemma,
                frequency = definition.frequency,
                phonetic = definition.phonetic,
                cefrLevel = CefrLevel.fromString(definition.cefrLevel),
                isBookmarked = isBookmarked,
                onToggleBookmark = onToggleBookmark,
                onBackToWords = onBackToWords,
                onChatClick = onChatClick
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Context-aware AI insight (appears when AI responds)
            AnimatedVisibility(
                visible = contextualInsight != null,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn()
            ) {
                contextualInsight?.let { insight ->
                    ContextInsightCard(insight = insight)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Animated POS tag row
            POSTagRow(meanings = definition.meanings)

            Spacer(modifier = Modifier.height(12.dp))

            // Expandable definition cards
            DefinitionList(meanings = definition.meanings)
        }

    }
}

// -- Word Header --

@Composable
private fun WordHeader(
    word: String,
    lemma: String,
    frequency: Int?,
    phonetic: String? = null,
    cefrLevel: CefrLevel? = null,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onBackToWords: () -> Unit = {},
    onChatClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Back arrow to return to difficult words list
        IconButton(
            onClick = onBackToWords,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to word list",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = word,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (phonetic != null) {
                Text(
                    text = phonetic,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            if (lemma != word) {
                Text(
                    text = "base: $lemma",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            cefrLevel?.let { CefrBadge(it) }
            Spacer(modifier = Modifier.width(4.dp))
            frequency?.let { FrequencyBadge(it) }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark word",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onChatClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Chat about this word",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FrequencyBadge(frequency: Int) {
    val (label, color) = when {
        frequency <= 100 -> "Top 100" to MaterialTheme.colorScheme.primary
        frequency <= 1000 -> "Common" to MaterialTheme.colorScheme.secondary
        frequency <= 5000 -> "Moderate" to MaterialTheme.colorScheme.onSurfaceVariant
        else -> "Rare" to MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun CefrBadge(level: CefrLevel) {
    val badgeColor = level.color().takeIf { it != Color.Transparent }
        ?: MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .background(
                color = badgeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = level.name,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

// -- Animated POS Tags --

@Composable
private fun POSTagRow(meanings: List<Meaning>) {
    val posSet = meanings.map { it.partOfSpeech }.distinct()

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(posSet) { index, pos ->
            var visible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(60L * index)
                visible = true
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(200)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
            ) {
                POSTag(pos)
            }
        }
    }
}

@Composable
private fun POSTag(pos: PartOfSpeech) {
    val label = when (pos) {
        PartOfSpeech.NOUN -> "noun"
        PartOfSpeech.VERB -> "verb"
        PartOfSpeech.ADJECTIVE -> "adj"
        PartOfSpeech.ADVERB -> "adv"
        PartOfSpeech.PREPOSITION -> "prep"
        PartOfSpeech.CONJUNCTION -> "conj"
        PartOfSpeech.INTERJECTION -> "intj"
        PartOfSpeech.UNKNOWN -> "other"
    }
    val tagColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .background(
                color = tagColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = tagColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tagColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// -- Expandable Definitions --

@Composable
private fun DefinitionList(meanings: List<Meaning>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        meanings.forEachIndexed { index, meaning ->
            var expanded by remember { mutableStateOf(index == 0) }

            DefinitionCard(
                meaning = meaning,
                index = index + 1,
                expanded = expanded,
                onToggle = { expanded = !expanded }
            )
        }
    }
}

@Composable
private fun DefinitionCard(
    meaning: Meaning,
    index: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val posColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .clickable { onToggle() },
        colors = glassCardColors(),
        border = GlassBorder,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.background(GlassGradient).padding(14.dp)) {
            // Header row: index + POS + definition + expand icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "$index.",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = posColor,
                    modifier = Modifier.padding(end = 6.dp, top = 1.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meaning.definition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expandable content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp, start = 20.dp)) {
                    // Examples
                    if (meaning.examples.isNotEmpty()) {
                        meaning.examples.forEach { example ->
                            Row(modifier = Modifier.padding(top = 4.dp)) {
                                Text(
                                    text = "\u201C",
                                    color = posColor,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(
                                    text = example,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Synonyms
                    if (meaning.synonyms.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LexicalChipGroup(
                            label = "Synonyms",
                            words = meaning.synonyms,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Antonyms
                    if (meaning.antonyms.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LexicalChipGroup(
                            label = "Antonyms",
                            words = meaning.antonyms,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

// -- Lexical Chip Group --

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LexicalChipGroup(
    label: String,
    words: List<String>,
    color: androidx.compose.ui.graphics.Color
) {
    if (words.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            words.take(8).forEach { word ->
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = word,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = color.copy(alpha = 0.12f),
                        labelColor = color
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = color.copy(alpha = 0.35f)
                    )
                )
            }
        }
    }
}

// -- Context Insight Card --

@Composable
private fun ContextInsightCard(insight: ContextualInsight) {
    val accentColor = MaterialTheme.colorScheme.tertiary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = "In this context",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold
                )
                if (insight.partOfSpeech.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                accentColor.copy(alpha = 0.1f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = insight.partOfSpeech,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Text(
                text = insight.meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            insight.note?.let { note ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -- Loading Skeleton --

@Composable
fun DefinitionSkeleton(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Word placeholder
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = shimmerAlpha))
        )

        Spacer(modifier = Modifier.height(12.dp))

        // POS tags placeholder
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = shimmerAlpha))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Definition card placeholders
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = shimmerAlpha))
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
