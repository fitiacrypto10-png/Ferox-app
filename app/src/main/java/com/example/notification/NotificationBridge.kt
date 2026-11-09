package com.example.notification

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Passerelle entre les notifications Android natives et l'application Web.
 * Gère la réception des messages du chat et l'injection des réponses saisies dans les notifications.
 */
object NotificationBridge {

    private const val TAG = "NotificationBridge"

    private var activeWebViewRef: WeakReference<WebView>? = null
    private var appContext: Context? = null
    var isAppInForeground: Boolean = true

    private val pendingReplies = mutableListOf<String>()

    fun init(context: Context) {
        appContext = context.applicationContext
        ChatNotificationManager.initNotificationChannel(context)
    }

    fun registerWebView(webView: WebView) {
        activeWebViewRef = WeakReference(webView)
        flushPendingReplies()
    }

    fun unregisterWebView() {
        activeWebViewRef = null
    }

    /**
     * Envoie la réponse tapée dans la notification directement dans la boîte de dialogue du chat Web.
     */
    fun sendReplyToWebView(replyText: String) {
        val webView = activeWebViewRef?.get()
        if (webView != null) {
            val jsonText = JSONObject.quote(replyText)
            webView.post {
                val script = """
                    (function() {
                        if (typeof window.__sendChatMessageFromNotification === 'function') {
                            window.__sendChatMessageFromNotification($jsonText);
                        } else {
                            var input = document.querySelector('textarea, input[placeholder*="message" i], input[placeholder*="envoyer" i], [contenteditable="true"]');
                            if (input) {
                                input.value = $jsonText;
                                input.dispatchEvent(new Event('input', { bubbles: true }));
                                setTimeout(function() {
                                    var btn = document.querySelector('button[type="submit"], button[aria-label*="send" i], button[aria-label*="envoyer" i]');
                                    if (btn) btn.click();
                                }, 100);
                            }
                        }
                    })();
                """.trimIndent()
                webView.evaluateJavascript(script, null)
                Log.d(TAG, "Réponse injectée dans le WebView avec succès: $replyText")
            }
        } else {
            Log.w(TAG, "WebView non disponible lors de l'envoi de la réponse. Mise en attente.")
            pendingReplies.add(replyText)
        }
    }

    private fun flushPendingReplies() {
        val webView = activeWebViewRef?.get() ?: return
        if (pendingReplies.isEmpty()) return

        val replies = pendingReplies.toList()
        pendingReplies.clear()
        replies.forEach { reply ->
            sendReplyToWebView(reply)
        }
    }

    /**
     * Interface JavaScript exposée au WebView sous le nom 'AndroidChatNotifier'.
     */
    class JsInterface(private val context: Context) {

        @JavascriptInterface
        fun onNewMessage(sender: String?, message: String?) {
            if (sender.isNullOrBlank() || message.isNullOrBlank()) return

            Log.d(TAG, "Nouveau message détecté du web: sender=$sender, msg=$message, isForeground=$isAppInForeground")

            // Afficher la notification si l'application est en arrière-plan ou minimisée
            if (!isAppInForeground) {
                ChatNotificationManager.showMessageNotification(
                    context = context,
                    sender = sender.trim(),
                    message = message.trim()
                )
            }
        }

        @JavascriptInterface
        fun isForeground(): Boolean {
            return isAppInForeground
        }
    }

    fun createJsInterface(context: Context): JsInterface {
        return JsInterface(context)
    }
}
