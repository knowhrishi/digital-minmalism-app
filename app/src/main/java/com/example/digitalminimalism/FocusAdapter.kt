package com.example.digitalminimalism

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class FocusAdapter(private val pomodoroSessions: List<FocusFragment.PomodoroSession>) :
    RecyclerView.Adapter<FocusAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskNameTextView: TextView = itemView.findViewById(R.id.tvTaskName)
        val notesTextView: TextView = itemView.findViewById(R.id.tvNotes)
        val startTimeTextView: TextView = itemView.findViewById(R.id.tvStartTime)
        val endTimeTextView: TextView = itemView.findViewById(R.id.tvEndTime)
        val durationTextView: TextView = itemView.findViewById(R.id.tvDuration)
        val completedTextView: TextView = itemView.findViewById(R.id.tvCompleted)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pomodoro_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = pomodoroSessions[position]
        holder.taskNameTextView.text = "Task: ${session.task}"
        holder.notesTextView.text = "Notes: ${session.notes}"
        holder.startTimeTextView.text = "Start Time: ${formatTime(session.startTime)}"
        holder.endTimeTextView.text = "End Time: ${formatTime(session.endTime)}"
        holder.durationTextView.text = "Duration: ${formatDuration(session.duration)}"
        holder.completedTextView.text = "Completed: ${session.completed}"
    }

    private fun formatTime(timeMillis: Long): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(timeMillis)
    }

    private fun formatDuration(durationMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun getItemCount(): Int {
        return pomodoroSessions.size
    }
}