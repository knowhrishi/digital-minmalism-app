// FocusSession.kt
package com.example.digitalminimalism.Focus

data class FocusSessionDataClass(
    val timerSetUntil: Long = 0,
    val duration: Long = 0,
    val setAt: Long = 0,
    val status: String = "unknown",
    val startTime: Long = 0,
    val endTime: Long = 0,
    val timerType: String = "" // Add this line
)