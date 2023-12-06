// ScheduledTimerAdapter.kt
package com.example.digitalminimalism.Focus.BottomNavigation.Schedule

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalminimalism.R
import java.text.SimpleDateFormat
import java.util.*

class ScheduledTimerAdapter(private var scheduledTimers: List<ScheduleNavFragment.ScheduledTimer>) :
    RecyclerView.Adapter<ScheduledTimerAdapter.ViewHolder>() {

    class ViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.item_scheduled_timer, parent, false)) {
//        private var dateTextView: TextView? = null
        private var timeRangeTextView: TextView? = null

        init {
//            dateTextView = itemView.findViewById(R.id.text_view_date)
            timeRangeTextView = itemView.findViewById(R.id.text_view_time_range)
        }

        fun bind(scheduledTimer: ScheduleNavFragment.ScheduledTimer) {
            val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
//            dateTextView?.text = dateFormat.format(Date(scheduledTimer.startTime))
            timeRangeTextView?.text = itemView.context.getString(
                R.string.time_range_format,
                timeFormat.format(Date(scheduledTimer.startTime)),
                timeFormat.format(Date(scheduledTimer.endTime))
            )

            val weekdaysLayout: LinearLayout = itemView.findViewById(R.id.weekdays_layout)
            for (i in 0 until weekdaysLayout.childCount) {
                val dayTextView = weekdaysLayout.getChildAt(i) as TextView
                val isSelected = scheduledTimer.selectedDays[i]
                dayTextView.setTextColor(if (isSelected) Color.WHITE else Color.BLACK)
                dayTextView.setBackgroundResource(if (isSelected) R.drawable.circle_background_selected else R.drawable.circle_background_notselected)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scheduledTimer: ScheduleNavFragment.ScheduledTimer = scheduledTimers[position]
        holder.bind(scheduledTimer)
    }

    override fun getItemCount(): Int = scheduledTimers.size

    fun updateData(newTimers: List<ScheduleNavFragment.ScheduledTimer>) {
        scheduledTimers = newTimers
        notifyDataSetChanged()
    }
}

