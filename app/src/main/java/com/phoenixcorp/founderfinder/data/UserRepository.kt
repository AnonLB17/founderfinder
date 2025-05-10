package com.phoenixcorp.founderfinder.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun registerUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onResult(false, "Email and password cannot be empty")
            return
        }

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                val user = hashMapOf("email" to email)

                db.collection("users").document(userId).set(user)
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { e -> onResult(false, e.message) }
            } else {
                onResult(false, task.exception?.message)
            }
        }
    }

    // Add sign-in method
    fun signInUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onResult(false, "Email and password cannot be empty")
            return
        }

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onResult(true, null)
            } else {
                onResult(false, task.exception?.message ?: "Authentication failed")
            }
        }
    }

    fun saveUserInfo(userId: String, firstName: String, lastName: String, birthDate: String, onResult: (Boolean) -> Unit) {
        val userInfo = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "birthDate" to birthDate
        )
        db.collection("users").document(userId).update(userInfo as Map<String, Any>)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun saveEducation(userId: String, education: List<String>, onResult: (Boolean) -> Unit) {
        db.collection("users").document(userId).update("educationEntries", education)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun saveWorkExperience(userId: String, workExperience: List<String>, onResult: (Boolean) -> Unit) {
        db.collection("users").document(userId).update("workExperiences", workExperience)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun saveFounderStatus(userId: String, founderStatus: List<String>, onResult: (Boolean) -> Unit) {
        db.collection("users").document(userId).update("founderEntries", founderStatus)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun saveAmbitionStatement(userId: String, ambitionStatement: String, onResult: (Boolean) -> Unit) {
        db.collection("users").document(userId).update("ambitionStatement", ambitionStatement)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun saveSocials(userId: String, linkedin: String, twitter: String, facebook: String, instagram: String, website: String, onResult: (Boolean) -> Unit) {
        val socials = hashMapOf(
            "linkedin" to linkedin,
            "twitter" to twitter,
            "facebook" to facebook,
            "instagram" to instagram,
            "website" to website
        )
        db.collection("users").document(userId).update(socials as Map<String, Any>)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun saveIndustriesOfInterest(userId: String, industries: List<String>, onResult: (Boolean) -> Unit) {
        db.collection("users").document(userId).update("industries", industries)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun saveOrganizationsOfInterest(userId: String, organizations: List<String>, onResult: (Boolean) -> Unit) {
        db.collection("users").document(userId).update("organizations", organizations)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun saveProfileImageUri(userId: String, profileImageUri: String, onResult: (Boolean) -> Unit) {
        db.collection("users").document(userId).update("profileImageUri", profileImageUri)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}