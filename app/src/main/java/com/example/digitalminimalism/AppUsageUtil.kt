// AppUsageUtil.kt
package com.example.digitalminimalism

import android.Manifest
import android.annotation.SuppressLint
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.getSystemService
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

class AppUsageUtil {
    companion object {

        private lateinit var usageStatsManager: UsageStatsManager
        private lateinit var firestoreDB: FirebaseFirestore
        private lateinit var uniqueID: String
        private lateinit var adapter: UsageAdapter
        private var allAppUsages: MutableList<UsageMonitoringFragment.AppUsage> = mutableListOf()
        fun startAppUsageMonitoring(context: Context) {
            Log.d("AppUsageUtil", "Starting app usage monitoring")
            usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            Thread {
                while (true) {
                    Log.d("AppUsageUtil", "Checking app usage")
                    checkAppsAndNotify(context)
                    checkCurrentApp(context, usageStatsManager)
                    SystemClock.sleep(60000) // Check every 60 seconds
                }
            }.start()
        }

        private fun checkAppsAndNotify(context: Context) {
            Log.d("AppUsageUtil", "Fetching timer data from Firestore")
            firestoreDB = FirebaseFirestore.getInstance()
            uniqueID = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

            val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
            userTrackingRef.collection("appTimers").get()
                .addOnSuccessListener { documents ->
                    Log.d("AppUsageUtil", "Successfully fetched timer data")
                    for (document in documents) {
                        val appName = document.getString("appName") ?: continue
                        val timerSetUntil = document.getLong("timerSetUntil") ?: 0

                        if (System.currentTimeMillis() < timerSetUntil) {
                            val usageStats = usageStatsManager.queryUsageStats(
                                UsageStatsManager.INTERVAL_DAILY,
                                System.currentTimeMillis() - 1000 * 3600 * 24, // Last 24 hours
                                System.currentTimeMillis()
                            )
                            val appUsageTime = usageStats.firstOrNull { it.packageName == appName }?.totalTimeInForeground ?: 0

                            // Check if usage exceeds the limit
                            if (appUsageTime > timerSetUntil) {
                                showNotification(context, appName)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    // Handle failure
                    Log.e("AppUsageUtil", "Failed to fetch timer data")

                }
        }


        private fun checkCurrentApp(context: Context, usageStatsManager: UsageStatsManager) {
            // Get the current foreground app
            val currentApp = getForegroundApp(usageStatsManager)
            if (currentApp.isNotEmpty()) {
                checkAppAgainstTimers(context, currentApp)
            }
        }

        private fun getForegroundApp(usageStatsManager: UsageStatsManager): String {
            val time = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 3600, time)
            return stats.maxByOrNull { it.lastTimeUsed }?.packageName ?: ""
        }

        private fun checkAppAgainstTimers(context: Context, currentApp: String) {
            val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
            userTrackingRef.collection("appTimers").document(currentApp).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val timerSetUntil = document.getLong("timerSetUntil") ?: 0
                        if (System.currentTimeMillis() < timerSetUntil) {
                            val intent = Intent(context, FullScreenReminderActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Important for starting activity from non-activity context
                            context.startActivity(intent)
                        }
                    }
                }
        }
        @SuppressLint("MissingPermission")
        private fun showNotification(context: Context, appName: String) {
            val notificationBuilder = NotificationCompat.Builder(context, "YOUR_CHANNEL_ID")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your icon
                .setContentTitle("Time Limit Reached")
                .setContentText("Timer for $appName has expired.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            with(NotificationManagerCompat.from(context)) {
                notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
            }
        }

        fun getSocialMediaUsage(context: Context): List<UsageMonitoringFragment.AppUsage> {
            Log.d("AppUsageUtil", "Getting social media usage")
            firestoreDB = FirebaseFirestore.getInstance()
            uniqueID = Settings.Secure.getString(context?.contentResolver, Settings.Secure.ANDROID_ID)

            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 1000 * 3600 * 24 * 7  // for the last 7 days
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
            Log.d("AppUsageUtil", "Total apps found before filtering: ${stats.size}")


            val socialMediaPackages = setOf(
                "com.facebook.katana", "com.instagram.android", "com.twitter.android",
                "com.snapchat.android", "com.pinterest", "com.whatsapp",
                "com.linkedin.android", "com.google.android.youtube",
                "com.reddit.frontpage", "com.spotify.music", "com.zhiliaoapp.musically"
                // Add any other social media package names here
            )
            val userTrackingRef = firestoreDB.collection("userTracking").document(uniqueID)
            val usageMap = mutableMapOf<String, UsageMonitoringFragment.AppUsage>()
            stats.asSequence()
                .filter { usageStats -> socialMediaPackages.contains(usageStats.packageName) }
                .forEach { usageStats ->
                    try {
                        val appName = getAppName(usageStats.packageName)
                        val icon = getAppIcon(usageStats.packageName)
                        val usageTime = usageStats.totalTimeInForeground / (1000 * 60) // Time in minutes
                        val remainingTime = (60 - usageTime).coerceAtLeast(0)
                        val (receivedWifi, sentWifi) = getNetworkDataUsage(context, usageStats.packageName, beginTime, endTime)
                        val lastUsedTime = usageStats.lastTimeUsed
                        Log.d("AppUsageUtil", "App: ${appName}, Usage Time: $usageTime")
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = usageStats.lastTimeUsed
                        val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())

                        val appUsage = UsageMonitoringFragment.AppUsage(
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
                        val appUsageInfoRef = userTrackingRef.collection("appUsageInfo").document(appName)
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
        private fun getNetworkDataUsage(context: Context, packageName: String, beginTime: Long, endTime: Long): Pair<Long, Long> {
            val packageManager = context.packageManager
            return try {
                val info: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
                val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
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


}