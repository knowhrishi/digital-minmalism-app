package com.example.digitalminimalism

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesManager {
    private const val PREF_NAME = "YourPrefName"
    private const val TIMER_DOC_ID_KEY = "currentTimerDocId"
    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveTimerDocId(docId: String?) {
        sharedPreferences.edit().putString(TIMER_DOC_ID_KEY, docId).apply()
    }

    fun getTimerDocId(): String? {
        return sharedPreferences.getString(TIMER_DOC_ID_KEY, null)
    }
}
