package com.jworks.eigolens.ui.capture

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.jworks.eigolens.domain.ai.AiResponse

@Composable
fun AiAnalysisPanel(
    selectedText: String,
    scopeLevel: ScopeLevel,
    response: AiResponse,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        AiPanelHeader(
            scopeLevel = scopeLevel,
            provider = response.provider,
            onDismiss = onDismiss
        )

        // Content
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SelectedQuoteCard(selectedText) }
            val sections = parseAiSections(response.content)
            items(sections) { section ->
                AiSectionCard(section)
            }
        }

        // Footer
        AiPanelFooter(
            processingTimeMs = response.processingTimeMs,
            tokensUsed = response.tokensUsed
        )
    }
}

@Composable
fun AiLoadingPanel(
    selectedText: String,
    scopeLevel: ScopeLevel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScopeBadge(scopeLevel)

        Spacer(modifier = Modifier.height(8.dp))

        SelectedQuoteCard(selectedText)

        Spacer(modifier = Modifier.height(24.dp))

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

// -- Header --

@Composable
private fun AiPanelHeader(
    scopeLevel: ScopeLevel,
    provider: String,
    onDismiss: () -> Unit
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
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
private fun AiPanelFooter(processingTimeMs: Long, tokensUsed: Int?) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
