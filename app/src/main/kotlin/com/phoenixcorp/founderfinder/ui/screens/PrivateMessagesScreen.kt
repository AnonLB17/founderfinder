package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.ChatMessage
import com.phoenixcorp.founderfinder.domain.model.Conversation
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PrivateMessagesScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) } // For confirmation dialog

    val coroutineScope = rememberCoroutineScope()
    val currentUser = auth.currentUser

    // Fetch conversations (same as before)
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            errorMessage = "You must be logged in."
            isLoading = false
            return@LaunchedEffect
        }

        coroutineScope.launch {
            try {
                val snapshot = firestore.collection("conversations")
                    .whereArrayContains("participantIds", currentUser.uid)
                    .get()
                    .await()

                val conversationList = mutableListOf<Conversation>()

                for (doc in snapshot.documents) {
                    val data = doc.data ?: continue
                    val senderId = data["senderId"] as? String ?: continue
                    val recipientId = data["recipientId"] as? String ?: continue
                    val otherUserId = if (senderId == currentUser.uid) recipientId else senderId

                    if (otherUserId == currentUser.uid) continue

                    val messageSnapshot = firestore.collection("conversations")
                        .document(doc.id)
                        .collection("messages")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .await()

                    val lastMessage = messageSnapshot.documents.firstOrNull()?.toObject(ChatMessage::class.java)

                    val profileDoc = firestore.collection("profiles")
                        .document(otherUserId)
                        .get()
                        .await()

                    val profile = profileDoc.toObject(UserProfile::class.java)
                    val recipientName = if (profile != null &&
                        (!profile.firstName.isNullOrBlank() || !profile.lastName.isNullOrBlank())) {
                        "${profile.firstName ?: ""} ${profile.lastName ?: ""}".trim()
                    } else {
                        "User ${otherUserId.takeLast(6)}"
                    }

                    val conversation = Conversation(
                        recipientId = otherUserId,
                        recipientName = recipientName,
                        lastMessage = lastMessage?.text ?: "No messages yet",
                        timestamp = lastMessage?.timestamp ?: 0L,
                        profilePicture = profile?.profilePicture,
                        conversationId = doc.id
                    )

                    conversationList.add(conversation)
                }

                conversations = conversationList
                    .distinctBy { it.recipientId }
                    .sortedByDescending { it.timestamp }

                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load conversations: ${e.message}"
                isLoading = false
                Log.e("PrivateMessages", "Error", e)
            }
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Messages") },
                navController = navController,
                showBackButton = true
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (errorMessage != null) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            } else if (conversations.isEmpty()) {
                Text(
                    text = "No conversations yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn {
                    items(conversations) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            onClick = {
                                navController.navigate(Screen.PrivateChat.createRoute(conversation.conversationId ?: conversation.recipientId))
                            },
                            onDelete = {
                                conversationToDelete = conversation
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (conversationToDelete != null) {
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("Delete Conversation") },
            text = {
                Text("Are you sure you want to delete this conversation with ${conversationToDelete?.recipientName}? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val conv = conversationToDelete!!
                        coroutineScope.launch {
                            try {
                                firestore.collection("conversations")
                                    .document(conv.conversationId ?: "")
                                    .delete()
                                    .await()

                                conversations = conversations.filter { it.recipientId != conv.recipientId }
                                Toast.makeText(context, "Conversation deleted", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to delete conversation", Toast.LENGTH_SHORT).show()
                            }
                        }
                        conversationToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = conversation.profilePicture?.let { rememberAsyncImagePainter(it) }
                    ?: painterResource(id = R.drawable.ic_profile_placeholder),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.recipientName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = conversation.lastMessage ?: "No messages yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                conversation.timestamp?.let { ts ->
                    val time = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(ts))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Conversation",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}