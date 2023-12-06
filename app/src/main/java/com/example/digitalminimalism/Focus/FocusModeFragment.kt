import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.digitalminimalism.Focus.BottomNavigation.Active.ActiveNavFragment
import com.example.digitalminimalism.Focus.BottomNavigation.History.HistoryNavFragment
import com.example.digitalminimalism.Focus.BottomNavigation.HomeNavFragment
import com.example.digitalminimalism.Focus.BottomNavigation.Schedule.ScheduleNavFragment
import com.example.digitalminimalism.Focus.BottomNavigation.Timer.TimerNavFragment
import com.example.digitalminimalism.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class FocusModeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_focus_mode, container, false)

        val bottomNavView: BottomNavigationView = view.findViewById(R.id.focus_bottom_nav)
        bottomNavView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_active_focus -> loadSubFragment(ActiveNavFragment())
                R.id.nav_home_focus -> loadSubFragment(HomeNavFragment())
                R.id.nav_schedule_focus -> loadSubFragment(ScheduleNavFragment())
                R.id.nav_timer_focus -> loadSubFragment(TimerNavFragment())
                R.id.nav_history_focus -> loadSubFragment(HistoryNavFragment())
            }
            true
        }

        // Load the default fragment on creation
        if (savedInstanceState == null) {
            loadSubFragment(ActiveNavFragment())
        }

        return view
    }

    private fun loadSubFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.focus_fragment_container, fragment)
            .commit()
    }
}
