package com.example.digitalminimalism.Challenges

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.digitalminimalism.Challenges.BottomNav.ChallengeHomeFragment
import com.example.digitalminimalism.Focus.BottomNavigation.Active.ActiveNavFragment
import com.example.digitalminimalism.Focus.BottomNavigation.History.HistoryNavFragment
import com.example.digitalminimalism.Focus.BottomNavigation.HomeNavFragment
import com.example.digitalminimalism.Focus.BottomNavigation.Schedule.ScheduleNavFragment
import com.example.digitalminimalism.Focus.BottomNavigation.Timer.TimerNavFragment
import com.example.digitalminimalism.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class ChallengeFragment : Fragment() {

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_challenge, container, false)

        val bottomNavView: BottomNavigationView = view.findViewById(R.id.challenge_bottom_nav)
        bottomNavView.setItemIconTintList(null);
        bottomNavView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_active_focus -> loadSubFragment(ChallengeHomeFragment())
//                R.id.nav_home_focus -> loadSubFragment(HomeNavFragment())
//                R.id.nav_schedule_focus -> loadSubFragment(ScheduleNavFragment())
            }
            true
        }

        // Load the default fragment on creation
        if (savedInstanceState == null) {
            loadSubFragment(ChallengeHomeFragment())
        }

        return view
    }

private fun loadSubFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.challenge_fragment_container, fragment)
            .commit()
    }
}
