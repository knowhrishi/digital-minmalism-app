package com.example.digitalminimalism.Focus

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.digitalminimalism.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FocusStats : AppCompatActivity() {
    private lateinit var barChart: BarChart
    private lateinit var uniqueID: String
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus_stats)
        barChart = findViewById(R.id.barChart)
        fetchFocusData()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    private fun fetchFocusData() {
        // Get a reference to the Firestore database
        val db = FirebaseFirestore.getInstance()
        uniqueID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        // Reference to the user's document in the 'userTracking' collection
        val userTrackingRef = db.collection("userTracking").document(uniqueID)

        // Fetch the focus mode data from Firestore
        userTrackingRef.collection("focusModeInfo")
            .get()
            .addOnSuccessListener { documents ->
                // Process the fetched data
                val focusSessionDataClasses = documents.map { document ->
                    FocusSessionDataClass(
                        startTime = document.getLong("startTime") ?: 0,
                        duration = document.getLong("duration") ?: 0,
                        status = document.getString("status") ?: "unknown"
                    )
                }

                // Group the focus sessions by day
                val focusSessionsByDay = focusSessionDataClasses.groupBy { session ->
                    // Convert the start time to a date
                    val date = Date(session.startTime)
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    format.format(date)
                }

                // Calculate the total focus time for each day
                val focusTimeByDay = focusSessionsByDay.mapValues { (_, sessions) ->
                    sessions.sumOf { it.duration }
                }

                // Display the chart with the fetched data
                displayChart(focusTimeByDay)
            }
            .addOnFailureListener { exception ->
                // Handle any errors here
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

    private fun displayChart(focusTimeByDay: Map<String, Long>) {
        val entries = focusTimeByDay.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        val barDataSet = BarDataSet(entries, "Focus Time")

        // Customize the appearance of the bars
        barDataSet.color = ContextCompat.getColor(this, R.color.black)
        barDataSet.valueTextColor = ContextCompat.getColor(this, R.color.black)
        barDataSet.valueTextSize = 10f

        val barData = BarData(barDataSet)
        barChart.data = barData

        // Customize the appearance of the chart
        barChart.description.text = "Focus Session Stats"
        barChart.description.textColor = ContextCompat.getColor(this, R.color.black)
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setDrawValueAboveBar(true)

        // Customize the appearance of the X axis
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = ContextCompat.getColor(this, R.color.light_blue_600)

        // Set the labels for the X axis
        xAxis.valueFormatter = IndexAxisValueFormatter(focusTimeByDay.keys.toList())

        // Customize the appearance of the Y axis
        val yAxisLeft = barChart.axisLeft
        yAxisLeft.setDrawGridLines(false)
        yAxisLeft.textColor = ContextCompat.getColor(this, R.color.light_blue_600)

        val yAxisRight = barChart.axisRight
        yAxisRight.setDrawGridLines(false)
        yAxisRight.textColor = ContextCompat.getColor(this, R.color.light_blue_600)

        // Add animation
        barChart.animateXY(2000, 2000)

        barChart.invalidate() // refresh the chart
    }
}