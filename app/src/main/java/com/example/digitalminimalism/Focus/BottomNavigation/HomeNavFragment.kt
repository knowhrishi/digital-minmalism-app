package com.example.digitalminimalism.Focus.BottomNavigation

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.digitalminimalism.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class HomeNavFragment : Fragment() {

    private lateinit var uniqueID: String
    private val firestoreDB: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_nav, container, false)

        fetchFocusModeData(view) // Load data and update adapter

        return view
    }

    @SuppressLint("HardwareIds")
    private fun fetchFocusModeData(view: View) {
        uniqueID = Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)
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
                view.findViewById<TextView>(R.id.total_focus_time_text_view).text =
                    "Total Time: $totalFocusTime"
                view.findViewById<TextView>(R.id.average_focus_time_text_view).text =
                    "Average Time: $averageFocusTime"
                view.findViewById<TextView>(R.id.longest_focus_session_text_view).text =
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

    private fun calculateTotalFocusTime(documents: QuerySnapshot): Int {
        return documents.documents
            .filter { it.getString("status") == "completed" }
            .sumOf { it.getLong("duration") ?: 0 }.toInt()
    }
}