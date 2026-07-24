package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.domain.model.Forum
import com.phoenixcorp.founderfinder.domain.model.Thread
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.ui.utils.fetchCurrentUserRole
import com.phoenixcorp.founderfinder.ui.utils.permissionsFor
import com.phoenixcorp.founderfinder.ui.components.ThreadCard
import com.phoenixcorp.founderfinder.ui.viewmodel.ForumViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumTemplateScreen(
    navController: NavHostController,
    institutionName: String, // Format: "categoryName/forumId"
    modifier: Modifier = Modifier,
    viewModel: ForumViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Spectator permissions
    var role by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        role = fetchCurrentUserRole()
    }
    val perms = remember(role) { permissionsFor(role) }

    // Parse from route (no hardcoding)
    val parts = institutionName.split("/")
    val category = parts.getOrNull(0)?.lowercase() ?: "requestedsolutions"
    val forumId = parts.getOrNull(1) ?: institutionName.lowercase()

    val forum by viewModel.forum.collectAsState()
    val threads by viewModel.threads.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var inputText by remember { mutableStateOf("") }

    // Load forum using the category from notification/route
    LaunchedEffect(category, forumId) {
        if (forumId.isNotBlank()) {
            Log.d("ForumTemplateScreen", "Loading forum → category=$category, forumId=$forumId")
            viewModel.loadForum(category, forumId)
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text(forum?.title?.ifBlank { forum?.name } ?: "Forum") },
                navController = navController,
                showBackButton = true
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = error ?: "Forum not found", color = MaterialTheme.colorScheme.error)
                }
            } else if (forum == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Forum not found")
                }
            } else {
                forum?.let { ForumHeader(forum = it) }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    if (threads.isEmpty()) {
                        item {
                            Text(
                                text = "No threads yet.\nBe the first to start a conversation!",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(32.dp)
                            )
                        }
                    } else {
                        items(threads) { thread ->
                            ThreadCard(
                                thread = thread,
                                navController = navController,
                                onFavorite = { _, _ ->
                                    if (!perms.requireEngage(context, "favorite")) return@ThreadCard
                                },
                                onLike = {
                                    if (!perms.requireEngage(context, "like")) return@ThreadCard
                                }
                            )
                        }
                    }
                }

                // Spectators can read threads but not post new ones
                if (perms.canCreate) {
                    NewThreadInput(
                        text = inputText,
                        onTextChange = { inputText = it },
                        onPost = {
                            if (!perms.requireCreate(context, "start a thread")) return@NewThreadInput
                            if (inputText.isNotBlank() && forum != null) {
                                viewModel.createThread(
                                    message = inputText,
                                    forumId = forumId,
                                    routeCategory = category,
                                    forumOwnerId = forum?.creatorId.orEmpty()
                                )
                                inputText = ""
                            } else {
                                Toast.makeText(context, "Please enter a message", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    Text(
                        text = "Spectators can view this forum but cannot post.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ==================== HELPER COMPOSABLES ====================

@Composable
private fun ForumHeader(forum: Forum) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = forum.description.ifBlank { forum.about.orEmpty() },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            forum.location?.let { loc ->
                if (loc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(loc, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun NewThreadInput(
    text: String,
    onTextChange: (String) -> Unit,
    onPost: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            label = { Text("Start a new thread") },
            modifier = Modifier.weight(1f),
            maxLines = 3
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onPost, enabled = text.isNotBlank()) {
            Text("Post")
        }
    }
}