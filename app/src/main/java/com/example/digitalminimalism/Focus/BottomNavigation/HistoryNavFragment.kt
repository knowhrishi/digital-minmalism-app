package com.example.digitalminimalism.Focus.BottomNavigation

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalminimalism.Focus.FocusAdapter
import com.example.digitalminimalism.Focus.FocusSessionDataClass
import com.example.digitalminimalism.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class HistoryNavFragment : Fragment() {

    private lateinit var uniqueID: String
    private val firestoreDB: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FocusAdapter

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
        val view = inflater.inflate(R.layout.fragment_history_nav, container, false)

        uniqueID =
            Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)
        recyclerView = view.findViewById(R.id.recycler_focus_sessions)
        adapter = FocusAdapter(
            requireContext(),
            listOf()
        ) // Initialize with an empty list or fetched data

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        fetchFocusModeData() // Load data and update adapter

        return view
    }

    private fun fetchFocusModeData() {
        // Fetch the focus mode data from Firestore
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        userTrackingRef.collection("focusModeInfo")
            .get()
            .addOnSuccessListener { documents ->
                val focusSessionDataClasses = documents.map { document ->
                    FocusSessionDataClass(
                        timerSetUntil = document.getLong("timerSetUntil") ?: 0,
                        duration = document.getLong("duration") ?: 0,
                        setAt = document.getLong("setAt") ?: 0,
                        status = document.getString("status") ?: "unknown",
                        startTime = document.getLong("startTime") ?: 0,
                        endTime = document.getLong("endTime") ?: 0
                    )
                }
                adapter.updateSessions(focusSessionDataClasses)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to fetch focus mode data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}