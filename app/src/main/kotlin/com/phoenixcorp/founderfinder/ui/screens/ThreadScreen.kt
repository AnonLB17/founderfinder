package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.CommentCard
import com.phoenixcorp.founderfinder.ui.viewmodel.ThreadViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    threadId: String,
    forumId: String,
    category: String = "marketpotential",
    navController: NavHostController
) {
    val viewModel: ThreadViewModel = hiltViewModel()
    val thread by viewModel.thread.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var inputText by remember { mutableStateOf("") }
    var replyToId by remember { mutableStateOf<String?>(null) }

    val expandedComments = remember { mutableStateMapOf<String, Boolean>() }

    // Load thread
    LaunchedEffect(forumId, threadId) {
        if (forumId.isNotBlank() && threadId.isNotBlank()) {
            val correctCategory = "requestedsolutions"
            viewModel.loadThread(correctCategory, forumId, threadId)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Thread") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // === PINNED ORIGINAL THREAD (Clickable to Reply) ===
            thread?.let { t ->
                var displayName by remember { mutableStateOf(t.creatorName ?: "Anonymous") }
                var displayProfilePic by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(t.creatorId) {
                    if ((t.creatorName.isNullOrBlank() || t.creatorName == "Anonymous") && t.creatorId.isNotEmpty()) {
                        try {
                            val doc = FirebaseFirestore.getInstance()
                                .collection("profiles")
                                .document(t.creatorId)
                                .get()
                                .await()
                            if (doc.exists()) {
                                val first = doc.getString("firstName") ?: ""
                                val last = doc.getString("lastName") ?: ""
                                displayName = "$first $last".trim().ifBlank { "Anonymous" }
                                displayProfilePic = doc.getString("profilePicture")
                            }
                        } catch (e: Exception) {
                            Log.e("ThreadScreen", "Failed to load thread creator profile", e)
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp)
                        .clickable {
                            // Clicking the pinned thread starts a top-level reply
                            replyToId = null
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Original Post",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = displayProfilePic?.let { rememberAsyncImagePainter(it) }
                                    ?: painterResource(id = R.drawable.ic_profile_placeholder),
                                contentDescription = "Creator",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { navController.navigate(Screen.UserProfile.createRoute(t.creatorId)) },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { navController.navigate(Screen.UserProfile.createRoute(t.creatorId)) }
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = t.message.ifBlank { "No message" },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            } ?: Text("Loading thread...", modifier = Modifier.padding(16.dp))

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Conversation (${comments.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(comments.filter { it.parentId == null }) { rootComment ->
                    CommentCard(
                        comment = rootComment,
                        depth = 0,
                        showReplies = expandedComments[rootComment.id] ?: false,
                        showRepliesMap = expandedComments,
                        comments = comments,
                        onCommentClick = { },
                        onToggleReplies = { id, expand ->
                            expandedComments[id] = expand
                        },
                        onReplyToComment = { id -> replyToId = id },
                        onFavorite = { _, _ -> },
                        onLike = { },
                        navController = navController
                    )
                }
            }

            // Input Field
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = {
                            Text(if (replyToId != null) "Reply to comment..." else "Add a comment...")
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            postComment(inputText, threadId, forumId, category, auth, context, viewModel, replyToId) {
                                inputText = ""
                                replyToId = null
                            }
                        })
                    )

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            postComment(inputText, threadId, forumId, category, auth, context, viewModel, replyToId) {
                                inputText = ""
                                replyToId = null
                            }
                        },
                        enabled = inputText.isNotBlank()
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}

// postComment function (unchanged)
private fun postComment(
    inputText: String,
    threadId: String,
    forumId: String,
    category: String,
    auth: FirebaseAuth,
    context: android.content.Context,
    viewModel: ThreadViewModel,
    parentId: String?,
    onSuccess: () -> Unit
) {
    if (inputText.isBlank() || threadId.isBlank() || forumId.isBlank()) return

    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Please sign in", Toast.LENGTH_SHORT).show()
            return@launch
        }

        val newComment = Comment(
            id = UUID.randomUUID().toString(),
            creatorId = currentUser.uid,
            creatorName = currentUser.displayName ?: "Anonymous",
            creatorProfilePicture = currentUser.photoUrl?.toString() ?: "",
            message = inputText,
            timestamp = System.currentTimeMillis(),
            threadId = threadId,
            forumId = forumId,
            category = category,
            parentId = parentId,
            depth = if (parentId == null) 1 else 2
        )

        viewModel.createComment(newComment)
        onSuccess()
        Toast.makeText(context, "Comment posted", Toast.LENGTH_SHORT).show()
    }
}