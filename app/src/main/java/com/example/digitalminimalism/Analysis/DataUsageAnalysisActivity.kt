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
//import com.example.digitalminimalism.databinding.ActivityDataUsageAnalysisBinding
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
//class DataUsageAnalysisActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityDataUsageAnalysisBinding
//    private lateinit var firestoreDB: FirebaseFirestore
//    private lateinit var uniqueID: String
//
//    @SuppressLint("HardwareIds")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityDataUsageAnalysisBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        // Set the toolbar as the action bar
//        setSupportActionBar(binding.toolbar)
//        // Enable the back button in the ActionBar
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        firestoreDB = FirebaseFirestore.getInstance()
//        uniqueID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
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
//}