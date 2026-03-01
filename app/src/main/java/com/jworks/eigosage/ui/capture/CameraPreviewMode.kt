package com.jworks.eigosage.ui.capture

import android.graphics.Bitmap
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import com.jworks.eigosage.domain.models.CefrLevel
import com.jworks.eigosage.domain.models.EnrichedWord
import com.jworks.eigosage.domain.models.color
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.foundation.border
import com.jworks.eigosage.R
import com.jworks.eigosage.ui.theme.GlassBorder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraPreviewMode(
    isFlashOn: Boolean,
    onCapture: (Bitmap) -> Unit,
    onFlashToggle: () -> Unit,
    onGalleryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    onRewardsClick: () -> Unit = {},
    cefrThreshold: CefrLevel = CefrLevel.B2,
    onCefrThresholdChange: (CefrLevel) -> Unit = {},
    coinBalance: Int? = null,
    liveEnrichedWords: List<EnrichedWord> = emptyList(),
    liveImageWidth: Int = 0,
    liveImageHeight: Int = 0,
    liveRotationDegrees: Int = 0,
    showIpaOverlay: Boolean = true,
    ipaFontScale: Float = 0.6f,
    onIpaToggle: () -> Unit = {},
    onFrameAvailable: (ImageProxy) -> Unit = { it.close() },
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Executor for ImageAnalysis — created once, shut down on dispose
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    // Apply flash state
    camera?.cameraControl?.enableTorch(isFlashOn)

    Box(modifier = modifier.fillMaxSize()) {
        // Full-screen camera preview with ImageAnalysis
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(surfaceProvider)
                        }
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                    onFrameAvailable(imageProxy)
                                }
                            }
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Live CEFR overlay on top of camera preview
        LiveOverlayCanvas(
            enrichedWords = liveEnrichedWords,
            imageWidth = liveImageWidth,
            imageHeight = liveImageHeight,
            rotationDegrees = liveRotationDegrees,
            cefrThreshold = cefrThreshold,
            showIpa = showIpaOverlay,
            ipaFontScale = ipaFontScale,
            modifier = Modifier.fillMaxSize()
        )

        // Settings + History buttons (top-left)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            SmallFloatingActionButton(
                onClick = onSettingsClick,
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Settings"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SmallFloatingActionButton(
                onClick = onHistoryClick,
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "History",
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SmallFloatingActionButton(
                onClick = onFeedbackClick,
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = "Send Feedback",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Flash toggle + IPA toggle + coin balance (top-right)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Coin balance chip (tappable → Rewards)
            if (coinBalance != null) {
                Row(
                    modifier = Modifier
                        .clickable { onRewardsClick() }
                        .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF4F46E5).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "\uD83E\uDE99",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$coinBalance",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFFD700)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            SmallFloatingActionButton(
                onClick = onFlashToggle,
                containerColor = if (isFlashOn) Color(0xFFFFEB3B) else Color.Black.copy(alpha = 0.5f),
                contentColor = if (isFlashOn) Color.Black else Color.White
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_flash),
                    contentDescription = if (isFlashOn) "Flash On" else "Flash Off"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // IPA toggle button
            SmallFloatingActionButton(
                onClick = onIpaToggle,
                containerColor = if (showIpaOverlay) Color(0xFF00E6FF).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.5f),
                contentColor = if (showIpaOverlay) Color.Black else Color.White
            ) {
                Text(
                    text = "/a/",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 13.sp
                )
            }
        }

        // Gallery import (bottom-left)
        SmallFloatingActionButton(
            onClick = onGalleryClick,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 40.dp),
            containerColor = Color.Black.copy(alpha = 0.5f),
            contentColor = Color.White
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_gallery),
                contentDescription = "Import from gallery"
            )
        }

        // CEFR threshold slider
        CefrThresholdBar(
            threshold = cefrThreshold,
            onThresholdChange = onCefrThresholdChange,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 112.dp)
        )

        // Capture button (bottom-center)
        FloatingActionButton(
            onClick = {
                val bitmap = previewView?.bitmap
                if (bitmap != null) {
                    onCapture(bitmap)
                } else {
                    Toast.makeText(context, "Camera not ready. Please try again.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp),
            shape = CircleShape,
            containerColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White, CircleShape)
                    .padding(4.dp)
                    .background(Color.White, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .background(Color(0xFF4F46E5), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun CefrThresholdBar(
    threshold: CefrLevel,
    onThresholdChange: (CefrLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val levels = CefrLevel.ALL

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF4F46E5).copy(alpha = 0.20f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Highlight: ${threshold.name}+ (${threshold.label})",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )

        Slider(
            value = threshold.ordinalIndex.toFloat(),
            onValueChange = { value ->
                val index = value.roundToInt().coerceIn(0, levels.lastIndex)
                onThresholdChange(levels[index])
            },
            valueRange = 0f..5f,
            steps = 4,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFFFF9800),
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            levels.forEach { level ->
                Text(
                    text = level.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (level == threshold) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
            }
        }
    }
}
