package com.example.digitalminimalism.Analysis

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.digitalminimalism.R
import com.example.digitalminimalism.databinding.ActivityDayOfWeekAnalysisBinding
import com.example.digitalminimalism.Usage.UsageMonitoringFragment.AppUsage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class DayOfWeekAnalysisActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDayOfWeekAnalysisBinding
    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var uniqueID: String

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDayOfWeekAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Set the toolbar as the action bar
        setSupportActionBar(binding.toolbar)
        // Enable the back button in the ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        firestoreDB = FirebaseFirestore.getInstance()
        uniqueID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        fetchAndDisplayUsageData()
        // Set OnClickListener for the FAB
        binding.fab.setOnClickListener {
            fetchAndDisplayUsageData()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle the back button's click event
                onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun fetchAndDisplayUsageData() {
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        userTrackingRef.collection("appUsageInfo").get()
            .addOnSuccessListener { documents ->
                val usages = documents.mapNotNull { it.toObject<AppUsage>() }
                updateDayOfWeekAnalysis(usages)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error getting documents: ", e)
            }
    }

    private fun updateDayOfWeekAnalysis(usages: List<AppUsage>) {
        // Group the usages by app name and day of week
        val usageByDayOfWeek = usages.groupBy { it.appName }.mapValues { entry ->
            entry.value.groupingBy { it.dayOfWeek }.eachCount()
        }

        // Create a header row for the days of the week
        val headerRow = TableRow(this)
        val daysOfWeek = listOf(
            "App",
            "MON",
            "TUE",
            "WED",
            "THUR",
            "FRI",
            "SAT",
            "SUN"
        )
        daysOfWeek.forEach { dayOfWeek ->
            headerRow.addView(TextView(this).apply {
                text = dayOfWeek
                textSize = 16f
                setTextColor(Color.BLACK)
                setPadding(8, 8, 8, 8)

            })
        }
        binding.tableLayout.addView(headerRow)

        // Create a table row for each app
        usageByDayOfWeek.forEach { (appName, usageByDay) ->
            val tableRow = TableRow(this)
            tableRow.addView(TextView(this).apply {
                text = appName
                textSize = 16f
                setTextColor(Color.BLACK)
                setPadding(8, 8, 8, 8)

            })

            // Create a table cell for each day of the week
            daysOfWeek.drop(1).forEach { dayOfWeek -> // drop the first element ("App")
                val usageTime = usageByDay[dayOfWeek] ?: 0
                val color = getColorForUsageTime(usageTime)
                tableRow.addView(TextView(this).apply {
                    text = usageTime.toString()
                    textSize = 14f
                    setTextColor(Color.BLACK)
                    setBackgroundColor(color)
                    setPadding(8, 8, 8, 8)

                })
            }

            binding.tableLayout.addView(tableRow)
        }
    }

    private fun getColorForUsageTime(usageTime: Int): Int {
        // Return a color based on the usage time
        // This is a simple example, you might want to use a more sophisticated color mapping
        return when {
            usageTime > 60 -> Color.parseColor("#FF0000") // red
            usageTime > 30 -> Color.parseColor("#FFFF00") // yellow
            else -> Color.parseColor("#00FF00") // green
        }
    }
}