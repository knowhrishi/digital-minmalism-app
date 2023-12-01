//package com.example.digitalminimalism.Analysis
//
//import android.annotation.SuppressLint
//import android.graphics.Color
//import android.os.Bundle
//import android.provider.Settings
//import android.util.Log
//import android.view.MenuItem
//import androidx.appcompat.app.AppCompatActivity
//import com.example.digitalminimalism.R
//import com.example.digitalminimalism.databinding.ActivityUsageTimeAnalysisBinding
//import com.example.digitalminimalism.Usage.UsageMonitoringFragment.AppUsage
//import com.github.mikephil.charting.components.Description
//import com.github.mikephil.charting.components.XAxis
//import com.github.mikephil.charting.data.Entry
//import com.github.mikephil.charting.data.LineData
//import com.github.mikephil.charting.data.LineDataSet
//import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
//import com.github.mikephil.charting.utils.ColorTemplate
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.ktx.toObject
//
//class UsageTimeAnalysisActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityUsageTimeAnalysisBinding
//    private lateinit var firestoreDB: FirebaseFirestore
//    private lateinit var uniqueID: String
//
//    @SuppressLint("HardwareIds")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityUsageTimeAnalysisBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        // Set the toolbar as the action bar
//        setSupportActionBar(binding.toolbar)
//        // Enable the back button in the ActionBar
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        firestoreDB = FirebaseFirestore.getInstance()
//        uniqueID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
//        fetchAndDisplayUsageData()
//// Set OnClickListener for the FAB
//        binding.fab.setOnClickListener {
//            fetchAndDisplayUsageData()
//        }
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            android.R.id.home -> {
//                // Handle the back button's click event
//                onBackPressed()
//                true
//            }
//
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
//
//    private fun fetchAndDisplayUsageData() {
//        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
//        userTrackingRef.collection("appUsageInfo").get()
//            .addOnSuccessListener { documents ->
//                val usages = documents.mapNotNull { it.toObject<AppUsage>() }
//                updateUsageTimeChart(usages)
//            }
//            .addOnFailureListener { e ->
//                Log.w("Firestore", "Error getting documents: ", e)
//            }
//    }
//
//    private fun updateUsageTimeChart(usages: List<AppUsage>) {
//        val entries = usages.map { appUsage ->
//            Entry(
//                convertDayOfWeekToIndex(appUsage.dayOfWeek).toFloat(),
//                appUsage.usageTime.toFloat()
//            )
//        }
//
//        val dataSet = LineDataSet(entries, "App Usage Time") // Change to BarDataSet for a bar chart
//        dataSet.color = ColorTemplate.getHoloBlue()
//        dataSet.valueTextColor = Color.BLACK
//        dataSet.setDrawCircles(true)
//        dataSet.setDrawCircleHole(true)
//        dataSet.circleHoleColor = Color.WHITE
//        dataSet.setDrawValues(true)
//        dataSet.valueTextSize = 12f
//        dataSet.lineWidth = 2.5f
//        dataSet.circleRadius = 4f
//        dataSet.setCircleColor(ColorTemplate.getHoloBlue())
//
//        val lineData = LineData(dataSet)
//
//        // Set x-axis labels
//        val daysOfWeek =
//            arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
//        val xAxis = binding.appsUsageChart.xAxis
//        xAxis.valueFormatter = IndexAxisValueFormatter(daysOfWeek)
//        xAxis.position = XAxis.XAxisPosition.BOTTOM
//        xAxis.textColor = Color.BLACK
//        xAxis.gridColor = Color.GRAY
//
//        // Set y-axis labels
//        val yAxisLeft = binding.appsUsageChart.axisLeft
//        yAxisLeft.axisMinimum = 0f // Start at zero
//        yAxisLeft.axisMaximum = 60f // The maximum value will be 60 minutes
//        yAxisLeft.labelCount = 6 // To display the values {0,10,20,30,40,50,60}
//        yAxisLeft.textColor = Color.BLACK
//        yAxisLeft.gridColor = Color.GRAY
//        yAxisLeft.setDrawGridLines(false)
//
//        val yAxisRight = binding.appsUsageChart.axisRight
//        yAxisRight.setDrawLabels(false) // no right axis labels
//        yAxisRight.setDrawGridLines(false) // no right axis grid lines
//
//        // Set chart description
//        val description = Description()
//        description.text = "App usage time per day of the week"
//        binding.appsUsageChart.description = description
//
//        binding.appsUsageChart.data = lineData
//        binding.appsUsageChart.invalidate() // Refresh the chart
//    }
//
//    private fun convertDayOfWeekToIndex(dayOfWeek: String): Int {
//        return when (dayOfWeek) {
//            "Monday" -> 1
//            "Tuesday" -> 2
//            "Wednesday" -> 3
//            "Thursday" -> 4
//            "Friday" -> 5
//            "Saturday" -> 6
//            "Sunday" -> 7
//            else -> 0
//        }
//    }
//}