package com.phoenixcorp.founderfinder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Shared ViewModel for the entire onboarding flow.
 *
 * Scoped to the nested NavGraph parent ("onboarding") so it survives
 * back/forward navigation without losing collected profile data.
 *
 * - Holds a full draft [UserProfile] in memory.
 * - Loads existing profile from Firestore on first creation.
 * - Each screen mutates the draft via update helpers.
 * - Screens call [saveDraft] (merge) on "Next" so data is durable if the app is killed.
 * - Final step calls [completeOnboarding].
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "OnboardingViewModel"

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        loadExistingProfile()
    }

    // ── Load ──────────────────────────────────────────────────────

    fun loadExistingProfile() {
        val uid = currentUserId ?: run {
            _errorMessage.value = "User not signed in"
            _isInitialized.value = true
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val doc = firestore.collection("profiles").document(uid).get().await()
                if (doc.exists()) {
                    val data = doc.data ?: emptyMap()
                    @Suppress("UNCHECKED_CAST")
                    _profile.value = UserProfile(
                        userId = uid,
                        email = auth.currentUser?.email,
                        firstName = data["firstName"] as? String,
                        lastName = data["lastName"] as? String,
                        birthDate = data["birthDate"] as? String,
                        educationEntries = (data["educationEntries"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        workExperiences = (data["workExperiences"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        isFounder = data["isFounder"] as? Boolean ?: false,
                        founderEntries = (data["founderEntries"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        ambitionStatement = data["ambitionStatement"] as? String,
                        role = data["role"] as? String ?: "FOUNDER",
                        expertise = data["expertise"] as? String,
                        experienceYears = (data["experienceYears"] as? Long)?.toInt()
                            ?: (data["experienceYears"] as? Int),
                        skills = (data["skills"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        industriesOfInterest = (data["industriesOfInterest"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        organizationsOfInterest = (data["organizationsOfInterest"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        linkedinUrl = data["linkedinUrl"] as? String,
                        twitterUrl = data["twitterUrl"] as? String,
                        facebookUrl = data["facebookUrl"] as? String,
                        instagramUrl = data["instagramUrl"] as? String,
                        websiteUrl = data["websiteUrl"] as? String,
                        profilePicture = data["profilePicture"] as? String,
                        onboardingComplete = data["onboardingComplete"] as? Boolean ?: false,
                        createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Long) ?: System.currentTimeMillis()
                    )
                    Log.d(TAG, "✅ Loaded existing profile for $uid")
                } else {
                    _profile.value = UserProfile(
                        userId = uid,
                        email = auth.currentUser?.email
                    )
                    Log.d(TAG, "No existing profile – starting fresh for $uid")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load profile", e)
                _errorMessage.value = "Failed to load profile: ${e.message}"
                _profile.value = UserProfile(userId = uid, email = auth.currentUser?.email)
            } finally {
                _isLoading.value = false
                _isInitialized.value = true
            }
        }
    }

    // ── Update helpers ────────────────────────────────────────────

    fun updateBasicInfo(firstName: String, lastName: String, birthDate: String) {
        _profile.update {
            it.copy(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                birthDate = birthDate.trim()
            )
        }
    }

    fun updateEducation(entries: List<String>) {
        _profile.update { it.copy(educationEntries = entries) }
    }

    fun updateWorkExperience(entries: List<String>) {
        _profile.update { it.copy(workExperiences = entries) }
    }

    fun updateFounderStatus(isFounder: Boolean, entries: List<String>) {
        _profile.update { it.copy(isFounder = isFounder, founderEntries = entries) }
    }

    fun updateAmbition(statement: String) {
        _profile.update { it.copy(ambitionStatement = statement.trim()) }
    }

    fun updateSocials(
        linkedin: String?,
        twitter: String?,
        facebook: String?,
        instagram: String?,
        website: String?
    ) {
        _profile.update {
            it.copy(
                linkedinUrl = linkedin?.trim()?.ifBlank { null },
                twitterUrl = twitter?.trim()?.ifBlank { null },
                facebookUrl = facebook?.trim()?.ifBlank { null },
                instagramUrl = instagram?.trim()?.ifBlank { null },
                websiteUrl = website?.trim()?.ifBlank { null }
            )
        }
    }

    fun updateIndustries(industries: List<String>) {
        _profile.update { it.copy(industriesOfInterest = industries) }
    }

    fun updateOrganizations(orgs: List<String>) {
        _profile.update { it.copy(organizationsOfInterest = orgs) }
    }

    fun updateRole(role: String) {
        _profile.update { it.copy(role = role) }
    }

    fun updateInvestorFields(
        expertise: String?,
        experienceYears: Int?,
        skills: List<String>
    ) {
        _profile.update {
            it.copy(
                expertise = expertise?.trim()?.ifBlank { null },
                experienceYears = experienceYears,
                skills = skills,
                role = "INVESTOR"
            )
        }
    }

    // ── Persist ───────────────────────────────────────────────────

    /** Merge-save current draft to Firestore. Call on every Next. */
    fun saveDraft(onComplete: (Boolean) -> Unit = {}) {
        val uid = currentUserId
        if (uid.isNullOrBlank()) {
            _errorMessage.value = "User not signed in"
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val p = _profile.value.copy(
                    userId = uid,
                    updatedAt = System.currentTimeMillis()
                )
                val data = buildMap<String, Any?> {
                    put("userId", p.userId)
                    put("email", p.email)
                    put("firstName", p.firstName)
                    put("lastName", p.lastName)
                    put("birthDate", p.birthDate)
                    put("educationEntries", p.educationEntries)
                    put("workExperiences", p.workExperiences)
                    put("isFounder", p.isFounder)
                    put("founderEntries", p.founderEntries)
                    put("ambitionStatement", p.ambitionStatement)
                    put("role", p.role)
                    put("expertise", p.expertise)
                    put("experienceYears", p.experienceYears)
                    put("skills", p.skills)
                    put("industriesOfInterest", p.industriesOfInterest)
                    put("organizationsOfInterest", p.organizationsOfInterest)
                    put("linkedinUrl", p.linkedinUrl)
                    put("twitterUrl", p.twitterUrl)
                    put("facebookUrl", p.facebookUrl)
                    put("instagramUrl", p.instagramUrl)
                    put("websiteUrl", p.websiteUrl)
                    put("profilePicture", p.profilePicture)
                    put("onboardingComplete", p.onboardingComplete)
                    put("updatedAt", p.updatedAt)
                    put("createdAt", if (p.createdAt > 0) p.createdAt else System.currentTimeMillis())
                }.filterValues { it != null }

                firestore.collection("profiles")
                    .document(uid)
                    .set(data, SetOptions.merge())
                    .await()

                _profile.value = p
                Log.d(TAG, "✅ Draft saved")
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "❌ saveDraft failed", e)
                _errorMessage.value = e.message ?: "Failed to save"
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Final save + mark onboarding complete. */
    fun completeOnboarding(onComplete: (Boolean) -> Unit) {
        val uid = currentUserId
        if (uid.isNullOrBlank()) {
            onComplete(false)
            return
        }
        _profile.update { it.copy(onboardingComplete = true) }
        saveDraft { success ->
            if (!success) {
                onComplete(false)
                return@saveDraft
            }
            viewModelScope.launch {
                try {
                    firestore.collection("profiles").document(uid)
                        .set(
                            mapOf(
                                "onboardingComplete" to true,
                                "updatedAt" to System.currentTimeMillis()
                            ),
                            SetOptions.merge()
                        ).await()
                    Log.d(TAG, "✅ Onboarding complete")
                    onComplete(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to mark complete", e)
                    onComplete(false)
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}