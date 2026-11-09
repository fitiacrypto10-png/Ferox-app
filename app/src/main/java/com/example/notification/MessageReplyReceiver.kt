package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput

/**
 * Récepteur appelé lorsqu'un utilisateur répond directement depuis la notification (Quick Reply).
 */
class MessageReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ChatNotificationManager.ACTION_REPLY_MESSAGE) return

        val remoteInputResults = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInputResults?.getCharSequence(ChatNotificationManager.KEY_TEXT_REPLY)?.toString()

        if (replyText.isNullOrBlank()) {
            return
        }

        val notificationId = intent.getIntExtra(ChatNotificationManager.EXTRA_NOTIFICATION_ID, 0)
        val sender = intent.getStringExtra(ChatNotificationManager.EXTRA_SENDER_NAME) ?: "Utilisateur"
        val originalMessage = intent.getStringExtra(ChatNotificationManager.EXTRA_ORIGINAL_MESSAGE) ?: ""

        Log.d("MessageReplyReceiver", "Réponse saisie depuis la notification: '$replyText' pour $sender")

        // 1. Mettre à jour la notification pour afficher la confirmation d'envoi immédiate
        ChatNotificationManager.updateNotificationWithReply(
            context = context,
            notificationId = notificationId,
            sender = sender,
            originalMessage = originalMessage,
            replyText = replyText
        )

        // 2. Transmettre le message au WebView pour qu'il soit envoyé dans le salon de discussion
        NotificationBridge.sendReplyToWebView(replyText)
    }
}
