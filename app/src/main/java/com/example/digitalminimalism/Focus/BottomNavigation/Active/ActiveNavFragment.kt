package com.example.digitalminimalism.Focus.BottomNavigation.Active

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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.digitalminimalism.Focus.FocusSessionDataClass
import com.example.digitalminimalism.Focus.FocusStats
import com.example.digitalminimalism.R
import com.example.digitalminimalism.SharedPreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class ActiveNavFragment : Fragment() {
    companion object {
        fun newInstance(remainingTime: Long, fullTime: Long, timerType: String): TimerBottomSheetFragment {
            return TimerBottomSheetFragment(remainingTime, fullTime, timerType)
        }
    }


    private lateinit var startTimerButton: MaterialButton
    private lateinit var timerInputDisplay: TextView
    private val numberPadButtons = mutableListOf<Button>()

    private lateinit var uniqueID: String
    private val firestoreDB: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val handler = Handler()
    private lateinit var countdownTextView: TextView
    private var countDownTimer: CountDownTimer? = null

    private var currentTimerDocId: String? = null

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
        val timerTypeDropdown: AutoCompleteTextView = view.findViewById(R.id.timer_type_dropdown)
        val timerTypes = resources.getStringArray(R.array.timer_types)
        val arrayAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, timerTypes)
        timerTypeDropdown.setAdapter(arrayAdapter)

        checkForActiveTimer()

        fetchFocusModeData() // Load data and update adapter


        startTimerButton = view.findViewById(R.id.start_timer_button)
        timerInputDisplay = view.findViewById(R.id.timer_input_display)

        // Get references to the number pad buttons
        for (i in 0..9) {
            val buttonId = resources.getIdentifier("button$i", "id", requireContext().packageName)
            numberPadButtons.add(view.findViewById(buttonId))
        }
        numberPadButtons.add(view.findViewById(R.id.button00))
        numberPadButtons.add(view.findViewById(R.id.buttonDelete))

        startTimerButton.setOnClickListener {
            // Parse the timer input
            val timeString = timerInputDisplay.text.toString()
            val timeParts = timeString.split(" ")
            val hours = timeParts[0].removeSuffix("h").toInt()
            val minutes = timeParts[1].removeSuffix("m").toInt()
            val seconds = timeParts[2].removeSuffix("s").toInt()
            val totalSeconds = hours * 3600 + minutes * 60 + seconds
            val timerTypeDropdown: AutoCompleteTextView = requireView().findViewById(R.id.timer_type_dropdown)
            val selectedTimerType = timerTypeDropdown.text.toString()
            if (selectedTimerType.isEmpty()) {
                // No timer type is selected, show a Toast message
                Toast.makeText(requireContext(), "Please select a type", Toast.LENGTH_SHORT).show()
            } else {
                // Check if the timer value is valid (non-zero)
                if (totalSeconds > 0) {
                    setFocusMode(totalSeconds)
                } else {
                    Toast.makeText(requireContext(), "Please set a valid timer", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }


        numberPadButtons.forEach { button ->
            button.setOnClickListener {
                val tag = button.tag.toString()
                var currentText =
                    timerInputDisplay.text.toString().filter { it.isDigit() }.padStart(6, '0')

                if (tag == "delete") {
                    // Shift digits to the right and add a zero at the start
                    currentText = "0" + currentText.dropLast(1)
                } else {
                    // Shift digits to the left and add the new digit at the end
                    currentText = (currentText + tag).takeLast(6)
                }

                // Format the text to maintain the "HHh MMm SSs" format
                val formattedText = "${currentText.substring(0, 2)}h ${
                    currentText.substring(
                        2,
                        4
                    )
                }m ${currentText.substring(4, 6)}s"
                timerInputDisplay.text = formattedText
                startTimerButton.visibility =
                    if (formattedText != "00h 00m 00s") View.VISIBLE else View.INVISIBLE
            }
        }



        return view
    }


    // Helper function to format the time text
    private fun formatTimeText(timeText: String): String {
        val digits = timeText.filter { it.isDigit() }.padStart(6, '0')
        return "${digits.substring(0, 2)}h ${digits.substring(2, 4)}m ${digits.substring(4, 6)}s"
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


    private fun setFocusMode(seconds: Int) {
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            // If permission is not granted, request it
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        } else {
            // Enable DND mode
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            // Set a timer to disable DND mode after the specified time (in milliseconds)
            val timerSetUntil = System.currentTimeMillis() + seconds * 1000L
            saveTimerToFirestore(
                timerSetUntil,
                seconds
            ) // Convert seconds to minutes for Firestore


            handler.postDelayed({
                if (isAdded) {
                    resetDndMode(notificationManager) // Reset DND mode after the timer expires
                }
            }, seconds * 1000L) // Convert seconds to milliseconds
        }
        checkForActiveTimer()

    }

    private fun resetDndMode(notificationManager: NotificationManager) {
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        Toast.makeText(requireContext(), "Focus mode ended. DND turned off.", Toast.LENGTH_SHORT)
            .show()
    }

    private fun saveTimerToFirestore(timerSetUntil: Long, seconds: Int) {
        val timerTypeDropdown: AutoCompleteTextView = requireView().findViewById(R.id.timer_type_dropdown)
        val selectedTimerType = timerTypeDropdown.text.toString()
        Log.d("ActiveNavFragment", "saveTimerToFirestore: selectedTimerType: $selectedTimerType")
        val startTime = System.currentTimeMillis()
        val endTime = startTime + seconds * 1000 // Convert seconds to milliseconds
        val sessionInfo = FocusSessionDataClass(
            timerSetUntil = timerSetUntil,
            duration = seconds.toLong(), // Save duration in seconds
            setAt = System.currentTimeMillis(),
            status = "active",
            startTime = startTime,
            endTime = endTime,
            timerType = selectedTimerType
        )

        val sessionInfoMap = hashMapOf(
            "timerSetUntil" to sessionInfo.timerSetUntil,
            "duration" to sessionInfo.duration,
            "setAt" to sessionInfo.setAt,
            "status" to sessionInfo.status,
            "startTime" to sessionInfo.startTime,
            "endTime" to sessionInfo.endTime,
            "timerType" to sessionInfo.timerType
        )
        // Reference to the user's document in   the 'userTracking' collection
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        // Add the timer information to a new document in the 'focusModeInfo' subcollection
        userTrackingRef.collection("focusModeInfo").add(sessionInfo)
            .addOnSuccessListener { documentReference ->
                currentTimerDocId = documentReference.id
                SharedPreferencesManager.init(requireContext())
                SharedPreferencesManager.saveTimerDocId(currentTimerDocId)
                Log.d(
                    "ActiveNavFragment",
                    "currentTimerDocId-saveTimerToFirestore: ${currentTimerDocId}"
                )
                Toast.makeText(
                    requireContext(),
                    "Focus mode set for $seconds seconds",
                    Toast.LENGTH_SHORT
                ).show()
                // Retrieve the document to check the timerType
                documentReference.get()
                    .addOnSuccessListener { documentSnapshot ->
                        val savedTimerType = documentSnapshot.getString("timerType")
                        Log.d("ActiveNavFragment", "Saved timerType: $savedTimerType")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ActiveNavFragment", "Failed to fetch document: ${e.message}")
                    }
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
        resetDndMode(requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        SharedPreferencesManager.init(requireContext())
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

        fetchFocusModeData()

    }

    private fun calculateTotalFocusTime(documents: QuerySnapshot): Int {
        return documents.documents
            .filter { it.getString("status") == "completed" }
            .sumOf { it.getLong("duration") ?: 0 }.toInt()
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
                        val durationMinutes =
                            document.getLong("duration") ?: 0 // Duration in minutes
                        val timerType = document.getString("timerType") ?: "default" // Assuming "default" is a valid timer type.


                        if (System.currentTimeMillis() < timerSetUntil) {
                            if (!activeTimerFound) {
                                // This is the first active timer found
                                activeTimerFound = true

                                val remainingTime = timerSetUntil - System.currentTimeMillis()
                                val fullTime =
                                    durationMinutes * 60 * 1000 // Convert minutes to milliseconds

                                // Show the bottom sheet
                                showTimerBottomSheet(remainingTime, fullTime, timerType)
                                startCountdown(timerSetUntil)
                            }
                        } else {
                            // The timer is no longer active, update its status in Firestore
                            document.reference.update("status", "completed")
                        }
                    }
                    if (!activeTimerFound) {
//                        countdownTextView.text = "No active focus mode."
                    }
                } else {
//                    countdownTextView.text = "No active focus mode."
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Failed to fetch focus mode data",
                    Toast.LENGTH_SHORT
                ).show()
            }
        fetchFocusModeData()
    }

    private fun showTimerBottomSheet(remainingTime: Long, fullTime: Long, timerType: String) {
        val timerBottomSheetFragment = TimerBottomSheetFragment.newInstance(remainingTime, fullTime, timerType)
        timerBottomSheetFragment.show(parentFragmentManager, timerBottomSheetFragment.tag)
    }

    private fun startCountdown(timerSetUntil: Long) {
        countDownTimer?.cancel() // Cancel any existing timer
        val totalDuration = timerSetUntil - System.currentTimeMillis()
        val remainingTime = timerSetUntil - System.currentTimeMillis()
        countDownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = millisUntilFinished / 1000 / 60 / 60
                val minutes = millisUntilFinished / 1000 / 60 % 60
                val seconds = millisUntilFinished / 1000 % 60
//                countdownTextView.text =
//                    String.format("%02d HRS : %02d MIN : %02d SEC", hours, minutes, seconds)
//
            }

            @SuppressLint("SetTextI18n")
            override fun onFinish() {
                resetDndMode(requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
//                countdownTextView.text = "00 HRS : 00 MIN : 00 SEC"
//                progressIndicator.visibility = View.GONE
//                cancelButton.visibility = View.GONE
//                setFocusButton.visibility = View.VISIBLE
//                countdownTextView.text = "No active focus mode."

//                fetchFocusModeData()
                currentTimerDocId?.let { docId ->
                    firestoreDB.collection("userTracking").document(uniqueID)
                        .collection("focusModeInfo").document(docId)
                        .update("status", "completed")
                        .addOnSuccessListener {
                            Log.d(
                                "FocusFragment",
                                "Focus mode successfully updated in Firestore."
                            )
                            Toast.makeText(
                                requireContext(),
                                "Focus mode completed and status updated in Firestore",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
        }.start()
//        cancelButton.visibility = View.VISIBLE // Show the cancel button
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

    override fun onDetach() {
        super.onDetach()
        countDownTimer?.cancel()
    }
}