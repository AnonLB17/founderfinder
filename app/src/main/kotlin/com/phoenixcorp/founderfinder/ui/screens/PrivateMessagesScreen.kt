package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.Conversation
import com.phoenixcorp.founderfinder.domain.model.Message
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
    var advisorConversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var partnerConversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var investorConversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val currentUser = auth.currentUser

    // Fetch conversations
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            Log.e("PrivateMessages", "No authenticated user found")
            errorMessage = "You must be logged in."
            isLoading = false
            return@LaunchedEffect
        }
        coroutineScope.launch {
            try {
                Log.d("PrivateMessages", "Fetching conversations for user: ${currentUser.uid}")
                // Fetch conversations where the user is in participantIds
                val snapshot = firestore.collection("conversations")
                    .whereArrayContains("participantIds", currentUser.uid)
                    .get()
                    .await()

                val conversationDocs = snapshot.documents
                Log.d("PrivateMessages", "Fetched ${conversationDocs.size} conversations")

                val advisorList = mutableListOf<Conversation>()
                val partnerList = mutableListOf<Conversation>()
                val investorList = mutableListOf<Conversation>()
                for (doc in conversationDocs) {
                    val conversationData = doc.data ?: continue
                    val senderId = conversationData["senderId"] as? String ?: continue
                    val recipientId = conversationData["recipientId"] as? String ?: continue
                    val otherUserId = if (senderId == currentUser.uid) recipientId else senderId
                    val conversationId = if (currentUser.uid < otherUserId) "${currentUser.uid}_$otherUserId" else "${otherUserId}_${currentUser.uid}"

                    // Fetch the latest message
                    val messageSnapshot = firestore.collection("conversations")
                        .document(conversationId)
                        .collection("messages")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .await()
                    val lastMessage = messageSnapshot.documents.firstOrNull()?.toObject(Message::class.java)

                    // Fetch recipient profile
                    try {
                        val profileDoc = firestore.collection("profiles")
                            .document(otherUserId)
                            .get()
                            .await()
                        val profile = profileDoc.toObject(UserProfile::class.java)
                        val recipientName = if (profile != null) {
                            "${profile.firstName ?: "Unknown"} ${profile.lastName ?: "User"}"
                        } else {
                            "Unknown User"
                        }

                        // Check roles
                        val isAdvisor = firestore.collection("profiles")
                            .document(otherUserId)
                            .collection("advisor")
                            .document("data")
                            .get()
                            .await()
                            .exists()
                        val isPartner = firestore.collection("profiles")
                            .document(otherUserId)
                            .collection("partner")
                            .document("data")
                            .get()
                            .await()
                            .exists()
                        val isInvestor = firestore.collection("investors")
                            .document(otherUserId)
                            .get()
                            .await()
                            .exists()

                        val conversation = Conversation(
                            recipientId = otherUserId,
                            recipientName = recipientName,
                            lastMessage = lastMessage?.content,
                            timestamp = lastMessage?.timestamp,
                            profilePicture = profile?.profilePicture
                        )

                        when {
                            isInvestor -> investorList.add(conversation)
                            isAdvisor -> advisorList.add(conversation)
                            isPartner -> partnerList.add(conversation)
                        }
                    } catch (e: Exception) {
                        Log.e("PrivateMessages", "Error fetching profile for $otherUserId: ${e.message}")
                    }
                }

                // Sort conversations by timestamp (newest first)
                advisorConversations = advisorList.sortedByDescending { it.timestamp }
                partnerConversations = partnerList.sortedByDescending { it.timestamp }
                investorConversations = investorList.sortedByDescending { it.timestamp }
                isLoading = false
                Log.d("PrivateMessages", "Loaded ${advisorConversations.size} advisor, ${partnerConversations.size} partner, ${investorConversations.size} investor conversations")
            } catch (e: Exception) {
                errorMessage = "Failed to load conversations: ${e.message}"
                isLoading = false
                Log.e("PrivateMessages", "Error fetching conversations: ${e.message}", e)
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
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn {
                    // Advisors Section
                    if (advisorConversations.isNotEmpty()) {
                        item {
                            Text(
                                text = "Advisors",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(advisorConversations) { conversation ->
                            ConversationItem(
                                conversation = conversation,
                                onClick = {
                                    val conversationId = if (currentUser?.uid != null && currentUser.uid < conversation.recipientId) {
                                        "${currentUser.uid}_${conversation.recipientId}"
                                    } else {
                                        "${conversation.recipientId}_${currentUser?.uid}"
                                    }
                                    navController.navigate(Screen.PrivateChat.createRoute(conversationId))
                                }
                            )
                        }
                    }

                    // Partners Section
                    if (partnerConversations.isNotEmpty()) {
                        item {
                            Text(
                                text = "Partners",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(partnerConversations) { conversation ->
                            ConversationItem(
                                conversation = conversation,
                                onClick = {
                                    val conversationId = if (currentUser?.uid != null && currentUser.uid < conversation.recipientId) {
                                        "${currentUser.uid}_${conversation.recipientId}"
                                    } else {
                                        "${conversation.recipientId}_${currentUser?.uid}"
                                    }
                                    navController.navigate(Screen.PrivateChat.createRoute(conversationId))
                                }
                            )
                        }
                    }

                    // Investors Section
                    if (investorConversations.isNotEmpty()) {
                        item {
                            Text(
                                text = "Investors",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(investorConversations) { conversation ->
                            ConversationItem(
                                conversation = conversation,
                                onClick = {
                                    val conversationId = if (currentUser?.uid != null && currentUser.uid < conversation.recipientId) {
                                        "${currentUser.uid}_${conversation.recipientId}"
                                    } else {
                                        "${conversation.recipientId}_${currentUser?.uid}"
                                    }
                                    navController.navigate(Screen.PrivateChat.createRoute(conversationId))
                                }
                            )
                        }
                    }

                    // Empty State
                    if (advisorConversations.isEmpty() && partnerConversations.isEmpty() && investorConversations.isEmpty()) {
                        item {
                            Text(
                                text = "No conversations yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationItem(conversation: Conversation, onClick: () -> Unit) {
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
            // Profile Picture
            if (conversation.profilePicture != null && conversation.profilePicture.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = conversation.profilePicture,
                        placeholder = painterResource(R.drawable.ic_profile_placeholder),
                        error = painterResource(R.drawable.ic_profile_placeholder)
                    ),
                    contentDescription = "Recipient Profile Picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_profile_placeholder),
                    contentDescription = "Recipient Profile Picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Conversation Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.recipientName,
                    style = MaterialTheme.typography.titleMedium
                )
                conversation.lastMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                conversation.timestamp?.let { timestamp ->
                    val formattedTime = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                        .format(Date(timestamp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}