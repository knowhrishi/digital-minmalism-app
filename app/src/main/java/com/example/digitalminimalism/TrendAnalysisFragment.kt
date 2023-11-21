import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.digitalminimalism.UsageMonitoringFragment
import com.example.digitalminimalism.databinding.FragmentTrendAnalysisBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.example.digitalminimalism.UsageMonitoringFragment.AppUsage
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class TrendAnalysisFragment : Fragment() {

    private var _binding: FragmentTrendAnalysisBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TrendAnalysisAdapter
    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var uniqueID: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentTrendAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("HardwareIds")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestoreDB = FirebaseFirestore.getInstance()
        uniqueID = Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)

        adapter = TrendAnalysisAdapter(listOf()) { appUsage: UsageMonitoringFragment.AppUsage ->
            fetchAndDisplayAppSpecificData(appUsage)
        }


        binding.appsUsageList.layoutManager = LinearLayoutManager(context)
        binding.appsUsageList.adapter = adapter
        setupChartAppearance()
        fetchAndDisplayUsageData()
    }
    private fun fetchAndDisplayUsageData() {
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        userTrackingRef.collection("appUsageInfo").get()
            .addOnSuccessListener { documents ->
                val usages = documents.mapNotNull { it.toObject<AppUsage>() }
                adapter.updateData(usages)
                updateLineChart(usages)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error getting documents: ", e)
            }
    }

    private fun showTotalUsageTime(appUsage: AppUsage) {
        val totalMinutes = appUsage.usageTime
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val timeText = "${hours}h ${minutes}min this week"
        binding.totalUsageTimeText.text = timeText
        binding.totalUsageTimeText.visibility = View.VISIBLE
    }
    private fun fetchAndDisplayAppSpecificData(appUsage: AppUsage) {
        // It is assumed that you have a field 'date' in your weeklyUsage documents to order by date
        firestoreDB.collection("userTracking")
            .document(uniqueID)
            .collection("appUsageInfo")
            .document(appUsage.appName)
            .collection("weeklyUsage")
            .orderBy("lastUsedTime") // Make sure to use the correct field for ordering
            .get()
            .addOnSuccessListener { documents ->
                val usageStatsForWeek = documents.mapNotNull { it.toObject<AppUsage>() }
                updateAppSpecificLineChart(usageStatsForWeek)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error getting documents for app ${appUsage.appName}: ", e)
            }
        showTotalUsageTime(appUsage) // Call this function when an app is selected

    }


//    private fun updateBarChart(usages: List<AppUsage>) {
//        // First, sort the usages by the day of the week
//        val sortedUsages = usages.sortedBy { convertDayOfWeekToIndex(it.dayOfWeek) }
//
//        val entries = sortedUsages.map {
//            BarEntry(convertDayOfWeekToIndex(it.dayOfWeek).toFloat(), it.usageTime.toFloat())
//        }
//
//        val dataSet = BarDataSet(entries, "App Usage")
//        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
//        val barData = BarData(dataSet)
//
//        val xAxis = binding.appsUsageChart.xAxis
//        xAxis.valueFormatter = IndexAxisValueFormatter(getDaysOfWeek())
//        xAxis.position = XAxis.XAxisPosition.BOTTOM
//        xAxis.setDrawGridLines(false)
//        xAxis.granularity = 1f
//
//        binding.appsUsageChart.data = barData
//        binding.appsUsageChart.invalidate() // Refresh the chart
//    }
private fun setupChartAppearance() {
    with(binding.appsUsageChart) {
        axisRight.isEnabled = false
        axisLeft.axisMinimum = 0f // Start from zero
        axisLeft.granularity = 1f // Show every minute
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(getDaysOfWeek())
        xAxis.labelRotationAngle = -45f // Rotate labels for better fit
        description.text = "App Usage Over the Week"
        legend.isEnabled = false
        setTouchEnabled(true)
        setPinchZoom(true)
        setDrawGridBackground(false)
        animateXY(2000, 2000) // Add some animations
    }
}


    private fun convertDayOfWeekToIndex(day: String): Int {
        return when (day) {
            "Sunday" -> 1
            "Monday" -> 2
            "Tuesday" -> 3
            "Wednesday" -> 4
            "Thursday" -> 5
            "Friday" -> 6
            "Saturday" -> 7
            else -> 0
        }
    }

    private fun getDaysOfWeek(): List<String> {
        return listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    }

//    private fun updateAppSpecificBarChart(usages: List<AppUsage>) {
//        // Assuming usages are already ordered by time, convert them to BarEntries
//        val entries = usages.mapIndexed { index, appUsage ->
//            BarEntry(index.toFloat(), appUsage.usageTime.toFloat())
//        }
//
//        val dataSet = BarDataSet(entries, "Usage for ${usages.firstOrNull()?.appName ?: "App"}")
//        dataSet.colors = ColorTemplate.JOYFUL_COLORS.toList()
//        val barData = BarData(dataSet)
//        barData.barWidth = 0.9f // Set custom bar width
//        binding.appsUsageChart.data = barData
//        binding.appsUsageChart.setFitBars(true) // make the x-axis fit exactly all bars
//        binding.appsUsageChart.description.isEnabled = false // Hide the description label
//        binding.appsUsageChart.invalidate() // Refresh the chart
//    }
    private fun updateLineChart(usages: List<AppUsage>) {
        val entries = usages.map { appUsage ->
            Entry(convertDayOfWeekToIndex(appUsage.dayOfWeek).toFloat(), appUsage.usageTime.toFloat())
        }

        val dataSet = LineDataSet(entries, "App Usage")
        dataSet.colors = listOf(ColorTemplate.getHoloBlue())
        dataSet.setDrawCircles(true)
        dataSet.lineWidth = 2.5f
        dataSet.circleRadius = 4f

        val lineData = LineData(dataSet)

        binding.appsUsageChart.data = lineData
        binding.appsUsageChart.invalidate() // Refresh the chart
    }

    private fun updateAppSpecificLineChart(usages: List<AppUsage>) {
        val entries = usages.map { appUsage ->
            Entry(convertDayOfWeekToIndex(appUsage.dayOfWeek).toFloat(), appUsage.usageTime.toFloat())
        }

        val dataSet = LineDataSet(entries, "Usage for ${usages.firstOrNull()?.appName ?: "App"}")
        dataSet.color = ColorTemplate.getHoloBlue()
        dataSet.valueTextColor = Color.BLACK
        dataSet.setDrawCircles(true)
        dataSet.setDrawValues(true)
        dataSet.lineWidth = 2.5f
        dataSet.circleRadius = 4f
        dataSet.setCircleColor(ColorTemplate.getHoloBlue())

        val lineData = LineData(dataSet)

        binding.appsUsageChart.data = lineData
        binding.appsUsageChart.invalidate() // Refresh the chart
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
