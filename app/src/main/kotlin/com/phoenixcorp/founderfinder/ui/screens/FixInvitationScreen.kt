package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.data.Invitation
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun FixInvitationScreen() {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()
    var isFixing by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Fix Missing Invitation",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (resultMessage != null) {
            Text(
                text = resultMessage!!,
                style = MaterialTheme.typography.bodyMedium,
                color = if (resultMessage!!.startsWith("Success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                isFixing = true
                resultMessage = null
                coroutineScope.launch {
                    try {
                        val invitationData = Invitation(
                            invitationId = "e60c2861-3e17-4d60-b6e1-198d84308594",
                            inviteeId = "A8bC6h412UgNMhwp9wIIP4nvkzB2",
                            orgId = "b22a2c69-1878-470c-a992-b250d7224c48",
                            status = "pending",
                            type = "collaborator",
                            inviterId = "HZzSX2dxWNg7qS2PIeBz97tFjas1",
                            createdAt = System.currentTimeMillis()
                        )
                        firestore.collection("organizations")
                            .document("b22a2c69-1878-470c-a992-b250d7224c48")
                            .collection("invitations")
                            .document("A8bC6h412UgNMhwp9wIIP4nvkzB2")
                            .set(invitationData)
                            .await()
                        Log.d("FixInvitation", "Invitation added to organization subcollection")
                        resultMessage = "Success: Invitation added to organization"
                        Toast.makeText(context, "Invitation fixed successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("FixInvitation", "Error adding invitation: ${e.message}")
                        resultMessage = "Error: Failed to add invitation - ${e.message}"
                        Toast.makeText(context, "Failed to fix invitation: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isFixing = false
                    }
                }
            },
            enabled = !isFixing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isFixing) "Fixing..." else "Fix Invitation")
        }
    }
}