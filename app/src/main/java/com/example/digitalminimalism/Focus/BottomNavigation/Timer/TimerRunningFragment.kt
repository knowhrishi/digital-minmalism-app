package com.example.digitalminimalism.Focus.BottomNavigation.Timer

import FocusModeFragment
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.example.digitalminimalism.Focus.BottomNavigation.Active.ActiveNavFragment
import com.example.digitalminimalism.R
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class TimerRunningFragment(
    private val duration: Long,
    private val timerType: TimerNavFragment.TimerType,
    private val selectedLinearLayoutType: TimerNavFragment.LinearLayoutType,
    private val currentTimerDocId: String?
) : Fragment() {

    private lateinit var uniqueID: String
    private val firestoreDB: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var timerTextView: TextView
    private lateinit var circularProgress: CircularProgressIndicator
    private lateinit var buttonNeedBreak: Button
    private lateinit var buttonEndSession: Button
    private var countDownTimer: CountDownTimer? = null
    private var remainingDuration: Long =
        0L // Variable to store the remaining duration before break
    private var workPeriodsCompleted = 0

    companion object {
        private const val POMODORO_WORK_TIME = 25 * 60 * 1000L // 25 minutes
        private const val POMODORO_BREAK_SHORT = 5 * 60 * 1000L // 5 minutes
        private const val POMODORO_BREAK_LONG = 15 * 60 * 1000L // 15 minutes
        private const val TIMER_52_17_WORK_TIME = 52 * 60 * 1000L // 52 minutes
        private const val TIMER_52_17_BREAK_TIME = 17 * 60 * 1000L // 17 minutes
        private const val TIMER_90_MIN_WORK_TIME = 90 * 60 * 1000L // 90 minutes
        private const val TIMER_90_MIN_BREAK_TIME = 10 * 60 * 1000L // 10 minutes
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_timer_running, container, false)



        uniqueID =
            Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)

        timerTextView = view.findViewById(R.id.timerTextView)
        circularProgress = view.findViewById(R.id.circular_progress)
        buttonNeedBreak = view.findViewById(R.id.buttonNeedBreak)
        buttonEndSession = view.findViewById(R.id.buttonEndSession)
        (activity as AppCompatActivity).supportActionBar?.hide()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonNeedBreak: Button = view.findViewById(R.id.buttonNeedBreak)
        val buttonEndSession: Button = view.findViewById(R.id.buttonEndSession)

        remainingDuration = when (timerType) {
            TimerNavFragment.TimerType.POMODORO -> POMODORO_WORK_TIME
            TimerNavFragment.TimerType.TIMER_52_17 -> TIMER_52_17_WORK_TIME
            TimerNavFragment.TimerType.TIMER_90_MIN -> TIMER_90_MIN_WORK_TIME
        }
        startWorkTimer()
        updateUIForSelectedType(selectedLinearLayoutType, false)
