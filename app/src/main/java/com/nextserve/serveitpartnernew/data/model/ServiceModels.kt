package com.nextserve.serveitpartnernew.data.model

import com.google.firebase.firestore.PropertyName

data class MainServiceModel(
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true
)

data class SubServiceModel(
    val name: String = "",
    val description: String = "",
    val unit: String = ""
)

