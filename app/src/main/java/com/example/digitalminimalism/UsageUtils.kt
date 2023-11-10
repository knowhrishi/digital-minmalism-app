package com.example.digitalminimalism

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import org.apache.commons.math3.stat.regression.SimpleRegression


object UsageUtils {

    data class AppUsage(val appName: String, val usageTime: Long, val icon: Int, val receivedWifi: Long, val sentWifi: Long) {
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
    data class AppInfo(val packageName: String, val displayName: String, val icon: Int)

    enum class AnalysisType {
        DAILY,
        WEEKLY,
        MONTHLY,
        MOST_USED_APPS,
        LEAST_USED_APPS;

        fun getInterval(): Long {
            val oneDayMillis = 1000L * 3600 * 24
            return when (this) {
                DAILY -> oneDayMillis
                WEEKLY -> oneDayMillis * 7
                MONTHLY -> oneDayMillis * 30
                else -> oneDayMillis
            }
        }
    }

    private val socialMediaInfo = mapOf(
        "com.facebook.katana" to AppInfo("com.facebook.katana", "Facebook", R.drawable.ic_facebook),
        "com.instagram.android" to AppInfo("com.instagram.android", "Instagram", R.drawable.ic_instagram),
        "com.twitter.android" to AppInfo("com.twitter.android", "X", R.drawable.ic_x),
        "com.snapchat.android" to AppInfo("com.snapchat.android", "Snapchat", R.drawable.ic_snapchat),
        "com.pinterest" to AppInfo("com.pinterest", "Pinterest", R.drawable.iconpinterest),
        "com.whatsapp" to AppInfo("com.whatsapp", "WhatsApp", R.drawable.ic_whatsapp),
        "com.linkedin.android" to AppInfo("com.linkedin.android", "LinkedIn", R.drawable.ic_linkedin),
        "com.google.android.youtube" to AppInfo("com.google.android.youtube", "Youtube", R.drawable.ic_youtube),
        "com.reddit.frontpage" to AppInfo("com.reddit.frontpage", "Reddit", R.drawable.ic_reddit),
        "com.spotify.music" to AppInfo("com.spotify.music", "Spotify", R.drawable.ic_spotify),
        "com.zhiliaoapp.musically" to AppInfo("com.zhiliaoapp.musically", "TikTok", R.drawable.ic_tiktok),

        )

    @SuppressLint("ServiceCast")
    private fun getNetworkDataUsage(context: Context, packageName: String, beginTime: Long, endTime: Long): Pair<Long, Long> {
        val packageManager: PackageManager = context.packageManager
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
            Pair(0, 0)
        }
    }


    fun getSocialMediaUsage(context: Context, analysisType: AnalysisType = AnalysisType.DAILY): List<AppUsage> {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - analysisType.getInterval()
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
        val usageList = stats.filter { socialMediaInfo.containsKey(it.packageName) }.map {
            val info = socialMediaInfo[it.packageName] ?: AppInfo(it.packageName, "Other", R.drawable.ic_other)
            val (receivedWifi, sentWifi) = getNetworkDataUsage(context, it.packageName, beginTime, endTime)
            AppUsage(info.displayName, it.totalTimeInForeground / (1000 * 60), info.icon, receivedWifi, sentWifi)
        }
        return usageList.toSet().toList()  // Convert to set to remove duplicates, then back to list
    }
    fun findTrend(usages: List<AppUsage>): String {
        val regression = SimpleRegression()
        for ((index, usage) in usages.withIndex()) {
            regression.addData(index.toDouble(), usage.usageTime.toDouble())
        }
        return if (regression.slope > 0) "Increasing" else "Decreasing"
    }

    fun compareUsage(previousUsages: List<AppUsage>, currentUsages: List<AppUsage>): Map<String, String> {
        val comparison = mutableMapOf<String, String>()
        for (i in currentUsages.indices) {
            val currentUsage = currentUsages[i].usageTime
            val previousUsage = previousUsages[i].usageTime
            comparison[currentUsages[i].appName] = if (currentUsage > previousUsage) "Increased" else "Decreased"
        }
        return comparison
    }

}
