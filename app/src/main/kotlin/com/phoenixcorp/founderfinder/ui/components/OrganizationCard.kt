package com.phoenixcorp.founderfinder.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.Organization
import com.phoenixcorp.founderfinder.navigation.Screen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

@Composable
fun OrganizationCard(
    organization: Organization? = null,
    orgId: String? = null,
    invitationId: String = "",
    navController: NavHostController,
    onSwipe: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val firestore = FirebaseFirestore.getInstance()
    var fetchedOrganization by remember { mutableStateOf<Organization?>(null) }
    var isLoading by remember { mutableStateOf(organization == null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Fetch organization data if not provided
    LaunchedEffect(orgId) {
        if (organization != null) {
            isLoading = false
            return@LaunchedEffect
        }
        if (orgId != null) {
            try {
                val doc = firestore.collection("organizations")
                    .document(orgId)
                    .get()
                    .await()
                fetchedOrganization = doc.toObject(Organization::class.java)?.copy(id = doc.id)
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load organization: ${e.message}"
                isLoading = false
            }
        }
    }

    val currentOrg = organization ?: fetchedOrganization

    Card(
        modifier = modifier
            .then(if (onSwipe != null) Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f).padding(16.dp).clip(RoundedCornerShape(12.dp)) else Modifier.fillMaxWidth().padding(8.dp))
            .then(if (onSwipe != null) Modifier.pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (abs(dragAmount) > 50) {
                        Log.d("OrganizationCard", "Swiped organization: ${currentOrg?.name}")
                        onSwipe?.invoke()
                    }
                }
            } else Modifier)
            .clickable(enabled = onSwipe == null) {
                val id = currentOrg?.id ?: orgId ?: return@clickable
                navController.navigate("organization_details/$id/$invitationId")
            },
        elevation = CardDefaults.cardElevation(defaultElevation = if (onSwipe != null) 8.dp else 4.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            )
        } else if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        } else if (currentOrg != null) {
            if (onSwipe != null) {
                // Full details view for swipe mode
                LazyColumn(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("organization_details/${currentOrg.id}/")
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            currentOrg.imageUri?.takeIf { it.isNotEmpty() }?.let { uri ->
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(uri)
                                            .crossfade(true)
                                            .placeholder(R.drawable.ic_profile_placeholder)
                                            .error(R.drawable.ic_profile_placeholder)
                                            .build(),
                                        onError = { error -> Log.e("OrganizationCard", "Coil Error: ${error.result.throwable.message}") }
                                    ),
                                    contentDescription = "Organization Logo",
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } ?: Image(
                                painter = painterResource(id = R.drawable.ic_profile_placeholder),
                                contentDescription = "Organization Logo",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentOrg.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentOrg.description.ifEmpty { "Not provided" },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (currentOrg.partnerIds.isNotEmpty()) {
                        item {
                            Text(
                                text = "Partners",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentOrg.partnerIds.joinToString(", "),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    if (currentOrg.financingDocuments.isNotEmpty()) {
                        item {
                            Text(
                                text = "Financing Documents",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentOrg.financingDocuments.joinToString(", "),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                val currentUser = auth.currentUser
                                if (currentUser == null) {
                                    Log.e("OrganizationCard", "No authenticated user found, navigating to SignIn")
                                    navController.navigate(Screen.SignIn.route)
                                } else {
                                    coroutineScope.launch {
                                        try {
                                            val recipientId = currentOrg.creatorId
                                            Log.d("OrganizationCard", "Creating conversation for user ${currentUser.uid} with recipient $recipientId")
                                            // Generate sorted conversation ID
                                            val conversationId = if (currentUser.uid < recipientId) {
                                                "${currentUser.uid}_${recipientId}"
                                            } else {
                                                "${recipientId}_${currentUser.uid}"
                                            }
                                            // Create conversation document
                                            val conversationData = hashMapOf(
                                                "senderId" to currentUser.uid,
                                                "recipientId" to recipientId,
                                                "participantIds" to listOf(currentUser.uid, recipientId),
                                                "lastUpdated" to System.currentTimeMillis()
                                            )
                                            firestore.collection("conversations")
                                                .document(conversationId)
                                                .set(conversationData, com.google.firebase.firestore.SetOptions.merge())
                                                .await()
                                            Log.d("OrganizationCard", "Conversation created: $conversationId")

                                            // Send request message
                                            val requestMessage = "I would like to request the financing documents for your organization ${currentOrg.name}: ${currentOrg.financingDocuments.joinToString(", ")}."
                                            val messageData = hashMapOf(
                                                "senderId" to currentUser.uid,
                                                "content" to requestMessage,
                                                "timestamp" to System.currentTimeMillis(),
                                                "read" to false
                                            )
                                            firestore.collection("conversations")
                                                .document(conversationId)
                                                .collection("messages")
                                                .add(messageData)
                                                .await()
                                            Log.d("OrganizationCard", "Request message sent")

                                            // Optional: Update conversation with last message
                                            firestore.collection("conversations")
                                                .document(conversationId)
                                                .update(
                                                    "lastMessage", requestMessage,
                                                    "lastUpdated", System.currentTimeMillis()
                                                )
                                                .await()

                                            // Navigate to PrivateChatScreen
                                            navController.navigate(Screen.PrivateChat.createRoute(conversationId))
                                        } catch (e: Exception) {
                                            Log.e("OrganizationCard", "Error handling request: ${e.message}", e)
                                            android.widget.Toast.makeText(
                                                context,
                                                "Failed to send request: ${e.message}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }) {
                                Text("Request Documents")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.weight(1f)) // Push mail icon to bottom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(onClick = {
                                val currentUser = auth.currentUser
                                if (currentUser == null) {
                                    Log.e("OrganizationCard", "No authenticated user found, navigating to SignIn")
                                    navController.navigate(Screen.SignIn.route)
                                } else {
                                    coroutineScope.launch {
                                        try {
                                            val recipientId = currentOrg.creatorId
                                            Log.d("OrganizationCard", "Creating conversation for user ${currentUser.uid} with recipient $recipientId")
                                            // Generate sorted conversation ID
                                            val conversationId = if (currentUser.uid < recipientId) {
                                                "${currentUser.uid}_${recipientId}"
                                            } else {
                                                "${recipientId}_${currentUser.uid}"
                                            }
                                            // Create conversation document
                                            val conversationData = hashMapOf(
                                                "senderId" to currentUser.uid,
                                                "recipientId" to recipientId,
                                                "participantIds" to listOf(currentUser.uid, recipientId),
                                                "lastUpdated" to System.currentTimeMillis()
                                            )
                                            firestore.collection("conversations")
                                                .document(conversationId)
                                                .set(conversationData, com.google.firebase.firestore.SetOptions.merge())
                                                .await()
                                            Log.d("OrganizationCard", "Conversation created: $conversationId")
                                            // Navigate to PrivateChatScreen
                                            navController.navigate(Screen.PrivateChat.createRoute(conversationId))
                                        } catch (e: Exception) {
                                            Log.e("OrganizationCard", "Error creating conversation: ${e.message}", e)
                                            android.widget.Toast.makeText(
                                                context,
                                                "Failed to start conversation: ${e.message}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.Mail, contentDescription = "Message Organization Creator")
                            }
                        }
                    }
                }
            } else {
                // Compact view for list mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = currentOrg.imageUri?.let { rememberAsyncImagePainter(it) }
                            ?: painterResource(id = R.drawable.ic_profile_placeholder),
                        contentDescription = "Organization Logo",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = currentOrg.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentOrg.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}