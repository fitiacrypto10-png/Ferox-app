package com.example.webview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.voice.VoiceStateHolder

const val VOICE_NOOK_URL = "https://voice-nook-community.lovable.app/app"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VoiceWebView(
    url: String = VOICE_NOOK_URL,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onConnectionError: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var pendingWebPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        FileChooserHelper.processResult(context, result.resultCode, result.data)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingWebPermissionRequest?.let { req ->
                try {
                    req.grant(req.resources)
                } catch (e: Exception) {
                    try {
                        req.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                    } catch (e2: Exception) {}
                }
            }
        } else {
            pendingWebPermissionRequest?.deny()
        }
        pendingWebPermissionRequest = null
    }

    val currentFileChooserLauncher by rememberUpdatedState(fileChooserLauncher)
    val currentMicLauncher by rememberUpdatedState(micPermissionLauncher)

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    // Configure hardware audio mode for VoIP communication (prevents earpiece routing and muting)
    DisposableEffect(Unit) {
        val prevMode = audioManager?.mode ?: AudioManager.MODE_NORMAL
        val prevSpeaker = audioManager?.isSpeakerphoneOn ?: false
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = true
        } catch (e: Exception) {}

        onDispose {
            try {
                audioManager?.mode = prevMode
                audioManager?.isSpeakerphoneOn = prevSpeaker
            } catch (e: Exception) {}
            pendingWebPermissionRequest = null
            com.example.notification.NotificationBridge.unregisterWebView()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER

                // Keep webview running audio and cookies even when backgrounded
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                // Enregistrer le pont de notifications pour les réponses rapides style Messenger
                com.example.notification.NotificationBridge.registerWebView(this)
                addJavascriptInterface(
                    com.example.notification.NotificationBridge.createJsInterface(ctx),
                    "AndroidChatNotifier"
                )

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadsImagesAutomatically = true
                    // Enable content and file access for photo and file picker uploads
                    allowContentAccess = true
                    allowFileAccess = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = false
                    textZoom = 100
                    displayZoomControls = false
                    builtInZoomControls = false
                    setSupportZoom(false)
                    layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    javaScriptCanOpenWindowsAutomatically = true

                    // Clean UserAgent to avoid WebRTC / LiveKit blocking on mobile WebViews
                    val defaultUa = userAgentString
                    var cleanedUa = defaultUa
                    if (cleanedUa.contains("; wv")) {
                        cleanedUa = cleanedUa.replace("; wv", "")
                    }
                    if (cleanedUa.contains("Version/4.0 ")) {
                        cleanedUa = cleanedUa.replace("Version/4.0 ", "")
                    }
                    userAgentString = cleanedUa
                }

                // Handle file and image downloads from web app
                setDownloadListener { downloadUrl, _, _, _, _ ->
                    try {
                        val downloadIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(downloadUrl)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        ctx.startActivity(downloadIntent)
                    } catch (e: Exception) {
                        // Ignore if no activity handles the download URL
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        if (request == null) return

                        // CRITICAL FOR SAMSUNG / XIAOMI / HUAWEI: PermissionRequest must be granted on the UI thread!
                        post {
                            val hasMic = ContextCompat.checkSelfPermission(
                                ctx,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasMic) {
                                try {
                                    request.grant(request.resources)
                                } catch (e: Exception) {
                                    try {
                                        request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                                    } catch (e2: Exception) {}
                                }
                            } else {
                                pendingWebPermissionRequest = request
                                currentMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }

                    override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                        super.onPermissionRequestCanceled(request)
                        if (pendingWebPermissionRequest == request) {
                            pendingWebPermissionRequest = null
                        }
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        if (filePathCallback == null) return false
                        FileChooserHelper.cancelPreviousIfAny()
                        FileChooserHelper.pendingUploadCallback = filePathCallback

                        return try {
                            val chooserIntent = FileChooserHelper.createFileChooserIntent(ctx, fileChooserParams)
                            currentFileChooserLauncher.launch(chooserIntent)
                            true
                        } catch (e: Exception) {
                            FileChooserHelper.cancel()
                            false
                        }
                    }
                }

                // Nettoyer les anciens caches lourds pour maintenir l'application sous les 20 Mo
                com.example.network.DataSaverManager.cleanupAppStorage(ctx, this)

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        // Pre-inject mobile viewport fit css, Lovable logo purge, and audio hook
                        view?.evaluateJavascript(LovableCleanScripts.MOBILE_VIEWPORT_FIT_JS, null)
                        view?.evaluateJavascript(LovableCleanScripts.HIDE_LOVABLE_JS, null)
                        view?.evaluateJavascript(LovableCleanScripts.SETUP_WEBRTC_MIC_HOOK_JS, null)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Execute mobile viewport fit, cleanup script and mutation observer
                        view?.evaluateJavascript(LovableCleanScripts.MOBILE_VIEWPORT_FIT_JS, null)
                        view?.evaluateJavascript(LovableCleanScripts.HIDE_LOVABLE_JS, null)
                        view?.evaluateJavascript(LovableCleanScripts.SETUP_WEBRTC_MIC_HOOK_JS, null)
                        view?.evaluateJavascript(LovableCleanScripts.CHAT_NOTIFICATION_JS, null)

                        // Apply current mic state to the web app
                        val isMuted = VoiceStateHolder.isMicMuted.value
                        view?.evaluateJavascript(LovableCleanScripts.getSetMicMutedScript(isMuted), null)

                        onConnectionError(false)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val errorCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                error?.errorCode
                            } else {
                                null
                            }
                            // Ignore unknown or navigation abort codes (-1) to prevent false offline triggers
                            if (errorCode != WebViewClient.ERROR_UNKNOWN && errorCode != -1) {
                                onConnectionError(true)
                            }
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 200) >= 400) {
                            if (errorResponse?.statusCode in 500..599 || errorResponse?.statusCode == 404) {
                                onConnectionError(true)
                            }
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        handler?.cancel()
                        onConnectionError(true)
                    }
                }

                loadUrl(url)
                onWebViewCreated(this)
            }
        }
    )
}
