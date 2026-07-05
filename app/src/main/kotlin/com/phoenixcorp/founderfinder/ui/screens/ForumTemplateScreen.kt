package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.domain.model.Thread
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.ui.components.ThreadCard
import com.phoenixcorp.founderfinder.ui.viewmodel.ForumViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumTemplateScreen(
    navController: NavHostController,
    institutionName: String,
    modifier: Modifier = Modifier,
    viewModel: ForumViewModel = hiltViewModel()
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
    var forumDescription by remember { mutableStateOf("No description available.") }   // ← Added
    var forumLocation by remember { mutableStateOf<String?>(null) }                   // ← Added
    var forumOwnerId by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val defaultAvatarUrl = "https://via.placeholder.com/150"

    // Parse institutionName
    val (category, forumId, locationParam) = if (institutionName.contains("/")) {
        val parts = institutionName.split("/")
        Triple(parts[0].lowercase(), parts.getOrNull(1) ?: institutionName.lowercase(), null)
    } else {
        Triple("requestedsolutions", institutionName.lowercase(), null)
    }

    // Fetch forum metadata
    LaunchedEffect(category, forumId) {
        try {
            Log.d("ForumTemplateScreen", "Trying to load forum: category=$category, forumId=$forumId")

            // Try the main path
            var forumDoc = firestore.collection("category")
                .document(category)
                .collection("forum")
                .document(forumId)
                .get()
                .await()

            if (!forumDoc.exists()) {
                Log.w("ForumTemplateScreen", "Forum not found in /category/... Trying alternative paths")
                // Try alternative path if needed
                forumDoc = firestore.collection("forums")
                    .document(category)
                    .collection("forums")
                    .document(forumId)
                    .get()
                    .await()
            }

            if (forumDoc.exists()) {
                forumTitle = forumDoc.getString("name")?.replaceFirstChar { it.uppercase() } ?: "Untitled Forum"
                forumDescription = forumDoc.getString("description") ?: forumDoc.getString("about") ?: "No description available."
                forumLocation = forumDoc.getString("location")
                forumOwnerId = forumDoc.getString("creatorId")

                Log.d("ForumTemplateScreen", "✅ Loaded forum - Title: $forumTitle")
            } else {
                Log.w("ForumTemplateScreen", "Forum document still not found!")
                forumTitle = "Forum Not Found"
            }
        } catch (e: Exception) {
            Log.e("ForumTemplateScreen", "Error fetching forum metadata", e)
            forumTitle = "Error Loading Forum"
        }
    }

    // Thread Action Callbacks
    val onThreadFavorite: (String, Boolean) -> Unit = { threadId, isFavoriting ->
        coroutineScope.launch {
            try {
                val threadRef = firestore.collection("category")
                    .document(category)
                    .collection("forum")
                    .document(forumId)
                    .collection("threads")
                    .document(threadId)

                threadRef.update("isFavorited", isFavoriting).await()

                threads = threads.map { thread ->
                    if (thread.id == threadId) thread.copy(isFavorited = isFavoriting) else thread
                }
            } catch (e: Exception) {
                Log.e("ForumTemplateScreen", "Failed to update thread favorite", e)
            }
        }
    }

    val onThreadLike: (String) -> Unit = { threadId ->
        coroutineScope.launch {
            val currentUserId = auth.currentUser?.uid ?: return@launch
            try {
                val threadRef = firestore.collection("category")
                    .document(category)
                    .collection("forum")
                    .document(forumId)
                    .collection("threads")
                    .document(threadId)

                val snap = threadRef.get().await()
                val likedBy = snap.get("likedBy") as? List<String> ?: emptyList()

                if (likedBy.contains(currentUserId)) {
                    threadRef.update(
                        mapOf(
                            "likes" to FieldValue.increment(-1),
                            "likedBy" to FieldValue.arrayRemove(currentUserId)
                        )
                    ).await()
                } else {
                    threadRef.update(
                        mapOf(
                            "likes" to FieldValue.increment(1),
                            "likedBy" to FieldValue.arrayUnion(currentUserId)
                        )
                    ).await()
                }
            } catch (e: Exception) {
                Log.e("ForumTemplateScreen", "Failed to toggle thread like", e)
            }
        }
    }

    // Post new thread
    fun postContent() {
        if (inputText.isBlank() || currentUser == null) return

        coroutineScope.launch {
            try {
                if (selectedThreadId == null) {
                    Log.d("ForumTemplateScreen", "Creating thread with category: $category")  // Add this

                    viewModel.createThread(
                        message = inputText,
                        forumId = forumId,
                        category = "requestedsolutions",   // ← Force correct category for now
                        forumOwnerId = forumOwnerId ?: ""
                    )
                } else {
                    Log.d("ForumTemplateScreen", "Comment posting not implemented yet")
                }

                inputText = ""
                selectedThreadId = null
                selectedCommentId = null
            } catch (e: Exception) {
                Log.e("ForumTemplateScreen", "Failed to post content", e)
                Toast.makeText(context, "Failed to post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fetch threads (keep your existing LaunchedEffect for threads)
    LaunchedEffect(category, forumId, locationParam) {
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
            firestore.collection("category")
                .document(category)
                .collection("forum")
                .document(forumId)
                .collection("threads")
                .apply {
                    if (locationParam != null) whereEqualTo("location", locationParam)
                }
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ForumTemplateScreen", "Snapshot listener error", error)
                        errorMessage = "Failed to load threads"
                        isLoading = false
                        return@addSnapshotListener
                    }

                    threads = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            Thread(
                                id = doc.id,
                                forumId = forumId,
                                category = category,
                                creatorId = data["creatorId"] as? String ?: "",
                                creatorName = data["creatorName"] as? String ?: "Anonymous",
                                creatorProfilePicture = data["creatorProfilePicture"] as? String ?: defaultAvatarUrl,
                                message = data["message"] as? String ?: "",
                                timestamp = (data["timestamp"] as? Long) ?: 0L,
                                likes = (data["likes"] as? Long) ?: 0L,
                                likedBy = (data["likedBy"] as? List<String>) ?: emptyList(),
                                isFavorited = data["isFavorited"] as? Boolean ?: false,
                                institutionName = forumId,
                                imageUrl = data["imageUrl"] as? String,
                                location = data["location"] as? String,
                                topicHeader = data["topicHeader"] as? String
                            )
                        } catch (e: Exception) {
                            Log.e("ForumTemplateScreen", "Error parsing thread ${doc.id}", e)
                            null
                        }
                    }?.sortedWith(
                        compareByDescending<Thread> { it.isFavorited }
                            .thenByDescending { it.timestamp }
                            .thenByDescending { it.likes }
                    ) ?: emptyList()

                    isLoading = false
                }
        } catch (e: Exception) {
            Log.e("ForumTemplateScreen", "Error initializing threads", e)
            errorMessage = "Failed to load threads"
            isLoading = false
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                // === PINNED FORUM HEADER WITH LOCATION ===
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = forumDescription,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )

                        forumLocation?.let { loc ->
                            if (loc.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Location",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = loc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Threads List
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
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    threads
                        .sortedWith(
                            compareByDescending<Thread> { it.isFavorited }
                                .thenByDescending { it.timestamp }
                                .thenByDescending { it.likes }
                        )
                        .forEach { thread ->
                            ThreadCard(
                                thread = thread.copy(
                                    creatorName = thread.creatorName.takeIf { !it.isNullOrBlank() } ?: "Anonymous",
                                    creatorProfilePicture = thread.creatorProfilePicture.takeIf { !it.isNullOrBlank() } ?: defaultAvatarUrl
                                ),
                                navController = navController,
                                onFavorite = onThreadFavorite,
                                onLike = onThreadLike
                            )
                        }
                }

                // Bottom Input Field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text(if (selectedThreadId == null) "Start a new thread" else "Add a comment") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) postContent()
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { postContent() },
                        enabled = inputText.isNotBlank()
                    ) {
                        Text("Post")
                    }
                }
            }
        }
    }
}