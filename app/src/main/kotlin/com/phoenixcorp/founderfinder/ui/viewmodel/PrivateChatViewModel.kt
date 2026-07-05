package com.phoenixcorp.founderfinder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.ChatMessage
import com.phoenixcorp.founderfinder.domain.model.Organization
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.domain.usecase.SendChatMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PrivateChatViewModel @Inject constructor(
    val sendChatMessageUseCase: SendChatMessageUseCase   // Exposed for legacy parts if needed
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _recipientName = MutableStateFlow("Loading...")
    val recipientName = _recipientName.asStateFlow()

    private val _recipientProfilePicture = MutableStateFlow<String?>(null)
    val recipientProfilePicture = _recipientProfilePicture.asStateFlow()

    private val _recipientId = MutableStateFlow<String?>(null)
    val recipientId = _recipientId.asStateFlow()

    private val _recipientType = MutableStateFlow<String?>(null)
    val recipientType = _recipientType.asStateFlow()

    private val _organizations = MutableStateFlow<List<Organization>>(emptyList())
    val organizations = _organizations.asStateFlow()

    val currentUser = auth.currentUser

    /**
     * Load all necessary data for the chat screen
     */
    fun loadChatData(conversationId: String) {
        viewModelScope.launch {
            val userId = currentUser?.uid ?: return@launch
            val recipient = extractRecipientId(conversationId, userId)

            _recipientId.value = recipient
            loadRecipientProfile(recipient)
            loadUserOrganizations()
        }
    }

    private fun extractRecipientId(conversationId: String, currentUserId: String): String {
        val parts = conversationId.split("_")
        return parts.firstOrNull { it != currentUserId } ?: conversationId
    }

    private suspend fun loadRecipientProfile(recipientId: String) {
        if (recipientId.isEmpty()) return

        try {
            val profileDoc = firestore.collection("profiles")
                .document(recipientId)
                .get()
                .await()

            val profile = profileDoc.toObject(UserProfile::class.java)

            _recipientName.value = if (profile != null) {
                "${profile.firstName ?: "Unknown"} ${profile.lastName ?: "User"}".trim()
            } else {
                "Unknown User"
            }

            _recipientProfilePicture.value = profile?.profilePicture

            // Determine recipient type (partner / advisor)
            val partnerDoc = firestore.collection("profiles")
                .document(recipientId)
                .collection("partner")
                .document("data")
                .get()
                .await()

            val advisorDoc = firestore.collection("profiles")
                .document(recipientId)
                .collection("advisor")
                .document("data")
                .get()
                .await()

            _recipientType.value = when {
                partnerDoc.exists() -> "partner"
                advisorDoc.exists() -> "advisor"
                else -> null
            }

            Log.d("PrivateChatViewModel", "Loaded profile for $recipientId: ${_recipientName.value}")

        } catch (e: Exception) {
            Log.e("PrivateChatViewModel", "Failed to load recipient profile", e)
            _recipientName.value = "Unknown User"
        }
    }

    private suspend fun loadUserOrganizations() {
        val userId = currentUser?.uid ?: return

        try {
            val snapshot = firestore.collection("organizations")
                .whereEqualTo("creatorId", userId)
                .get()
                .await()

            _organizations.value = snapshot.documents.mapNotNull { doc ->
                try {
                    Organization(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        imageUri = doc.getString("imageUri"),
                        creatorId = userId
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("PrivateChatViewModel", "Failed to load organizations", e)
            _organizations.value = emptyList()
        }
    }

    /**
     * Send organization as a special message + create invitation
     */
    fun sendOrganization(
        conversationId: String,
        orgId: String,
        recipientType: String
    ) {
        val senderId = currentUser?.uid ?: return
        val recipient = _recipientId.value ?: return

        viewModelScope.launch {
            try {
                val invitationId = UUID.randomUUID().toString()

                // 1. Create Invitation
                val invitation = mapOf(
                    "invitationId" to invitationId,
                    "orgId" to orgId,
                    "inviterId" to senderId,
                    "inviteeId" to recipient,
                    "status" to "pending",
                    "type" to recipientType,
                    "createdAt" to System.currentTimeMillis()
                )

                firestore.collection("invitations")
                    .document(invitationId)
                    .set(invitation)
                    .await()

                // 2. Add to organization invitations
                firestore.collection("organizations")
                    .document(orgId)
                    .collection("invitations")
                    .document(recipient)
                    .set(invitation)
                    .await()

                // 3. Send organization message via proper UseCase path
                val orgMessage = ChatMessage(
                    id = invitationId,
                    chatId = conversationId,
                    senderId = senderId,
                    recipientId = recipient,
                    text = "Shared an organization",
                    timestamp = System.currentTimeMillis(),
                    type = "organization",
                    orgId = orgId
                )

                val result = sendChatMessageUseCase(orgMessage)

                if (result.isSuccess) {
                    Log.d("PrivateChatViewModel", "Organization shared successfully")
                } else {
                    Log.e("PrivateChatViewModel", "Failed to send org message", result.exceptionOrNull())
                }

            } catch (e: Exception) {
                Log.e("PrivateChatViewModel", "Error sending organization", e)
            }
        }
    }
}