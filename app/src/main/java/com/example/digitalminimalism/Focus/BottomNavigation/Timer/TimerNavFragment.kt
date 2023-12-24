package com.example.digitalminimalism.Focus.BottomNavigation.Timer

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.digitalminimalism.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimerNavFragment : Fragment() {

    private var selectedTime: Int = 0

    // Initialize counter and sum at the top of your class
    private var timerCounter = 0
    private var totalTimerTime = 0

    companion object {
        const val POMODORO_WORK_TIME = 25 * 60 * 1000 // 25 minutes in milliseconds
        const val POMODORO_BREAK_SHORT = 5 * 60 * 1000 // 5 minutes
        const val POMODORO_BREAK_LONG = 15 * 60 * 1000 // 15 minutes

        private const val TIMER_52_17_WORK_TIME = 52 * 60 * 1000 // 52 minutes
        internal const val TIMER_52_17_BREAK_TIME = 17 * 60 * 1000 // 17 minutes

        private const val TIMER_90_MIN_WORK_TIME = 90 * 60 * 1000 // 90 minutes
        internal const val TIMER_90_MIN_BREAK_TIME = 10 * 60 * 1000 // 10 minutes
    }

    private var currentTimerType: TimerType? = TimerType.POMODORO
    private var currentTimerDocId: String? = null

    private lateinit var uniqueID: String
    private val firestoreDB: FirebaseFirestore = FirebaseFirestore.getInstance()

    enum class TimerType {
        POMODORO, TIMER_52_17, TIMER_90_MIN
    }

    enum class LinearLayoutType {
        STUDY, WORK, EXERCISE, RELAX, OTHER
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timer_nav, container, false)
        uniqueID =
            Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)


        // Query Firestore for an active timer
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        userTrackingRef.collection("timers")
            .whereEqualTo("status", "active")
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // If an active timer is found, start the TimerRunningFragment with the timer's data
                    val timerType = TimerType.valueOf(document.getString("timerType")!!)
                    val selectedLinearLayoutType = LinearLayoutType.valueOf(document.getString("linearLayoutType")!!)
                    val selectedTime = document.getLong("selectedTime")!!.toInt()
                    currentTimerDocId = document.id

                    val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
                    fragmentManager.beginTransaction().replace(
                        R.id.fragment_container,
                        TimerRunningFragment(selectedTime.toLong(), timerType, selectedLinearLayoutType, currentTimerDocId)
                    ).commit()
                }
            }



        val buttonPomodoroTimer: TextView = view.findViewById(R.id.button_pomo_timer)
        val buttonFiftyTwoSeventeenTimer: TextView = view.findViewById(R.id.button_second_timer)
        val buttonNintyMinTimer: TextView = view.findViewById(R.id.button_90_min_timer)
        val buttonBegin: Button = view.findViewById(R.id.button_begin)
        val LinearLayoutStudy: LinearLayout = view.findViewById(R.id.LinearLayout_study)
        val LinearLayoutWork: LinearLayout = view.findViewById(R.id.LinearLayoutWork)
        val LinearLayoutExercise: LinearLayout = view.findViewById(R.id.LinearLayoutExercise)
        val LinearLayoutRelax: LinearLayout = view.findViewById(R.id.LinearLayoutRelax)
        val LinearLayoutOther: LinearLayout = view.findViewById(R.id.LinearLayoutOther)

        val linearLayouts = listOf(
            LinearLayoutStudy,
            LinearLayoutWork,
            LinearLayoutExercise,
            LinearLayoutRelax,
            LinearLayoutOther
        )

        var selectedLinearLayout: LinearLayout? = null
        var selectedLinearLayoutType: LinearLayoutType? = LinearLayoutType.STUDY
        for (linearLayout in linearLayouts) {
            linearLayout.setOnClickListener {
                if (selectedLinearLayout != it) {
                    selectedLinearLayout?.setBackgroundResource(R.drawable.cardview_background_default)
                    it.setBackgroundResource(R.drawable.cardview_background_pressed)
                    selectedLinearLayout = it as LinearLayout
                }

                when (it.id) {
                    R.id.LinearLayout_study -> {
                        Toast.makeText(context, "Study LinearLayout clicked", Toast.LENGTH_SHORT)
                            .show()
                        selectedLinearLayoutType = LinearLayoutType.STUDY
                    }

                    R.id.LinearLayoutWork -> {
                        Toast.makeText(context, "Work LinearLayout clicked", Toast.LENGTH_SHORT)
                            .show()
                        selectedLinearLayoutType = LinearLayoutType.WORK
                    }

                    R.id.LinearLayoutExercise -> {
                        Toast.makeText(context, "Exercise LinearLayout clicked", Toast.LENGTH_SHORT)
                            .show()
                        selectedLinearLayoutType = LinearLayoutType.EXERCISE
                    }

                    R.id.LinearLayoutRelax -> {
                        Toast.makeText(context, "Relax LinearLayout clicked", Toast.LENGTH_SHORT)
                            .show()
                        selectedLinearLayoutType = LinearLayoutType.RELAX
                    }

                    R.id.LinearLayoutOther -> {
                        Toast.makeText(context, "Other LinearLayout clicked", Toast.LENGTH_SHORT)
                            .show()
                        selectedLinearLayoutType = LinearLayoutType.OTHER
                    }
                }
            }
        }

        val buttonPrepare: TextView = view.findViewById(R.id.button_prepare)
        val buttonStartNow: TextView = view.findViewById(R.id.button_start_now)


        buttonBegin.setOnClickListener(View.OnClickListener {
            if (currentTimerType != null && selectedLinearLayoutType != null) {
                // Save to Firestore
                val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
                val timerRef = userTrackingRef.collection("timers").document()
                // Increment counter and add to sum
                timerCounter++
                totalTimerTime += selectedTime

                val timerData = hashMapOf(
                    "timerType" to currentTimerType.toString(),
                    "linearLayoutType" to selectedLinearLayoutType.toString(),
                    "selectedTime" to selectedTime,
                    "setAt" to System.currentTimeMillis(), // Current time in milliseconds
                    "date" to SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                    ).format(Date()), // Current date
                    "timerCounter" to timerCounter, // Number of timers set by the user
                    "totalTimerTime" to totalTimerTime, // Total time for which timers have been set
                    "status" to "active" // Timer status
                )

                timerRef.set(timerData)
                    .addOnSuccessListener {
                        Log.d("Firestore", "DocumentSnapshot successfully written!")
                        currentTimerDocId = timerRef.id // Store the ID of the new document

                        // Start the TimerRunningFragment
                        val fragmentManager: FragmentManager =
                            requireActivity().supportFragmentManager
                        fragmentManager.beginTransaction().replace(
                            R.id.fragment_container,
                            TimerRunningFragment(
                                selectedTime.toLong(),
                                currentTimerType!!,
                                selectedLinearLayoutType!!,
                                currentTimerDocId
                            )
                        ).commit()
                    }
                    .addOnFailureListener { e -> Log.w("Firestore", "Error writing document", e) }
            } else {
                // Handle the case where currentTimerType or selectedLinearLayoutType is null
                Toast.makeText(
                    context,
                    "Please select a timer type and a linear layout type",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        val buttons = listOf(buttonPrepare, buttonStartNow)
        var selectedButton: TextView? = null
        for (button in buttons) {
            button.setOnClickListener {
                if (selectedButton != it) {
                    selectedButton?.setBackgroundResource(R.drawable.circle_background_notselected)
                    selectedButton?.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.black
                        )
                    ) // Set the text color for the unselected state
                    it.setBackgroundResource(R.drawable.circle_background_selected)
                    (it as TextView).setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.white
                        )
                    ) // Set the text color for the selected state
                    selectedButton = it
                }
                when (it.id) {

                    R.id.button_prepare -> Toast.makeText(
                        context,
                        "Prepare button clicked",
                        Toast.LENGTH_SHORT
                    ).show()

                    R.id.button_start_now -> Toast.makeText(
                        context,
                        "Start Now button clicked",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        val timeSelectionTextViews = listOf(
            buttonPomodoroTimer,
            buttonFiftyTwoSeventeenTimer,
            buttonNintyMinTimer,
        )

        fun resetTimeSelectionBackgrounds() {
            for (textView in timeSelectionTextViews) {
                textView.setBackgroundResource(R.drawable.circle_background_notselected)
                (textView as? TextView)?.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.black
                    )
                ) // Set the text color for the selected state
            }
        }
        buttonPomodoroTimer.setOnClickListener {
            resetTimeSelectionBackgrounds()
            it.setBackgroundResource(R.drawable.circle_background_selected)
            (it as? TextView)?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            ) // Set the text color for the selected state

            // Set current timer type to Pomodoro
            currentTimerType = TimerType.POMODORO
            selectedTime = 25
            Toast.makeText(context, "10 minutes selected", Toast.LENGTH_SHORT).show()
        }

        buttonFiftyTwoSeventeenTimer.setOnClickListener {
            resetTimeSelectionBackgrounds()
            it.setBackgroundResource(R.drawable.circle_background_selected)
            (it as? TextView)?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            ) // Set the text color for the selected state
            currentTimerType = TimerType.TIMER_52_17
            selectedTime = 52
            Toast.makeText(context, "15 minutes selected", Toast.LENGTH_SHORT).show()
        }

        buttonNintyMinTimer.setOnClickListener {
            resetTimeSelectionBackgrounds()
            it.setBackgroundResource(R.drawable.circle_background_selected)
            (it as? TextView)?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            ) // Set the text color for the selected state
            currentTimerType = TimerType.TIMER_90_MIN
            selectedTime = 90
            Toast.makeText(context, "25 minutes selected", Toast.LENGTH_SHORT).show()
        }


        return view
    }


}