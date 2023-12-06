package com.example.digitalminimalism.Focus.BottomNavigation.Timer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.digitalminimalism.R

class TimerNavFragment : Fragment() {

    private var selectedTime: Int = 0

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timer_nav, container, false)

        val buttonTenMinutesTv: TextView = view.findViewById(R.id.button_ten_minutes)
        val buttonFifteenMinutesTv: TextView = view.findViewById(R.id.button_fifteen_minutes)
        val buttontwentyfiveMinutesTv: TextView = view.findViewById(R.id.button_25_minutes)
        val buttonfourtynMinutesTv: TextView = view.findViewById(R.id.button_fourty_minutes)
        val setOwnTimeTv: TextView = view.findViewById(R.id.set_own_time)
        val buttonBegin: Button = view.findViewById(R.id.button_begin)
        val LinearLayoutStudy: LinearLayout = view.findViewById(R.id.LinearLayout_study)
        val LinearLayoutWork: LinearLayout = view.findViewById(R.id.LinearLayoutWork)
        val LinearLayoutExercise: LinearLayout = view.findViewById(R.id.LinearLayoutExercise)
        val LinearLayoutRelax: LinearLayout = view.findViewById(R.id.LinearLayoutRelax)
        val LinearLayoutOther: LinearLayout = view.findViewById(R.id.LinearLayoutOther)

        val linearLayouts = listOf(
            LinearLayoutStudy,
            LinearLayoutWork,
            LinearLayoutExercise,
            LinearLayoutRelax,
            LinearLayoutOther
        )

        var selectedLinearLayout: LinearLayout? = null

        for (linearLayout in linearLayouts) {
            linearLayout.setOnClickListener {
                if (selectedLinearLayout != it) {
                    selectedLinearLayout?.setBackgroundResource(R.drawable.cardview_background_default)
                    it.setBackgroundResource(R.drawable.cardview_background_pressed)
                    selectedLinearLayout = it as LinearLayout
                }

                when (it.id) {
                    R.id.LinearLayout_study -> Toast.makeText(
                        context,
                        "Study LinearLayout clicked",
                        Toast.LENGTH_SHORT
                    ).show()

                    R.id.LinearLayoutWork -> Toast.makeText(
                        context,
                        "Work LinearLayout clicked",
                        Toast.LENGTH_SHORT
                    ).show()

                    R.id.LinearLayoutExercise -> Toast.makeText(
                        context,
                        "Exercise LinearLayout clicked",
                        Toast.LENGTH_SHORT
                    ).show()

                    R.id.LinearLayoutRelax -> Toast.makeText(
                        context,
                        "Relax LinearLayout clicked",
                        Toast.LENGTH_SHORT
                    ).show()

                    R.id.LinearLayoutOther -> Toast.makeText(
                        context,
                        "Other LinearLayout clicked",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val buttonPrepare: TextView = view.findViewById(R.id.button_prepare)
        val buttonStartNow: TextView = view.findViewById(R.id.button_start_now)

        val buttons = listOf(buttonPrepare, buttonStartNow)

        var selectedButton: TextView? = null

        for (button in buttons) {
            button.setOnClickListener {
                if (selectedButton != it) {
                    selectedButton?.setBackgroundResource(R.drawable.circle_background_notselected)
                    selectedButton?.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.black
                        )
                    ) // Set the text color for the unselected state
                    it.setBackgroundResource(R.drawable.circle_background_selected)
                    (it as TextView).setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.white
                        )
                    ) // Set the text color for the selected state
                    selectedButton = it
                }
                when (it.id) {

                    R.id.button_prepare -> Toast.makeText(
                        context,
                        "Prepare button clicked",
                        Toast.LENGTH_SHORT
                    ).show()

                    R.id.button_start_now -> Toast.makeText(
                        context,
                        "Start Now button clicked",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        val timeSelectionTextViews = listOf(
            buttonTenMinutesTv,
            buttonFifteenMinutesTv,
            buttontwentyfiveMinutesTv,
            buttonfourtynMinutesTv
        )

        fun resetTimeSelectionBackgrounds() {
            for (textView in timeSelectionTextViews) {
                textView.setBackgroundResource(R.drawable.circle_background_notselected)
                (textView as? TextView)?.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.black
                    )
                ) // Set the text color for the selected state
            }
        }
        buttonTenMinutesTv.setOnClickListener {
            resetTimeSelectionBackgrounds()
            it.setBackgroundResource(R.drawable.circle_background_selected)
            (it as? TextView)?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            ) // Set the text color for the selected state
            selectedTime = 10
            Toast.makeText(context, "10 minutes selected", Toast.LENGTH_SHORT).show()
        }

        buttonFifteenMinutesTv.setOnClickListener {
            resetTimeSelectionBackgrounds()
            it.setBackgroundResource(R.drawable.circle_background_selected)
            (it as? TextView)?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            ) // Set the text color for the selected state
            selectedTime = 15
            Toast.makeText(context, "15 minutes selected", Toast.LENGTH_SHORT).show()
        }

        buttontwentyfiveMinutesTv.setOnClickListener {
            resetTimeSelectionBackgrounds()
            it.setBackgroundResource(R.drawable.circle_background_selected)
            (it as? TextView)?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            ) // Set the text color for the selected state
            selectedTime = 25
            Toast.makeText(context, "25 minutes selected", Toast.LENGTH_SHORT).show()
        }

        buttonfourtynMinutesTv.setOnClickListener {
            resetTimeSelectionBackgrounds()
            it.setBackgroundResource(R.drawable.circle_background_selected)
            (it as? TextView)?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            ) // Set the text color for the selected state
            selectedTime = 40
            Toast.makeText(context, "40 minutes selected", Toast.LENGTH_SHORT).show()
        }

        setOwnTimeTv.setOnClickListener {
            selectedTime = 15
        }

        return view
    }

    private fun startTimer(minutes: Int) {
        // Implement your timer logic here
    }
}