package com.phoenixcorp.founderfinder.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.Match
import com.phoenixcorp.founderfinder.domain.model.MatchStatus
import com.phoenixcorp.founderfinder.domain.model.MatchType
import com.phoenixcorp.founderfinder.domain.repository.MatchRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MatchRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MatchRepository {

    private val matchesCollection = firestore.collection("matches")

    override suspend fun getRecommendedMatches(
        userId: String,
        type: MatchType,
        limit: Int
    ): List<Match> {
        return try {
            matchesCollection.whereEqualTo("userId1", userId)
                .whereEqualTo("matchType", type.name)
                .whereEqualTo("status", MatchStatus.PENDING.name)
                .limit(limit.toLong())
                .get().await()
                .toObjects(Match::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun createMatch(match: Match): Result<Unit> {
        return try {
            matchesCollection.document(match.id).set(match).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMatchStatus(matchId: String, status: MatchStatus) {
        try {
            matchesCollection.document(matchId)
                .update("status", status.name)
                .await()
        } catch (e: Exception) {}
    }
}