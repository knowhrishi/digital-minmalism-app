package com.example.digitalminimalism.Challenges.BottomNav

import com.example.digitalminimalism.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChallengeAdapter(private val challenges: List<ChallengeHomeFragment.Challenge>, private val clickListener: ChallengeClickListener) : RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder>() {
    interface ChallengeClickListener {
        fun onChallengeClick(challenge: ChallengeHomeFragment.Challenge)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_challenge, parent, false)
        return ChallengeViewHolder(view)
    }
    class ChallengeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewChallengeIcon: ImageView = itemView.findViewById(R.id.imageViewChallengeIcon)
        val textViewChallengeName: TextView = itemView.findViewById(R.id.textViewChallengeName)

    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        val challenge = challenges[position]
        holder.textViewChallengeName.text = challenge.name
        holder.imageViewChallengeIcon.setImageResource(challenge.icon)
        holder.itemView.setOnClickListener {
            clickListener.onChallengeClick(challenge)
        }
    }
    override fun getItemCount(): Int = challenges.size
}