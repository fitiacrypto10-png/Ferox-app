package com.example.ui

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.network.NetworkMonitor
import com.example.voice.VoiceChatViewModel
import com.example.voice.VoiceStateHolder
import com.example.webview.LovableCleanScripts
import com.example.webview.VOICE_NOOK_URL
import com.example.webview.VoiceWebView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceNookApp(
    viewModel: VoiceChatViewModel = viewModel(),
    hasOverlayPermission: Boolean = false,
    onRequestOverlayPermission: () -> Unit = {},
    onOpenTerms: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val networkMonitor = remember { NetworkMonitor(context) }
    val isOnlineState by networkMonitor.isOnline.collectAsState(initial = true)

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pageProgress by remember { mutableIntStateOf(0) }
    var webHasError by remember { mutableStateOf(false) }
    var isRetrying by remember { mutableStateOf(false) }
    var isActuallyOffline by remember { mutableStateOf(false) }

    // Buffer Wi-Fi fluctuations with a grace period before showing offline screen
    LaunchedEffect(isOnlineState, webHasError) {
        if (!isOnlineState || webHasError) {
            delay(1800)
            val stillDisconnected = !networkMonitor.checkCurrentConnectivity() || webHasError
            if (stillDisconnected) {
                isActuallyOffline = true
            }
        } else {
            isActuallyOffline = false
        }
    }

    // Connect VoiceStateHolder to WebView for instant mic mute/unmute
    DisposableEffect(webViewInstance) {
        VoiceStateHolder.onMicStateChangedListener = { isMuted ->
            webViewInstance?.post {
                webViewInstance?.evaluateJavascript(
                    LovableCleanScripts.getSetMicMutedScript(isMuted),
                    null
                )
            }
        }
        onDispose {
            VoiceStateHolder.onMicStateChangedListener = null
        }
    }

    // Automatically trigger reload when device comes back online
    LaunchedEffect(isOnlineState) {
        if (isOnlineState && webHasError) {
            webHasError = false
            webViewInstance?.reload()
        }
    }

    // Back button handling
    BackHandler(enabled = true) {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF141218)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = isActuallyOffline,
                label = "OfflineCrossfade"
            ) { offline ->
                if (offline) {
                    OfflineScreen(
                        isRetrying = isRetrying,
                        onRetry = {
                            isRetrying = true
                            scope.launch {
                                val online = networkMonitor.checkCurrentConnectivity()
                                if (online) {
                                    webHasError = false
                                    webViewInstance?.reload()
                                } else {
                                    delay(800)
                                }
                                delay(600)
                                isRetrying = false
                            }
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Fullscreen Web View without navigation bars
                        VoiceWebView(
                            url = VOICE_NOOK_URL,
                            modifier = Modifier.fillMaxSize(),
                            onWebViewCreated = { webView ->
                                webViewInstance = webView
                            },
                            onProgressChanged = { progress ->
                                pageProgress = progress
                            },
                            onConnectionError = { hasError ->
                                webHasError = hasError
                            }
                        )

                        // Elegant slim top loading indicator when page is loading
                        if (pageProgress in 1..99) {
                            LinearProgressIndicator(
                                progress = { pageProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .align(Alignment.TopCenter),
                                color = Color(0xFF8B5CF6),
                                trackColor = Color.Transparent
                            )
                        }

                        // Subtle banner to prompt enabling overlay if not yet permitted
                        if (!hasOverlayPermission) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E1B2E).copy(alpha = 0.94f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Layers,
                                        contentDescription = null,
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Bulle vocale ronde en jeu (Messenger)",
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(
                                        onClick = onRequestOverlayPermission,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Activer",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
