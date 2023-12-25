// ScheduleNavFragment.kt
package com.example.digitalminimalism.Focus.BottomNavigation.Schedule

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.digitalminimalism.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ScheduleNavFragment : Fragment() {

    private lateinit var uniqueID: String
    private val firestoreDB: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ScheduledTimerAdapter

    data class DndSchedule(
        var startTime: Calendar,
        var endTime: Calendar,
        val selectedDays: BooleanArray // Holds which days are selected, starting from Sunday
    )

    @SuppressLint("MissingInflatedId", "HardwareIds")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_schedule_nav, container, false)

        uniqueID =
            Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)
        recyclerView = view.findViewById(R.id.recycler_scheduled_timers)
        adapter = ScheduledTimerAdapter(mutableListOf()) // Initialize with an empty mutable list
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        fetchScheduledTimers() // Load data and update adapter

        // Setup click listeners for your UI elements here
        // For example:
        val setScheduleButton: Button = view.findViewById(R.id.button_set_schedule)
        setScheduleButton.setOnClickListener {
            // Your logic to set a schedule
            showSetScheduleDialog()
        }
        return view
    }

    // Global variable in your Fragment to hold the schedule data
    val dndSchedule = DndSchedule(
        startTime = Calendar.getInstance(), // Set the start time
        endTime = Calendar.getInstance(), // Set the end time
        selectedDays = BooleanArray(7) { false } // Set the selected days
    )

    private fun showSetScheduleDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_set_schedule, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Setup UI references
        val startTimeTextView: TextView = dialogView.findViewById(R.id.tvStartTime)
        val endTimeTextView: TextView = dialogView.findViewById(R.id.tvEndTime)
        val daysButtons = listOf(
            dialogView.findViewById<Button>(R.id.btnDaySun),
            dialogView.findViewById<Button>(R.id.btnDayMon),
            dialogView.findViewById<Button>(R.id.btnDayTue),
            dialogView.findViewById<Button>(R.id.btnDayWed),
            dialogView.findViewById<Button>(R.id.btnDayThur),
            dialogView.findViewById<Button>(R.id.btnDayFri),
            dialogView.findViewById<Button>(R.id.btnDaySat)
        )

        // Set onClickListeners for start and end time TextViews
        startTimeTextView.setOnClickListener {
            showTimePickerDialog(startTimeTextView)
        }
        endTimeTextView.setOnClickListener {
            showTimePickerDialog(endTimeTextView)
        }

        // Set onClickListeners for day buttons
        daysButtons.forEachIndexed { index, button ->
            // Initialize the button state based on dndSchedule.selectedDays
            updateDayButtonUI(button, dndSchedule.selectedDays[index])

            button.setOnClickListener {
                // Toggle the selected state
                dndSchedule.selectedDays[index] = !dndSchedule.selectedDays[index]
                updateDayButtonUI(button, dndSchedule.selectedDays[index])
                Log.d(
                    "ScheduleNavFragment",
                    "Day button clicked: ${button.text}, State: ${dndSchedule.selectedDays[index]}"
                )
            }
        }

        // Set the schedule when the "Set" button is clicked
        val btnSetSchedule = dialogView.findViewById<Button>(R.id.btnSetSchedule)
        btnSetSchedule.setOnClickListener {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startTimeText = startTimeTextView.text.toString()
            val endTimeText = endTimeTextView.text.toString()

            Log.d("ScheduleNavFragment", "Parsing start time: $startTimeText")
            dndSchedule.startTime = Calendar.getInstance().apply {
                time = timeFormat.parse(startTimeText) ?: return@setOnClickListener.apply {
                    Log.e("ScheduleNavFragment", "Failed to parse start time")
                }
            }

            Log.d("ScheduleNavFragment", "Parsing end time: $endTimeText")
            dndSchedule.endTime = Calendar.getInstance().apply {
                time = timeFormat.parse(endTimeText) ?: return@setOnClickListener.apply {
                    Log.e("ScheduleNavFragment", "Failed to parse end time")
                }
            }

            Log.d(
                "ScheduleNavFragment",
                "Set button clicked. Start Time: ${dndSchedule.startTime.time}, End Time: ${dndSchedule.endTime.time}"
            )
            Log.d(
                "ScheduleNavFragment",
                "Selected Days: ${dndSchedule.selectedDays.contentToString()}"
            )
            setDndSchedule(dndSchedule)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showTimePickerDialog(timeTextView: TextView) {
        val timePickerDialog = TimePickerDialog(view?.context, { _, hourOfDay, minute ->
            // Use 24-hour format for displaying the time
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeTextView.text = timeFormat.format(calendar.time)
        }, 0, 0, true) // Use 24-hour format
        timePickerDialog.show()
    }

    private fun updateDayButtonUI(button: Button, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(Color.BLACK) // Selected
            button.setTextColor(Color.WHITE)
        } else {
            button.setBackgroundColor(Color.WHITE) // Not selected
            button.setTextColor(Color.BLACK)
        }
    }

    private fun setDndSchedule(dndSchedule: DndSchedule) {
        Log.d("ScheduleNavFragment", "Setting DND schedule")
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val daysOfWeek = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

        // Prepare a HashMap to hold all timer information for the week
        val timerId = UUID.randomUUID().toString()

        val weeklyTimerInfo = hashMapOf<String, Any>(
            "id" to timerId,
            "startTime" to dndSchedule.startTime.timeInMillis,
            "endTime" to dndSchedule.endTime.timeInMillis,
            "selectedDays" to dndSchedule.selectedDays.mapIndexed { index, selected -> daysOfWeek[index] to selected }.toMap()
        )

        dndSchedule.selectedDays.forEachIndexed { index, isSelected ->
            if (isSelected) {
                // Creating intents that will be triggered when the alarm goes off
                val startDndIntent = Intent(context, StartDndReceiver::class.java)
                val endDndIntent = Intent(context, EndDndReceiver::class.java)

                // Unique request codes for the PendingIntent
                val requestCodeStart = index * 100 + dndSchedule.startTime.get(Calendar.HOUR_OF_DAY)
                val requestCodeEnd = index * 100 + dndSchedule.endTime.get(Calendar.HOUR_OF_DAY)

                val startDndPendingIntent = PendingIntent.getBroadcast(
                    context, requestCodeStart, startDndIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val endDndPendingIntent = PendingIntent.getBroadcast(
                    context, requestCodeEnd, endDndIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Scheduling the DND mode to be enabled and disabled at the chosen time, repeating weekly
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    getStartOfDayWithOffset(dndSchedule.startTime, index).timeInMillis,
                    AlarmManager.INTERVAL_DAY * 7,
                    startDndPendingIntent
                )
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    getStartOfDayWithOffset(dndSchedule.endTime, index).timeInMillis,
                    AlarmManager.INTERVAL_DAY * 7,
                    endDndPendingIntent
                )
            }
        }

        // Save the consolidated weekly timer information in Firestore
        firestoreDB.collection("userTracking").document(uniqueID)
            .collection("scheduledTimers").document("weeklySchedule")
            .set(weeklyTimerInfo)
            .addOnSuccessListener {
                Log.d("ScheduleNavFragment", "Weekly DND schedule saved to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("ScheduleNavFragment", "Failed to save weekly schedule to Firestore", e)
            }
    }

    private fun getStartOfDayWithOffset(time: Calendar, dayOffset: Int): Calendar {
        val offsetTime = time.clone() as Calendar
        offsetTime.set(Calendar.DAY_OF_WEEK, dayOffset + 1) // +1 because Calendar.DAY_OF_WEEK starts from Sunday as 1
        return offsetTime
    }

    data class ScheduledTimer(
        val id: String,
        val startTime: Long,
        val endTime: Long,
        val selectedDays: BooleanArray
    )

    private fun fetchScheduledTimers() {
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        val noDataAnimation: LottieAnimationView = view?.findViewById(R.id.no_data_animation) ?: return

        userTrackingRef.collection("scheduledTimers").document("weeklySchedule")
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val id = document.id
                    val startTime = document.getLong("startTime") ?: 0
                    val endTime = document.getLong("endTime") ?: 0
                    val selectedDaysMap = document.get("selectedDays") as Map<String, Boolean>

                    // Convert the selectedDays map to an array in the same order as daysOfWeek
                    val daysOfWeek = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                    val selectedDaysArray = BooleanArray(daysOfWeek.size) { index -> selectedDaysMap[daysOfWeek[index]] ?: false }

                    val scheduledTimer = ScheduledTimer(id, startTime, endTime, selectedDaysArray)
                    adapter.updateData(listOf(scheduledTimer)) // Update the adapter with a single fetched timer
                    noDataAnimation.visibility = View.GONE // Hide the animation
                } else {
                    Log.d("Firestore", "No scheduled timers found")
                    noDataAnimation.visibility = View.VISIBLE // Show the animation
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to fetch scheduled timers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
