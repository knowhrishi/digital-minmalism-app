package com.example.digitalminimalism.Focus.BottomNavigation

import TimerStatusService
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalminimalism.Focus.FocusAdapter
import com.example.digitalminimalism.Focus.FocusSessionDataClass
import com.example.digitalminimalism.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormat
import java.util.Calendar

class ScheduleNavFragment : Fragment() {

    private lateinit var uniqueID: String
    private val firestoreDB: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var setTimerButton: MaterialButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FocusAdapter

    @SuppressLint("MissingInflatedId", "HardwareIds")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_schedule_nav, container, false)

        uniqueID =
            Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)
        setTimerButton = view.findViewById(R.id.set_timer_button)
        recyclerView = view.findViewById(R.id.recycler_scheduled_timers)
        adapter = FocusAdapter(
            requireContext(),
            listOf()
        ) // Initialize with an empty list or fetched data

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        fetchScheduledTimers() // Load data and update adapter

        val datePickerButton: Button = view.findViewById(R.id.date_picker_button)
        val dateTextView: TextView = view.findViewById(R.id.date_text_view)
        val startTimePickerButton: Button = view.findViewById(R.id.start_time_picker_button)
        val startTimeTextView: TextView = view.findViewById(R.id.start_time_text_view)
        val endTimePickerButton: Button = view.findViewById(R.id.end_time_picker_button)
        val endTimeTextView: TextView = view.findViewById(R.id.end_time_text_view)

        datePickerButton.setOnClickListener {
            showDatePickerDialog(dateTextView)
        }

        startTimePickerButton.setOnClickListener {
            showTimePickerDialog(startTimeTextView)
        }

        endTimePickerButton.setOnClickListener {
            showTimePickerDialog(endTimeTextView)
        }

        setTimerButton.setOnClickListener {
            val selectedDate = DateFormat.getDateInstance(DateFormat.MEDIUM).parse(dateTextView.text.toString())
            val selectedStartTime = DateFormat.getTimeInstance(DateFormat.SHORT).parse(startTimeTextView.text.toString())
            val selectedEndTime = DateFormat.getTimeInstance(DateFormat.SHORT).parse(endTimeTextView.text.toString())

            if (selectedDate != null && selectedStartTime != null && selectedEndTime != null) {
                val startTime = Calendar.getInstance()
                startTime.time = selectedDate
                startTime.set(Calendar.HOUR_OF_DAY, selectedStartTime.hours)
                startTime.set(Calendar.MINUTE, selectedStartTime.minutes)

                val endTime = Calendar.getInstance()
                endTime.time = selectedDate
                endTime.set(Calendar.HOUR_OF_DAY, selectedEndTime.hours)
                endTime.set(Calendar.MINUTE, selectedEndTime.minutes)

                if (startTime.timeInMillis < System.currentTimeMillis() || endTime.timeInMillis <= startTime.timeInMillis) {
                    Toast.makeText(requireContext(), "Invalid start or end time", Toast.LENGTH_SHORT).show()
                } else {
                    scheduleTimer(startTime.timeInMillis, endTime.timeInMillis)
                }
            }
        }

        return view
    }

    private fun showDatePickerDialog(dateTextView: TextView) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDateInMillis ->
            val selectedDate = Calendar.getInstance()
            selectedDate.timeInMillis = selectedDateInMillis
            dateTextView.text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(selectedDate.time)
        }

        datePicker.show(parentFragmentManager, "MaterialDatePickerTag")
    }

    private fun showTimePickerDialog(timeTextView: TextView) {
        val materialTimePicker = MaterialTimePicker.Builder()
            .setTitleText("Select Time")
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .build()

        materialTimePicker.addOnPositiveButtonClickListener {
            val selectedTime = Calendar.getInstance()
            selectedTime.set(Calendar.HOUR_OF_DAY, materialTimePicker.hour)
            selectedTime.set(Calendar.MINUTE, materialTimePicker.minute)
            timeTextView.text = DateFormat.getTimeInstance(DateFormat.SHORT).format(selectedTime.time)
        }

        materialTimePicker.show(parentFragmentManager, "MaterialTimePickerTag")
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleTimer(startTimeInMillis: Long, endTimeInMillis: Long) {
        val timerSetAt = startTimeInMillis
        val timerSetUntil = endTimeInMillis
        val durationInMinutes = ((timerSetUntil - timerSetAt) / 1000 / 60).toInt()

        val sessionInfo = FocusSessionDataClass(
            timerSetUntil = timerSetUntil,
            duration = durationInMinutes.toLong(),
            setAt = System.currentTimeMillis(),
            status = "scheduled",
            startTime = startTimeInMillis,
            endTime = endTimeInMillis
        )

        val sessionInfoMap = hashMapOf(
            "timerSetUntil" to sessionInfo.timerSetUntil,
            "duration" to sessionInfo.duration,
            "setAt" to sessionInfo.setAt,
            "status" to sessionInfo.status,
            "startTime" to sessionInfo.startTime,
            "endTime" to sessionInfo.endTime
        )

        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        userTrackingRef.collection("focusModeInfo").add(sessionInfo)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Timer scheduled successfully", Toast.LENGTH_SHORT)
                    .show()
                fetchScheduledTimers() // Refresh the list of scheduled timers

                // Schedule the start and end of DND mode
                val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val startDndIntent = Intent("com.example.digitalminimalism.START_DND_MODE")
                val endDndIntent = Intent("com.example.digitalminimalism.END_DND_MODE")
                val startDndPendingIntent = PendingIntent.getBroadcast(requireContext(), 0, startDndIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val endDndPendingIntent = PendingIntent.getBroadcast(requireContext(), 1, endDndIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timerSetAt, startDndPendingIntent)
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timerSetUntil, endDndPendingIntent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to schedule timer: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun fetchScheduledTimers() {
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        userTrackingRef.collection("focusModeInfo")
            .whereEqualTo("status", "scheduled")
            .get()
            .addOnSuccessListener { documents ->
                val scheduledTimers = documents.map { document ->
                    FocusSessionDataClass(
                        timerSetUntil = document.getLong("timerSetUntil") ?: 0,
                        duration = document.getLong("duration") ?: 0,
                        setAt = document.getLong("setAt") ?: 0,
                        status = document.getString("status") ?: "unknown",
                        startTime = document.getLong("startTime") ?: 0,
                        endTime = document.getLong("endTime") ?: 0
                    )
                }
                adapter.updateSessions(scheduledTimers) // Update the adapter with the fetched data
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to fetch scheduled timers: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}