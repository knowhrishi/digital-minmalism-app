package com.example.digitalminimalism.Usage

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalminimalism.R
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class UsageMonitoringFragment : Fragment() {
    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var uniqueID: String


    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart

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
        if (!isNotificationServiceEnabled()) {
            val enableNotificationListenerIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(enableNotificationListenerIntent)
        }
        if (!hasUsageStatsPermission(view.context)) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }


        return view
    }
    private fun isNotificationServiceEnabled(): Boolean {
        val packageNames = NotificationManagerCompat.getEnabledListenerPackages(requireContext())
        return packageNames.contains(requireContext().packageName)
    }
    enum class TimePeriod {
        TODAY, WEEKLY, MONTHLY
    }
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.context?.let { safeContext ->
            (activity as AppCompatActivity).supportActionBar?.title = "Usage Monitoring"

            pieChart = view.findViewById(R.id.pieChartUsage)
            val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewUsage)
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()
            val usageStats = getUsageStatistics(startTime, endTime)
            setupPieChart(usageStats)
            var adapter = UsageAdapter(usageStats)
            recyclerView.layoutManager = LinearLayoutManager(safeContext)
            recyclerView.adapter = adapter

            val totalTime = usageStats.sumOf { it.totalTime }
            // Set the total unlocks and total notifications
            val totalUnlocksTextView: TextView = view.findViewById(R.id.textViewTotalUnlocks)
            val totalNotificationsTextView: TextView =
                view.findViewById(R.id.textViewTotalNotifications)

            saveUsageDataToFirestore(usageStats)

            totalUnlocksTextView.text = "Unlocks \n ${getUnlocks()}"
            totalNotificationsTextView.text =
                "Notifications \n ${NotificationCounterService.notificationCount}"


            val totalTimeTextView: TextView = view.findViewById(R.id.totalTimeTextView)
            val imageViewTotalTimeTextView: ImageView = view.findViewById(R.id.imageViewTotalTimeTextView)
            recyclerView.layoutManager = LinearLayoutManager(safeContext)
            recyclerView.adapter = adapter
            totalTimeTextView.setOnClickListener(object : View.OnClickListener {
                var currentState = TimePeriod.TODAY

                override fun onClick(v: View?) {
                    val calendar = Calendar.getInstance()
                    val endTime = calendar.timeInMillis
                    var startTime: Long

                    when (currentState) {
                        TimePeriod.TODAY -> {
                            currentState = TimePeriod.WEEKLY
                            totalTimeTextView.text = "This Week's Screen Time"
                            imageViewTotalTimeTextView.setImageResource(R.drawable.ic_minusweek) // Set the image for weekly view
                            calendar.add(Calendar.DAY_OF_YEAR, -7)
                            startTime = calendar.timeInMillis
                        }
                        TimePeriod.WEEKLY -> {
                            currentState = TimePeriod.MONTHLY
                            totalTimeTextView.text = "This Month's Screen Time"
                            imageViewTotalTimeTextView.setImageResource(R.drawable.ic_minusmonth) // Set the image for weekly view
                            calendar.add(Calendar.MONTH, -1)
                            startTime = calendar.timeInMillis
                        }
                        TimePeriod.MONTHLY -> {
                            currentState = TimePeriod.TODAY
                            totalTimeTextView.text = "Today's Screen Time"
                            imageViewTotalTimeTextView.setImageResource(R.drawable.ic_caltoday) // Set the image for weekly view
                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                            calendar.set(Calendar.MINUTE, 0)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            startTime = calendar.timeInMillis
                        }
                    }

                    val usageStats = getUsageStatistics(startTime, endTime)
                    adapter = UsageAdapter(usageStats)
                    recyclerView.adapter = adapter
                    setupPieChart(usageStats)
                }
            })
        }
    }


    private fun getAppSpecificColor(appName: String): Int {
        return when (appName) {
            "Facebook" -> Color.parseColor("#3b5998") // Facebook Blue
            "Instagram" -> Color.parseColor("#C13584") // Instagram Pink
            "Twitter" -> Color.parseColor("#1DA1F2") // Twitter Blue
            "Snapchat" -> Color.parseColor("#FFFC00") // Snapchat Yellow
            "Pinterest" -> Color.parseColor("#E60023") // Pinterest Red
            "WhatsApp" -> Color.parseColor("#25D366") // WhatsApp Green
            "LinkedIn" -> Color.parseColor("#0077B5") // LinkedIn Blue
            "Youtube" -> Color.parseColor("#FF0000") // YouTube Red
            "Reddit" -> Color.parseColor("#FF4500") // Reddit Orange
            "Spotify" -> Color.parseColor("#1DB954") // Spotify Green
            "TikTok" -> Color.parseColor("#000000") // TikTok Black, assuming a default
            else -> ColorTemplate.MATERIAL_COLORS[0] // Default color
        }
    }

    @SuppressLint("NewApi")
    private fun getUnlocks(): Int {
        val usageStatsManager =
            context?.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val queryEvents = usageStatsManager.queryEvents(startTime, endTime)
        var unlocks = 0
        while (queryEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            queryEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.USER_INTERACTION || event.eventType == UsageEvents.Event.SCREEN_INTERACTIVE) {
                unlocks++
            }
        }
        return unlocks
    }


    private fun setupPieChart(usageStats: List<UsageStat>) {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        var totalTime = 0L
        usageStats.forEach { totalTime += it.totalTime }

        for (usageStat in usageStats) {
            val timeInForeground = usageStat.totalTime.toFloat()
            if (timeInForeground > 0) {
                entries.add(PieEntry(timeInForeground, usageStat.appName))
                colors.add(getAppSpecificColor(usageStat.appName))
            }
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
//            colors = ColorTemplate.MATERIAL_COLORS.toList()
            sliceSpace = 3f
            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.1f
            valueLinePart2Length = 0.3f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        }

        val pieData = PieData(dataSet).apply {
            setValueTextSize(12f)
            setValueTextColor(Color.BLACK)

            // Set formatter to display the app name along with the value
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0) {
                        val appName = dataSet.values.find { it.y == value }?.label
                        "$appName"
                    } else {
                        ""
                    }
                }
            })
        }

        pieChart.apply {
            data = pieData
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 80f
            transparentCircleRadius = 98f // Adjust this as necessary
            setTransparentCircleAlpha(0)
            setDrawEntryLabels(false) // Disable the entry labels
            animateY(1400, Easing.EaseInOutQuad)
            legend.isEnabled = false
            setCenterTextSize(15f)
            setCenterTextColor(Color.DKGRAY)
            setExtraOffsets(10f, 0f, 10f, 0f) // Add offsets to make more room for labels
            val totalScreenTime = usageStats.sumOf { it.totalTime }
            centerText = generateCenterSpannableText(totalScreenTime)
            invalidate()
        }
    }

    private fun generateCenterSpannableText(totalScreenTime: Long): SpannableString {
        val formattedTime = formatTime(totalScreenTime)
        val spannableString = SpannableString(formattedTime).apply {
            setSpan(RelativeSizeSpan(1.2f), 0, this.length, 0) // Smaller than 2.0f for subtlety
            setSpan(ForegroundColorSpan(Color.DKGRAY), 0, this.length, 0) // Use a softer color like dark gray
            setSpan(TypefaceSpan("font/poppins_black.ttf"), 0, this.length, 0)
        }
        return spannableString
    }



    fun formatTime(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = millis % (1000 * 60 * 60) / (1000 * 60)
        return String.format("%d hr, %02d min", hours, minutes)
    }

    // Placeholder function for usage statistics
    @SuppressLint("SimpleDateFormat")
   public fun getUsageStatistics(startTime: Long, endTime: Long): List<UsageStat> {
        val usageStatsManager =
            context?.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val queryUsageStats =
            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        val usageStatsMap = mutableMapOf<String, UsageStat>()

        for (usageStats in queryUsageStats) {
            if (usageStats.packageName in getSocialMediaPackages()) {
                val appName = getAppName(usageStats.packageName)
                val existingStat = usageStatsMap[appName]
                if (existingStat != null) {
                    existingStat.totalTime += usageStats.totalTimeInForeground
                } else {
                    usageStatsMap[appName] = UsageStat(
                        appName,
                        usageStats.totalTimeInForeground,
                        usageStats.packageName,
                        usageStats.firstTimeStamp,
                        usageStats.lastTimeStamp,
                        usageStats.lastTimeUsed
                    )
                }
            }
        }

        return usageStatsMap.values.toList()
    }    // Data class for usage statistics (replace this with your actual data model)

    private fun saveUsageDataToFirestore(usageStats: List<UsageStat>) {
        val firestoreDB = FirebaseFirestore.getInstance()
        val userId = Settings.Secure.getString(context?.contentResolver, Settings.Secure.ANDROID_ID)
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStats = getUsageStatistics(startTime, endTime)

        val usageStatsList = usageStats.map {
            mapOf(
                "appName" to it.appName,
                "totalTime" to it.totalTime,
                "packageName" to it.packageName,
                "firstTimeStamp" to it.firstTimeStamp,
                "lastTimeStamp" to it.lastTimeStamp,
                "lastTimeUsed" to it.lastTimeUsed
            )
        }
        val unlocks = getUnlocks()
        val notifications = NotificationCounterService.notificationCount// Replace with your actual method to get the notifications


        val usageData = mapOf(
            "userId" to userId,
            "date" to date,
            "usageStats" to usageStatsList,
            "unlocks" to unlocks,
            "notifications" to notifications
        )

        firestoreDB.collection("userTracking")
            .document(userId)
            .collection("dailyUsage")
            .document(date)
            .set(usageData)
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot successfully written!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing document", e)
            }
    }
    data class UsageStat(
        val appName: String,
        var totalTime: Long,
        val packageName: String,
        val firstTimeStamp: Long,
        val lastTimeStamp: Long,
        val lastTimeUsed: Long
    )
    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }


    private fun getSocialMediaPackages(): Set<String> {
        return setOf(
            "com.facebook.katana", "com.instagram.android", "com.twitter.android",
            "com.snapchat.android", "com.pinterest", "com.whatsapp",
            "com.linkedin.android", "com.google.android.youtube",
            "com.reddit.frontpage", "com.spotify.music", "com.zhiliaoapp.musically"
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


}

