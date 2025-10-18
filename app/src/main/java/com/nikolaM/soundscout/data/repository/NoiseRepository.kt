package com.nikolaM.soundscout.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nikolaM.soundscout.data.model.NoiseReport
import com.nikolaM.soundscout.ui.filter.DateFilterType
import com.nikolaM.soundscout.ui.filter.ReportFilters
import kotlinx.coroutines.tasks.await

object NoiseRepository {

    private val noiseReportsCollection = Firebase.firestore.collection("noise_reports")

    // Funkcija za dodavanje novog izveštaja o buci
    suspend fun addNoiseReport(report: NoiseReport) {
        noiseReportsCollection.add(report).await()
    }

    // Funkcija koja će nam u realnom vremenu davati SVE izveštaje iz baze
    // Koristićemo je da prikažemo markere na mapi
    fun getFilteredNoiseReports(filters: ReportFilters = ReportFilters()): Query {
        var query: Query = noiseReportsCollection

        // Dodajemo filtere jedan po jedan, ako su postavljeni
        if (filters.authorId != null) {
            query = query.whereEqualTo("userId", filters.authorId)
        }
        if (filters.noiseTypes.isNotEmpty()) {
            // Koristimo 'whereIn' koji proverava da li je vrednost polja
            // JEDNA OD vrednosti u našoj listi.
            // Moramo da mapiramo Enum u String, jer su tako sačuvani u bazi.
            query = query.whereIn("noiseType", filters.noiseTypes.map { it.name })
        }
        if (filters.noiseLevel != null) {
            query = query.whereEqualTo("noiseLevel", filters.noiseLevel.name)
        }
        val dateField = if (filters.dateFilterType == DateFilterType.INTERACTION) {
            "lastInteractionTimestamp"
        } else {
            "timestamp"
        }

        if (filters.startDate != null) {
            query = query.whereGreaterThanOrEqualTo(dateField, filters.startDate)
        }
        if (filters.endDate != null) {
            query = query.whereLessThanOrEqualTo(dateField, filters.endDate)
        }
            // Na kraju, sortiraj po datumu da najnoviji budu prvi
            return query.orderBy(dateField, Query.Direction.DESCENDING)
        }
        // Opciono: .orderBy("timestamp", Query.Direction.DESCENDING) // Da budu sortirani
        // Opciono: .limit(100) // Da ne učitavamo previše podataka

        suspend fun voteOnReport(
            reportId: String,
            creatorId: String,
            voterId: String,
            isLike: Boolean
        ) {
            val db = Firebase.firestore
            val reportRef = db.collection("noise_reports").document(reportId)
            val creatorRef = db.collection("users").document(creatorId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(reportRef)
                val report = snapshot.toObject(NoiseReport::class.java) ?: return@runTransaction


                // Proveri da li je korisnik već glasao
                if (report.likedBy.contains(voterId) || report.dislikedBy.contains(voterId)) {
                    // Korisnik je već glasao, ne radi ništa.
                    // (Kasnije se može dodati logika za promenu glasa)
                    return@runTransaction
                }

                transaction.update(
                    reportRef,
                    "lastInteractionTimestamp",
                    FieldValue.serverTimestamp()
                )
                val pointsChange = if (isLike) 3L else -3L

                // 1. Ažuriraj poene kreatoru
                transaction.update(creatorRef, "points", FieldValue.increment(pointsChange))

                // 2. Ažuriraj izveštaj o buci
                if (isLike) {
                    transaction.update(reportRef, "likes", FieldValue.increment(1))
                    transaction.update(reportRef, "likedBy", FieldValue.arrayUnion(voterId))
                } else {
                    transaction.update(reportRef, "dislikes", FieldValue.increment(1))
                    transaction.update(reportRef, "dislikedBy", FieldValue.arrayUnion(voterId))
                }
            }.await()
        }
    }