//        startTimer(remainingDuration)

        buttonNeedBreak.setOnClickListener {
            // Store remaining duration before starting the break
            countDownTimer?.cancel()
            countDownTimer = null // Set the timer to null
            startBreakTimer(POMODORO_BREAK_SHORT) // Start break with defined duration

            // Update status and increment breakCounter in Firestore
            val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
            val timerRef = userTrackingRef.collection("timers").document(currentTimerDocId.toString())
            timerRef.update("status", "break")
            timerRef.update("breakCounter", FieldValue.increment(1))
        }

        buttonEndSession.setOnClickListener {
            // Cancel the timer and return to the previous fragment
            countDownTimer?.cancel()
            returnToTimerNavFragment()

            // Update status in Firestore
            val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
            val timerRef = userTrackingRef.collection("timers").document(currentTimerDocId.toString())
            timerRef.update("status", "cancelled")
        }
    }

    private fun startBreakTimer(breakDuration: Long) {
        countDownTimer?.cancel() // Cancel any ongoing timer
        updateUIForSelectedType(selectedLinearLayoutType, isBreak = true)
        buttonNeedBreak.visibility = View.GONE // Hide the "I need a break" button
        startTimer(breakDuration) {
            // After the break, resume the previous timer with the remaining duration
            updateUIForSelectedType(selectedLinearLayoutType, isBreak = false)
            startTimer(remainingDuration)
        }
    }

    private fun startWorkTimer() {
        buttonNeedBreak.visibility = View.VISIBLE // Show the "I need a break" button

        val workTime = when (timerType) {
            TimerNavFragment.TimerType.POMODORO -> POMODORO_WORK_TIME
            TimerNavFragment.TimerType.TIMER_52_17 -> TIMER_52_17_WORK_TIME
            TimerNavFragment.TimerType.TIMER_90_MIN -> TIMER_90_MIN_WORK_TIME
        }

        startTimer(workTime) {
            when (timerType) {
                TimerNavFragment.TimerType.POMODORO -> startPomodoroBreak()
                TimerNavFragment.TimerType.TIMER_52_17 -> start5217Break()
                TimerNavFragment.TimerType.TIMER_90_MIN -> start90MinBreak()
            }
        }
    }

    private fun startPomodoroBreak() {
        workPeriodsCompleted++
        val breakTime =
            if (workPeriodsCompleted % 4 == 0) POMODORO_BREAK_LONG else POMODORO_BREAK_SHORT

        startTimer(breakTime) {
            startWorkTimer() // Go back to work timer
        }
    }

    private fun start5217Break() {
        startTimer(TIMER_52_17_BREAK_TIME) {
            startWorkTimer() // Go back to work timer
        }
    }

    private fun start90MinBreak() {
        startTimer(TIMER_90_MIN_BREAK_TIME) {
            startWorkTimer() // Go back to work timer
        }
    }

    private fun startTimer(duration: Long, onFinish: () -> Unit = {}) {
        countDownTimer?.cancel() // Ensure to cancel any ongoing timer
        circularProgress.max = duration.toInt()
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Calculate progress as a percentage of the remaining time
                val progress =
                    ((millisUntilFinished / duration.toFloat()) * circularProgress.max).toInt()
                circularProgress.setProgressCompat(progress, true)
                updateTimerTextView(millisUntilFinished)
            }

            override fun onFinish() {
                circularProgress.setProgressCompat(0, true) // Set progress to 0 when finished
                returnToTimerNavFragment()
                onFinish()
            }
        }.start()
    }

    private fun updateTimerTextView(millisUntilFinished: Long) {
        val seconds = (millisUntilFinished / 1000) % 60
        val minutes = (millisUntilFinished / (1000 * 60)) % 60
        val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
        timerTextView.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun returnToTimerNavFragment() {
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FocusModeFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }

    @SuppressLint("SetTextI18n")
    private fun updateUIForSelectedType(
        selectedType: TimerNavFragment.LinearLayoutType,
        isBreak: Boolean
    ) {
        val imageView = view?.findViewById<ImageView>(R.id.imageViewtimerType)
        val timerTypeTextView = view?.findViewById<TextView>(R.id.timerTypeTextView)
        if (!isBreak) {
            when (selectedType) {
                TimerNavFragment.LinearLayoutType.STUDY -> {
                    imageView?.setImageResource(R.drawable.ic_study_sticker) // Replace with your study icon
                    timerTypeTextView?.text = "This is study time. Let’s focus and hit the books!"
                }

                TimerNavFragment.LinearLayoutType.WORK -> {
                    imageView?.setImageResource(R.drawable.ic_work_sticker) // Replace with your work icon
                    timerTypeTextView?.text =
                        "This is work time. Let’s focus on getting things done."
                }

                TimerNavFragment.LinearLayoutType.EXERCISE -> {
                    imageView?.setImageResource(R.drawable.ic_gym_sticker) // Replace with your exercise icon
                    timerTypeTextView?.text =
                        "This is exercise time. Push your limits and stay strong."
                }

                TimerNavFragment.LinearLayoutType.RELAX -> {
                    imageView?.setImageResource(R.drawable.ic_relax_sticker) // Replace with your relax icon
                    timerTypeTextView?.text =
                        "Relaxation time is on. Time to calm the mind and find peace."
                }

                TimerNavFragment.LinearLayoutType.OTHER -> {
                    imageView?.setImageResource(R.drawable.ic_other) // Replace with your other icon
                    timerTypeTextView?.text =
                        "Focused time for your activity. Dive in and make progress."
                }
            }
        } else {
            // Break time UI updates
            imageView?.setImageResource(R.drawable.ic_sticker_break) // Use a generic break icon or make it specific for each type
            timerTypeTextView?.text = "This is your break time. Let's breathe and relax for a bit."
        }
    }
}