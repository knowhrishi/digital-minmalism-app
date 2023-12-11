package com.example.digitalminimalism.Focus.BottomNavigation.Active

import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.digitalminimalism.R
import com.example.digitalminimalism.SharedPreferencesManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.firestore.FirebaseFirestore

class TimerBottomSheetFragment(private val remainingTime: Long, private val fullTime: Long,  private val timerType: String) : BottomSheetDialogFragment() {

    private var countDownTimer: CountDownTimer? = null
    private lateinit var timerProgress: CircularProgressIndicator
    private lateinit var timerTextView: TextView
    private lateinit var uniqueID: String
    private val firestoreDB: FirebaseFirestore = FirebaseFirestore.getInstance()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_timer_bottom_sheet, container, false)
    }

    companion object {
        fun newInstance(remainingTime: Long, fullTime: Long, timerType: String): TimerBottomSheetFragment { // Add timerType parameter here
            val fragment = TimerBottomSheetFragment(remainingTime, fullTime, timerType) // Pass timerType here
            val args = Bundle()
            args.putLong("remainingTime", remainingTime)
            args.putLong("fullTime", fullTime)
            args.putString("timerType", timerType) // Add this line
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uniqueID =
            Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)
        timerTextView = view.findViewById(R.id.timerTextView)
        val timerTitle: TextView = view.findViewById(R.id.timerTitle) // Get reference to timerTitle

        val timerTypeTextView: TextView = view.findViewById(R.id.timerTypeTextView)
        timerTypeTextView.text = timerType

        val pauseButton: Button = view.findViewById(R.id.pauseButton)
        val addButton: Button = view.findViewById(R.id.buttonCancelTimer)
        timerProgress = view.findViewById(R.id.timerProgress)

        // Set the max value for the progress bar
        timerProgress.max = fullTime.toInt()
        // Set the timerTitle text to the full duration of the timer
        timerTitle.text = formatTimeInLongString(fullTime)

        // Initialize and start your countdown timer here
        countDownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTextView.text = formatTime(millisUntilFinished)
                val progress = (fullTime - millisUntilFinished).toInt()
                timerProgress.setProgress(progress, true)
            }

            override fun onFinish() {
                timerTextView.text = formatTime(0)
                timerProgress.setProgress(fullTime.toInt(), true)
                dismiss() // Dismiss the bottom sheet when the timer is done

            }
        }.start()

        pauseButton.setOnClickListener {
            // Handle pause button click
        }

        addButton.setOnClickListener {
            // Handle add time button click
        }

        val closeButton: ImageView = view.findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            countDownTimer?.cancel()
            dismiss()

            // Get the document ID of the current timer from the shared preferences
            val currentTimerDocId = SharedPreferencesManager.getTimerDocId()

            // Update the status of the current timer in Firestore to "cancel"
            if (currentTimerDocId != null) {
                val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
                userTrackingRef.collection("focusModeInfo").document(currentTimerDocId)
                    .update("status", "cancelled")
                    .addOnSuccessListener {
                        Log.d("ActiveNavFragment", "Timer status successfully updated to 'cancel' in Firestore.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ActiveNavFragment", "Failed to update timer status in Firestore: ${e.message}")
                    }
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        val seconds = (millis / 1000) % 60
        return String.format("%02dh:%02dm:%02ds", hours, minutes, seconds)
    }

    private fun formatTimeInLongString(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        val seconds = (millis / 1000) % 60

        val formattedHours = if (hours > 0) "$hours hour${if (hours > 1) "s" else ""} " else ""
        val formattedMinutes = if (minutes > 0) "$minutes minute${if (minutes > 1) "s" else ""} " else ""
        val formattedSeconds = if (seconds > 0 || (hours == 0L && minutes == 0L)) "$seconds second${if (seconds > 1 || seconds == 0L) "s" else ""}" else ""

        return formattedHours + formattedMinutes + formattedSeconds
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }


}
