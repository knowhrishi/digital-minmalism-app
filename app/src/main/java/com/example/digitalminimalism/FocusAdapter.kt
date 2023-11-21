import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalminimalism.FocusFragment
import com.example.digitalminimalism.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class FocusAdapter(private val context: Context, private var focusSessions: List<FocusFragment.FocusSession>) :
    RecyclerView.Adapter<FocusAdapter.FocusViewHolder>() {

    class FocusViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val focusDateTextView: TextView = view.findViewById(R.id.focus_date_text_view)
        val focusTimeTextView: TextView = view.findViewById(R.id.focus_time_text_view)
        val focusStatusTextView: TextView = view.findViewById(R.id.focus_status_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FocusViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.focus_history_item, parent, false)
        return FocusViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: FocusViewHolder, position: Int) {
        val focusSession = focusSessions[position]
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        holder.focusDateTextView.text = dateFormat.format(Date(focusSession.startTime))
        holder.focusTimeTextView.text = "${timeFormat.format(Date(focusSession.startTime))} - ${timeFormat.format(Date(focusSession.startTime + focusSession.duration * 60 * 1000))}"
        holder.focusStatusTextView.text = focusSession.status

        when (focusSession.status) {
            "completed" -> holder.focusStatusTextView.setBackgroundResource(R.drawable.green_circular_background)
            "incomplete" -> holder.focusStatusTextView.setBackgroundResource(R.drawable.red_circular_background)
            else -> holder.focusStatusTextView.setBackgroundResource(R.drawable.yellow_circular_background)
        }
    }

    override fun getItemCount(): Int = focusSessions.size

    fun updateFocusSessions(newSessions: List<FocusFragment.FocusSession>) {
        focusSessions = newSessions
        notifyDataSetChanged()
    }
}
