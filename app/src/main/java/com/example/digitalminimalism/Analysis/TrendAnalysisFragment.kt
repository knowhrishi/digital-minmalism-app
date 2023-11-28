package com.example.digitalminimalism.Analysis

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.digitalminimalism.R
import com.example.digitalminimalism.databinding.FragmentTrendAnalysisBinding
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.example.digitalminimalism.Usage.UsageMonitoringFragment.AppUsage
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class TrendAnalysisFragment : Fragment() {

    private var _binding: FragmentTrendAnalysisBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TrendAnalysisAdapter
    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var uniqueID: String

    data class AnalysisType(val name: String, val icon: Int, val description: String)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTrendAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("HardwareIds")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestoreDB = FirebaseFirestore.getInstance()
        uniqueID =
            Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)

        val analysisList = listOf(
            AnalysisType("Usage Time Analysis", R.drawable.ic_usage_time, "Analyze the total usage time of each app per day or over a week."),
            AnalysisType("Data Usage Analysis", R.drawable.ic_data_usage, "Analyze the data usage of each app."),
            AnalysisType("App Usage Frequency", R.drawable.ic_app_frequency, "Analyze how frequently each app is used."),
            AnalysisType("Day of Week Analysis", R.drawable.ic_day_of_week, "Analyze app usage based on the day of the week."),
            AnalysisType("Remaining Time Analysis", R.drawable.ic_remaining_time, "Analyze the remaining time for each app.")
            // Add more analysis types here
        )

        adapter = TrendAnalysisAdapter(analysisList) { analysisType: AnalysisType ->
            // Handle analysis type selection
            when (analysisType.name) {
                "Usage Time Analysis" -> {
                    Intent(requireContext(), UsageTimeAnalysisActivity::class.java).also {
                        startActivity(it)
                    }
                }

                "Data Usage Analysis" -> {
                    Intent(requireContext(), DataUsageAnalysisActivity::class.java).also {
                        startActivity(it)
                    }
                }

                "App Usage Frequency" -> {
                    Intent(requireContext(), AppUsageFrequencyActivity::class.java).also {
                        startActivity(it)
                    }
                }

                "Day of Week Analysis" -> {
                    Intent(requireContext(), DayOfWeekAnalysisActivity::class.java).also {
                        startActivity(it)
                    }
                }
            }
        }
        binding.appsUsageList.layoutManager = GridLayoutManager(context, 2) // 2 columns in grid
        binding.appsUsageList.adapter = adapter
    }
}
