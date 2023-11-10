package com.example.digitalminimalism

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore


class UsageMonitoringFragment : Fragment() {
    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var uniqueID: String
    private lateinit var adapter: UsageAdapter
    private var allAppUsages: List<AppUsage> = listOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true) // Enable options menu in this fragment
        val view = inflater.inflate(R.layout.fragment_usage_monitoring, container, false)
        firestoreDB = FirebaseFirestore.getInstance()
        uniqueID = Settings.Secure.getString(context?.contentResolver, Settings.Secure.ANDROID_ID)

        if (!hasUsageStatsPermission(view.context)) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        allAppUsages = getSocialMediaUsage(view.context)
        adapter = UsageAdapter(allAppUsages) { appUsage -> showPopup(appUsage) }
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_apps_usage)
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.adapter = adapter

        saveAppUsageToFirestore(allAppUsages)

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_usage_filter, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
    private fun filterUsage(filterType: String) {
        val filteredList = when (filterType) {
            "Daily" -> allAppUsages.filter { it.lastUsedTime >= System.currentTimeMillis() - 86400000 } // Last 24 hours
            "Weekly" -> allAppUsages.filter { it.lastUsedTime >= System.currentTimeMillis() - 604800000 } // Last 7 days
            "Most Used" -> allAppUsages.sortedByDescending { it.usageTime }
            "Least Used" -> allAppUsages.sortedBy { it.usageTime }
            else -> allAppUsages
        }
        adapter.updateData(filteredList)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_filter_daily -> filterUsage("Daily")
            R.id.action_filter_weekly -> filterUsage("Weekly")
            R.id.action_filter_most_used -> filterUsage("Most Used")
            R.id.action_filter_least_used -> filterUsage("Least Used")
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.context?.let { safeContext ->
            val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_apps_usage)
            recyclerView.layoutManager = LinearLayoutManager(safeContext)

            val usages = getSocialMediaUsage(safeContext)
            adapter = UsageAdapter(usages) { appUsage ->
                showPopup(appUsage)
            }
            recyclerView.adapter = adapter

            saveAppUsageToFirestore(usages)
        }
    }

    private fun saveAppUsageToFirestore(appUsages: List<AppUsage>) {
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        val appUsageInfoRef = userTrackingRef.collection("appUsageInfo")

        appUsages.forEach { appUsage ->
            val usageDocRef = appUsageInfoRef.document(appUsage.appName)
            usageDocRef.get().addOnSuccessListener { documentSnapshot ->
                if (!documentSnapshot.exists()) {
                    // Document does not exist, create a new one
                    usageDocRef.set(appUsage)
                        .addOnSuccessListener { Log.d("Firestore", "App usage for ${appUsage.appName} saved successfully!") }
                        .addOnFailureListener { e -> Log.w("Firestore", "Error saving app usage for ${appUsage.appName}", e) }
                } else {
                    // Document exists, you might want to update it or do nothing
                    Log.d("Firestore", "App usage for ${appUsage.appName} already exists.")
                }
            }.addOnFailureListener { e ->
                Log.w("Firestore", "Error checking for app usage for ${appUsage.appName}", e)
            }
        }
    }
    data class AppUsage(
        val appName: String = "", // Default value
        val usageTime: Long = 0L, // Default value
        val icon: Int = 0, // Default value for an integer is usually 0
        val receivedWifi: Long = 0L, // Default value
        val sentWifi: Long = 0L, // Default value
        val remainingTime: Long = 0L, // Default value
        val lastUsedTime: Long = 0L, // Default value
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as AppUsage
            return appName == other.appName
        }

        override fun hashCode(): Int {
            return appName.hashCode()
        }
    }
    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }


    @SuppressLint("MissingInflatedId")
    fun showPopup(appUsage: AppUsage) {
        // Inflate the popup layout
        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_app_detail, null)

        // Create the popup window
        val popupWindow = PopupWindow(popupView, ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT)

        popupWindow.isOutsideTouchable = true

        // Set the app usage details in the popup
        val appIconImageView: ImageView = popupView.findViewById(R.id.popup_app_icon)
        appIconImageView.setImageResource(appUsage.icon) // Set the app icon image

        val appNameTextView: TextView = popupView.findViewById(R.id.popup_app_name)
        appNameTextView.text = appUsage.appName // Set the app name

        val usageDetailTextView: TextView = popupView.findViewById(R.id.usage_detail_text_view)
        usageDetailTextView.text = "Time spent: ${appUsage.usageTime} minutes" // Set the usage time

        // TODO: Set any other details you want to show in the popup

        // Show the popup window
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
    }




    private fun getSocialMediaUsage(context: Context): List<AppUsage> {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 * 3600 * 24 * 7  // for the last 7 days
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
        val socialMediaPackages = listOf(
            "com.facebook.katana",
            "com.instagram.android",
            "com.twitter.android",
            "com.snapchat.android",
            "com.pinterest",
            "com.whatsapp",
            "com.linkedin.android",
            "com.google.android.youtube",
            "com.reddit.frontpage",
            "com.spotify.music",
            "com.zhiliaoapp.musically",
        )

        val usageSet = LinkedHashSet<AppUsage>()
        stats.filter { socialMediaPackages.contains(it.packageName) }.forEach {
            val name = when (it.packageName) {
                "com.facebook.katana" -> "Facebook"
                "com.instagram.android" -> "Instagram"
                "com.twitter.android" -> "Twitter"
                "com.snapchat.android" -> "Snapchat"
                "com.pinterest" -> "Pinterest"
                "com.whatsapp" -> "WhatsApp"
                "com.linkedin.android" -> "LinkedIn"
                "com.google.android.youtube" -> "Youtube"
                "com.reddit.frontpage" -> "Reddit"
                "com.spotify.music" -> "Spotify"
                "com.zhiliaoapp.musically" -> "TikTok"
                else -> "Other"
            }
            val icon = when (it.packageName) {
                "com.facebook.katana" -> R.drawable.ic_facebook
                "com.instagram.android" -> R.drawable.ic_instagram
                "com.twitter.android" -> R.drawable.ic_twitter
                "com.snapchat.android" -> R.drawable.ic_snapchat
                "com.pinterest" -> R.drawable.iconpinterest
                "com.whatsapp" -> R.drawable.ic_whatsapp
                "com.linkedin.android" -> R.drawable.ic_linkedin
                "com.google.android.youtube" -> R.drawable.ic_youtube
                "com.reddit.frontpage" -> R.drawable.ic_reddit
                "com.spotify.music" -> R.drawable.ic_spotify
                "com.zhiliaoapp.musically" -> R.drawable.ic_tiktok
                else -> R.drawable.ic_other
            }
            val usageTime = it.totalTimeInForeground / (1000 * 60) // Time in minutes
            val remainingTime = (60 - usageTime).coerceAtLeast(0) // Calculate remaining time from 1 hour

            val (receivedWifi, sentWifi) = getNetworkDataUsage(context, it.packageName, beginTime, endTime)
            val lastUsedTime = it.lastTimeUsed  // Fetch the last used time

            usageSet.add(AppUsage(name, usageTime, icon, receivedWifi, sentWifi, remainingTime, lastUsedTime))
        }
        return usageSet.toList()
    }

    @SuppressLint("ServiceCast")
    private fun getNetworkDataUsage(context: Context, packageName: String, beginTime: Long, endTime: Long): Pair<Long, Long> {
        val packageManager = context.packageManager
        return try {
            val info: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val networkStatsManager = requireContext().getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
            val uid = info.uid
            val nwStatsWifi = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, null, beginTime, endTime, uid)
            var receivedWifi: Long = 0
            var sentWifi: Long = 0
            val bucketWifi = NetworkStats.Bucket()
            while (nwStatsWifi.hasNextBucket()) {
                nwStatsWifi.getNextBucket(bucketWifi)
                receivedWifi += bucketWifi.rxBytes
                sentWifi += bucketWifi.txBytes
            }
            nwStatsWifi.close()
            Pair(receivedWifi, sentWifi)
        } catch (e: PackageManager.NameNotFoundException) {
            Pair(0, 0) // Return zeros if the package is not found
        }
    }

}

