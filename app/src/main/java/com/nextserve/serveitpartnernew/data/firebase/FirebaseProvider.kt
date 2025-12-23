package com.nextserve.serveitpartnernew.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseProvider {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { 
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = settings
        }
    }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
}

