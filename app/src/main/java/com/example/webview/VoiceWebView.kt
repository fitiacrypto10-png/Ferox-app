package com.example.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
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
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER

                // Keep webview running audio even when activity loses focus
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadsImagesAutomatically = true
                    allowFileAccess = false
                    allowContentAccess = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = false
                    textZoom = 100
                    displayZoomControls = false
                    builtInZoomControls = false
                    setSupportZoom(false)
                    layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        // Automatically grant audio recording permission to the WebRTC web app
                        request?.grant(request.resources)
                    }
                }

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
