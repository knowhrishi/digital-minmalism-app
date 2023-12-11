import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalminimalism.R
import java.util.Locale

class OneWeekAppAdapter(
    private val apps: List<OneWeekChallengeActivity.App>,
    private val clickListener: AppClickListener
) : RecyclerView.Adapter<OneWeekAppAdapter.AppViewHolder>(), Filterable {
    private var appsFiltered: List<OneWeekChallengeActivity.App> = apps

    interface AppClickListener {
        fun onAppClick(app: OneWeekChallengeActivity.App)
    }

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewAppIcon: ImageView = itemView.findViewById(R.id.icon)
        val textViewAppName: TextView = itemView.findViewById(R.id.app_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_oneweekchallenge, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.textViewAppName.text = app.name
        holder.imageViewAppIcon.setImageResource(app.icon)

        holder.itemView.setOnClickListener {
            clickListener.onAppClick(app)
        }
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
                        if (app.name.toLowerCase(Locale.ROOT).contains(charString.toLowerCase(Locale.ROOT))) {
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
}