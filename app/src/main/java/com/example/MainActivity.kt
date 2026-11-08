package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.ui.SplashScreen
import com.example.ui.TermsAndLicenseScreen
import com.example.ui.TermsPreferences
import com.example.ui.VoiceNookApp
import com.example.ui.theme.MyApplicationTheme
import com.example.updater.AppUpdaterManager
import com.example.updater.UpdateDialog
import com.example.updater.UpdateInfo
import com.example.voice.FloatingBubbleService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var hasOverlayPermission by mutableStateOf(false)
    private var availableUpdate by mutableStateOf<UpdateInfo?>(null)
    private var dismissedVersionCode: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                var isSplashVisible by remember {
                    mutableStateOf(true)
                }
                var isTermsAccepted by remember {
                    mutableStateOf(TermsPreferences.isTermsAccepted(this@MainActivity))
                }
                var showTermsModal by remember {
                    mutableStateOf(false)
                }

                LaunchedEffect(Unit) {
                    val hasRecordAudio = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasRecordAudio) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasNotification = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasNotification) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Crossfade(targetState = isSplashVisible, label = "SplashCrossfade") { inSplash ->
                    if (inSplash) {
                        SplashScreen(
                            onSplashFinished = {
                                isSplashVisible = false
                            }
                        )
                    } else {
                        Crossfade(targetState = isTermsAccepted, label = "TermsCrossfade") { accepted ->
                            if (!accepted) {
                                TermsAndLicenseScreen(
                                    onAccept = {
                                        TermsPreferences.setTermsAccepted(this@MainActivity, true)
                                        isTermsAccepted = true
                                    }
                                )
                            } else if (showTermsModal) {
                                BackHandler {
                                    showTermsModal = false
                                }
                                TermsAndLicenseScreen(
                                    onAccept = {
                                        showTermsModal = false
                                    },
                                    isReadOnly = true,
                                    onClose = {
                                        showTermsModal = false
                                    }
                                )
                            } else {
                                VoiceNookApp(
                                    hasOverlayPermission = hasOverlayPermission,
                                    onRequestOverlayPermission = {
                                        requestOverlayPermission()
                                    },
                                    onOpenTerms = {
                                        showTermsModal = true
                                    }
                                )
                            }
                        }
                    }
                }

                // Afficher la boîte de dialogue sombre de mise à jour une fois le splash terminé
                if (!isSplashVisible) {
                    availableUpdate?.let { updateInfo ->
                        UpdateDialog(
                            updateInfo = updateInfo,
                            onUpdate = {
                                val info = updateInfo
                                availableUpdate = null
                                AppUpdaterManager.downloadAndInstallApk(this@MainActivity, info)
                            },
                            onDismiss = {
                                dismissedVersionCode = updateInfo.versionCode
                                availableUpdate = null
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasOverlayPermission = Settings.canDrawOverlays(this)
        // Inside the application: hide bubble as requested ("pas dans l'application")
        FloatingBubbleService.setOverlayVisible(this, false)

        // Vérification de mise à jour GitHub en arrière-plan à chaque retour au premier plan
        checkForAppUpdate()
    }

    private fun checkForAppUpdate() {
        lifecycleScope.launch(Dispatchers.IO) {
            val update = AppUpdaterManager.checkForUpdate(this@MainActivity)
            if (update != null && update.versionCode != dismissedVersionCode) {
                withContext(Dispatchers.Main) {
                    availableUpdate = update
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Outside the application (Home, background): show bubble
        if (Settings.canDrawOverlays(this)) {
            FloatingBubbleService.setOverlayVisible(this, true)
        }
    }

    override fun onPause() {
        super.onPause()
        // User switching away: show bubble outside
        if (!isFinishing && Settings.canDrawOverlays(this)) {
            FloatingBubbleService.setOverlayVisible(this, true)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isFinishing && Settings.canDrawOverlays(this)) {
            FloatingBubbleService.setOverlayVisible(this, true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            FloatingBubbleService.stop(this)
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}
