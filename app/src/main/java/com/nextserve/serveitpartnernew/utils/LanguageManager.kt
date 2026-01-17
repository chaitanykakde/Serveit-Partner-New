package com.nextserve.serveitpartnernew.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {
    private const val PREFS_NAME = "serveit_partner_prefs"
    private const val KEY_LANGUAGE = "app_language"
    private const val DEFAULT_LANGUAGE = "en"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSavedLanguage(context: Context): String {
        return getSharedPreferences(context).getString(KEY_LANGUAGE, "") ?: ""
    }
    
    /**
     * Check if a language has been explicitly selected by the user.
     * Returns true if language exists in local storage (even if it's "en").
     */
    fun isLanguageSelected(context: Context): Boolean {
        return getSharedPreferences(context).contains(KEY_LANGUAGE)
    }

    fun saveLanguage(context: Context, languageCode: String) {
        getSharedPreferences(context).edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    fun applyLanguage(context: Context, languageCode: String) {
        saveLanguage(context, languageCode)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            try {
                val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                localeManager?.applicationLocales = android.os.LocaleList.forLanguageTags(languageCode)
            } catch (e: Exception) {
                // Fallback to AppCompat
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(languageCode)
                )
            }
        } else {
            // Android 12 and below
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageCode)
            )
        }
    }

    fun applySavedLanguage(context: Context) {
        val savedLanguage = getSavedLanguage(context)
        applyLanguage(context, savedLanguage)
    }

    fun getLanguageDisplayName(languageCode: String, context: Context): String {
        return when (languageCode) {
            "en" -> context.getString(com.nextserve.serveitpartnernew.R.string.language_english)
            "hi" -> context.getString(com.nextserve.serveitpartnernew.R.string.language_hindi)
            "mr" -> context.getString(com.nextserve.serveitpartnernew.R.string.language_marathi)
            else -> context.getString(com.nextserve.serveitpartnernew.R.string.language_english)
        }
    }

    fun getAvailableLanguages(): List<Pair<String, String>> {
        return listOf(
            "en" to "English",
            "hi" to "हिंदी",
            "mr" to "मराठी"
        )
    }
}

