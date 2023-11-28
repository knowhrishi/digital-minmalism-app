package com.example.digitalminimalism.Analysis

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.digitalminimalism.R
import com.example.digitalminimalism.databinding.ActivityDataUsageAnalysisBinding
import com.example.digitalminimalism.Usage.UsageMonitoringFragment.AppUsage
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class DataUsageAnalysisActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDataUsageAnalysisBinding
    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var uniqueID: String

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataUsageAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Set the toolbar as the action bar
        setSupportActionBar(binding.toolbar)
        // Enable the back button in the ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        firestoreDB = FirebaseFirestore.getInstance()
        uniqueID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        fetchAndDisplayUsageData()
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
                updateDataUsageChart(usages)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error getting documents: ", e)
            }
    }

    private fun updateDataUsageChart(usages: List<AppUsage>) {
        val entries = usages.map { appUsage ->
            BarEntry(convertDayOfWeekToIndex(appUsage.dayOfWeek).toFloat(), appUsage.receivedWifi.toFloat())
        }

        val dataSet = BarDataSet(entries, "Data Usage")
        dataSet.color = ColorTemplate.getHoloBlue()
        dataSet.valueTextColor = Color.BLACK
        dataSet.setDrawValues(true)

        val barData = BarData(dataSet)

        // Set x-axis labels
        val daysOfWeek = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val xAxis = binding.appsUsageChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(daysOfWeek)
        xAxis.position = XAxis.XAxisPosition.BOTTOM

        // Set y-axis labels
        val yAxisLeft = binding.appsUsageChart.axisLeft
        yAxisLeft.axisMinimum = 0f // Start at zero
        yAxisLeft.axisMaximum = 60f // The maximum value will be 60 minutes
        yAxisLeft.labelCount = 6 // To display the values {0,10,20,30,40,50,60}
        yAxisLeft.setDrawGridLines(false)

        val yAxisRight = binding.appsUsageChart.axisRight
        yAxisRight.setDrawLabels(false) // no right axis labels
        yAxisRight.setDrawGridLines(false) // no right axis grid lines

        // Set chart description
        val description = Description()
        description.text = "Data usage per day of the week"
        binding.appsUsageChart.description = description

        binding.appsUsageChart.data = barData
        binding.appsUsageChart.invalidate() // Refresh the chart
    }

    private fun convertDayOfWeekToIndex(dayOfWeek: String): Int {
        return when (dayOfWeek) {
            "Monday" -> 1
            "Tuesday" -> 2
            "Wednesday" -> 3
            "Thursday" -> 4
            "Friday" -> 5
            "Saturday" -> 6
            "Sunday" -> 7
            else -> 0
        }
    }
}