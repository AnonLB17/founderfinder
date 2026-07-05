package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.phoenixcorp.founderfinder.domain.model.ChatMessage
import com.phoenixcorp.founderfinder.domain.model.Organization
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.OrganizationCard
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.ui.viewmodel.ChatViewModel
import com.phoenixcorp.founderfinder.ui.viewmodel.PrivateChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.toObject
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import kotlinx.coroutines.tasks.await
import kotlin.jvm.java

@Composable
fun PrivateChatScreen(
    navController: NavHostController,
    conversationId: String,                    // Should be the sorted chatId
    privateChatViewModel: PrivateChatViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // PrivateChatViewModel states
    val recipientName by privateChatViewModel.recipientName.collectAsState()
    val recipientProfilePicture by privateChatViewModel.recipientProfilePicture.collectAsState()
    val recipientId by privateChatViewModel.recipientId.collectAsState()
    val organizations by privateChatViewModel.organizations.collectAsState()
    val recipientType by privateChatViewModel.recipientType.collectAsState()
    val currentUser = privateChatViewModel.currentUser

    // ChatViewModel states
    val messages by chatViewModel.messages.collectAsState()
    val isSending by chatViewModel.isSending.collectAsState()
    val error by chatViewModel.error.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var showOrgDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var selectedOrg by remember { mutableStateOf<Organization?>(null) }

    // Load data when screen opens
    LaunchedEffect(conversationId) {
        chatViewModel.loadMessages(conversationId)
        privateChatViewModel.loadChatData(conversationId)   // ← New method you'll add in ViewModel
    }

    // Show error as Toast
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            chatViewModel.clearError()
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
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onProfileClick = {
                    recipientId?.let {
                        navController.navigate(Screen.UserProfile.createRoute(it))
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
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = true,           // ← Keep this (important for newest at bottom)
                contentPadding = PaddingValues(16.dp)
            ) {
                // No need to reverse the list manually
                items(messages) { message ->    // ← Use messages as-is (no .reversed())
                    when (message.type) {
                        "text" -> MessageBubble(
                            message = message,
                            isSentByCurrentUser = message.senderId == currentUser?.uid
                        )
                        "organization" -> message.orgId?.let { orgId ->
                            OrganizationCard(
                                orgId = orgId,
                                invitationId = message.id ?: "",
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

            // Message Input Bar
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
                        sendTextMessage(
                            text = messageText,
                            chatId = conversationId,           // or sortedConversationId
                            recipientId = recipientId ?: "",   // ← Important
                            currentUser = currentUser,
                            chatViewModel = chatViewModel,
                            onSuccess = { messageText = "" }
                        )
                    }),
                    enabled = !isSending
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        sendTextMessage(
                            text = messageText,
                            chatId = conversationId,
                            recipientId = recipientId ?: "",
                            currentUser = currentUser,
                            chatViewModel = chatViewModel,
                            onSuccess = { messageText = "" }
                        )
                    },
                    enabled = messageText.isNotBlank() && !isSending
                ) {
                    Text("Send")
                }
            }
        }
    }

    // Organization Selection Dialogs (kept mostly as-is)
    if (showOrgDialog) {
        AlertDialog(
            onDismissRequest = { showOrgDialog = false },
            title = { Text("Select Organization") },
            text = {
                if (organizations.isEmpty()) {
                    Text("No organizations found.")
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
                                            privateChatViewModel.sendOrganization(
                                                conversationId = conversationId,
                                                orgId = org.id,
                                                recipientType = recipientType!!
                                            )
                                            showOrgDialog = false
                                        }
                                    }
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOrgDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showTypeDialog && selectedOrg != null) {
        // Keep your existing type selection dialog logic or move it to ViewModel
        // For brevity, you can keep the current implementation here for now
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isSentByCurrentUser: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp),
        contentAlignment = if (isSentByCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 320.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSentByCurrentUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = if (isSentByCurrentUser) 16.dp else 4.dp,
                topEnd = if (isSentByCurrentUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Show sender name ONLY for received messages
                if (!isSentByCurrentUser && !message.senderName.isNullOrBlank()) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSentByCurrentUser)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Date + Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    val dateTime = remember(message.timestamp) {
                        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
                            .format(Date(message.timestamp))
                    }

                    Text(
                        text = dateTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSentByCurrentUser)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Helper function
private fun sendTextMessage(
    text: String,
    chatId: String,
    recipientId: String,
    currentUser: FirebaseUser?,
    chatViewModel: ChatViewModel,
    onSuccess: () -> Unit
) {
    if (text.isBlank() || currentUser == null) return

    // Fetch sender profile for real name
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val profileDoc = Firebase.firestore.collection("profiles")
                .document(currentUser.uid)
                .get()
                .await()

            val profile = profileDoc.toObject(UserProfile::class.java)
            val senderName = "${profile?.firstName ?: ""} ${profile?.lastName ?: ""}".trim()
                .ifBlank { currentUser.displayName ?: "You" }

            val message = ChatMessage(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = currentUser.uid,
                senderName = senderName,           // Real name from profile
                recipientId = recipientId,
                text = text,
                timestamp = System.currentTimeMillis(),
                type = "text"
            )

            withContext(Dispatchers.Main) {
                chatViewModel.sendMessage(message)
                onSuccess()
            }
        } catch (e: Exception) {
            Log.e("sendTextMessage", "Error fetching sender name", e)
            // Fallback
            val fallbackMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = currentUser.uid,
                senderName = "You",
                recipientId = recipientId,
                text = text,
                timestamp = System.currentTimeMillis(),
                type = "text"
            )
            chatViewModel.sendMessage(fallbackMessage)
            onSuccess()
        }
    }
}