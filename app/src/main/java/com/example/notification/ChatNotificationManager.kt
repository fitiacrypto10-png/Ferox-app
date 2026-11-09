package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.R

/**
 * Gestionnaire des notifications interactives style Messenger.
 * Permet d'afficher les messages reçus et d'y répondre directement depuis
 * la barre de notification (Quick Reply) sans ouvrir l'application.
 */
object ChatNotificationManager {

    const val CHANNEL_ID = "ferox_chat_messages"
    const val KEY_TEXT_REPLY = "key_text_reply"
    const val ACTION_REPLY_MESSAGE = "com.aistudio.voicenook.xvrq.ACTION_REPLY_MESSAGE"

    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_SENDER_NAME = "extra_sender_name"
    const val EXTRA_ORIGINAL_MESSAGE = "extra_original_message"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Messages et Salons Ferox"
            val descriptionText = "Notifications pour les nouveaux messages avec réponse directe"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Affiche une notification de message avec le bouton "Répondre" (RemoteInput).
     */
    fun showMessageNotification(
        context: Context,
        sender: String,
        message: String,
        notificationId: Int = sender.hashCode()
    ) {
        initNotificationChannel(context)

        // 1. Intent d'ouverture de l'application
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Configuration de la réponse rapide (RemoteInput)
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Répondre...")
            .build()

        val replyIntent = Intent(context, MessageReplyReceiver::class.java).apply {
            action = ACTION_REPLY_MESSAGE
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_SENDER_NAME, sender)
            putExtra(EXTRA_ORIGINAL_MESSAGE, message)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Répondre",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        // 3. Style Messenger (MessagingStyle)
        val userMe = Person.Builder()
            .setName("Moi")
            .setKey("current_user")
            .build()

        val senderPerson = Person.Builder()
            .setName(sender.ifBlank { "Utilisateur" })
            .setKey(sender)
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(userMe)
            .setConversationTitle(sender)
            .addMessage(message, System.currentTimeMillis(), senderPerson)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(messagingStyle)
            .setContentTitle(sender)
            .setContentText(message)
            .setContentIntent(openAppPendingIntent)
            .addAction(replyAction)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission de notification manquante
        }
    }

    /**
     * Met à jour la notification après l'envoi de la réponse depuis la barre de notification.
     */
    fun updateNotificationWithReply(
        context: Context,
        notificationId: Int,
        sender: String,
        originalMessage: String,
        replyText: String
    ) {
        initNotificationChannel(context)

        val userMe = Person.Builder().setName("Moi").build()
        val senderPerson = Person.Builder().setName(sender).build()

        val messagingStyle = NotificationCompat.MessagingStyle(userMe)
            .setConversationTitle(sender)
            .addMessage(originalMessage, System.currentTimeMillis() - 5000, senderPerson)
            .addMessage(replyText, System.currentTimeMillis(), userMe)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(messagingStyle)
            .setContentTitle(sender)
            .setContentText("Réponse envoyée: $replyText")
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (ignored: Exception) {}
    }
}
