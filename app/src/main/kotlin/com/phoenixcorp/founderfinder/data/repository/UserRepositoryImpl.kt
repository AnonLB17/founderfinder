package com.phoenixcorp.founderfinder.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.User
import com.phoenixcorp.founderfinder.domain.model.UserRole
import com.phoenixcorp.founderfinder.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return getUserById(uid)
    }

    override suspend fun getUserById(uid: String): User? {
        return try {
            val document = usersCollection.document(uid).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchUsers(
        query: String,
        role: UserRole?,
        school: String?
    ): List<User> {
        return try {
            var querySnapshot = usersCollection
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")

            if (role != null) {
                querySnapshot = querySnapshot.whereEqualTo("role", role.name)
            }
            if (school != null) {
                querySnapshot = querySnapshot.whereEqualTo("school", school)
            }

            querySnapshot.get().await().toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getUsersBySchool(school: String): List<User> {
        return try {
            usersCollection.whereEqualTo("school", school)
                .get().await()
                .toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}