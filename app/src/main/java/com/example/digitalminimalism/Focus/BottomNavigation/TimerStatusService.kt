import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class TimerStatusService : Service() {

    private val handler = Handler()
    private val runnableCode: Runnable = object : Runnable {
        override fun run() {
            checkTimerStatus()
            handler.postDelayed(this, 60000) // Repeat every 60 seconds
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "channelId",
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "channelId")
            .setContentTitle("Foreground Service")
            .setContentText("Checking timer status...")
            .build()

        startForeground(1, notification)

        handler.post(runnableCode)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnableCode)
    }

    private fun checkTimerStatus() {
        // Get an instance of the Firestore database
        val firestoreDB = FirebaseFirestore.getInstance()

        // Get the current time in milliseconds
        val currentTime = System.currentTimeMillis()

        // Fetch all documents from the 'userTracking' collection
        firestoreDB.collection("userTracking")
            .get()
            .addOnSuccessListener { documents ->
                // Iterate over each document
                for (document in documents) {
                    // Get the unique ID of the document
                    val uniqueID = document.id

                    // Fetch all documents from the 'focusModeInfo' subcollection of the current document
                    // where the 'status' field is either 'scheduled' or 'active'
                    firestoreDB.collection("userTracking").document(uniqueID)
                        .collection("focusModeInfo")
                        .whereIn("status", listOf("scheduled", "active"))
                        .get()
                        .addOnSuccessListener { timerDocuments ->
                            // Iterate over each timer document
                            for (timerDocument in timerDocuments) {
                                // Get the start and end times of the timer
                                val startTime = timerDocument.getLong("startTime") ?: 0
                                val endTime = timerDocument.getLong("endTime") ?: 0

                                // Determine the new status of the timer based on the current time and the start and end times
                                val newStatus = when {
                                    currentTime in startTime until endTime -> "active" // Current time is between start and end times
                                    currentTime >= endTime -> "completed" // Current time is after end time
                                    else -> "scheduled" // Current time is before start time
                                }

                                // Update the 'status' field of the timer document with the new status
                                timerDocument.reference.update("status", newStatus)
                            }
                        }
                }
            }
    }
}