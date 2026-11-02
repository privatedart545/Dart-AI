package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.example.platform.ShakeDetector
import com.example.ui.screens.MainChatScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.components.TutorialModal
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DevAiTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AssistantViewModel

enum class AppScreen {
    SPLASH,
    MAIN,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()
    private var shakeDetector: ShakeDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        shakeDetector = ShakeDetector(this) {
            if (viewModel.shakeToWakeEnabled.value) {
                viewModel.openVoiceModal()
            }
        }

        setContent {
            val selectedTheme by viewModel.selectedTheme.collectAsState()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val hasCompletedTutorial by viewModel.hasCompletedTutorial.collectAsState()

            DevAiTheme(themeName = selectedTheme, darkTheme = isDarkMode) {
                var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
                var showFolderPermissionDialog by remember { mutableStateOf(false) }

                // SAF Folder picker launcher
                val folderPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri: Uri? ->
                    if (uri != null) {
                        viewModel.onFolderSelected(uri)
                    }
                }

                // Startup multi-permissions launcher (Audio, Camera, Contacts, Calendar)
                val multiPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    // Check if SAF folder is already set up; if not, prompt folder selection
                    if (viewModel.safFileManager.getAuthorizedFolders().isEmpty()) {
                        showFolderPermissionDialog = true
                    }
                }

                // Request initial permissions on app startup
                LaunchedEffect(Unit) {
                    val permissionsNeeded = mutableListOf<String>()
                    val neededList = arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR
                    )

                    for (p in neededList) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, p) != PackageManager.PERMISSION_GRANTED) {
                            permissionsNeeded.add(p)
                        }
                    }

                    if (permissionsNeeded.isNotEmpty()) {
                        multiPermissionLauncher.launch(permissionsNeeded.toTypedArray())
                    } else if (viewModel.safFileManager.getAuthorizedFolders().isEmpty()) {
                        showFolderPermissionDialog = true
                    }

                    // Check if launched from Quick Settings Tile
                    if (intent.getBooleanExtra("EXTRA_START_VOICE", false)) {
                        viewModel.openVoiceModal()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            AppScreen.SPLASH -> {
                                SplashScreen(
                                    onEnter = {
                                        currentScreen = AppScreen.MAIN
                                        // Also trigger folder prompt if not yet selected
                                        if (viewModel.safFileManager.getAuthorizedFolders().isEmpty()) {
                                            showFolderPermissionDialog = true
                                        }
                                    }
                                )
                            }
                            AppScreen.MAIN -> {
                                MainChatScreen(
                                    viewModel = viewModel,
                                    onOpenSettings = { currentScreen = AppScreen.SETTINGS }
                                )
                            }
                            AppScreen.SETTINGS -> {
                                BackHandler {
                                    currentScreen = AppScreen.MAIN
                                }
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = AppScreen.MAIN },
                                    onPickFolder = { folderPickerLauncher.launch(null) }
                                )
                            }
                        }
                    }

                    // First-usage interactive tutorial walkthrough
                    if (!hasCompletedTutorial && currentScreen != AppScreen.SPLASH) {
                        TutorialModal(
                            onDismiss = { viewModel.completeTutorial() },
                            onComplete = { viewModel.completeTutorial() }
                        )
                    }

                    // Dialog to request folder permission explicitly on opening
                    if (showFolderPermissionDialog) {
                        AlertDialog(
                            onDismissRequest = { showFolderPermissionDialog = false },
                            title = {
                                Text(
                                    text = "Authorize Storage Folder",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            },
                            text = {
                                Text(
                                    text = "DEV AI needs access to at least one folder (such as Documents or Downloads) to search documents, move files, create notes, and open files.\n\nUnder Android privacy rules, you choose exactly which folder to share.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextSecondary
                                    )
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showFolderPermissionDialog = false
                                        folderPickerLauncher.launch(null)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                                ) {
                                    Text("Select Folder Now", color = CyberBackground, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showFolderPermissionDialog = false }) {
                                    Text("Skip for Now", color = TextMuted)
                                }
                            },
                            containerColor = CyberSurfaceElevated
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        shakeDetector?.start()
    }

    override fun onPause() {
        super.onPause()
        shakeDetector?.stop()
    }
}
