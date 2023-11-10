package com.example.digitalminimalism

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.firestore.FirebaseFirestore

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appName = intent.getStringExtra("appName") ?: "App"
        val firestoreDB = FirebaseFirestore.getInstance()

        firestoreDB.collection("appUsageInfo").document(appName)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usageTime = document.getLong("usageTime") ?: 0
                    if (usageTime > 60) { // 60 minutes threshold
                        triggerNotification(context, appName, usageTime)
                    }
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun triggerNotification(context: Context, appName: String, usageTime: Long) {
        createNotificationChannel(context)

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0)

        val notificationId = appName.hashCode() // Unique ID based on the app's name
        val textContent = "You have used $appName for more than ${usageTime} minutes today!"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_notifications_active_24)
            .setContentTitle("Usage Alert")
            .setContentText(textContent)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Usage Notifications"
            val descriptionText = "Notifications for app usage"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "usage_notification_channel"
    }
}
