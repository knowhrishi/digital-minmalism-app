// OneWeekChallengeActivity.kt
import android.os.Bundle
import android.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalminimalism.R
import com.example.digitalminimalism.UsageStatsHelper
import java.util.Calendar

public class OneWeekChallengeActivity : AppCompatActivity() {

    private lateinit var recyclerViewApps: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var appAdapter: OneWeekAppAdapter
    private lateinit var apps: List<App> // Your data source

    data class App(val name: String, val icon: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_one_week_challenge)

        recyclerViewApps = findViewById(R.id.recyclerViewApps)
        searchView = findViewById(R.id.searchView)
        val numberOfColumns = 2 // You can calculate this based on screen size if you like
        recyclerViewApps.layoutManager = GridLayoutManager(this, numberOfColumns)

        // Initialize your data source here
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStats = UsageStatsHelper.getUsageStatistics(this, startTime, endTime)
        apps = usageStats.map { App(it.appName, R.drawable.ic_nav_challenge) } // Replace R.drawable.ic_app_1 with the actual app icon

        appAdapter = OneWeekAppAdapter(apps, object : OneWeekAppAdapter.AppClickListener {
            override fun onAppClick(app: App) {
                AlertDialog.Builder(this@OneWeekChallengeActivity)
                    .setTitle("Start Challenge")
                    .setMessage("Do you want to start the challenge with ${app.name}?")
                    .setPositiveButton("Yes") { _, _ ->
                        // Start the challenge with the selected app
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        })
        recyclerViewApps.adapter = appAdapter

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
}