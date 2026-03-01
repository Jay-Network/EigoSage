package com.jworks.eigosage.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.eigosage.domain.models.DifficultyLevel
import com.jworks.eigosage.domain.models.ReadabilityMetrics

@Composable
fun ReadabilityPanel(
    metrics: ReadabilityMetrics,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Readability Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF333333)
            )
            Text(
                text = "Back",
                color = Color(0xFF2196F3),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Overall grade card
        val diffColor = difficultyColor(metrics.difficulty)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(diffColor.copy(alpha = 0.15f))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Grade Level",
                    fontSize = 12.sp,
                    color = diffColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "%.1f".format(metrics.averageGrade),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = diffColor
                )
                Text(
                    text = metrics.difficulty.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = diffColor
                )
                Text(
                    text = metrics.targetAudience,
                    fontSize = 13.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(1.dp)
                .background(Color(0xFFE0E0E0))
        )

        // Individual scores
        Text(
            text = "Scores",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ScoreRow(
            name = "Flesch-Kincaid Grade",
            value = "%.1f".format(metrics.fleschKincaidGrade),
            detail = "U.S. grade level"
        )
        ScoreRow(
            name = "Reading Ease",
            value = "%.0f".format(metrics.fleschReadingEase),
            detail = readingEaseLabel(metrics.fleschReadingEase)
        )
        ScoreRow(
            name = "SMOG Index",
            value = "%.1f".format(metrics.smogIndex),
            detail = "Education years needed"
        )
        ScoreRow(
            name = "Coleman-Liau",
            value = "%.1f".format(metrics.colemanLiauIndex),
            detail = "Character-based grade"
        )

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(1.dp)
                .background(Color(0xFFE0E0E0))
        )

        // Text statistics
        Text(
            text = "Statistics",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        StatRow("Words", metrics.statistics.totalWords.toString())
        StatRow("Sentences", metrics.statistics.totalSentences.toString())
        StatRow("Syllables", metrics.statistics.totalSyllables.toString())
        StatRow("Avg words/sentence", "%.1f".format(metrics.statistics.averageWordsPerSentence))
        StatRow("Avg syllables/word", "%.2f".format(metrics.statistics.averageSyllablesPerWord))
        StatRow("Polysyllable words", metrics.statistics.polysyllableCount.toString())
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
                fontSize = 14.sp,
                color = Color(0xFF333333)
            )
            Text(
                text = detail,
                fontSize = 11.sp,
                color = Color(0xFF999999)
            )
        }
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1565C0)
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF666666))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
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
