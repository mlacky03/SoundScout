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
    // Polja koja se automatski popunjavaju
    val id: String = "", // ID dokumenta iz Firestore-a
    val userId: String = "", // ID korisnika koji je kreirao
    @ServerTimestamp
    val timestamp: Date? = null, // Vreme kada je kreirano
    @ServerTimestamp
    val lastInteractionTimestamp: Date? = null,

    // Polja koja korisnik unosi
    val username: String = "", // Ime korisnika za prikaz
    val location: GeoPoint = GeoPoint(0.0, 0.0), // Lokacija
    val noiseLevel: NoiseLevel = NoiseLevel.TIHO ,
    val noiseType: NoiseType= NoiseType.SAOBRACAJ,

    val likes: Long = 0,
    val dislikes: Long = 0,
    val likedBy: List<String> = emptyList(),
    val dislikedBy: List<String> = emptyList()
)