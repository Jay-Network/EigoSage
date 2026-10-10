package com.jworks.eigosage.ui.live

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import androidx.compose.material3.ExperimentalMaterial3Api
import com.jworks.eigosage.data.ai.ChatPersona

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LiveSessionScreen(
    onBackClick: () -> Unit,
    viewModel: LiveSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    DisposableEffect(Unit) {
        onDispose { viewModel.stopSession() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Voice Tutor") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!micPermissionState.status.isGranted) {
            MicPermissionRequest(
                modifier = Modifier.padding(padding),
                shouldShowRationale = micPermissionState.status.shouldShowRationale,
                onRequestPermission = { micPermissionState.launchPermissionRequest() }
            )
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChatPersona.entries.forEach { persona ->
                    FilterChip(
                        selected = uiState.persona == persona,
                        onClick = { viewModel.selectPersona(persona) },
                        enabled = uiState.connectionState == LiveConnectionState.IDLE ||
                            uiState.connectionState == LiveConnectionState.ERROR,
                        label = { Text(persona.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(uiState.transcript) { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                when (uiState.connectionState) {
                    LiveConnectionState.IDLE, LiveConnectionState.ERROR -> {
                        Button(onClick = { viewModel.startSession() }) {
                            Text("Start Talking")
                        }
                    }
                    LiveConnectionState.CONNECTING -> {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp))
                    }
                    LiveConnectionState.LISTENING -> {
                        Button(onClick = { viewModel.stopSession() }) {
                            Text("End Session")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MicPermissionRequest(
    modifier: Modifier = Modifier,
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (shouldShowRationale) {
                "EigoSage needs microphone access for the voice tutor to hear you. Please grant microphone permission to continue."
            } else {
                "Microphone permission is required for the voice tutor. Tap below to grant access."
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Microphone Permission")
        }
    }
}
