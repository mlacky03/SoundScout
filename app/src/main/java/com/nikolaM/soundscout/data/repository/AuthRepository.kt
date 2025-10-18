package com.nikolaM.soundscout.data.repository

import android.location.Location
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nikolaM.soundscout.data.model.UserProfile
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit

object AuthRepository {
    private fun unameToEmail(username: String) =
        "${username.trim().lowercase()}@soundscout.local"

//    suspend fun signUp(
//        username: String,
//        password: String,
//        name: String,
//        surname: String,
//        phone: String,
//        photoUrl: String?
//    ) {
//        val email = unameToEmail(username)
//        val cred = Firebase.auth.createUserWithEmailAndPassword(email, password).await()
//        val uid = cred.user?.uid ?: error("No UID")
//
//        val profile = UserProfile(
//            uid = uid,
//            username = username.trim(),
//            name = name.trim(),
//            surname = surname.trim(),
//            phone = phone.trim(),
//            photoUrl = photoUrl,
//            isOnline= true
//        )
//        Firebase.firestore.collection("users").document(uid).set(profile).await()
//    }




    suspend fun preSignUp(username: String, password: String): com.google.firebase.auth.AuthResult {
        val email = unameToEmail(username)
        return Firebase.auth.createUserWithEmailAndPassword(email, password).await()
    }


    suspend fun createProfile(
        uid: String,
        username: String,
        name: String,
        surname: String,
        phone: String,
        photoUrl: String?
    ) {
        val profile = UserProfile(
            uid = uid,
            username = username.trim(),
            name = name.trim(),
            surname = surname.trim(),
            phone = phone.trim(),
            photoUrl = photoUrl,
            isOnline = true
        )
        Firebase.firestore.collection("users").document(uid).set(profile).await()
    }

    suspend fun signIn(username: String, password: String) {
        Firebase.auth.signInWithEmailAndPassword(unameToEmail(username), password).await()
    }

    fun signOut() = Firebase.auth.signOut()
    fun currentUid(): String? = Firebase.auth.currentUser?.uid

    suspend fun getUserProfile(uid: String): UserProfile? {
        val doc = Firebase.firestore.collection("users").document(uid).get().await()
        return doc.toObject(UserProfile::class.java)
    }

    suspend fun incrementUserPoints(uid: String, amount: Long) {
        val userDocRef = Firebase.firestore.collection("users").document(uid)
        userDocRef.update("points", FieldValue.increment(amount)).await()
    }

    fun getLeaderboard(limit: Long = 20): Query {
        return Firebase.firestore.collection("users")
            .orderBy("points", Query.Direction.DESCENDING)
            .limit(limit)
    }

    suspend fun updateUserLocation(uid: String, location: Location) {
        val userDocRef = Firebase.firestore.collection("users").document(uid)
        val updates = mapOf(
            "lastKnownLocation" to GeoPoint(location.latitude, location.longitude),
            "lastSeen" to FieldValue.serverTimestamp() // Koristimo serversko vreme
        )
        userDocRef.update(updates).await()
    }

    fun getActiveUsers(): Query {
        return Firebase.firestore.collection("users")
            .whereEqualTo("isOnline", true)
    }

    fun setUserOnlineStatus(uid: String, isOnline: Boolean) {
        Firebase.firestore.collection("users").document(uid)
            .update("isOnline", isOnline)
    }

}