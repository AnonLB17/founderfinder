package com.phoenixcorp.founderfinder.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.ForumPost
import com.phoenixcorp.founderfinder.domain.model.ForumReply
import com.phoenixcorp.founderfinder.domain.repository.ForumRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ForumRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ForumRepository {

    private val postsCollection = firestore.collection("forum_posts")

    override suspend fun createPost(post: ForumPost): Result<String> {
        return try {
            val docRef = postsCollection.document(post.id)
            docRef.set(post).await()
            Result.success(post.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPostsByCategory(category: String): List<ForumPost> {
        return try {
            postsCollection.whereEqualTo("category", category)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
                .toObjects(ForumPost::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getPostsBySchool(school: String): List<ForumPost> {
        return try {
            postsCollection.whereEqualTo("school", school)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
                .toObjects(ForumPost::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getPostReplies(postId: String): Flow<List<ForumReply>> = callbackFlow {
        val listener = postsCollection.document(postId)
            .collection("replies")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val replies = snapshot?.toObjects(ForumReply::class.java) ?: emptyList()
                trySend(replies)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun likePost(postId: String) {
        try {
            postsCollection.document(postId)
                .update("likes", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
        } catch (e: Exception) {}
    }

    override suspend fun createReply(reply: ForumReply): Result<Unit> {
        return try {
            postsCollection.document(reply.postId)
                .collection("replies")
                .document(reply.id)
                .set(reply)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}