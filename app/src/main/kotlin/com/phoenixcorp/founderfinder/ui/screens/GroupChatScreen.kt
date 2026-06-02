package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.Message
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroupChatScreen(navController: NavHostController, orgId: String) {
    val auth = FirebaseAuth.getInstance()
    val firestore = Firebase.firestore
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentUser = auth.currentUser
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var userProfiles by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
    var orgName by remember { mutableStateOf("Loading...") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var messageListener by remember { mutableStateOf<ListenerRegistration?>(null) }
    var hasError by remember { mutableStateOf(false) }

    Log.d("GroupChat", "GroupChatScreen initialized with orgId: $orgId, user: ${currentUser?.uid}")

    // Fetch organization name and participant profiles
    LaunchedEffect(orgId) {
        if (currentUser == null) {
            Log.e("GroupChat", "No authenticated user found")
            errorMessage = "You must be logged in."
            isLoading = false
            hasError = true
            Toast.makeText(context, "Please sign in", Toast.LENGTH_LONG).show()
            return@LaunchedEffect
        }
        if (orgId.isNotEmpty()) {
            try {
                Log.d("GroupChat", "Fetching organization and participants for orgId: $orgId")
                // Fetch organization name
                val orgDoc = firestore.collection("organizations")
                    .document(orgId)
                    .get()
                    .await()
                if (!orgDoc.exists()) {
                    Log.e("GroupChat", "Organization $orgId does not exist")
                    errorMessage = "Organization not found"
                    isLoading = false
                    hasError = true
                    Toast.makeText(context, "Organization not found", Toast.LENGTH_LONG).show()
                    return@LaunchedEffect
                }
                orgName = orgDoc.getString("name") ?: "Organization Chat"
                Log.d("GroupChat", "Organization name: $orgName")

                // Check if user is a member
                val creatorId = orgDoc.getString("creatorId") ?: ""
                val partnerSnapshot = firestore.collection("organizations")
                    .document(orgId)
                    .collection("partners")
                    .get()
                    .await()
                val partnerIds = partnerSnapshot.documents.mapNotNull { it.id }
                val collaboratorSnapshot = firestore.collection("organizations")
                    .document(orgId)
                    .collection("collaborators")
                    .get()
                    .await()
                val collaboratorIds = collaboratorSnapshot.documents.mapNotNull { it.id }
                val participantIds = (listOf(creatorId) + partnerIds + collaboratorIds).distinct()
                Log.d("GroupChat", "Fetched participant IDs: $participantIds")

                if (currentUser.uid !in participantIds) {
                    Log.e("GroupChat", "User ${currentUser.uid} is not a member of organization $orgId")
                    errorMessage = "You are not authorized to access this chat"
                    isLoading = false
                    hasError = true
                    Toast.makeText(context, "You are not authorized to access this chat", Toast.LENGTH_LONG).show()
                    return@LaunchedEffect
                }

                // Fetch user profiles, including current user
                val profiles = mutableMapOf<String, UserProfile>()
                participantIds.forEach { id ->
                    try {
                        val profileDoc = firestore.collection("profiles").document(id).get().await()
                        profileDoc.toObject(UserProfile::class.java)?.let { profiles[id] = it }
                        Log.d("GroupChat", "Fetched profile for user $id")
                    } catch (e: Exception) {
                        Log.e("GroupChat", "Error fetching profile $id: ${e.message}")
                    }
                }
                userProfiles = profiles
                Log.d("GroupChat", "Fetched ${profiles.size} profiles")

                isLoading = false
            } catch (e: Exception) {
                Log.e("GroupChat", "Error fetching organization or profiles: ${e.message}", e)
                errorMessage = "Failed to load organization: ${e.message}"
                isLoading = false
                hasError = true
                Toast.makeText(context, "Failed to load organization: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.e("GroupChat", "Invalid orgId: $orgId")
            errorMessage = "Invalid organization ID"
            isLoading = false
            hasError = true
            Toast.makeText(context, "Invalid organization ID", Toast.LENGTH_LONG).show()
        }
    }

    // Fetch group messages
    LaunchedEffect(orgId, hasError) {
        if (hasError || currentUser == null || orgId.isEmpty()) {
            Log.d("GroupChat", "Skipping message listener: hasError=$hasError, currentUser=${currentUser?.uid}, orgId=$orgId")
            return@LaunchedEffect
        }
        try {
            Log.d("GroupChat", "Setting up message listener for orgId: $orgId")
            messageListener?.remove()
            messageListener = firestore.collection("group_conversations")
                .document(orgId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("GroupChat", "Message listener error: ${error.message}", error)
                        errorMessage = "Failed to load messages: ${error.message}"
                        isLoading = false
                        hasError = true
                        Toast.makeText(context, "Failed to load messages: ${error.message}", Toast.LENGTH_LONG).show()
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val newMessages = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Message::class.java)?.copy(id = doc.id)
                            } catch (e: Exception) {
                                Log.e("GroupChat", "Error parsing message ${doc.id}: ${e.message}")
                                null
                            }
                        }
                        messages = (messages + newMessages).distinctBy { it.id }.sortedBy { it.timestamp }
                        Log.d("GroupChat", "Fetched ${newMessages.size} new messages, total ${messages.size} messages for orgId: $orgId")
                        isLoading = false
                        hasError = false
                    } else {
                        Log.w("GroupChat", "Message snapshot is null for orgId: $orgId")
                        errorMessage = "No messages found"
                        isLoading = false
                        hasError = false // Allow UI to show empty chat
                    }
                }
        } catch (e: Exception) {
            Log.e("GroupChat", "Error setting up message listener: ${e.message}", e)
            errorMessage = "Failed to load messages: ${e.message}"
            isLoading = false
            hasError = true
            Toast.makeText(context, "Failed to load messages: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Clean up listener on dispose
    DisposableEffect(orgId) {
        onDispose {
            messageListener?.remove()
            Log.d("GroupChat", "Message listener removed")
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text(orgName) },
                navController = navController,
                showBackButton = true,
                onBackClick = {
                    Log.d("GroupChat", "Back button clicked, navigating to HomeScreen")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                )
                Log.d("GroupChat", "Showing loading indicator")
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                )
                Log.d("GroupChat", "Showing error: $errorMessage")
                Button(
                    onClick = {
                        Log.d("GroupChat", "Retry button clicked, resetting state")
                        isLoading = true
                        errorMessage = null
                        hasError = false
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp)
                ) {
                    Text("Retry")
                }
            } else {
                // Messages
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        Text(
                            text = "${userProfiles.size} participants",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                    items(messages.reversed()) { message ->
                        val sender = userProfiles[message.senderId]
                        val isCurrentUser = message.senderId == currentUser?.uid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isCurrentUser) {
                                sender?.profilePicture?.takeIf { it.isNotEmpty() }?.let { picture ->
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(picture)
                                                .crossfade(true)
                                                .placeholder(R.drawable.ic_profile_placeholder)
                                                .error(R.drawable.ic_profile_placeholder)
                                                .build(),
                                            onError = { error -> Log.e("GroupChat", "Coil Error: ${error.result.throwable.message}") }
                                        ),
                                        contentDescription = "Sender Profile Picture",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } ?: Image(
                                    painter = painterResource(id = R.drawable.ic_profile_placeholder),
                                    contentDescription = "Sender Profile Picture",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column(
                                modifier = Modifier
                                    .background(
                                        if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                                    .weight(1f, fill = false)
                            ) {
                                if (!isCurrentUser) {
                                    Text(
                                        text = "${sender?.firstName ?: "Unknown"} ${sender?.lastName ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                            if (isCurrentUser) {
                                Spacer(modifier = Modifier.width(8.dp))
                                userProfiles[currentUser?.uid]?.profilePicture?.takeIf { it.isNotEmpty() }?.let { picture ->
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(picture)
                                                .crossfade(true)
                                                .placeholder(R.drawable.ic_profile_placeholder)
                                                .error(R.drawable.ic_profile_placeholder)
                                                .build(),
                                            onError = { error -> Log.e("GroupChat", "Coil Error: ${error.result.throwable.message}") }
                                        ),
                                        contentDescription = "Your Profile Picture",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } ?: Image(
                                    painter = painterResource(id = R.drawable.ic_profile_placeholder),
                                    contentDescription = "Your Profile Picture",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                // Message Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("Type a message") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            Log.d("GroupChat", "Keyboard send action triggered")
                            sendGroupMessage(
                                currentUser,
                                orgId,
                                messageText,
                                firestore,
                                coroutineScope,
                                context,
                                onSuccess = { messageText = "" },
                                onError = { error ->
                                    errorMessage = "Failed to send message: ${error.message}"
                                    Log.e("GroupChat", "Error sending message: ${error.message}", error)
                                    Toast.makeText(context, "Failed to send message: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            Log.d("GroupChat", "Send button clicked")
                            sendGroupMessage(
                                currentUser,
                                orgId,
                                messageText,
                                firestore,
                                coroutineScope,
                                context,
                                onSuccess = { messageText = "" },
                                onError = { error ->
                                    errorMessage = "Failed to send message: ${error.message}"
                                    Log.e("GroupChat", "Error sending message: ${error.message}", error)
                                    Toast.makeText(context, "Failed to send message: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        enabled = messageText.isNotBlank() && currentUser != null
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send Message")
                    }
                }
                Log.d("GroupChat", "Showing message input field")
            }
        }
    }
}

private fun sendGroupMessage(
    currentUser: com.google.firebase.auth.FirebaseUser?,
    orgId: String,
    messageText: String,
    firestore: FirebaseFirestore,
    coroutineScope: CoroutineScope,
    context: android.content.Context,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    if (messageText.isNotBlank() && currentUser != null) {
        coroutineScope.launch {
            try {
                Log.d("GroupChat", "Sending group message from ${currentUser.uid} in org $orgId")
                // Create or update group conversation document
                val conversationData = mapOf(
                    "orgId" to orgId,
                    "lastUpdated" to System.currentTimeMillis()
                )
                firestore.collection("group_conversations")
                    .document(orgId)
                    .set(conversationData, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                // Fetch participant IDs for notification
                val orgDoc = firestore.collection("organizations")
                    .document(orgId)
                    .get()
                    .await()
                val creatorId = orgDoc.getString("creatorId") ?: ""
                val partnerSnapshot = firestore.collection("organizations")
                    .document(orgId)
                    .collection("partners")
                    .get()
                    .await()
                val partnerIds = partnerSnapshot.documents.mapNotNull { it.id }
                val collaboratorSnapshot = firestore.collection("organizations")
                    .document(orgId)
                    .collection("collaborators")
                    .get()
                    .await()
                val collaboratorIds = collaboratorSnapshot.documents.mapNotNull { it.id }
                val participantIds = (listOf(creatorId) + partnerIds + collaboratorIds).distinct()
                    .filter { it != currentUser.uid } // Exclude sender
                Log.d("GroupChat", "Participants for notification: $participantIds")

                // Add message to sub-collection
                val message = Message(
                    id = UUID.randomUUID().toString(),
                    senderId = currentUser.uid,
                    content = messageText,
                    timestamp = System.currentTimeMillis(),
                    type = "text",
                    orgId = orgId
                )
                firestore.collection("group_conversations")
                    .document(orgId)
                    .collection("messages")
                    .document(message.id!!)
                    .set(message)
                    .await()
                Log.d("GroupChat", "Group message sent successfully: ${message.id}")

                // Send notifications to participants
                participantIds.forEach { participantId ->
                    try {
                        val profileDoc = firestore.collection("profiles").document(currentUser.uid).get().await()
                        val senderName = profileDoc.getString("firstName") ?: "User"
                        val notificationData = mapOf(
                            "senderId" to currentUser.uid,
                            "recipientId" to participantId,
                            "orgId" to orgId,
                            "message" to messageText,
                            "title" to "$senderName sent a message in ${orgDoc.getString("name") ?: "Group Chat"}",
                            "timestamp" to System.currentTimeMillis(),
                            "screen" to "GroupChat",
                            "id" to orgId
                        )
                        firestore.collection("notifications")
                            .add(notificationData)
                            .await()
                        Log.d("GroupChat", "Notification sent to $participantId for message ${message.id}")
                    } catch (e: Exception) {
                        Log.e("GroupChat", "Error sending notification to $participantId: ${e.message}", e)
                    }
                }

                Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show()
                onSuccess()
            } catch (e: Exception) {
                Log.e("GroupChat", "Error sending group message: ${e.message}", e)
                onError(e)
            }
        }
    } else {
        val errorMsg = "Cannot send message: currentUser=${currentUser?.uid}, messageText=$messageText"
        Log.e("GroupChat", errorMsg)
        onError(Exception(errorMsg))
    }
}