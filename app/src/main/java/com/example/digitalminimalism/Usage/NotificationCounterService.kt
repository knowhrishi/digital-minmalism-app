package com.example.digitalminimalism.Usage

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationCounterService : NotificationListenerService() {
    companion object {
        var notificationCount = 0
            set(value) {
                field = value
            }

        private fun saveNotificationCount(context: Context) {
            val sharedPreferences = context.getSharedPreferences(
                "NotificationCounterService",
                Context.MODE_PRIVATE
            )
            val editor = sharedPreferences.edit()
            editor.putInt("notificationCount", notificationCount)
            editor.apply()
        }

        fun loadNotificationCount(context: Context): Int {
            val sharedPreferences =
                context.getSharedPreferences("NotificationCounterService", Context.MODE_PRIVATE)
            return sharedPreferences.getInt("notificationCount", 0)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        notificationCount++
        saveNotificationCount(applicationContext)

    }
}