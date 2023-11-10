package com.example.digitalminimalism

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
class TrendAnalysisFragment : Fragment() {

    private lateinit var chart: BarChart

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trend_analysis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chart = view.findViewById(R.id.chart)

        // Populate Spinner with Analysis Types (assuming you have a string-array named analysis_types in res/values/strings.xml)
        val spinner = view.findViewById<Spinner>(R.id.analysis_type_spinner)
        ArrayAdapter.createFromResource(requireContext(), R.array.analysis_types, android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View, position: Int, id: Long) {
                val selectedAnalysisType = UsageUtils.AnalysisType.values()[position]
                updateChart(selectedAnalysisType)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                // Do nothing here.
            }
        }

        updateChart(UsageUtils.AnalysisType.DAILY)
    }

    private fun updatePieChart(usages: List<UsageUtils.AppUsage>) {
        val pieChart = view?.findViewById<PieChart>(R.id.pie_chart)

        val entries = ArrayList<PieEntry>()
        for (usage in usages) {
            entries.add(PieEntry(usage.usageTime.toFloat(), usage.appName))
        }

        val dataSet = PieDataSet(entries, "App Usage")
        dataSet.setColors(*ColorTemplate.COLORFUL_COLORS)

        val data = PieData(dataSet)
        pieChart?.data = data

        // Customize the Pie Chart
        pieChart?.description?.isEnabled = false
        pieChart?.isDrawHoleEnabled = true
        pieChart?.setUsePercentValues(true)
        pieChart?.setEntryLabelTextSize(12f)
        pieChart?.setEntryLabelColor(Color.BLACK)

        pieChart?.invalidate()  // Invalidate the Pie Chart to refresh the display
    }


    private fun updateChart(analysisType: UsageUtils.AnalysisType) {
        val usages = UsageUtils.getSocialMediaUsage(requireContext(), analysisType)
        updatePieChart(usages)  // Update the Pie Chart



        val barEntries = ArrayList<BarEntry>()
        for ((index, usage) in usages.withIndex()) {
            barEntries.add(BarEntry(index.toFloat(), usage.usageTime.toFloat()))
        }

        val dataSet = BarDataSet(barEntries, "Social Media Usage")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()

        val barData = BarData(dataSet)
        chart.data = barData

//         Customizing the Bar Chart
        chart.description.isEnabled = false // Hide the default description on the bottom right

// X-Axis Customizations
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false) // Remove grid lines
        xAxis.setDrawAxisLine(true)
        xAxis.granularity = 1f
        xAxis.labelCount = usages.size
        xAxis.valueFormatter = IndexAxisValueFormatter(usages.map { it.appName })

// Y-Axis Customizations (Left)
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.spaceTop = 30f
        leftAxis.axisMinimum = 0f

// Y-Axis Customizations (Right)
        val rightAxis = chart.axisRight
        rightAxis.setDrawGridLines(false)
        rightAxis.spaceTop = 30f
        rightAxis.axisMinimum = 0f

// Legend Customizations
        val legend = chart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)

// Bar Width Customization
        val barSpace = 0.03f
        val barWidth = 0.2f

// Other Customizations
        chart.setPinchZoom(false)
        chart.setDrawBarShadow(false)
        chart.setDrawGridBackground(false)

// Customizations like animation, axis settings, etc.
        chart.animateY(500)
        chart.invalidate()

        chart.invalidate()
    }
}
