package com.example.digitalminimalism.Challenges.BottomNav




import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalminimalism.R
class ChallengeHomeFragment : Fragment(), ChallengeAdapter.ChallengeClickListener {

    private lateinit var recyclerViewChallenges: RecyclerView
    private lateinit var challengeAdapter: ChallengeAdapter
    private lateinit var challenges: List<Challenge> // Your data source
    data class Challenge(val name: String, val icon: Int)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_challenge_home, container, false)

        recyclerViewChallenges = view.findViewById(R.id.recyclerViewChallenges)
        val numberOfColumns = 2 // You can calculate this based on screen size if you like
        recyclerViewChallenges.layoutManager = GridLayoutManager(context, numberOfColumns)

        // Initialize your data source here
        challenges = listOf(
            Challenge("1 Week Challenge", R.drawable.ic_minusweek),
            Challenge("Challenge 2", R.drawable.ic_nav_challenge),
        )
        challengeAdapter = ChallengeAdapter(challenges, this)
        recyclerViewChallenges.adapter = challengeAdapter

        return view
    }
    override fun onChallengeClick(challenge: Challenge) {
        val intent = when (challenge.name) {
            "1 Week Challenge" -> Intent().setClassName(requireContext(), "com.example.digitalminimalism.Challenges.BottomNav.OneWeekChallengeActivity")
            // "Challenge 2" -> Intent().setClassName(requireContext(), "com.example.digitalminimalism.Challenges.BottomNav.ActivityForChallenge2")
            else -> null
        }
        intent?.let {
            it.putExtra("challengeName", challenge.name)
            startActivity(it)
        }
    }

}
