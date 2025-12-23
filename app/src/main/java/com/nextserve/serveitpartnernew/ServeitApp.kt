package com.nextserve.serveitpartnernew

import android.app.Application
import com.google.firebase.FirebaseApp

class ServeitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}

