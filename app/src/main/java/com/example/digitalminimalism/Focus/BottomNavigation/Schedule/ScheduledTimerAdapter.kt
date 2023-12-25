// ScheduledTimerAdapter.kt
package com.example.digitalminimalism.Focus.BottomNavigation.Schedule

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalminimalism.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ScheduledTimerAdapter(private var scheduledTimers: MutableList<ScheduleNavFragment.ScheduledTimer>) :
    RecyclerView.Adapter<ScheduledTimerAdapter.ViewHolder>() {
    private val firestoreDB: FirebaseFirestore = FirebaseFirestore.getInstance() // Add this line
    private lateinit var uniqueID: String

    inner class ViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.item_scheduled_timer, parent, false)) {
        private var timeRangeTextView: TextView? = null
        private var binImageView: ImageView? = null

        init {
            timeRangeTextView = itemView.findViewById(R.id.text_view_time_range)
            binImageView = itemView.findViewById(R.id.imageView_bin)
        }

        fun bind(scheduledTimer: ScheduleNavFragment.ScheduledTimer) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeRangeTextView?.text = itemView.context.getString(
                R.string.time_range_format,
                timeFormat.format(Date(scheduledTimer.startTime)),
                timeFormat.format(Date(scheduledTimer.endTime))
            )

            val weekdaysLayout: LinearLayout = itemView.findViewById(R.id.weekdays_layout)
            for (i in 0 until weekdaysLayout.childCount) {
                val dayTextView = weekdaysLayout.getChildAt(i) as TextView
                val isSelected = scheduledTimer.selectedDays[i]
                dayTextView.setTextColor(if (isSelected) Color.WHITE else Color.BLACK)
                dayTextView.setBackgroundResource(if (isSelected) R.drawable.circle_background_selected else R.drawable.circle_background_notselected)
            }
            binImageView?.setOnClickListener {
                showDeletionConfirmationDialog(scheduledTimer.id, itemView.context)
            }
        }
    }

    private fun showDeletionConfirmationDialog(timerId: String, context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete this timer?")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteScheduledTimer(timerId, context)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun deleteScheduledTimer(timerId: String, context: Context) {
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        userTrackingRef.collection("scheduledTimers").document(timerId)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot successfully deleted!")
                removeTimerFromList(timerId)
                Toast.makeText(context, "Timer deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error deleting document", e)
                Toast.makeText(context, "Error deleting timer", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun removeTimerFromList(timerId: String) {
        scheduledTimers.removeAll { it.id == timerId }
        notifyDataSetChanged()
    }


    @SuppressLint("HardwareIds")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        uniqueID = Settings.Secure.getString(parent.context.contentResolver, Settings.Secure.ANDROID_ID)
        return ViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scheduledTimer: ScheduleNavFragment.ScheduledTimer = scheduledTimers[position]
        holder.bind(scheduledTimer)
    }

    override fun getItemCount(): Int = scheduledTimers.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newTimers: List<ScheduleNavFragment.ScheduledTimer>) {
        scheduledTimers = newTimers.toMutableList()
        notifyDataSetChanged()
    }
}

