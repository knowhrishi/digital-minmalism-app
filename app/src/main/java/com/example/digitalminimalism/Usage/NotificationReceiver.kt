package com.example.digitalminimalism.Usage

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = NotificationManagerCompat.from(context)

        val notification = intent.getParcelableExtra<Notification>("notification")
        val notificationId = intent.getIntExtra("notificationId", 0)

        if (notification != null) {
            try {
                notificationManager.notify(notificationId, notification)
            } catch (e: SecurityException) {
                // Handle the SecurityException
            }
        }    }
}