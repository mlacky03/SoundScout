package com.nikolaM.soundscout.data.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date


data class UserProfile (
    val uid: String = "",
    val username: String = "",
    val name: String = "",
    val surname: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val points: Long= 0,

    val lastKnownLocation: GeoPoint? = null,
    @ServerTimestamp
    val lastSeen: Date? = null,
    val isOnline: Boolean = false
)