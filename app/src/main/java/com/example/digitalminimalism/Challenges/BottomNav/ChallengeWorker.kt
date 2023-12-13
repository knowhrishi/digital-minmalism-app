//ChallengeWorker.kt
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Context.USAGE_STATS_SERVICE
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChallengeWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val firestoreDB = FirebaseFirestore.getInstance()
        val userId = inputData.getString("userId")
        val packageName = inputData.getString("packageName")
        val date = inputData.getString("date")
        val documentId = inputData.getString("documentId")

        // Check if the app was used
        if (wasAppUsed(packageName)) {
            // If the app was used, update the challenge status to "Lost"
            firestoreDB.collection("userTracking")
                .document(userId!!)
                .collection("challenges")
                .document(documentId!!)
                .update("status", "Lost")
        }


        Result.success()
    }

    private fun wasAppUsed(packageName: String?): Boolean {
        val usageStatsManager = applicationContext.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val startTime = inputData.getLong("startTime", 0) // Get the start time from the input data

        val usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        val usageStats = usageStatsList.find { it.packageName == packageName }

        return usageStats?.totalTimeInForeground ?: 0 > 0
    }
}