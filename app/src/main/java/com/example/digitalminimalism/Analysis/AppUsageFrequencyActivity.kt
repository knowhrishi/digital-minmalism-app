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
//import com.example.digitalminimalism.databinding.ActivityAppUsageFrequencyBinding
//import com.example.digitalminimalism.Usage.UsageMonitoringFragment.AppUsage
//import com.github.mikephil.charting.components.Description
//import com.github.mikephil.charting.components.XAxis
//import com.github.mikephil.charting.data.BarData
//import com.github.mikephil.charting.data.BarDataSet
//import com.github.mikephil.charting.data.BarEntry
//import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
//import com.github.mikephil.charting.utils.ColorTemplate
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.ktx.toObject
//
//class AppUsageFrequencyActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityAppUsageFrequencyBinding
//    private lateinit var firestoreDB: FirebaseFirestore
//    private lateinit var uniqueID: String
//
//    @SuppressLint("HardwareIds")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityAppUsageFrequencyBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        // Set the toolbar as the action bar
//        setSupportActionBar(binding.toolbar)
//        // Enable the back button in the ActionBar
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        firestoreDB = FirebaseFirestore.getInstance()
//        uniqueID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
//        fetchAndDisplayUsageData()
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
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
//
//    private fun fetchAndDisplayUsageData() {
//        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
//        userTrackingRef.collection("appUsageInfo").get()
//            .addOnSuccessListener { documents ->
//                val usages = documents.mapNotNull { it.toObject<AppUsage>() }
//                updateAppUsageFrequencyChart(usages)
//            }
//            .addOnFailureListener { e ->
//                Log.w("Firestore", "Error getting documents: ", e)
//            }
//    }
//
//    private fun updateAppUsageFrequencyChart(usages: List<AppUsage>) {
//        // Group the usages by app name and count the number of usages for each app
//        val usageFrequency = usages.groupingBy { it.appName }.eachCount()
//
//        // Create a BarEntry for each app
//        val entries = usageFrequency.entries.mapIndexed { index, entry ->
//            val (appName, frequency) = entry
//            BarEntry(index.toFloat(), frequency.toFloat())
//        }
//        val dataSet = BarDataSet(entries, "App Usage Frequency")
//        dataSet.color = ColorTemplate.getHoloBlue()
//        dataSet.valueTextColor = Color.BLACK
//        dataSet.setDrawValues(true)
//
//        val barData = BarData(dataSet)
//
//        // Set x-axis labels
//        val appNames = usageFrequency.keys.toTypedArray()
//        val xAxis = binding.appsUsageChart.xAxis
//        xAxis.valueFormatter = IndexAxisValueFormatter(appNames)
//        xAxis.position = XAxis.XAxisPosition.BOTTOM
//
//        // Set y-axis labels
//        val yAxisLeft = binding.appsUsageChart.axisLeft
//        yAxisLeft.axisMinimum = 0f // Start at zero
//        yAxisLeft.setDrawGridLines(false)
//
//        val yAxisRight = binding.appsUsageChart.axisRight
//        yAxisRight.setDrawLabels(false) // no right axis labels
//        yAxisRight.setDrawGridLines(false) // no right axis grid lines
//
//        // Set chart description
//        val description = Description()
//        description.text = "App usage frequency"
//        binding.appsUsageChart.description = description
//
//        binding.appsUsageChart.data = barData
//        binding.appsUsageChart.invalidate() // Refresh the chart
//    }
//}