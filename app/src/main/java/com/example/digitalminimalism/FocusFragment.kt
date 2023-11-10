package com.example.digitalminimalism

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class FocusFragment : Fragment() {

    private var isTimerRunning: Boolean = false
    private var timeLeftInMilliseconds: Long = 1500000 // 25 min
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var tvTimer: TextView
    private lateinit var etTaskName: EditText
    private lateinit var etNotes: EditText
    private lateinit var btnStartPause: Button
    private lateinit var btnReset: Button
    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var uniqueID: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var pomodoroSessions: MutableList<PomodoroSession>

    data class PomodoroSession(
        val task: String = "",
        val startTime: Long = 0L,
        val endTime: Long = 0L,
        val duration: Long = 0L,
        val completed: Boolean = false,
        val notes: String = ""
    )



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_focus, container, false)

        tvTimer = view.findViewById(R.id.tvTimer)
        etTaskName = view.findViewById(R.id.etTaskName)
        etNotes = view.findViewById(R.id.etNotes)
        btnStartPause = view.findViewById(R.id.btnStartPause)
        btnReset = view.findViewById(R.id.btnReset)
        firestoreDB = FirebaseFirestore.getInstance()
        uniqueID = Settings.Secure.getString(context?.contentResolver, Settings.Secure.ANDROID_ID)
        recyclerView = view.findViewById(R.id.rvHistory)
        recyclerView.layoutManager = LinearLayoutManager(context)
        pomodoroSessions = mutableListOf()
        recyclerView.adapter = FocusAdapter(pomodoroSessions)

        btnStartPause.setOnClickListener {
            if (isTimerRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        btnReset.setOnClickListener {
            resetTimer()
        }

        updateTimer()
        retrievePomodoroSessions()
        return view
    }



    private fun startTimer() {
        hideKeyboard()
        countDownTimer = object : CountDownTimer(timeLeftInMilliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMilliseconds = millisUntilFinished
                updateTimer()
            }

            override fun onFinish() {
                isTimerRunning = false
                btnStartPause.text = "Start"

                val session = PomodoroSession(
                    task = etTaskName.text.toString(),
                    notes = etNotes.text.toString(),
                    startTime = System.currentTimeMillis() - timeLeftInMilliseconds,
                    endTime = System.currentTimeMillis(),
                    duration = 1500000L - timeLeftInMilliseconds,
                    completed = timeLeftInMilliseconds <= 0
                )
                savePomodoroSessionToFirestore(session)
                pomodoroSessions.add(session)
                recyclerView.adapter?.notifyDataSetChanged()
            }

        }.start()

        btnStartPause.text = "Pause"
        isTimerRunning = true
    }

    private fun retrievePomodoroSessions() {
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        userTrackingRef.collection("pomodoroSessions")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val session = document.toObject(PomodoroSession::class.java)
                    pomodoroSessions.add(session)
                }
                recyclerView.adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }


    private fun savePomodoroSessionToFirestore(session: PomodoroSession) {
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        val pomodoroSessionRef = userTrackingRef.collection("pomodoroSessions").document()

        pomodoroSessionRef.set(session)
            .addOnSuccessListener { Log.d("Firestore", "Pomodoro session saved successfully!") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error saving Pomodoro session", e) }
    }

    private fun pauseTimer() {
        countDownTimer.cancel()
        btnStartPause.text = "Start"
        isTimerRunning = false
    }

    private fun resetTimer() {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle("Reset Timer")
        alertDialogBuilder.setMessage("Are you sure you want to reset the timer?")
        alertDialogBuilder.setPositiveButton("Yes") { dialog, which ->
            val currentTask = etTaskName.text.toString()
            val currentNotes = etNotes.text.toString()

            if (isTimerRunning) {
                countDownTimer.cancel()
                val session = PomodoroSession(
                    task = currentTask,
                    startTime = System.currentTimeMillis() - (1500000 - timeLeftInMilliseconds),
                    endTime = System.currentTimeMillis(),
                    duration = 1500000L - timeLeftInMilliseconds,
                    completed = timeLeftInMilliseconds <= 0,
                    notes = currentNotes
                )
                savePomodoroSessionToFirestore(session)
                pomodoroSessions.add(session)
                recyclerView.adapter?.notifyDataSetChanged()
            }

            timeLeftInMilliseconds = 1500000 // 25 min
            updateTimer()
            btnStartPause.text = "Start"
            isTimerRunning = false
        }
        alertDialogBuilder.setNegativeButton("No") { dialog, which ->
            // User canceled the reset action
        }
        alertDialogBuilder.show()
    }

    private fun updateTimer() {
        val minutes = (timeLeftInMilliseconds / 1000) / 60
        val seconds = (timeLeftInMilliseconds / 1000) % 60
        val timeLeftText = String.format("%02d:%02d", minutes, seconds)
        tvTimer.text = timeLeftText
    }
    @SuppressLint("ServiceCast")
    private fun hideKeyboard() {
        val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

}