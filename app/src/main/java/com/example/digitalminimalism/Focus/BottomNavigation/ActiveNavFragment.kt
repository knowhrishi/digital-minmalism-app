package com.example.digitalminimalism.Focus.BottomNavigation

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalminimalism.Focus.FocusAdapter
import com.example.digitalminimalism.Focus.FocusSessionDataClass
import com.example.digitalminimalism.Focus.FocusStats
import com.example.digitalminimalism.R
import com.example.digitalminimalism.SharedPreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class ActiveNavFragment : Fragment() {

    private lateinit var uniqueID: String
    private val firestoreDB: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val handler = Handler()
    private lateinit var countdownTextView: TextView
    private var countDownTimer: CountDownTimer? = null
    private lateinit var cancelButton: MaterialButton
    private lateinit var setFocusButton: MaterialButton
    private var currentTimerDocId: String? = null
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var statusTextView: TextView
    private lateinit var streaksTextView: TextView
    private lateinit var goalProgressBar: ProgressBar

    @SuppressLint("MissingInflatedId", "HardwareIds")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_active_nav, container, false)
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Focus Mode"

        uniqueID =
            Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)
        setFocusButton = view.findViewById(R.id.set_focus_button)
        countdownTextView = view.findViewById(R.id.countdown_text_view)
        cancelButton = view.findViewById(R.id.cancel_button)
        progressIndicator = view.findViewById(R.id.progress_circular)
        statusTextView = view.findViewById(R.id.status_text_view)
        streaksTextView = view.findViewById(R.id.streaks_text_view)
        goalProgressBar = view.findViewById(R.id.goal_progress_bar)
        cancelButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancel Focus Mode")
                .setMessage("Are you sure you want to cancel the focus mode?")
                .setNegativeButton("No") { dialog, _ ->
                    // Dismiss the dialog if user chooses 'No'
                    dialog.dismiss()
                }
                .setPositiveButton("Yes") { dialog, _ ->
                    // Cancel the focus mode if user chooses 'Yes'
                    cancelFocusMode()
                    dialog.dismiss()
                }
                .show()
        }
        cancelButton.visibility = View.GONE
        checkForActiveTimer()


        fetchFocusModeData() // Load data and update adapter
        setFocusButton.setOnClickListener {
            showTimePickerDialog()
        }

        return view
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_statistics -> {
                // Navigate to Statistics Fragment or Activity
                Intent(requireContext(), FocusStats::class.java).also {
                    startActivity(it)
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchFocusModeData() {
        // Fetch the focus mode data from Firestore
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        userTrackingRef.collection("focusModeInfo")
            .get()
            .addOnSuccessListener { documents ->
                // Process the data to calculate the stats
                val totalFocusTime = calculateTotalFocusTime(documents)
                val averageFocusTime = calculateAverageFocusTime(documents)
                val longestFocusSession = calculateLongestFocusSession(documents)

                // Display the stats
                view?.findViewById<TextView>(R.id.total_focus_time_text_view)?.text =
                    "Total Time: $totalFocusTime"
                view?.findViewById<TextView>(R.id.average_focus_time_text_view)?.text =
                    "Average Time: $averageFocusTime"
                view?.findViewById<TextView>(R.id.longest_focus_session_text_view)?.text =
                    "Longest Session: $longestFocusSession"
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to fetch focus mode data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private fun calculateAverageFocusTime(documents: QuerySnapshot): Int {
        val completedSessions = documents.documents.filter { it.getString("status") == "completed" }
        return if (completedSessions.isNotEmpty()) {
            completedSessions.sumOf { it.getLong("duration") ?: 0 }.toInt() / completedSessions.size
        } else {
            0
        }
    }

    private fun calculateLongestFocusSession(documents: QuerySnapshot): Int {
        return documents.documents
            .filter { it.getString("status") == "completed" }
            .maxOfOrNull { it.getLong("duration") ?: 0 }?.toInt() ?: 0
    }


    @SuppressLint("SetTextI18n")
    private fun updateStatusTextView(status: String) {
        statusTextView.text = "Status: $status"
    }

    private fun showTimePickerDialog() {
        val materialTimePicker = MaterialTimePicker.Builder()
            .setTitleText("Set Focus Mode Timer")
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .build()

        materialTimePicker.addOnPositiveButtonClickListener {
            val totalMinutes = materialTimePicker.hour * 60 + materialTimePicker.minute
            setFocusMode(totalMinutes)
        }

//        materialTimePicker.show(childFragmentManager, "MaterialTimePickerTag")
        materialTimePicker.show(parentFragmentManager, "MaterialTimePickerTag")
    }

    private fun setFocusMode(minutes: Int) {
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            // If permission is not granted, request it
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        } else {
            // Enable DND mode
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            // Set a timer to disable DND mode after the specified time
            val timerSetUntil = System.currentTimeMillis() + minutes * 60 * 1000
            saveTimerToFirestore(timerSetUntil, minutes)

            handler.postDelayed({
                resetDndMode(notificationManager)
            }, minutes * 60 * 1000L)
        }
        checkForActiveTimer()

    }

    private fun resetDndMode(notificationManager: NotificationManager) {
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        Toast.makeText(requireContext(), "Focus mode ended. DND turned off.", Toast.LENGTH_SHORT)
            .show()
    }

    private fun saveTimerToFirestore(timerSetUntil: Long, durationInMinutes: Int) {

        val startTime = System.currentTimeMillis()
        val endTime = startTime + durationInMinutes * 60 * 1000
        val sessionInfo = FocusSessionDataClass(
            timerSetUntil = timerSetUntil,
            duration = durationInMinutes.toLong(),
            setAt = System.currentTimeMillis(),
            status = "active",
            startTime = startTime,
            endTime = endTime
        )

        val sessionInfoMap = hashMapOf(
            "timerSetUntil" to sessionInfo.timerSetUntil,
            "duration" to sessionInfo.duration,
            "setAt" to sessionInfo.setAt,
            "status" to sessionInfo.status,
            "startTime" to sessionInfo.startTime,
            "endTime" to sessionInfo.endTime
        )
        // Reference to the user's document in   the 'userTracking' collection
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        // Add the timer information to a new document in the 'focusModeInfo' subcollection
        userTrackingRef.collection("focusModeInfo").add(sessionInfo)
            .addOnSuccessListener { documentReference ->
                currentTimerDocId = documentReference.id
                SharedPreferencesManager.saveTimerDocId(currentTimerDocId)
                Log.d(
                    "FocusFragment",
                    "currentTimerDocId-saveTimerToFirestore: ${currentTimerDocId}"
                )
                Toast.makeText(
                    requireContext(),
                    "Focus mode set for $durationInMinutes minutes",
                    Toast.LENGTH_SHORT
                ).show()

            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to set focus mode", Toast.LENGTH_SHORT)
                    .show()
            }

    }

    @SuppressLint("ServiceCast", "SetTextI18n")
    private fun cancelFocusMode() {
        // Cancel the focus mode if user chooses 'Yes'
        countDownTimer?.cancel()
        countdownTextView.text = "Focus mode canceled."
        progressIndicator.visibility = View.GONE
        setFocusButton.visibility = View.VISIBLE
        resetDndMode(requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        val docId1 = SharedPreferencesManager.getTimerDocId()
        Log.d(
            "FocusFragment",
            "Attempting to cancel focus mode in Firestore for Doc ID: $docId1"
        )
        docId1?.let { docId ->
            val userTrackingRef =
                firestoreDB.collection("userTracking").document(uniqueID)
            userTrackingRef.collection("focusModeInfo").document(docId)
                .update("status", "incomplete")
                .addOnSuccessListener {
                    Log.d(
                        "FocusFragment",
                        "Focus mode successfully updated in Firestore."
                    )
                    Toast.makeText(
                        requireContext(),
                        "Focus mode canceled and status updated in Firestore",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateStatusTextView("Inactive") // Update UI here
                }
                .addOnFailureListener { e ->
                    Log.e("FocusFragment", "Failed to update Firestore: ${e.message}")
                    Toast.makeText(
                        requireContext(),
                        "Failed to update Firestore: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: Log.e("FocusFragment", "Document ID is null, cannot update Firestore.")

        updateStreaksAndTotalFocusTime()
        fetchFocusModeData()
        updateStatusTextView("Inactive")
        countdownTextView.text = "Focus mode canceled. Start New Focus Mode."
        updateStatusTextView("Inactive")
        progressIndicator.visibility = View.GONE
        setFocusButton.visibility = View.VISIBLE
        cancelButton.visibility = View.GONE

    }

    private fun calculateTotalFocusTime(documents: QuerySnapshot): Int {
        return documents.documents
            .filter { it.getString("status") == "completed" }
            .sumOf { it.getLong("duration") ?: 0 }.toInt()
    }

    private fun calculateStreaks(documents: QuerySnapshot): Int {
        val sortedSessions = documents.documents
            .filter { it.getString("status") == "completed" }
            .sortedByDescending { it.getLong("startTime") }

        var streaks = 0
        var previousSessionDay = 0L

        for (session in sortedSessions) {
            val sessionDay = session.getLong("startTime")?.let { it / (1000 * 60 * 60 * 24) } ?: 0L
            if (previousSessionDay == 0L || sessionDay == previousSessionDay - 1) {
                streaks++
                previousSessionDay = sessionDay
            } else if (sessionDay < previousSessionDay - 1) {
                break
            }
        }

        return streaks
    }

    @SuppressLint("SetTextI18n")
    private fun checkForActiveTimer() {
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        userTrackingRef.collection("focusModeInfo")
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { documents ->
                // Explicitly check if documents contain any elements
                if (!documents.isEmpty) {
                    var activeTimerFound = false
                    for (document in documents) {
                        val timerSetUntil = document.getLong("timerSetUntil") ?: 0
                        if (System.currentTimeMillis() < timerSetUntil) {
                            if (!activeTimerFound) {
                                // This is the first active timer found
                                activeTimerFound = true
                                setFocusButton.visibility = View.GONE
                                startCountdown(timerSetUntil)
                                updateStatusTextView("Active")
                                streaksTextView.text =
                                    "Current Streak: ${document.getLong("currentStreak")}"
                            }
                        } else {
                            // The timer is no longer active, update its status in Firestore
                            document.reference.update("status", "completed")
                        }
                    }
                    if (!activeTimerFound) {
                        countdownTextView.text = "No active focus mode."
                        updateStatusTextView("Inactive")
                    }
                } else {
                    countdownTextView.text = "No active focus mode."
                    updateStatusTextView("Inactive")
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Failed to fetch focus mode data",
                    Toast.LENGTH_SHORT
                ).show()
            }
        updateStreaksAndTotalFocusTime()
        fetchFocusModeData()
    }

    private fun startCountdown(timerSetUntil: Long) {
        countDownTimer?.cancel() // Cancel any existing timer
        val totalDuration = timerSetUntil - System.currentTimeMillis()
        val remainingTime = timerSetUntil - System.currentTimeMillis()
        progressIndicator.max = totalDuration.toInt()
        progressIndicator.setProgressCompat(progressIndicator.max, true) // Start at full progress
        progressIndicator.visibility = View.VISIBLE // Make the progress indicator visible
        countDownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = millisUntilFinished / 1000 / 60 / 60
                val minutes = millisUntilFinished / 1000 / 60 % 60
                countdownTextView.text = if (hours > 0) {
                    "Remaining time: ${formatTime(millisUntilFinished)}"
                } else {
                    "Remaining time: ${formatTime(millisUntilFinished)}"
                }

                // Decrease the progress indicator
                val progress = millisUntilFinished.toInt()
                progressIndicator.setProgressCompat(progress, true)
            }

            @SuppressLint("SetTextI18n")
            override fun onFinish() {
                resetDndMode(requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                countdownTextView.text = "Focus mode completed."
                progressIndicator.visibility = View.GONE
                cancelButton.visibility = View.GONE
                setFocusButton.visibility = View.VISIBLE
                countdownTextView.text = "No active focus mode."

                fetchFocusModeData()
                currentTimerDocId?.let { docId ->
                    firestoreDB.collection("userTracking").document(uniqueID)
                        .collection("focusModeInfo").document(docId)
                        .update("status", "completed")
                        .addOnSuccessListener {
                            updateStreaksAndTotalFocusTime()
                        }
                }
            }
        }.start()
        cancelButton.visibility = View.VISIBLE // Show the cancel button
        updateStreaksAndTotalFocusTime()
    }

    @SuppressLint("SetTextI18n")
    private fun updateStreaksAndTotalFocusTime() {
        val WEEKLY_FOCUS_GOAL = 5

        firestoreDB.collection("userTracking").document(uniqueID)
            .collection("focusModeInfo")
            .orderBy("startTime", Query.Direction.DESCENDING)
            .limit(7) // Modify as needed
            .get()
            .addOnSuccessListener { documents ->
                val streaks = calculateStreaks(documents)
                val totalFocusTime = calculateTotalFocusTime(documents)

                // Check if the total focus time meets the weekly goal
                if (totalFocusTime >= WEEKLY_FOCUS_GOAL) {
                    // Code to update Firestore with the achieved goal
                    firestoreDB.collection("userTracking").document(uniqueID)
                        .update("weeklyGoalAchieved", true)
                }
                // Code to update Firestore with the new streaks count
                firestoreDB.collection("userTracking").document(uniqueID)
                    .update("currentStreak", streaks)
                streaksTextView.text = "Current Streak: $streaks"
                goalProgressBar.progress =
                    (totalFocusTime.toFloat() / WEEKLY_FOCUS_GOAL * 100).toInt()

            }
            .addOnFailureListener { /* Handle failure */ }
    }

    private fun formatTime(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = millis / (1000 * 60) % 60
        val seconds = millis / 1000 % 60
        return when {
            hours > 0 -> "$hours hrs $minutes mins $seconds secs"
            minutes > 0 -> "$minutes mins $seconds secs"
            else -> "$seconds secs"
        }
    }


}