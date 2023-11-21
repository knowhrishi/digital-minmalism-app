//FocusAdapter.kt
package com.example.digitalminimalism

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FocusAdapter(
    private val focusDurations: List<Long>, // List of focus mode durations in milliseconds
    private val context: Context
) : RecyclerView.Adapter<FocusAdapter.FocusViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FocusViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_focus, parent, false)
        return FocusViewHolder(view)
    }

    override fun onBindViewHolder(holder: FocusViewHolder, position: Int) {
        val duration = focusDurations[position]
        holder.bind(duration)
    }

    override fun getItemCount(): Int = focusDurations.size

    class FocusViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val durationTextView: TextView = view.findViewById(R.id.duration_text)

        fun bind(duration: Long) {
            // Convert milliseconds to minutes and display
            val minutes = duration / 60000
            durationTextView.text = "$minutes minutes"
        }
    }
}
