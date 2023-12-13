//OneWeekChallengeActivity.kt
package com.example.digitalminimalism.Challenges.BottomNav

import ChallengeWorker
import OneWeekAppAdapter
import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.anychart.core.series.renderingsettings.Context
import com.example.digitalminimalism.R
import com.example.digitalminimalism.UsageStatsHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class OneWeekChallengeActivity : AppCompatActivity() {

    private lateinit var recyclerViewApps: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var appAdapter: OneWeekAppAdapter
    private lateinit var apps: List<App> // Your data source

    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var uniqueID: String
    private var selectedApp: App? = null // Add this line to store the selected app
    private lateinit var allApps: MutableList<App> // All apps including those not in the database

    data class App(val name: String, val iconResId: Int, val packageName: String, var status: String, var startTime: Long)

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_one_week_challenge)

        initializeRecyclerView()
        initializeSearchView()
        initializeFab()

        uniqueID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        // Initialize your data source here
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStats = UsageStatsHelper.getUsageStatistics(this, startTime, endTime)
        val apps = usageStats.map { App(it.appName, R.drawable.ic_nav_challenge, it.packageName, "Not started", 0) }.toMutableList()

        allApps = UsageStatsHelper.getUsageStatistics(this, startTime, endTime)
            .map { App(it.appName, getAppIconResId(it.packageName), it.packageName, "Not started", 0L) }
            .toMutableList()
        appAdapter.updateApps(allApps)
        fetchChallenges() // Call this after initializing allApps

        appAdapter.updateApps(apps)

    }
    private fun wasAppUsed(packageName: String?, startTime: Long, endTime: Long): Boolean {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager

        val usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        val usageStats = usageStatsList.find { it.packageName == packageName }

        return (usageStats?.totalTimeInForeground ?: 0) > 0
    }
    @SuppressLint("HardwareIds")
    private fun startChallenge(app: App) {

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        firestoreDB = FirebaseFirestore.getInstance()
        uniqueID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        val challengeData = mapOf(
            "date" to date,
            "appName" to app.name,
            "packageName" to app.packageName,
            "challengeType" to "one-week-challenge",
            "startTime" to System.currentTimeMillis(),
            "endTime" to System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000, // 7 days later
            "status" to "In Progress"
        )
        val documentId = "$date-${app.packageName}"

        firestoreDB.collection("userTracking")
            .document(uniqueID)
            .collection("challenges")
            .document(documentId)
            .set(challengeData)
            .addOnSuccessListener {
                Log.d("Firestore", "Challenge data successfully written!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing challenge data", e)
            }
    }

    private fun fetchChallenges() {
        firestoreDB = FirebaseFirestore.getInstance()
        uniqueID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        // Fetch the challenges from Firestore
        firestoreDB.collection("userTracking")
            .document(uniqueID)
            .collection("challenges")
            .get()
            .addOnSuccessListener { documents ->
                val updatedApps = documents.mapNotNull { document ->
                    val appName = document.getString("appName") ?: ""
                    val packageName = document.getString("packageName") ?: ""
                    val status = document.getString("status") ?: "Not started"
                    val startTime = document.getLong("startTime") ?: 0
                    App(appName, getAppIconResId(packageName), packageName, status, startTime)
                    // Find the app in allApps and update its status
                    allApps.find { it.packageName == packageName }?.let { app ->
                        app.status = status
                        app.startTime = startTime
                    }
                }
                appAdapter.updateApps(allApps) // Update the adapter with the merged list
            }
            .addOnFailureListener { exception ->
                Log.d("Firestore", "get failed with ", exception)
            }
    }

    private fun initializeSearchView() {
        searchView = findViewById(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                appAdapter.filter.filter(newText)
                return false
            }
        })
    }

    private fun initializeFab() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            // Fetch the challenge info from Firestore
            firestoreDB.collection("userTracking")
                .document(uniqueID)
                .collection("challenges")
                .document("$date-${selectedApp?.packageName}") // Use the selected app here
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        Log.d("Firestore", "DocumentSnapshot data: ${document.data}")
                        // Check the app usage stats
                        val startTime = document.getLong("startTime") ?: 0
                        val endTime = document.getLong("endTime") ?: System.currentTimeMillis()
                        val wasAppUsed = wasAppUsed(selectedApp?.packageName, startTime, endTime)
                        if (wasAppUsed) {
                            // If the app was used, show a message to the user
                            Toast.makeText(this, "You used the app!", Toast.LENGTH_SHORT).show()
                        } else {
                            // If the app was not used, show a different message to the user
                            Toast.makeText(this, "You didn't use the app!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.d("Firestore", "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("Firestore", "get failed with ", exception)
                }
        }
    }

    private fun initializeRecyclerView() {
        recyclerViewApps = findViewById(R.id.recyclerViewApps)
        recyclerViewApps.layoutManager = GridLayoutManager(this, 1)
        appAdapter = OneWeekAppAdapter(mutableListOf(), object : OneWeekAppAdapter.AppClickListener {
            override fun onAppClick(app: App) {
                selectedApp = app
                showStartChallengeDialog(app)
            }
        })
        recyclerViewApps.adapter = appAdapter
    }

    private fun showStartChallengeDialog(app: App) {
        AlertDialog.Builder(this)
            .setTitle("Start Challenge")
            .setMessage("Do you want to start the challenge with ${app.name}?")
            .setPositiveButton("Yes") { _, _ ->
                startChallenge(app)
            }
            .setNegativeButton("No", null)
            .show()
    }
    private fun getAppIconResId(packageName: String): Int {
        return when (packageName) {
            "com.facebook.katana" -> R.drawable.ic_facebook
            "com.instagram.android" -> R.drawable.ic_instagram
            "com.twitter.android" -> R.drawable.ic_twitter
            "com.snapchat.android" ->  R.drawable.ic_snapchat
            "com.pinterest" ->R.drawable.iconpinterest
            "com.whatsapp" -> R.drawable.ic_whatsapp
            "com.linkedin.android" -> R.drawable.ic_linkedin
            "com.google.android.youtube" -> R.drawable.ic_youtube
            "com.reddit.frontpage" -> R.drawable.ic_reddit
            "com.spotify.music" -> R.drawable.ic_spotify
            "com.zhiliaoapp.musically" -> R.drawable.ic_tiktok
            else -> R.drawable.ic_other
        }
    }
    // Handle the action of the home button
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
