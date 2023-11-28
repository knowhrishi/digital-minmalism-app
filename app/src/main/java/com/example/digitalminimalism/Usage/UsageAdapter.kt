package com.example.digitalminimalism.Usage
// UsageAdapter.kt

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalminimalism.R

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
            val appIconImageView: ImageView = view.findViewById(R.id.icon)
            val appIcon = getAppIcon(appUsage.packageName, view.context)
            appIconImageView.setImageDrawable(appIcon)
            appName.text = appUsage.appName
            usageTime.text = "${appUsage.usageTime} minutes"
            view.setOnClickListener {
                itemClick(appUsage) // Call the passed lambda function
            }
        }
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

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newUsages: List<UsageMonitoringFragment.AppUsage>) {
        usages.clear() // Clear old data
        usages.addAll(newUsages) // Add new data
        notifyDataSetChanged() // Notify the adapter about the data change
    }
}
