package com.jworks.eigolens.ui.capture

import android.Manifest
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureFlowScreen(
    onSettingsClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onHistoryClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    viewModel: CaptureFlowViewModel = hiltViewModel()
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    if (cameraPermissionState.status.isGranted) {
        CaptureFlowContent(
            onSettingsClick = onSettingsClick,
            onGalleryClick = onGalleryClick,
            onHistoryClick = onHistoryClick,
            onFeedbackClick = onFeedbackClick,
            viewModel = viewModel
        )
    } else {
        CameraPermissionRequest(
            shouldShowRationale = cameraPermissionState.status.shouldShowRationale,
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
        )
    }
}

@Composable
private fun CameraPermissionRequest(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (shouldShowRationale) {
                "EigoLens needs camera access to capture and analyze English text. Please grant camera permission to continue."
            } else {
                "Camera permission is required to scan text. Tap below to grant access."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Camera Permission")
        }
    }
}

@Composable
private fun CaptureFlowContent(
    onSettingsClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    viewModel: CaptureFlowViewModel
) {
    val captureState by viewModel.captureState.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap?.let { bmp -> viewModel.onGalleryImport(bmp) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = captureState) {
            is CaptureState.Camera -> {
                val cefrThreshold by viewModel.cefrThreshold.collectAsState()
                val coinBalance by viewModel.coinBalance.collectAsState()
                val liveEnrichedWords by viewModel.liveEnrichedWords.collectAsState()
                val liveImageSize by viewModel.liveImageSize.collectAsState()
                val liveRotationDegrees by viewModel.liveRotationDegrees.collectAsState()
                val showIpaOverlay by viewModel.showIpaOverlay.collectAsState()
                val ipaFontScale by viewModel.ipaFontScale.collectAsState()
                CameraPreviewMode(
                    isFlashOn = viewModel.isFlashOn.collectAsState().value,
                    onCapture = { bitmap -> viewModel.onPhotoCapture(bitmap) },
                    onFlashToggle = { viewModel.toggleFlash() },
                    onGalleryClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSettingsClick = onSettingsClick,
                    onHistoryClick = onHistoryClick,
                    onFeedbackClick = onFeedbackClick,
                    cefrThreshold = cefrThreshold,
                    onCefrThresholdChange = { viewModel.setCefrThreshold(it) },
                    coinBalance = coinBalance,
                    liveEnrichedWords = liveEnrichedWords,
                    liveImageWidth = liveImageSize.width,
                    liveImageHeight = liveImageSize.height,
                    liveRotationDegrees = liveRotationDegrees,
                    showIpaOverlay = showIpaOverlay,
                    ipaFontScale = ipaFontScale,
                    onIpaToggle = { viewModel.toggleIpaOverlay() },
                    onFrameAvailable = { imageProxy -> viewModel.processLiveFrame(imageProxy) }
                )
            }

            is CaptureState.Annotate -> {
                AnnotationMode(
                    capturedImage = state.capturedImage,
                    viewModel = viewModel,
                    onBack = { viewModel.resetToCamera() }
                )
            }
        }

        if (isProcessing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}
