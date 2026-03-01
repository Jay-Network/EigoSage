package com.jworks.eigosage

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
import com.jworks.eigosage.ui.auth.LoginScreen
import com.jworks.eigosage.ui.capture.CaptureFlowScreen
import com.jworks.eigosage.ui.feedback.FeedbackDialog
import com.jworks.eigosage.ui.feedback.FeedbackViewModel
import com.jworks.eigosage.ui.gallery.GalleryImportScreen
import com.jworks.eigosage.ui.history.HistoryScreen
import com.jworks.eigosage.ui.rewards.RewardsScreen
import com.jworks.eigosage.ui.settings.SettingsScreen
import com.jworks.eigosage.ui.theme.EigoSageTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EigoSageTheme {
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
                                    onFeedbackClick = { feedbackViewModel.openDialog() },
                                    onRewardsClick = { navController.navigate("rewards") }
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onRewardsClick = { navController.navigate("rewards") }
                                )
                            }

                            composable("rewards") {
                                RewardsScreen(
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
