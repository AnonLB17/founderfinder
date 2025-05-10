package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class Message(
    val id: String? = null,
    val senderId: String? = null,
    val recipientId: String? = null,
    val content: String? = null,
    val timestamp: Long? = null
)

@Composable
fun PrivateMessagesScreen(navController: NavHostController, recipientId: String) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = Firebase.firestore
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val currentUser = auth.currentUser

    // Fetch messages
    LaunchedEffect(Unit) {
        if (currentUser != null) {
            coroutineScope.launch {
                try {
                    val snapshot = firestore.collection("messages")
                        .whereIn("senderId", listOf(currentUser.uid, recipientId))
                        .whereIn("recipientId", listOf(currentUser.uid, recipientId))
                        .orderBy("timestamp", Query.Direction.ASCENDING)
                        .get()
                        .await()
                    messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    }
                    isLoading = false
                } catch (e: Exception) {
                    errorMessage = "Failed to load messages: ${e.message}"
                    isLoading = false
                }
            }
        } else {
            errorMessage = "You must be logged in."
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = "Private Messages",
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
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    reverseLayout = true
                ) {
                    items(messages.size) { index ->
                        val message = messages[messages.size - 1 - index]
                        MessageBubble(
                            message = message,
                            isSentByCurrentUser = message.senderId == currentUser?.uid
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("Type a message") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (messageText.isNotBlank() && currentUser != null) {
                                coroutineScope.launch {
                                    try {
                                        val message = Message(
                                            id = UUID.randomUUID().toString(),
                                            senderId = currentUser.uid,
                                            recipientId = recipientId,
                                            content = messageText,
                                            timestamp = System.currentTimeMillis()
                                        )
                                        firestore.collection("messages")
                                            .document(message.id!!)
                                            .set(message)
                                            .await()
                                        messageText = ""
                                        // Refresh messages
                                        val snapshot = firestore.collection("messages")
                                            .whereIn("senderId", listOf(currentUser.uid, recipientId))
                                            .whereIn("recipientId", listOf(currentUser.uid, recipientId))
                                            .orderBy("timestamp", Query.Direction.ASCENDING)
                                            .get()
                                            .await()
                                        messages = snapshot.documents.mapNotNull { doc ->
                                            doc.toObject(Message::class.java)?.copy(id = doc.id)
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to send message: ${e.message}"
                                    }
                                }
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (messageText.isNotBlank() && currentUser != null) {
                                coroutineScope.launch {
                                    try {
                                        val message = Message(
                                            id = UUID.randomUUID().toString(),
                                            senderId = currentUser.uid,
                                            recipientId = recipientId,
                                            content = messageText,
                                            timestamp = System.currentTimeMillis()
                                        )
                                        firestore.collection("messages")
                                            .document(message.id!!)
                                            .set(message)
                                            .await()
                                        messageText = ""
                                        // Refresh messages
                                        val snapshot = firestore.collection("messages")
                                            .whereIn("senderId", listOf(currentUser.uid, recipientId))
                                            .whereIn("recipientId", listOf(currentUser.uid, recipientId))
                                            .orderBy("timestamp", Query.Direction.ASCENDING)
                                            .get()
                                            .await()
                                        messages = snapshot.documents.mapNotNull { doc ->
                                            doc.toObject(Message::class.java)?.copy(id = doc.id)
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to send message: ${e.message}"
                                    }
                                }
                            }
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Text("Send")
                    }
                }
            }
        }
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
            Text(
                text = message.content ?: "",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}