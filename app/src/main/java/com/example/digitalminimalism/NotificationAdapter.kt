package com.example.digitalminimalism

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class NotificationAdapter(private var usages: List<UsageMonitoringFragment.AppUsage>, private val context: Context) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {
    private val uniqueID = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    private val firestoreDB = FirebaseFirestore.getInstance()
    private val userAppUsageRef = firestoreDB.collection("userTracking").document(uniqueID)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_notification, parent, false)
        return NotificationViewHolder(view, userAppUsageRef) // Pass the reference to the ViewHolder
    }
    fun updateData(newUsages: List<UsageMonitoringFragment.AppUsage>) {
        this.usages = newUsages
        notifyDataSetChanged() // This will refresh the RecyclerView
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val usage = usages[position]
        holder.bind(usage, position)
    }

    override fun getItemCount(): Int = usages.size

    class NotificationViewHolder(private val view: View, private val userAppUsageRef: DocumentReference) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.icon)
        private val appName: TextView = view.findViewById(R.id.app_name)
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        private val notificationSwitch: Switch = view.findViewById(R.id.switch_notification)
        private val remainingTimeTextView: TextView = view.findViewById(R.id.remaining_time) // Change: New TextView for remaining time
        private val firestoreDB = FirebaseFirestore.getInstance()



        @SuppressLint("ScheduleExactAlarm", "HardwareIds", "SetTextI18n")
        fun bind(appUsage: UsageMonitoringFragment.AppUsage, position: Int) { // Change: Receive position parameter
            icon.setImageResource(appUsage.icon)
            val data = hashMapOf(
                "usageTime" to appUsage.usageTime,
                "lastUpdated" to System.currentTimeMillis(),
                // Add other properties as needed
            )
            appName.text = appUsage.appName
            // Change: Calculate remaining time and set it on the TextView
            val remainingTime = (60 - appUsage.usageTime).coerceAtLeast(0) // Ensure it doesn't go below 0
            remainingTimeTextView.text = "Remaining: $remainingTime min"

            userAppUsageRef.collection("appUsageInfo").document(appUsage.appName)
                .get()
                .addOnSuccessListener { document ->
                    val isNotificationEnabled = document.getBoolean("isNotificationEnabled") ?: false
                    notificationSwitch.isChecked = isNotificationEnabled
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error checking document", e)
                }
            notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
                val alarmManager = view.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                val intent = Intent(view.context, BroadcastReceiver::class.java).apply {
                    putExtra("appName", appUsage.appName)
                    putExtra("notificationId", adapterPosition) // Unique ID for each app's notification
                    val data = hashMapOf(
                        "isNotificationEnabled" to isChecked
                        // Add other properties if needed
                    )
                    userAppUsageRef.collection("appUsageInfo").document(appUsage.appName)
                        .update("isNotificationEnabled", isChecked)
                        .addOnSuccessListener { Log.d("Firestore", "Notification status updated for ${appUsage.appName}") }
                        .addOnFailureListener { e -> Log.w("Firestore", "Error updating document", e) }

                }
                // PendingIntent flags
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getBroadcast(view.context, adapterPosition, intent, flags)

                if (isChecked) {
                    // Schedule the notification for 24 hours later
                    val triggerTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    Log.d("NotificationAdapter", "Alarm set for ${appUsage.appName}")

                } else {
                    // Cancel the notification if it is already scheduled
                    alarmManager.cancel(pendingIntent)
                    Log.d("NotificationAdapter", "Alarm canceled for ${appUsage.appName}")

                }
            }
            Log.d("NotificationAdapter", "Binding view for ${appUsage.appName} at position $position")


        }
    }
}
