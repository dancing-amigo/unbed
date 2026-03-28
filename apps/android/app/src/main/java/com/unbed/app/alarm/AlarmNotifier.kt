package com.unbed.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.unbed.app.ringing.AlarmRingingActivity

class AlarmNotifier(
    private val context: Context,
) {
    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)

    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    RINGING_CHANNEL_ID,
                    "Ringing alarms",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Shows active alarm sessions with full-screen takeover."
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun showRingingNotification(sessionId: String) {
        val contentIntent = createRingingPendingIntent(sessionId)
        val notification =
            NotificationCompat.Builder(context, RINGING_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Alarm ringing")
                .setContentText("Scan your QR code away from bed to continue.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(contentIntent, true)
                .setContentIntent(contentIntent)
                .build()

        notificationManager.notify(RINGING_NOTIFICATION_ID, notification)
    }

    fun dismissRingingNotification() {
        notificationManager.cancel(RINGING_NOTIFICATION_ID)
    }

    private fun createRingingPendingIntent(sessionId: String): PendingIntent {
        val intent =
            Intent(context, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_RINGING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val EXTRA_SESSION_ID: String = "extra_session_id"
        const val RINGING_CHANNEL_ID: String = "ringing_alarm_channel"
        private const val REQUEST_CODE_RINGING: Int = 3101
        private const val RINGING_NOTIFICATION_ID: Int = 3102
    }
}
