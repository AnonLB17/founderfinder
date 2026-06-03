package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore


import com.phoenixcorp.founderfinder.domain.model.Invitation
import com.phoenixcorp.founderfinder.domain.model.Message
import com.phoenixcorp.founderfinder.domain.model.Organization
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.OrganizationCard
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PrivateChatScreen(navController: NavHostController, conversationId: String) {
    val auth = FirebaseAuth.getInstance()
    val firestore = Firebase.firestore
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentUser = auth.currentUser
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var recipientName by remember { mutableStateOf("Loading...") }
    var recipientProfilePicture by remember { mutableStateOf<String?>(null) }
    var showOrgDialog by remember { mutableStateOf(false) }
    var organizations by remember { mutableStateOf<List<Organization>>(emptyList()) }
    var recipientType by remember { mutableStateOf<String?>(null) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var selectedOrg by remember { mutableStateOf<Organization?>(null) }
    var messageListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    // Compute sorted conversationId
    val userId = currentUser?.uid ?: ""
    val parts = conversationId.split("_")
    val recipientId = parts.firstOrNull { it != userId } ?: conversationId
    val sortedConversationId = if (userId.isNotEmpty() && recipientId.isNotEmpty()) {
        if (userId < recipientId) "${userId}_$recipientId" else "${recipientId}_$userId"
    } else {
        conversationId
    }

    // Log authentication state
    LaunchedEffect(Unit) {
        Log.d("PrivateChat", "Screen initialized with conversationId: $conversationId, recipientId: $recipientId, sortedConversationId: $sortedConversationId")
        if (currentUser == null) {
            Log.e("PrivateChat", "No authenticated user found")
            errorMessage = "You must be logged in."
            isLoading = false
            Toast.makeText(context, "Please sign in", Toast.LENGTH_SHORT).show()
            navController.navigate(Screen.SignIn.route)
        } else {
            Log.d("PrivateChat", "Authenticated user: ${currentUser.uid}")
        }
    }

    // Initialize conversation document
    LaunchedEffect(sortedConversationId) {
        if (currentUser != null && recipientId.isNotEmpty()) {
            try {
                Log.d("PrivateChat", "Initializing conversation: $sortedConversationId")
                val conversationData = hashMapOf(
                    "senderId" to currentUser.uid,
                    "recipientId" to recipientId,
                    "participantIds" to listOf(currentUser.uid, recipientId),
                    "lastUpdated" to System.currentTimeMillis()
                )
                firestore.collection("conversations")
                    .document(sortedConversationId)
                    .set(conversationData, com.google.firebase.firestore.SetOptions.merge())
                    .await()
                Log.d("PrivateChat", "Conversation document initialized: $sortedConversationId")
            } catch (e: Exception) {
                Log.e("PrivateChat", "Error initializing conversation: ${e.message}", e)
                errorMessage = "Failed to initialize conversation: ${e.message}"
                isLoading = false
            }
        } else {
            Log.e("PrivateChat", "Invalid conversationId: $sortedConversationId or recipientId: $recipientId")
            errorMessage = "Invalid conversation or recipient ID"
            isLoading = false
        }
    }

    // Fetch recipient's name, profile picture, and type
    LaunchedEffect(recipientId) {
        if (recipientId.isNotEmpty()) {
            try {
                Log.d("PrivateChat", "Fetching profile for recipient: $recipientId")
                val profileDoc = firestore.collection("profiles")
                    .document(recipientId)
                    .get()
                    .await()
                val profile = profileDoc.toObject(UserProfile::class.java)
                recipientName = if (profile != null) {
                    "${profile.firstName ?: "Unknown"} ${profile.lastName ?: "User"}"
                } else {
                    "Unknown User"
                }
                recipientProfilePicture = profile?.profilePicture

                // Determine recipient type
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
                recipientType = when {
                    partnerDoc.exists() -> "partner"
                    advisorDoc.exists() -> "advisor"
                    else -> null
                }
                Log.d("PrivateChat", "Recipient name: $recipientName, profilePicture: $recipientProfilePicture, type: $recipientType")
                isLoading = false
            } catch (e: Exception) {
                Log.e("PrivateChat", "Error fetching recipient profile: ${e.message}", e)
                errorMessage = "Failed to load recipient profile: ${e.message}"
                recipientName = "Unknown User"
                recipientProfilePicture = null
                recipientType = null
                isLoading = false
            }
        } else {
            Log.e("PrivateChat", "Invalid recipientId: $recipientId")
            errorMessage = "Invalid recipient ID"
            recipientName = "Unknown User"
            isLoading = false
        }
    }

    // Fetch organizations for the dialog
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                Log.d("PrivateChat", "Fetching organizations for user: ${currentUser.uid}")
                val snapshot = firestore.collection("organizations")
                    .whereEqualTo("creatorId", currentUser.uid)
                    .get()
                    .await()
                organizations = snapshot.documents.mapNotNull { doc ->
                    try {
                        Organization(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            imageUri = doc.getString("imageUri"),
                            creatorId = doc.getString("creatorId") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("PrivateChat", "Error parsing organization ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                Log.d("PrivateChat", "Fetched ${organizations.size} organizations")
            } catch (e: Exception) {
                Log.e("PrivateChat", "Error fetching organizations: ${e.message}", e)
                errorMessage = "Failed to load organizations: ${e.message}"
                isLoading = false
            }
        }
    }

    // Fetch messages with stable listener
    LaunchedEffect(sortedConversationId) {
        if (currentUser != null && sortedConversationId.isNotEmpty()) {
            try {
                Log.d("PrivateChat", "Setting up message listener for conversation: $sortedConversationId")
                messageListener?.remove()
                messageListener = firestore.collection("conversations")
                    .document(sortedConversationId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("PrivateChat", "Message listener error: ${error.message}", error)
                            errorMessage = "Failed to load messages: ${error.message}"
                            isLoading = false
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val newMessages = snapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                                } catch (e: Exception) {
                                    Log.e("PrivateChat", "Error parsing message ${doc.id}: ${e.message}")
                                    null
                                }
                            }
                            messages = (messages + newMessages).distinctBy { it.id }.sortedBy { it.timestamp }
                            Log.d("PrivateChat", "Fetched ${newMessages.size} new messages, total ${messages.size} messages for $sortedConversationId")
                            isLoading = false
                        } else {
                            Log.w("PrivateChat", "Message snapshot is null for $sortedConversationId")
                        }
                    }
            } catch (e: Exception) {
                Log.e("PrivateChat", "Error setting up message listener: ${e.message}", e)
                errorMessage = "Failed to load messages: ${e.message}"
                isLoading = false
            }
        } else {
            Log.e("PrivateChat", "No user logged in or invalid conversation ID: $sortedConversationId")
            errorMessage = "You must be logged in or invalid conversation ID"
            isLoading = false
        }
    }

    // Clean up listener on dispose
    DisposableEffect(sortedConversationId) {
        onDispose {
            messageListener?.remove()
            Log.d("PrivateChat", "Message listener removed")
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text(recipientName) },
                profilePicture = recipientProfilePicture,
                navController = navController,
                showBackButton = true,
                onBackClick = {
                    Log.d("PrivateChat", "Back button clicked, navigating to HomeScreen")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onProfileClick = {
                    if (recipientId.isNotEmpty()) {
                        Log.d("PrivateChat", "Navigating to profile: $recipientId")
                        navController.navigate(Screen.UserProfile.createRoute(recipientId))
                    } else {
                        Log.e("PrivateChat", "Cannot navigate to profile: recipientId is empty")
                        Toast.makeText(context, "Invalid recipient ID", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                )
                Log.d("PrivateChat", "Showing loading indicator")
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                )
                Log.d("PrivateChat", "Showing error: $errorMessage")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Text(
                                text = "No messages yet. Send your first message!",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                            Log.d("PrivateChat", "Showing no messages text")
                        }
                    }
                    items(messages.reversed()) { message ->
                        Log.d("PrivateChat", "Rendering message: ${message.content}")
                        when (message.type) {
                            "text" -> MessageBubble(
                                message = message,
                                isSentByCurrentUser = message.senderId == currentUser?.uid
                            )
                            "organization" -> message.orgId?.let { orgId ->
                                OrganizationCard(
                                    orgId = orgId,
                                    invitationId = message.id ?: UUID.randomUUID().toString(),
                                    navController = navController,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = if (message.senderId == currentUser?.uid) 32.dp else 8.dp,
                                            end = if (message.senderId == currentUser?.uid) 8.dp else 32.dp
                                        )
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showOrgDialog = true }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach Organization")
                    }
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("Type a message") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            Log.d("PrivateChat", "Keyboard send action triggered")
                            sendMessage(
                                currentUser,
                                sortedConversationId,
                                recipientId,
                                messageText,
                                firestore,
                                coroutineScope,
                                onSuccess = { messageText = "" },
                                onError = { error ->
                                    errorMessage = "Failed to send message: ${error.message}"
                                    Log.e("PrivateChat", "Error sending message: ${error.message}", error)
                                    Toast.makeText(context, "Failed to send message: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            Log.d("PrivateChat", "Enter button clicked")
                            sendMessage(
                                currentUser,
                                sortedConversationId,
                                recipientId,
                                messageText,
                                firestore,
                                coroutineScope,
                                onSuccess = { messageText = "" },
                                onError = { error ->
                                    errorMessage = "Failed to send message: ${error.message}"
                                    Log.e("PrivateChat", "Error sending message: ${error.message}", error)
                                    Toast.makeText(context, "Failed to send message: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        enabled = messageText.isNotBlank() && currentUser != null
                    ) {
                        Text("Enter")
                    }
                }
                Log.d("PrivateChat", "Showing message input field")
            }
        }
    }

    // Organization selection dialog
    if (showOrgDialog) {
        AlertDialog(
            onDismissRequest = { showOrgDialog = false },
            title = { Text("Select Organization") },
            text = {
                if (organizations.isEmpty()) {
                    Text("No organizations found. Create one in Idea Creation.")
                    Log.d("PrivateChat", "Showing no organizations dialog")
                } else {
                    LazyColumn {
                        items(organizations) { org ->
                            Text(
                                text = org.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedOrg = org
                                        if (recipientType == null) {
                                            showTypeDialog = true
                                        } else {
                                            sendOrganization(
                                                currentUser,
                                                sortedConversationId,
                                                recipientId,
                                                org.id,
                                                recipientType!!,
                                                firestore,
                                                coroutineScope,
                                                context,
                                                onSuccess = { showOrgDialog = false },
                                                onError = { error ->
                                                    errorMessage = "Failed to share organization: ${error.message}"
                                                    Log.e("PrivateChat", "Error sharing organization: ${error.message}", error)
                                                    Toast.makeText(context, "Failed to share organization: ${error.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }
                    Log.d("PrivateChat", "Showing organization selection dialog")
                }
            },
            confirmButton = {
                TextButton(onClick = { showOrgDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Recipient type selection dialog
    if (showTypeDialog && selectedOrg != null) {
        AlertDialog(
            onDismissRequest = { showTypeDialog = false },
            title = { Text("Select Recipient Role") },
            text = {
                Column {
                    Text("Recipient’s role is unknown. Please select their role for the invitation:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Partner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sendOrganization(
                                    currentUser,
                                    sortedConversationId,
                                    recipientId,
                                    selectedOrg!!.id,
                                    "partner",
                                    firestore,
                                    coroutineScope,
                                    context,
                                    onSuccess = {
                                        showOrgDialog = false
                                        showTypeDialog = false
                                    },
                                    onError = { error ->
                                        errorMessage = "Failed to share organization: ${error.message}"
                                        Log.e("PrivateChat", "Error sharing organization: ${error.message}", error)
                                        Toast.makeText(context, "Failed to share organization: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            .padding(8.dp)
                    )
                    Text(
                        text = "Advisor",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sendOrganization(
                                    currentUser,
                                    sortedConversationId,
                                    recipientId,
                                    selectedOrg!!.id,
                                    "advisor",
                                    firestore,
                                    coroutineScope,
                                    context,
                                    onSuccess = {
                                        showOrgDialog = false
                                        showTypeDialog = false
                                    },
                                    onError = { error ->
                                        errorMessage = "Failed to share organization: ${error.message}"
                                        Log.e("PrivateChat", "Error sharing organization: ${error.message}", error)
                                        Toast.makeText(context, "Failed to share organization: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            .padding(8.dp)
                    )
                    Text(
                        text = "Collaborator",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sendOrganization(
                                    currentUser,
                                    sortedConversationId,
                                    recipientId,
                                    selectedOrg!!.id,
                                    "collaborator",
                                    firestore,
                                    coroutineScope,
                                    context,
                                    onSuccess = {
                                        showOrgDialog = false
                                        showTypeDialog = false
                                    },
                                    onError = { error ->
                                        errorMessage = "Failed to share organization: ${error.message}"
                                        Log.e("PrivateChat", "Error sharing organization: ${error.message}", error)
                                        Toast.makeText(context, "Failed to share organization: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            .padding(8.dp)
                    )
                }
                Log.d("PrivateChat", "Showing recipient type dialog")
            },
            confirmButton = {
                TextButton(onClick = { showTypeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MessageBubble(message: Message, isSentByCurrentUser: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        contentAlignment = if (isSentByCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            modifier = Modifier
                .wrapContentWidth()
                .padding(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSentByCurrentUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = message.content ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private fun sendMessage(
    currentUser: com.google.firebase.auth.FirebaseUser?,
    conversationId: String,
    recipientId: String,
    messageText: String,
    firestore: FirebaseFirestore,
    coroutineScope: CoroutineScope,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    if (messageText.isNotBlank() && currentUser != null) {
        coroutineScope.launch {
            try {
                Log.d("PrivateChat", "Sending text message from ${currentUser.uid} to $recipientId in conversation $conversationId")
                // Create or update conversation document
                val conversationData = hashMapOf(
                    "senderId" to currentUser.uid,
                    "recipientId" to recipientId,
                    "lastUpdated" to System.currentTimeMillis()
                )
                firestore.collection("conversations")
                    .document(conversationId)
                    .set(conversationData, com.google.firebase.firestore.SetOptions.merge())
                    .await()
                Log.d("PrivateChat", "Conversation document created/updated: $conversationId")

                // Add message to sub-collection
                val message = Message(
                    id = UUID.randomUUID().toString(),
                    senderId = currentUser.uid,
                    recipientId = recipientId,
                    content = messageText,
                    timestamp = System.currentTimeMillis(),
                    type = "text"
                )
                firestore.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document(message.id!!)
                    .set(message)
                    .await()
                Log.d("PrivateChat", "Text message sent successfully: ${message.id}")
                onSuccess()
            } catch (e: Exception) {
                Log.e("PrivateChat", "Error sending text message: ${e.message}", e)
                onError(e)
            }
        }
    } else {
        val errorMsg = "Cannot send message: currentUser=${currentUser?.uid}, messageText=$messageText"
        Log.e("PrivateChat", errorMsg)
        onError(Exception(errorMsg))
    }
}

private fun sendOrganization(
    currentUser: com.google.firebase.auth.FirebaseUser?,
    conversationId: String,
    recipientId: String,
    orgId: String,
    recipientType: String,
    firestore: FirebaseFirestore,
    coroutineScope: CoroutineScope,
    context: android.content.Context,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    if (currentUser != null) {
        coroutineScope.launch {
            try {
                Log.d("PrivateChat", "Sending organization $orgId from ${currentUser.uid} to $recipientId in conversation $conversationId")
                // Create invitation
                val invitationId = UUID.randomUUID().toString()
                val invitation = Invitation(
                    invitationId = invitationId,
                    orgId = orgId,
                    inviterId = currentUser.uid,
                    inviteeId = recipientId,
                    status = "pending",
                    type = recipientType,
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
                    .document(recipientId)
                    .set(invitation)
                    .await()
                Log.d("PrivateChat", "Invitation created: $invitationId")

                // Create or update conversation document
                val conversationData = mapOf(
                    "senderId" to currentUser.uid,
                    "recipientId" to recipientId,
                    "lastUpdated" to System.currentTimeMillis()
                )
                firestore.collection("conversations")
                    .document(conversationId)
                    .set(conversationData, com.google.firebase.firestore.SetOptions.merge())
                    .await()
                Log.d("PrivateChat", "Conversation document created/updated: $conversationId")

                // Add organization message to sub-collection
                val message = Message(
                    id = invitationId,
                    senderId = currentUser.uid,
                    recipientId = recipientId,
                    content = "Shared an organization",
                    timestamp = System.currentTimeMillis(),
                    type = "organization",
                    orgId = orgId
                )
                firestore.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document(message.id!!)
                    .set(message)
                    .await()
                Log.d("PrivateChat", "Organization message sent successfully: ${message.id}")
                Toast.makeText(context, "Organization shared successfully", Toast.LENGTH_SHORT).show()
                onSuccess()
            } catch (error: Exception) {
                Log.e("PrivateChat", "Error sending organization message: ${error.message}", error)
                Toast.makeText(context, "Failed to share organization: ${error.message}", Toast.LENGTH_LONG).show()
                onError(error)
            }
        }
    } else {
        val errorMsg = "Cannot send organization: currentUser=${currentUser?.uid}"
        Log.e("PrivateChat", errorMsg)
        Toast.makeText(context, "Please sign in to share organization", Toast.LENGTH_SHORT).show()
        onError(Exception(errorMsg))
    }
}