package com.nikolaM.soundscout.data.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date


enum class NoiseLevel {
        TIHO,UMERENO, BUCNO, VEOMA_BUCNO, VRLO_VISOKA_BUKA, EKSTRENMA_BUKA
}

enum class NoiseType {
    SAOBRACAJ, GRADNJA, MUZIKA, KOMSIJE, INDUSTRIJA, PRIRODA
}

data class NoiseReport(

    val id: String = "",
    val userId: String = "",
    @ServerTimestamp
    val timestamp: Date? = null,
    @ServerTimestamp
    val lastInteractionTimestamp: Date? = null,


    val username: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val noiseLevel: NoiseLevel = NoiseLevel.TIHO ,
    val noiseType: NoiseType= NoiseType.SAOBRACAJ,

    val likes: Long = 0,
    val dislikes: Long = 0,
    val likedBy: List<String> = emptyList(),
    val dislikedBy: List<String> = emptyList()
)