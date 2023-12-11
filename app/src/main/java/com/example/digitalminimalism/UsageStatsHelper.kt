package com.example.digitalminimalism

import android.app.usage.UsageStatsManager
import android.content.Context

object UsageStatsHelper {
    data class UsageStat(
        val appName: String,
        var totalTime: Long,
        val packageName: String,
        val firstTimeStamp: Long,
        val lastTimeStamp: Long,
        val lastTimeUsed: Long
    )

    fun getUsageStatistics(context: Context, startTime: Long, endTime: Long): List<UsageStat> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

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