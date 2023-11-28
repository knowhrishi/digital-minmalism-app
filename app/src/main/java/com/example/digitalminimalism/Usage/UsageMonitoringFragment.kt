package com.example.digitalminimalism.Usage

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.TrafficStats
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.digitalminimalism.R
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
import com.google.firebase.firestore.toObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class UsageMonitoringFragment : Fragment() {
    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var uniqueID: String
    private lateinit var adapter: UsageAdapter
    private var allAppUsages: MutableList<AppUsage> = mutableListOf()

    @SuppressLint("HardwareIds", "MissingInflatedId")
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

        val spinner: Spinner = view.findViewById(R.id.spinner_time_period)
        var timePeriod = spinner.selectedItem.toString()

        allAppUsages = getSocialMediaUsage(view.context, timePeriod).toMutableList()
        adapter = UsageAdapter(allAppUsages) { appUsage -> showPopup(appUsage) }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_apps_usage)
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.adapter = adapter

        saveAppUsageToFirestore(allAppUsages, timePeriod)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                timePeriod = parent.getItemAtPosition(position).toString()
                allAppUsages = getSocialMediaUsage(view.context, timePeriod).toMutableList()
                adapter.updateData(allAppUsages)
                saveAppUsageToFirestore(allAppUsages, timePeriod)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        return view
    }

    private fun initializeUserDocument() {
        val userDocRef = firestoreDB.collection("userTracking").document(uniqueID)
        userDocRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val deviceInfo = mapOf(
                    "manufacturer" to android.os.Build.MANUFACTURER,
                    "model" to android.os.Build.MODEL,
                    "version" to android.os.Build.VERSION.SDK_INT
                )
                val newUser = mapOf(
                    "lastActive" to System.currentTimeMillis(),
                    "totalScreenTime" to 0L,
                    "createdAt" to System.currentTimeMillis(),
                    "deviceInfo" to deviceInfo
                )
                userDocRef.set(newUser)
            } else if (!document.contains("deviceInfo")) {
                // If the document exists but does not contain the "deviceInfo" field, add it
                val deviceInfo = mapOf(
                    "manufacturer" to android.os.Build.MANUFACTURER,
                    "model" to android.os.Build.MODEL,
                    "version" to android.os.Build.VERSION.SDK_INT
                )
                userDocRef.update("deviceInfo", deviceInfo)
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.context?.let { safeContext ->
            val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_apps_usage)
            recyclerView.layoutManager = LinearLayoutManager(safeContext)
            val lottieAnimationView =
                view.findViewById<LottieAnimationView>(R.id.lottieAnimationView)

            val spinner: Spinner = view.findViewById(R.id.spinner_time_period)
            var timePeriod = spinner.selectedItem.toString()

            var usages = getSocialMediaUsage(safeContext, timePeriod)
            adapter = UsageAdapter(usages.toMutableList()) { appUsage ->
                showPopup(appUsage)
            }
            recyclerView.adapter = adapter

            saveAppUsageToFirestore(usages, timePeriod)

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    timePeriod = parent.getItemAtPosition(position).toString()
                    val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
                    userTrackingRef.collection("appUsageInfo").get()
                        .addOnSuccessListener { documents ->
                            allAppUsages = documents.mapNotNull { it.toObject<AppUsage>() }.toMutableList()
                            adapter.updateData(allAppUsages)
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error getting documents: ", e)
                        }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Do nothing
                }
            }
            // Set up FloatingActionButton
            val fab: FloatingActionButton = view.findViewById(R.id.fab)
            fab.setOnClickListener {
                lottieAnimationView.visibility = View.VISIBLE
                // Refresh data when FAB is clicked
                timePeriod = spinner.selectedItem.toString()
                val refreshedUsages = getSocialMediaUsage(safeContext, timePeriod)
                adapter.updateData(refreshedUsages)
                // Hide the LottieAnimationView
                lottieAnimationView.visibility = View.GONE
            }
        }
    }

    private fun saveAppUsageToFirestore(appUsages: List<AppUsage>, timePeriod: String) {
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
        updateSummaryCollections(appUsages, timePeriod)
    }

    private fun updateSummaryCollections(appUsages: List<AppUsage>, timePeriod: String) {
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

        when (timePeriod) {
            "Today" -> dailySummaryRef.set(dailySummary, SetOptions.merge())
            "Last 7 days" -> weeklySummaryRef.update(
                "totalUsageTime",
                FieldValue.increment(totalDailyUsageTime)
            )

            "Last 30 days" -> monthlySummaryRef.update(
                "totalUsageTime",
                FieldValue.increment(totalDailyUsageTime)
            )
        }

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
        val packageName: String = "",
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
//        appIconImageView.setImageResource(appUsage.icon)

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


    private fun generateRecommendation(appUsage: AppUsage): String {
        // Example recommendation logic
        return if (appUsage.usageTime > 60) "Consider reducing usage" else "Usage is moderate"
    }

    private fun getSocialMediaUsage(context: Context, timePeriod: String): List<AppUsage> {
        val endTime = System.currentTimeMillis()
        val beginTime = when (timePeriod) {
            "Today" -> endTime - 1000 * 3600 * 24  // for the last 24 hours
            "Last 7 days" -> endTime - 1000 * 3600 * 24 * 7  // for the last 7 days
            "Last 30 days" -> endTime - 1000L * 3600 * 24 * 30  // for the last 30 days
            "Last 60 minutes" -> endTime - 1000 * 60 * 60  // for the last 60 minutes
            else -> endTime - 1000 * 3600 * 24 * 7  // default to the last 7 days
        }
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
                    val appUsage = createAppUsage(usageStats, context)
                    val appUsageInfoRef =
                        userTrackingRef.collection("appUsageInfo").document(appUsage.appName) // Use app name instead of package name

                    appUsageInfoRef.get().addOnSuccessListener { document ->
                        if (document.exists()) {
                            val lastUsedTimeInDb = document.getLong("lastUsedTime") ?: 0L
                            if (usageStats.lastTimeUsed > lastUsedTimeInDb) {
                                // The data in the database is not up-to-date, update it
//                                val appUsage = createAppUsage(usageStats, context)
                                appUsageInfoRef.set(appUsage.toMap())
                            }
                        } else {
                            // The app usage data does not exist in the database, add it
                            val appUsage = createAppUsage(usageStats, context)
                            appUsageInfoRef.set(appUsage.toMap())
                        }
                    }
                    usageMap[appUsage.appName] = appUsage
                } catch (e: Exception) {
                    Log.e("UsageMonitoring", "Error checking and updating app usage data", e)
                }
            }

        Log.d("UsageMonitoring", "Total apps included in allAppUsages: ${usageMap.size}")

        return usageMap.values.toList()
    }

    private fun createAppUsage(usageStats: UsageStats, context: Context): AppUsage {
    val appName = getAppName(usageStats.packageName)
    val icon = getAppIcon(usageStats.packageName)
    val usageTime = usageStats.totalTimeInForeground / (1000 * 60) // Time in minutes
    val remainingTime = (60 - usageTime).coerceAtLeast(0)
    val lastUsedTime = usageStats.lastTimeUsed
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = usageStats.lastTimeUsed
    val dayOfWeek = calendar.getDisplayName(
        Calendar.DAY_OF_WEEK,
        Calendar.LONG,
        Locale.getDefault()
    )
    val (receivedWifi, sentWifi) = getNetworkDataUsage(context, usageStats.packageName)

    return AppUsage(
        appName,
        usageTime,
        dayOfWeek,
        usageStats.packageName, // Use usageStats.packageName instead of packageName
        receivedWifi,
        sentWifi,
        remainingTime,
        lastUsedTime
    )
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

    private fun getAppIcon(packageName: String): Drawable? {
        return when (packageName) {
            "com.facebook.katana" -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_facebook)
            "com.instagram.android" -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_instagram)
            "com.twitter.android" -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_twitter)
            "com.snapchat.android" -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_snapchat)
            "com.pinterest" -> ContextCompat.getDrawable(requireContext(), R.drawable.iconpinterest)
            "com.whatsapp" -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_whatsapp)
            "com.linkedin.android" -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_linkedin)
            "com.google.android.youtube" -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_youtube)
            "com.reddit.frontpage" -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_reddit)
            "com.spotify.music" -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_spotify)
            "com.zhiliaoapp.musically" -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_tiktok)
            else -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_other)
        }
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

