package com.example.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
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
    var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris: Array<Uri>? = if (result.resultCode == Activity.RESULT_OK) {
            val intentData = result.data
            val parsed = WebChromeClient.FileChooserParams.parseResult(result.resultCode, intentData)
            if (parsed != null && parsed.isNotEmpty()) {
                parsed
            } else if (intentData?.data != null) {
                arrayOf(intentData.data!!)
            } else if (intentData?.clipData != null) {
                val clipData = intentData.clipData!!
                Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            } else {
                null
            }
        } else {
            null
        }
        fileUploadCallback?.onReceiveValue(uris)
        fileUploadCallback = null
    }

    val currentLauncher by rememberUpdatedState(fileChooserLauncher)

    // Ensure any pending callback is released when leaving composition
    DisposableEffect(Unit) {
        onDispose {
            fileUploadCallback?.onReceiveValue(null)
            fileUploadCallback = null
        }
    }

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

                // Keep webview running audio and cookies even when backgrounded
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

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
                }

                // Handle file and image downloads from web app
                setDownloadListener { downloadUrl, _, _, _, _ ->
                    try {
                        val downloadIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(downloadUrl)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(downloadIntent)
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
                        // Automatically grant audio recording permission to the WebRTC web app
                        request?.grant(request.resources)
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        // Cancel previous pending callback if any
                        fileUploadCallback?.onReceiveValue(null)
                        fileUploadCallback = filePathCallback

                        return try {
                            val intent = try {
                                val base = fileChooserParams?.createIntent()
                                if (base != null) {
                                    if (base.type.isNullOrBlank()) {
                                        base.type = "*/*"
                                    }
                                    base
                                } else {
                                    Intent(Intent.ACTION_GET_CONTENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "*/*"
                                    }
                                }
                            } catch (e: Exception) {
                                Intent(Intent.ACTION_GET_CONTENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "*/*"
                                }
                            }

                            if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            }

                            val chooser = Intent.createChooser(intent, "Choisir une image ou un fichier")
                            currentLauncher.launch(chooser)
                            true
                        } catch (e: Exception) {
                            fileUploadCallback?.onReceiveValue(null)
                            fileUploadCallback = null
                            false
                        }
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
