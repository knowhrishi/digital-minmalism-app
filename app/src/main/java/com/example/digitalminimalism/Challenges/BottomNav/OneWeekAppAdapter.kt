import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalminimalism.Challenges.BottomNav.OneWeekChallengeActivity
import com.example.digitalminimalism.R
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.Locale

class OneWeekAppAdapter(
    var apps: MutableList<OneWeekChallengeActivity.App>, // Change to MutableList
    private val clickListener: AppClickListener
) : RecyclerView.Adapter<OneWeekAppAdapter.AppViewHolder>(), Filterable {

    private var appsFiltered: List<OneWeekChallengeActivity.App> = apps

    interface AppClickListener {
        fun onAppClick(app: OneWeekChallengeActivity.App)
    }

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewAppIcon: ImageView = itemView.findViewById(R.id.icon)
        val textViewAppName: TextView = itemView.findViewById(R.id.app_name)
        val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progressBar)
        val statusTextView: TextView = itemView.findViewById(R.id.statusTextView) // Add this line
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_oneweekchallenge, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.textViewAppName.text = app.name
        holder.imageViewAppIcon.setImageDrawable(
            getAppIcon(
                app.packageName,
                holder.itemView.context
            )
        )

        // Check the challenge status
        if (app.status == "In Progress") {
            holder.progressBar.visibility = View.VISIBLE
            holder.statusTextView.visibility = View.GONE

            // Calculate the number of days since the challenge started
            val currentTime = System.currentTimeMillis()
            val challengeStartTime = app.startTime
            val oneDayInMillis = 24 * 60 * 60 * 1000
            val daysSinceStart = ((currentTime - challengeStartTime) / oneDayInMillis).toInt()

            holder.progressBar.progress = daysSinceStart
        } else {
            holder.progressBar.visibility = View.GONE
            holder.statusTextView.visibility = View.VISIBLE
            holder.statusTextView.text = app.status
        }

        holder.itemView.setOnClickListener {
            clickListener.onAppClick(app)
        }
    }
    fun updateApps(newApps: MutableList<OneWeekChallengeActivity.App>) {
        this.apps = newApps
        this.appsFiltered = newApps
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int = apps.size
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                appsFiltered = if (charString.isEmpty()) {
                    apps
                } else {
                    val filteredList = ArrayList<OneWeekChallengeActivity.App>()
                    for (app in apps) {
                        if (app.name.toLowerCase(Locale.ROOT)
                                .contains(charString.toLowerCase(Locale.ROOT))
                        ) {
                            filteredList.add(app)
                        }
                    }
                    filteredList
                }
                val filterResults = FilterResults()
                filterResults.values = appsFiltered
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                appsFiltered = filterResults.values as List<OneWeekChallengeActivity.App>
                notifyDataSetChanged()
            }
        }
    }

    private fun getAppIcon(packageName: String, context: Context): Drawable? {
        return when (packageName) {
            "com.facebook.katana" -> ContextCompat.getDrawable(context, R.drawable.ic_facebook)
            "com.instagram.android" -> ContextCompat.getDrawable(context, R.drawable.ic_instagram)
            "com.twitter.android" -> ContextCompat.getDrawable(context, R.drawable.ic_twitter)
            "com.snapchat.android" -> ContextCompat.getDrawable(context, R.drawable.ic_snapchat)
            "com.pinterest" -> ContextCompat.getDrawable(context, R.drawable.iconpinterest)
            "com.whatsapp" -> ContextCompat.getDrawable(context, R.drawable.ic_whatsapp)
            "com.linkedin.android" -> ContextCompat.getDrawable(context, R.drawable.ic_linkedin)
            "com.google.android.youtube" -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_youtube
            )

            "com.reddit.frontpage" -> ContextCompat.getDrawable(context, R.drawable.ic_reddit)
            "com.spotify.music" -> ContextCompat.getDrawable(context, R.drawable.ic_spotify)
            "com.zhiliaoapp.musically" -> ContextCompat.getDrawable(context, R.drawable.ic_tiktok)
            else -> ContextCompat.getDrawable(context, R.drawable.ic_other)
        }
    }
}