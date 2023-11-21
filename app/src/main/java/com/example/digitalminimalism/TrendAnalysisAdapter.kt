import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalminimalism.R
import com.example.digitalminimalism.UsageMonitoringFragment
import com.example.digitalminimalism.databinding.ItemAppUsageBinding

class TrendAnalysisAdapter(
    private var usages: List<UsageMonitoringFragment.AppUsage>,
    private val onAppSelected: (UsageMonitoringFragment.AppUsage) -> Unit
) : RecyclerView.Adapter<TrendAnalysisAdapter.UsageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsageViewHolder {
        val binding = ItemAppUsageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UsageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsageViewHolder, position: Int) {
        holder.bind(usages[position])
    }

    override fun getItemCount(): Int = usages.size

    inner class UsageViewHolder(private val binding: ItemAppUsageBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(appUsage: UsageMonitoringFragment.AppUsage) {
//            private val icon: ImageView = findViewByIdwById(R.id.icon)
            binding.appName.text = appUsage.appName
            binding.usageTime.text = "${appUsage.usageTime} min"
            // Set icon logic if required

            binding.root.setOnClickListener {
                onAppSelected(appUsage)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newUsages: List<UsageMonitoringFragment.AppUsage>) {
        usages = newUsages
        notifyDataSetChanged()
    }
}