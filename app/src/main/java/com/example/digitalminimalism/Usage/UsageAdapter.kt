package com.example.digitalminimalism.Usage
// UsageAdapter.kt

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalminimalism.R

class UsageAdapter(private val usageStats: List<UsageMonitoringFragment.UsageStat>) : RecyclerView.Adapter<UsageAdapter.UsageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsageViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_app_usage, parent, false)
        return UsageViewHolder(view)
    }

    override fun getItemCount(): Int {
        return usageStats.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UsageViewHolder, position: Int) {
        val usageStat = usageStats[position]
        holder.bind(usageStat)
    }

    inner class UsageViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

        @SuppressLint("SetTextI18n")
        fun bind(usageStat: UsageMonitoringFragment.UsageStat) {
            val appNameTextView: TextView = view.findViewById(R.id.app_name)
            val totalTimeTextView: TextView = view.findViewById(R.id.usage_time)
            val icon: ImageView = itemView.findViewById(R.id.icon)

            appNameTextView.text = usageStat.appName
            totalTimeTextView.text = formatTime(usageStat.totalTime)
            icon.setImageDrawable(getAppIcon(usageStat.packageName, view.context)) // Pass the packageName here

        }

private fun getAppIcon(packageName: String, context: Context): Drawable? {
    return when (packageName) {
        "com.facebook.katana" -> ContextCompat.getDrawable(context, R.drawable.ic_facebook)
        "com.instagram.android" -> ContextCompat.getDrawable(context, R.drawable.ic_instagram)
        "com.twitter.android" -> ContextCompat.getDrawable(context, R.drawable.ic_twitter)
        "com.snapchat.android" -> ContextCompat.getDrawable(context, R.drawable.ic_snapchat)
        "com.pinterest" -> ContextCompat.getDrawable(context, R.drawable.iconpinterest)
        "com.whatsapp" -> ContextCompat.getDrawable(context, R.drawable.ic_whatsapp)
        "com.linkedin.android" -> ContextCompat.getDrawable(context, R.drawable.ic_linkedin)
        "com.google.android.youtube" -> ContextCompat.getDrawable(context, R.drawable.ic_youtube)
        "com.reddit.frontpage" -> ContextCompat.getDrawable(context, R.drawable.ic_reddit)
        "com.spotify.music" -> ContextCompat.getDrawable(context, R.drawable.ic_spotify)
        "com.zhiliaoapp.musically" -> ContextCompat.getDrawable(context, R.drawable.ic_tiktok)
        else -> ContextCompat.getDrawable(context, R.drawable.ic_other)
    }
}

        fun formatTime(millis: Long): String {
            val hours = millis / (1000 * 60 * 60)
            val minutes = millis % (1000 * 60 * 60) / (1000 * 60)
            return String.format("%d hr, %02d min", hours, minutes)
        }
    }
}
