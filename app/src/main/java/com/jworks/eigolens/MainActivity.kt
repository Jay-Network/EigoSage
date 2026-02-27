package com.jworks.eigolens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jworks.eigolens.ui.auth.LoginScreen
import com.jworks.eigolens.ui.capture.CaptureFlowScreen
import com.jworks.eigolens.ui.feedback.FeedbackDialog
import com.jworks.eigolens.ui.feedback.FeedbackViewModel
import com.jworks.eigolens.ui.gallery.GalleryImportScreen
import com.jworks.eigolens.ui.history.HistoryScreen
import com.jworks.eigolens.ui.settings.SettingsScreen
import com.jworks.eigolens.ui.theme.EigoLensTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EigoLensTheme {
                val navController = rememberNavController()
                val feedbackViewModel: FeedbackViewModel = hiltViewModel()
                val feedbackUiState by feedbackViewModel.uiState.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    Scaffold(
                        containerColor = Color.Transparent
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "login",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("login") {
                                LoginScreen(
                                    onLoginSuccess = {
                                        navController.navigate("capture") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    },
                                    onContinueAsGuest = {
                                        navController.navigate("capture") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("capture") {
                                CaptureFlowScreen(
                                    onSettingsClick = { navController.navigate("settings") },
                                    onGalleryClick = { navController.navigate("gallery") },
                                    onHistoryClick = { navController.navigate("history") },
                                    onFeedbackClick = { feedbackViewModel.openDialog() }
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable("gallery") {
                                GalleryImportScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("history") {
                                HistoryScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        }

                        if (feedbackUiState.isDialogOpen) {
                            FeedbackDialog(
                                onDismiss = { feedbackViewModel.closeDialog() },
                                viewModel = feedbackViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
