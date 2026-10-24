package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "securesms_messages_channel"
    private const val CHANNEL_NAME = "پیام‌های دریافتی"

    fun showMessageNotification(
        context: Context,
        senderName: String,
        senderPhone: String,
        messagePreview: String,
        isEncrypted: Boolean
    ) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // Create notification channel for Android O+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "اطلاع‌رسانی پیامک‌های ورودی امن و عادی"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Launch MainActivity when tapped
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("senderPhone", senderPhone)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                senderPhone.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (senderName.isNotBlank() && senderName != senderPhone) {
                senderName
            } else {
                senderPhone
            }

            val content = if (isEncrypted) {
                "🔒 پیام رمزگذاری‌شده سرتاسری جدید: $messagePreview"
            } else {
                "💬 پیامک عادی: $messagePreview"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(senderPhone.hashCode(), notification)
        } catch (e: Exception) {
            // Notification failed gracefully
        }
    }
}
