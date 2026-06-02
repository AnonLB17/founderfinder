package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.domain.model.Thread
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.ui.components.ThreadCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumTemplateScreen(
    navController: NavHostController,
    institutionName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestore = Firebase.firestore
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var inputText by remember { mutableStateOf("") }
    var threads by remember { mutableStateOf<List<Thread>>(emptyList()) }
    var commentsByThread by remember { mutableStateOf<Map<String, List<Comment>>>(emptyMap()) }
    var selectedThreadId by remember { mutableStateOf<String?>(null) }
    var selectedCommentId by remember { mutableStateOf<String?>(null) }
    var showCommentsMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var showRepliesMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var forumTitle by remember { mutableStateOf("Forum") }
    val scrollState = rememberScrollState()
    val defaultAvatarUrl = "https://via.placeholder.com/150"

    // Parse institutionName for category, forumId, and location
    val (category, forumId, location) = if (institutionName.contains(" -> ")) {
        val parts = institutionName.split(" -> ")
        Triple(parts[0].lowercase(), parts[1], parts.getOrNull(2))
    } else {
        val parts = institutionName.split("/")
        Triple(parts[0].lowercase(), parts.getOrNull(1) ?: institutionName.lowercase(), null)
    }

    // Fetch forum title
    LaunchedEffect(category, forumId) {
        try {
            Log.d("ForumTemplateScreen", "Fetching forum title from: /category/$category/forum/$forumId")
            val forumDoc = firestore.collection("category")
                .document(category)
                .collection("forum")
                .document(forumId)
                .get()
                .await()
            forumTitle = forumDoc.getString("name")?.replaceFirstChar { it.uppercase() } ?: "Untitled Forum"
            Log.d("ForumTemplateScreen", "Fetched forum title: $forumTitle")
        } catch (e: Exception) {
            Log.e("ForumTemplateScreen", "Error fetching forum title: ${e.message}", e)
            forumTitle = "Forum"
        }
    }

    // Fetch threads
    LaunchedEffect(category, forumId, location) {
        Log.d("ForumTemplateScreen", "LaunchedEffect triggered for category=$category, forumId=$forumId, location=$location")
        if (currentUser == null) {
            errorMessage = "You must be logged in to view the forum."
            isLoading = false
            navController.navigate(Screen.SignIn.route) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
            return@LaunchedEffect
        }
        try {
            currentUser.getIdToken(true).await()
            Log.d("ForumTemplateScreen", "Fetching threads from: /category/$category/forum/$forumId/threads")
            firestore.collection("category")
                .document(category)
                .collection("forum")
                .document(forumId)
                .collection("threads")
                .apply {
                    if (location != null) whereEqualTo("location", location)
                }
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ForumTemplateScreen", "Snapshot listener error: ${error.message}", error)
                        errorMessage = "Failed to load threads: ${error.message}"
                        isLoading = false
                        return@addSnapshotListener
                    }
                    Log.d("ForumTemplateScreen", "Snapshot received: documentCount=${snapshot?.documents?.size}")
                    if (snapshot != null) {
                        threads = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data ?: return@mapNotNull null
                                Thread(
                                    id = doc.id,
                                    creatorId = data["creatorId"] as? String ?: "",
                                    creatorName = data["creatorName"] as? String ?: "Anonymous",
                                    creatorProfilePicture = data["creatorProfilePicture"] as? String ?: defaultAvatarUrl,
                                    message = data["message"] as? String ?: "",
                                    timestamp = (data["timestamp"] as? Long) ?: 0L,
                                    likes = (data["likes"] as? Long) ?: 0L,
                                    isFavorited = data["isFavorited"] as? Boolean ?: false,
                                    institutionName = forumId,
                                    imageUrl = data["imageUrl"] as? String,
                                    location = data["location"] as? String,
                                    topicHeader = data["topicHeader"] as? String
                                )
                            } catch (e: Exception) {
                                Log.e("ForumTemplateScreen", "Error parsing thread ${doc.id}: ${e.message}", e)
                                null
                            }
                        }
                        isLoading = false
                        Log.d("ForumTemplateScreen", "Fetched ${threads.size} threads")
                    } else {
                        Log.w("ForumTemplateScreen", "Thread snapshot is null")
                        isLoading = false
                    }
                }
        } catch (e: Exception) {
            Log.e("ForumTemplateScreen", "Error initializing threads: ${e.message}", e)
            errorMessage = "Failed to load threads: ${e.message}"
            isLoading = false
        }
    }

    // Fetch comments for selected thread
    LaunchedEffect(selectedThreadId) {
        if (selectedThreadId != null) {
            try {
                firestore.collection("category")
                    .document(category)
                    .collection("forum")
                    .document(forumId)
                    .collection("threads")
                    .document(selectedThreadId!!)
                    .collection("comments")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("ForumTemplateScreen", "Error fetching comments: ${error.message}", error)
                            errorMessage = "Failed to load comments: ${error.message}"
                            return@addSnapshotListener
                        }
                        Log.d("ForumTemplateScreen", "Comment snapshot received: threadId=$selectedThreadId, documentCount=${snapshot?.documents?.size}")
                        if (snapshot != null) {
                            val newComments = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val data = doc.data ?: return@mapNotNull null
                                    Comment(
                                        id = doc.id,
                                        creatorId = data["creatorId"] as? String ?: "",
                                        creatorName = data["creatorName"] as? String ?: "Anonymous",
                                        creatorProfilePicture = data["creatorProfilePicture"] as? String ?: defaultAvatarUrl,
                                        message = data["message"] as? String ?: "",
                                        timestamp = (data["timestamp"] as? Long) ?: 0L,
                                        parentId = data["parentId"] as? String,
                                        depth = (data["depth"] as? Long)?.toInt() ?: 1,
                                        isFavorited = data["isFavorited"] as? Boolean ?: false,
                                        likes = (data["likes"] as? Long) ?: 0L
                                    )
                                } catch (e: Exception) {
                                    Log.e("ForumTemplateScreen", "Error parsing comment ${doc.id}: ${e.message}", e)
                                    null
                                }
                            }
                            commentsByThread = commentsByThread.toMutableMap().apply {
                                this[selectedThreadId!!] = newComments
                            }
                            Log.d("ForumTemplateScreen", "Fetched ${newComments.size} comments for thread $selectedThreadId")
                        } else {
                            Log.w("ForumTemplateScreen", "Comment snapshot is null for thread $selectedThreadId")
                            commentsByThread = commentsByThread.toMutableMap().apply {
                                this[selectedThreadId!!] = emptyList()
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("ForumTemplateScreen", "Error initializing comments: ${e.message}", e)
                errorMessage = "Failed to load comments: ${e.message}"
            }
        } else {
            commentsByThread = emptyMap()
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text(forumTitle) },
                navController = navController,
                showBackButton = true
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { selectedThreadId = null; selectedCommentId = null },
                        onDoubleTap = { selectedThreadId = null; selectedCommentId = null }
                    )
                }
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(8.dp)
                ) {
                    if (threads.isEmpty()) {
                        Text(
                            text = "No threads yet. Start a conversation!",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    threads.forEach { thread ->
                        ThreadCard(
                            thread = thread.copy(
                                creatorName = thread.creatorName.takeIf { !it.isNullOrBlank() } ?: "Anonymous",
                                creatorProfilePicture = thread.creatorProfilePicture.takeIf { !it.isNullOrBlank() } ?: defaultAvatarUrl
                            ),
                            showComments = showCommentsMap[thread.id] ?: false,
                            comments = commentsByThread[thread.id] ?: emptyList(),
                            showRepliesMap = showRepliesMap,
                            onToggleComments = { threadId, show ->
                                showCommentsMap = showCommentsMap.toMutableMap().apply { this[threadId] = show }
                                selectedThreadId = if (show) threadId else selectedThreadId
                                Log.d("ForumTemplateScreen", "Toggled comments for thread $threadId: show=$show")
                            },
                            onToggleReplies = { commentId, show ->
                                showRepliesMap = showRepliesMap.toMutableMap().apply { this[commentId] = show }
                            },
                            onCommentClick = { commentId ->
                                selectedCommentId = commentId
                                Log.d("ForumTemplateScreen", "Selected comment: $commentId")
                            },
                            onFavorite = { id, isFavorited ->
                                coroutineScope.launch {
                                    try {
                                        if (threads.any { it.id == id }) {
                                            firestore.collection("category")
                                                .document(category)
                                                .collection("forum")
                                                .document(forumId)
                                                .collection("threads")
                                                .document(id)
                                                .update("isFavorited", isFavorited)
                                                .await()
                                            threads = threads.map { t ->
                                                if (t.id == id) t.copy(isFavorited = isFavorited) else t
                                            }
                                        } else if (commentsByThread[selectedThreadId]?.any { it.id == id } == true) {
                                            firestore.collection("category")
                                                .document(category)
                                                .collection("forum")
                                                .document(forumId)
                                                .collection("threads")
                                                .document(selectedThreadId!!)
                                                .collection("comments")
                                                .document(id)
                                                .update("isFavorited", isFavorited)
                                                .await()
                                            commentsByThread = commentsByThread.toMutableMap().apply {
                                                this[selectedThreadId!!] = this[selectedThreadId!!]?.map { c ->
                                                    if (c.id == id) c.copy(isFavorited = isFavorited) else c
                                                } ?: emptyList()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ForumTemplateScreen", "Error favoriting: ${e.message}", e)
                                        Toast.makeText(context, "Error favoriting: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onLike = { id ->
                                coroutineScope.launch {
                                    try {
                                        if (threads.any { it.id == id }) {
                                            val snapshot = firestore.collection("category")
                                                .document(category)
                                                .collection("forum")
                                                .document(forumId)
                                                .collection("threads")
                                                .document(id)
                                                .get()
                                                .await()
                                            val currentLikes = snapshot.getLong("likes") ?: 0
                                            firestore.collection("category")
                                                .document(category)
                                                .collection("forum")
                                                .document(forumId)
                                                .collection("threads")
                                                .document(id)
                                                .update("likes", currentLikes + 1)
                                                .await()
                                            threads = threads.map { t ->
                                                if (t.id == id) t.copy(likes = currentLikes + 1) else t
                                            }
                                        } else if (commentsByThread[selectedThreadId]?.any { it.id == id } == true) {
                                            val snapshot = firestore.collection("category")
                                                .document(category)
                                                .collection("forum")
                                                .document(forumId)
                                                .collection("threads")
                                                .document(selectedThreadId!!)
                                                .collection("comments")
                                                .document(id)
                                                .get()
                                                .await()
                                            val currentLikes = snapshot.getLong("likes") ?: 0
                                            firestore.collection("category")
                                                .document(category)
                                                .collection("forum")
                                                .document(forumId)
                                                .collection("threads")
                                                .document(selectedThreadId!!)
                                                .collection("comments")
                                                .document(id)
                                                .update("likes", currentLikes + 1)
                                                .await()
                                            commentsByThread = commentsByThread.toMutableMap().apply {
                                                this[selectedThreadId!!] = this[selectedThreadId!!]?.map { c ->
                                                    if (c.id == id) c.copy(likes = currentLikes + 1) else c
                                                } ?: emptyList()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ForumTemplateScreen", "Error liking: ${e.message}", e)
                                        Toast.makeText(context, "Error liking: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            navController = navController
                        )
                    }
                }

                // Bottom input field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text(if (selectedThreadId == null) "Start a new thread" else if (selectedCommentId == null) "Add a comment" else "Reply to comment") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            coroutineScope.launch {
                                val currentUser = auth.currentUser
                                if (currentUser == null) {
                                    Toast.makeText(context, "Please sign in to post", Toast.LENGTH_SHORT).show()
                                    navController.navigate(Screen.SignIn.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                    return@launch
                                }
                                if (inputText.isBlank()) {
                                    Toast.makeText(context, "Input cannot be empty", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                Log.d("ForumTemplateScreen", "Posting: threadId=$selectedThreadId, commentId=$selectedCommentId, inputText=$inputText, user=${currentUser.uid}")
                                try {
                                    val profileDoc = firestore.collection("users")
                                        .document(currentUser.uid)
                                        .get()
                                        .await()
                                    val profile = profileDoc.toObject(UserProfile::class.java)
                                    val creatorName = when {
                                        profile?.firstName.isNullOrBlank() && profile?.lastName.isNullOrBlank() -> "Anonymous"
                                        profile?.firstName.isNullOrBlank() -> profile?.lastName ?: "Anonymous"
                                        profile?.lastName.isNullOrBlank() -> profile?.firstName ?: "Anonymous"
                                        else -> "${profile?.firstName} ${profile?.lastName}"
                                    }
                                    val creatorProfilePicture = profile?.profilePicture?.takeIf { it.isNotBlank() } ?: defaultAvatarUrl

                                    if (selectedThreadId == null) {
                                        val newThreadId = UUID.randomUUID().toString()
                                        val newThread = Thread(
                                            id = newThreadId,
                                            creatorId = currentUser.uid,
                                            creatorName = creatorName,
                                            creatorProfilePicture = creatorProfilePicture,
                                            message = inputText,
                                            timestamp = System.currentTimeMillis(),
                                            likes = 0,
                                            isFavorited = false,
                                            institutionName = forumId,
                                            location = location
                                        )
                                        firestore.collection("category")
                                            .document(category)
                                            .collection("forum")
                                            .document(forumId)
                                            .collection("threads")
                                            .document(newThreadId)
                                            .set(newThread)
                                            .await()
                                        threads = threads + newThread
                                        selectedThreadId = newThreadId
                                        showCommentsMap = showCommentsMap.toMutableMap().apply {
                                            this[newThreadId] = true
                                        }
                                        Toast.makeText(context, "Thread created successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val comment = Comment(
                                            id = UUID.randomUUID().toString(),
                                            creatorId = currentUser.uid,
                                            creatorName = creatorName,
                                            creatorProfilePicture = creatorProfilePicture,
                                            message = inputText,
                                            timestamp = System.currentTimeMillis(),
                                            parentId = selectedCommentId,
                                            depth = if (selectedCommentId == null) 1 else (commentsByThread[selectedThreadId]?.find { it.id == selectedCommentId }?.depth?.plus(1) ?: 1),
                                            isFavorited = false,
                                            likes = 0
                                        )
                                        firestore.collection("category")
                                            .document(category)
                                            .collection("forum")
                                            .document(forumId)
                                            .collection("threads")
                                            .document(selectedThreadId!!)
                                            .collection("comments")
                                            .document(comment.id)
                                            .set(comment)
                                            .await()
                                        commentsByThread = commentsByThread.toMutableMap().apply {
                                            this[selectedThreadId!!] = (this[selectedThreadId!!] ?: emptyList()) + comment
                                        }
                                        Toast.makeText(context, "Comment posted successfully", Toast.LENGTH_SHORT).show()
                                    }
                                    inputText = ""
                                    selectedCommentId = null
                                } catch (e: Exception) {
                                    Log.e("ForumTemplateScreen", "Error posting: ${e.message}", e)
                                    Toast.makeText(context, "Error posting: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val currentUser = auth.currentUser
                                if (currentUser == null) {
                                    Toast.makeText(context, "Please sign in to post", Toast.LENGTH_SHORT).show()
                                    navController.navigate(Screen.SignIn.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                    return@launch
                                }
                                if (inputText.isBlank()) {
                                    Toast.makeText(context, "Input cannot be empty", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                Log.d("ForumTemplateScreen", "Posting: threadId=$selectedThreadId, commentId=$selectedCommentId, inputText=$inputText, user=${currentUser.uid}")
                                try {
                                    val profileDoc = firestore.collection("users")
                                        .document(currentUser.uid)
                                        .get()
                                        .await()
                                    val profile = profileDoc.toObject(UserProfile::class.java)
                                    val creatorName = when {
                                        profile?.firstName.isNullOrBlank() && profile?.lastName.isNullOrBlank() -> "Anonymous"
                                        profile?.firstName.isNullOrBlank() -> profile?.lastName ?: "Anonymous"
                                        profile?.lastName.isNullOrBlank() -> profile?.firstName ?: "Anonymous"
                                        else -> "${profile?.firstName} ${profile?.lastName}"
                                    }
                                    val creatorProfilePicture = profile?.profilePicture?.takeIf { it.isNotBlank() } ?: defaultAvatarUrl

                                    if (selectedThreadId == null) {
                                        val newThreadId = UUID.randomUUID().toString()
                                        val newThread = Thread(
                                            id = newThreadId,
                                            creatorId = currentUser.uid,
                                            creatorName = creatorName,
                                            creatorProfilePicture = creatorProfilePicture,
                                            message = inputText,
                                            timestamp = System.currentTimeMillis(),
                                            likes = 0,
                                            isFavorited = false,
                                            institutionName = forumId,
                                            location = location
                                        )
                                        firestore.collection("category")
                                            .document(category)
                                            .collection("forum")
                                            .document(forumId)
                                            .collection("threads")
                                            .document(newThreadId)
                                            .set(newThread)
                                            .await()
                                        threads = threads + newThread
                                        selectedThreadId = newThreadId
                                        showCommentsMap = showCommentsMap.toMutableMap().apply {
                                            this[newThreadId] = true
                                        }
                                        Toast.makeText(context, "Thread created successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val comment = Comment(
                                            id = UUID.randomUUID().toString(),
                                            creatorId = currentUser.uid,
                                            creatorName = creatorName,
                                            creatorProfilePicture = creatorProfilePicture,
                                            message = inputText,
                                            timestamp = System.currentTimeMillis(),
                                            parentId = selectedCommentId,
                                            depth = if (selectedCommentId == null) 1 else (commentsByThread[selectedThreadId]?.find { it.id == selectedCommentId }?.depth?.plus(1) ?: 1),
                                            isFavorited = false,
                                            likes = 0
                                        )
                                        firestore.collection("category")
                                            .document(category)
                                            .collection("forum")
                                            .document(forumId)
                                            .collection("threads")
                                            .document(selectedThreadId!!)
                                            .collection("comments")
                                            .document(comment.id)
                                            .set(comment)
                                            .await()
                                        commentsByThread = commentsByThread.toMutableMap().apply {
                                            this[selectedThreadId!!] = (this[selectedThreadId!!] ?: emptyList()) + comment
                                        }
                                        Toast.makeText(context, "Comment posted successfully", Toast.LENGTH_SHORT).show()
                                    }
                                    inputText = ""
                                    selectedCommentId = null
                                } catch (e: Exception) {
                                    Log.e("ForumTemplateScreen", "Error posting: ${e.message}", e)
                                    Toast.makeText(context, "Error posting: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = inputText.isNotBlank()
                    ) {
                        Text("Post")
                    }
                }
            }
        }
    }
}