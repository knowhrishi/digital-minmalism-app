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
import android.net.TrafficStats
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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class UsageMonitoringFragment : Fragment() {
    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var uniqueID: String
    private lateinit var adapter: UsageAdapter
    private var allAppUsages: MutableList<AppUsage> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.fragment_usage_monitoring, container, false)
        firestoreDB = FirebaseFirestore.getInstance()
        uniqueID = Settings.Secure.getString(context?.contentResolver, Settings.Secure.ANDROID_ID)
        initializeUserDocument()

        if (!hasUsageStatsPermission(view.context)) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

// In onCreateView or onViewCreated
        allAppUsages = getSocialMediaUsage(view.context).toMutableList()
        adapter = UsageAdapter(allAppUsages) { appUsage -> showPopup(appUsage) }

        saveAppUsageToFirestore(allAppUsages)

        return view
    }

    private fun initializeUserDocument() {
        val userDocRef = firestoreDB.collection("userTracking").document(uniqueID)
        userDocRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val newUser = mapOf(
                    "lastActive" to System.currentTimeMillis(),
                    "totalScreenTime" to 0L,
                    "createdAt" to System.currentTimeMillis()
                )
                userDocRef.set(newUser)
            }
        }
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
            val lottieAnimationView = view.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
            val usages = getSocialMediaUsage(safeContext)
            adapter = UsageAdapter(usages.toMutableList()) { appUsage ->
                showPopup(appUsage)
            }
            recyclerView.adapter = adapter

            saveAppUsageToFirestore(usages)
            // Set up FloatingActionButton
            val fab: FloatingActionButton = view.findViewById(R.id.fab)
            fab.setOnClickListener {
                lottieAnimationView.visibility = View.VISIBLE
                // Refresh data when FAB is clicked
                val refreshedUsages = getSocialMediaUsage(safeContext)
                adapter.updateData(refreshedUsages)
                // Hide the LottieAnimationView
                lottieAnimationView.visibility = View.GONE
            }
        }
    }

    private fun saveAppUsageToFirestore(appUsages: List<AppUsage>) {
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)

        appUsages.forEach { appUsage ->
            val appUsageInfoRef =
                userTrackingRef.collection("appUsageInfo").document(appUsage.appName)

            appUsageInfoRef.set(appUsage.toMap())
                .addOnSuccessListener {
                    Log.d("Firestore", "App usage for ${appUsage.appName} saved successfully!")
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error saving app usage for ${appUsage.appName}", e)
                }
        }
        updateSummaryCollections(appUsages)

    }

    private fun updateSummaryCollections(appUsages: List<AppUsage>) {
        val currentDate = Calendar.getInstance()
        val dailyDocId =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate.time)
        val weeklyDocId =
            "${currentDate.get(Calendar.YEAR)}-${currentDate.get(Calendar.WEEK_OF_YEAR)}"
        val monthlyDocId = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentDate.time)

        val dailySummaryRef = firestoreDB.collection("userTracking")
            .document(uniqueID)
            .collection("dailySummary")
            .document(dailyDocId)

        val weeklySummaryRef = firestoreDB.collection("userTracking")
            .document(uniqueID)
            .collection("weeklySummary")
            .document(weeklyDocId)

        val monthlySummaryRef = firestoreDB.collection("userTracking")
            .document(uniqueID)
            .collection("monthlySummary")
            .document(monthlyDocId)

        val totalDailyUsageTime = appUsages.sumOf { it.usageTime }
        val mostUsedApp = appUsages.maxByOrNull { it.usageTime }?.appName ?: ""
        val leastUsedApp = appUsages.minByOrNull { it.usageTime }?.appName ?: ""

        val dailySummary = mapOf(
            "totalUsageTime" to totalDailyUsageTime,
            "mostUsedApp" to mostUsedApp,
            "leastUsedApp" to leastUsedApp
        )

        dailySummaryRef.set(dailySummary, SetOptions.merge())
        weeklySummaryRef.update("totalUsageTime", FieldValue.increment(totalDailyUsageTime))
        monthlySummaryRef.update("totalUsageTime", FieldValue.increment(totalDailyUsageTime))

        // Update weekly and monthly averages
        updateAverage(
            weeklySummaryRef,
            "dailyAverages",
            totalDailyUsageTime,
            currentDate.get(Calendar.DAY_OF_WEEK)
        )
        updateAverage(
            monthlySummaryRef,
            "weeklyAverages",
            totalDailyUsageTime,
            currentDate.get(Calendar.WEEK_OF_MONTH)
        )
    }

    private fun updateAverage(
        documentRef: DocumentReference,
        fieldName: String,
        usageTime: Long,
        index: Int
    ) {
        documentRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val averages =
                    document.data?.get(fieldName) as? MutableMap<String, Long> ?: mutableMapOf()
                val key = index.toString()
                averages[key] = (averages[key] ?: 0L) + usageTime

                documentRef.update(fieldName, averages)
            } else {
                val averages = mapOf(index.toString() to usageTime)
                documentRef.set(mapOf(fieldName to averages), SetOptions.merge())
            }
        }
    }

    data class AppUsage(
        val appName: String = "",
        val usageTime: Long = 0L,
        val dayOfWeek: String = "",
        val icon: Int = 0,
        val receivedWifi: Long = 0L,
        val sentWifi: Long = 0L,
        val remainingTime: Long = 0L,
        val lastUsedTime: Long = 0L
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as AppUsage
            return appName == other.appName
        }

        fun toMap(): Map<String, Any> = mapOf(
            "appName" to appName,
            "usageTime" to usageTime,
            "icon" to icon,
            "receivedWifi" to receivedWifi,
            "sentWifi" to sentWifi,
            "remainingTime" to remainingTime,
            "lastUsedTime" to lastUsedTime,
            "dayOfWeek" to dayOfWeek
        )

        override fun hashCode(): Int {
            return appName.hashCode()
        }

    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }


    @SuppressLint("InflateParams", "SetTextI18n")
    fun showPopup(appUsage: AppUsage) {
        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_app_detail, null)

        val popupWindow = PopupWindow(
            popupView,
            ActionBar.LayoutParams.MATCH_PARENT,
            ActionBar.LayoutParams.MATCH_PARENT
        )
        popupWindow.isOutsideTouchable = true

        // Set up close button
        val closeButton: ImageView = popupView.findViewById(R.id.close_button)
        closeButton.setOnClickListener { popupWindow.dismiss() }

        // Set app details
        val appIconImageView: ImageView = popupView.findViewById(R.id.popup_app_icon)
        appIconImageView.setImageResource(appUsage.icon)

        val appNameTextView: TextView = popupView.findViewById(R.id.popup_app_name)
        appNameTextView.text = appUsage.appName

        val usageDetailTextView: TextView = popupView.findViewById(R.id.usage_detail_text_view)
        usageDetailTextView.text = "Time spent: ${appUsage.usageTime} minutes\n" +
                "WiFi Received: ${formatDataUsage(appUsage.receivedWifi)}\n" +
                "WiFi Sent: ${formatDataUsage(appUsage.sentWifi)}"
        // Setup chart
        val chart: BarChart = popupView.findViewById(R.id.chart) // Replace with your chart ID
        setupChart(chart, appUsage)

        val recommendation = generateRecommendation(appUsage)
        val recommendationTextView: TextView = popupView.findViewById(R.id.recommendation_text_view)
        recommendationTextView.text = recommendation

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
    }

    private fun formatDataUsage(bytes: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            bytes < kb -> "$bytes B"
            bytes < mb -> String.format("%.2f KB", bytes / kb.toFloat())
            bytes < gb -> String.format("%.2f MB", bytes / mb.toFloat())
            else -> String.format("%.2f GB", bytes / gb.toFloat())
        }
    }


    private fun setupChart(chart: BarChart, appUsage: AppUsage) {
        val entries = listOf(BarEntry(1f, appUsage.usageTime.toFloat()))
        val dataSet = BarDataSet(entries, "Usage Time").apply {
            color = ContextCompat.getColor(chart.context, R.color.black)
            valueTextColor = ContextCompat.getColor(chart.context, R.color.black)
            valueTextSize = 12f
            setDrawValues(true)
        }

        val data = BarData(dataSet)
        chart.data = data
        chart.description.isEnabled = false
        chart.setFitBars(true)
        chart.animateY(1000)

        // Custom Y-axis formatter
        chart.axisLeft.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return convertMinutesToHoursMinutes(value.toInt())
            }
        }

        // Custom X-axis formatter
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return value.toInt().toString() // Display the value as a string
            }
        }

        // Adjusting the maximum Y-axis value based on data
        chart.axisLeft.axisMaximum =
            appUsage.usageTime + (appUsage.usageTime * 0.1f) // 10% extra space at the top

        chart.invalidate()
    }

    private fun convertMinutesToHoursMinutes(minutes: Int): String {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return if (hours > 0) "${hours}h ${remainingMinutes}m" else "${remainingMinutes}m"
    }


    private fun generateRecommendation(appUsage: UsageMonitoringFragment.AppUsage): String {
        // Example recommendation logic
        return if (appUsage.usageTime > 60) "Consider reducing usage" else "Usage is moderate"
    }

    private fun getSocialMediaUsage(context: Context): List<AppUsage> {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 * 3600 * 24 * 7  // for the last 7 days
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats =
            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
        Log.d("UsageMonitoring", "Total apps found before filtering: ${stats.size}")


        val socialMediaPackages = setOf(
            "com.facebook.katana", "com.instagram.android", "com.twitter.android",
            "com.snapchat.android", "com.pinterest", "com.whatsapp",
            "com.linkedin.android", "com.google.android.youtube",
            "com.reddit.frontpage", "com.spotify.music", "com.zhiliaoapp.musically"
            // Add any other social media package names here
        )
        val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
        val usageMap = mutableMapOf<String, AppUsage>()
        stats.asSequence()
            .filter { usageStats -> socialMediaPackages.contains(usageStats.packageName) }
            .forEach { usageStats ->
                try {
                    val (receivedWifi, sentWifi) = getNetworkDataUsage(
                        context,
                        usageStats.packageName,
                    )
                    Log.d(
                        "NetworkStats",
                        "Package: ${usageStats.packageName}, Received WiFi: $receivedWifi, Sent WiFi: $sentWifi"
                    )
                    val appName = getAppName(usageStats.packageName)
                    val icon = getAppIcon(usageStats.packageName)
                    val usageTime =
                        usageStats.totalTimeInForeground / (1000 * 60) // Time in minutes
                    val remainingTime = (60 - usageTime).coerceAtLeast(0)
                    val lastUsedTime = usageStats.lastTimeUsed
                    Log.d("UsageTimeCheck", "App: ${appName}, Usage Time: $usageTime")
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = usageStats.lastTimeUsed
                    val dayOfWeek = calendar.getDisplayName(
                        Calendar.DAY_OF_WEEK,
                        Calendar.LONG,
                        Locale.getDefault()
                    )

                    val appUsage = AppUsage(
                        appName,
                        usageTime,
                        dayOfWeek,
                        icon,
                        receivedWifi,
                        sentWifi,
                        remainingTime,
                        lastUsedTime
                    )

                    // Save weekly usage to Firestore
                    val appUsageInfoRef =
                        userTrackingRef.collection("appUsageInfo").document(appName)
                    val weeklyUsageCollectionRef = appUsageInfoRef.collection("weeklyUsage")
                    val weeklyUsageDocRef = weeklyUsageCollectionRef.document(endTime.toString())
                    weeklyUsageDocRef.set(appUsage)

                    usageMap[usageStats.packageName] = appUsage
                } catch (e: PackageManager.NameNotFoundException) {
                    // Handle the exception as necessary
                }
            }

        return usageMap.values.toList()
    }

    private fun getAppName(packageName: String): String = when (packageName) {
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

    private fun getAppIcon(packageName: String): Int = when (packageName) {
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


    @SuppressLint("ServiceCast")
    private fun getNetworkDataUsage(
        context: Context,
        packageName: String
    ): Pair<Long, Long> {
        val packageManager = context.packageManager
        return try {
            val info: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val uid = info.uid

            val receivedWifi = TrafficStats.getUidRxBytes(uid)
            val sentWifi = TrafficStats.getUidTxBytes(uid)

            if (receivedWifi == TrafficStats.UNSUPPORTED.toLong() || sentWifi == TrafficStats.UNSUPPORTED.toLong()) {
                Log.e("NetworkStats", "The device does not support tracking of network usage.")
                Pair(0, 0)
            } else {
                Log.d("NetworkStats", "Received: $receivedWifi, Sent: $sentWifi")
                Pair(receivedWifi, sentWifi)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("NetworkStats", "Package not found: ${e.message}")
            Pair(0, 0)
        } catch (e: Exception) {
            Log.e("NetworkStats", "Error fetching network data: ${e.message}")
            Pair(0, 0)
        }
    }
}

