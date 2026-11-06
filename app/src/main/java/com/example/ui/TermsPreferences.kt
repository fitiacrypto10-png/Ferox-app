package com.example.ui

import android.content.Context
import android.content.SharedPreferences

object TermsPreferences {
    private const val PREFS_NAME = "ferox_prefs"
    private const val KEY_TERMS_ACCEPTED = "is_terms_accepted"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isTermsAccepted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TERMS_ACCEPTED, false)
    }

    fun setTermsAccepted(context: Context, accepted: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_TERMS_ACCEPTED, accepted).apply()
    }
}
