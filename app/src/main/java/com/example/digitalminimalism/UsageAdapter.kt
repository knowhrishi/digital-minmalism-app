package com.example.digitalminimalism
// UsageAdapter.kt

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry

class UsageAdapter(
    private var usages: MutableList<UsageMonitoringFragment.AppUsage>,
    private val itemClick: (UsageMonitoringFragment.AppUsage) -> Unit
) : RecyclerView.Adapter<UsageAdapter.UsageViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsageViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_app_usage, parent, false)
        return UsageViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UsageViewHolder, position: Int) {
        val usage = usages[position]
        holder.usageTime.text = "${usage.usageTime} min"
        holder.bind(usage)
    }

    override fun getItemCount(): Int = usages.size
    inner class UsageViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.icon)
        private val appName: TextView = view.findViewById(R.id.app_name)
        val usageTime: TextView = view.findViewById(R.id.usage_time)


        @SuppressLint("SetTextI18n")
        fun bind(appUsage: UsageMonitoringFragment.AppUsage) {
            icon.setImageResource(appUsage.icon)
            appName.text = appUsage.appName
            usageTime.text = "${appUsage.usageTime} minutes"
            view.setOnClickListener {
                itemClick(appUsage) // Call the passed lambda function
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newUsages: List<UsageMonitoringFragment.AppUsage>) {
        usages.clear() // Clear old data
        usages.addAll(newUsages) // Add new data
        notifyDataSetChanged() // Notify the adapter about the data change
    }
}
