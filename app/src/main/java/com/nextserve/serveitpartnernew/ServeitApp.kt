package com.nextserve.serveitpartnernew

import android.app.Application
import com.google.firebase.FirebaseApp
import com.nextserve.serveitpartnernew.utils.LanguageManager

class ServeitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // Apply saved language before UI loads
        LanguageManager.applySavedLanguage(this)
    }
}

