package com.phoenixcorp.founderfinder.ui.components

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.data.Invitation
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Composable
fun CreateInvitation(
    orgId: String,
    modifier: Modifier = Modifier,
    onSuccess: (String) -> Unit = {}, // Returns invitation ID
    onError: (String) -> Unit = {}
) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var inviteeUid by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("collaborator") }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val types = listOf("collaborator", "advisor", "partner")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Send Organization Invitation",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = inviteeUid,
            onValueChange = { inviteeUid = it },
            label = { Text("Invitee User ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errorMessage != null,
            supportingText = {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            }
        )

        Text("Select Role", style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            types.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Button(
            onClick = {
                if (currentUser == null) {
                    errorMessage = "Please sign in to send invitations"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    onError(errorMessage!!)
                    return@Button
                }
                if (inviteeUid.isBlank()) {
                    errorMessage = "Please enter a valid user ID"
                    return@Button
                }
                if (inviteeUid == currentUser.uid) {
                    errorMessage = "Cannot invite yourself"
                    return@Button
                }
                isSending = true
                errorMessage = null
                coroutineScope.launch {
                    try {
                        val invitationId = UUID.randomUUID().toString()
                        val invitation = Invitation(
                            invitationId = invitationId,
                            inviteeId = inviteeUid,
                            orgId = orgId,
                            status = "pending",
                            type = selectedType,
                            inviterId = currentUser.uid,
                            createdAt = System.currentTimeMillis()
                        )
                        // Save to /invitations
                        firestore.collection("invitations")
                            .document(invitationId)
                            .set(invitation)
                            .await()
                        // Save to /organizations/{orgId}/invitations
                        firestore.collection("organizations")
                            .document(orgId)
                            .collection("invitations")
                            .document(inviteeUid)
                            .set(invitation)
                            .await()
                        Log.d("CreateInvitation", "Invitation created: $invitationId")
                        Toast.makeText(context, "Invitation sent successfully", Toast.LENGTH_SHORT).show()
                        isSending = false
                        inviteeUid = ""
                        selectedType = "collaborator"
                        onSuccess(invitationId)
                    } catch (e: Exception) {
                        Log.e("CreateInvitation", "Error creating invitation: ${e.message}")
                        errorMessage = "Failed to send invitation: ${e.message}"
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        isSending = false
                        onError(errorMessage!!)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSending
        ) {
            Text(if (isSending) "Sending..." else "Send Invitation")
        }
    }
}