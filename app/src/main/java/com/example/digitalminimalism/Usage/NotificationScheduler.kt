package com.example.digitalminimalism.Usage

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.digitalminimalism.R
import kotlin.random.Random

class NotificationScheduler {

   fun scheduleNotification(minutes: Int, appName: String, context: Context) {
    val notificationManager = NotificationManagerCompat.from(context)

    val notification = NotificationCompat.Builder(context, "channelId")
        .setSmallIcon(R.drawable.ic_silentnotif)
        .setContentTitle("Time's up!")
        .setContentText("You've reached your set time for $appName.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    val delay = minutes * 60 * 1000L
    val notificationId = Random.nextInt()

    val futureInMillis = SystemClock.elapsedRealtime() + delay
    AlarmManagerCompat.setExactAndAllowWhileIdle(
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager,
        AlarmManager.ELAPSED_REALTIME_WAKEUP,
        futureInMillis,
        PendingIntent.getBroadcast(
            context,
            notificationId,
            Intent(context, NotificationReceiver::class.java).apply {
                putExtra("notification", notification)
                putExtra("notificationId", notificationId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // Add FLAG_IMMUTABLE here
        )
    )
}
}