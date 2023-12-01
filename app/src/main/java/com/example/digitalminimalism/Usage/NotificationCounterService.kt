package com.example.digitalminimalism.Usage

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationCounterService : NotificationListenerService() {
    companion object {
        var notificationCount = 0
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        notificationCount++
    }
}