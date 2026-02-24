package com.jworks.eigolens.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jworks.eigolens.R

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val claudeKey by viewModel.claudeApiKey.collectAsState()
    val geminiKey by viewModel.geminiApiKey.collectAsState()
    val activeProvider by viewModel.activeProvider.collectAsState()
    val availableProviders by viewModel.availableProviders.collectAsState()
    val allProviderNames by viewModel.allProviderNames.collectAsState()

    var claudeKeyInput by remember(claudeKey) { mutableStateOf(claudeKey) }
    var geminiKeyInput by remember(geminiKey) { mutableStateOf(geminiKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "AI Analysis",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Add API keys to enable phrase/paragraph analysis for circled text.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val providersForUi = if (allProviderNames.isNotEmpty()) allProviderNames else listOf("Claude", "Gemini")
                Text(
                    text = "Preferred Provider",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    providersForUi.forEachIndexed { index, provider ->
                        val enabled = provider in availableProviders
                        SegmentedButton(
                            selected = provider == activeProvider,
                            onClick = { if (enabled) viewModel.setActiveProvider(provider) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = providersForUi.size),
                            enabled = enabled
                        ) {
                            Text(provider)
                        }
                    }
                }

                if (availableProviders.isEmpty()) {
                    Text(
                        text = "No AI provider configured. Offline word definitions still work.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        KeyEditorCard(
            title = "Claude API Key",
            placeholder = "sk-ant-...",
            input = claudeKeyInput,
            saved = claudeKey,
            onInputChanged = { claudeKeyInput = it },
            onSave = { viewModel.setClaudeApiKey(claudeKeyInput.trim()) },
            onClear = { claudeKeyInput = "" }
        )

        Spacer(modifier = Modifier.height(12.dp))

        KeyEditorCard(
            title = "Gemini API Key",
            placeholder = "AIza...",
            input = geminiKeyInput,
            saved = geminiKey,
            onInputChanged = { geminiKeyInput = it },
            onSave = { viewModel.setGeminiApiKey(geminiKeyInput.trim()) },
            onClear = { geminiKeyInput = "" }
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsItem(label = "Version", value = "0.3.0")
        SettingsItem(label = "Dictionary", value = "WordNet (147,000+ words)")
        SettingsItem(label = "OCR Engine", value = "ML Kit Text Recognition")

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Features",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsItem(label = "Readability Analysis", value = "Flesch-Kincaid, SMOG, Coleman-Liau")
        SettingsItem(label = "NLP", value = "POS Tagging, NER, Lemmatization")
        SettingsItem(label = "Export", value = "PDF (A4 format)")

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "EigoLens by JWorks",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun KeyEditorCard(
    title: String,
    placeholder: String,
    input: String,
    saved: String,
    onInputChanged: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    val dirty = input != saved

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = input,
                onValueChange = onInputChanged,
                placeholder = { Text(placeholder) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSave,
                    enabled = dirty && input.isNotBlank()
                ) {
                    Text("Save")
                }

                OutlinedButton(
                    onClick = onClear,
                    enabled = input.isNotEmpty()
                ) {
                    Text("Clear")
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = if (saved.isNotBlank()) "Saved" else "Not saved",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (saved.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
