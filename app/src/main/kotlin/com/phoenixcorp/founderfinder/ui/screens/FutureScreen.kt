package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.data.Forum
import com.phoenixcorp.founderfinder.ui.components.ForumCard
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutureScreen(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var forums by remember { mutableStateOf<List<Forum>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val category = "future"

    LaunchedEffect(Unit) {
        try {
            Log.d("FutureScreen", "Fetching forums from: /category/$category/forum")
            val snapshot = firestore.collection("category")
                .document(category)
                .collection("forum")
                .get()
                .await()
            forums = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    Forum(
                        id = doc.id,
                        title = data["name"] as? String ?: "Untitled",
                        description = data["description"] as? String ?: "",
                        creatorId = data["creatorId"] as? String ?: "",
                        creatorName = data["creatorName"] as? String ?: "Anonymous",
                        timestamp = (data["timestamp"] as? Long) ?: 0L,
                        imageUrl = data["imageUrl"] as? String,
                        likes = data["likes"]?.toString()?.toIntOrNull() ?: 0,
                        isFavorited = data["isFavorited"] as? Boolean ?: false,
                        hasLiked = if (currentUser != null) {
                            firestore.collection("category")
                                .document(category)
                                .collection("forum")
                                .document(doc.id)
                                .collection("likes")
                                .document(currentUser.uid)
                                .get()
                                .await()
                                .exists()
                        } else false,
                        category = category,
                        location = data["location"] as? String
                    )
                } catch (e: Exception) {
                    Log.e("FutureScreen", "Error parsing forum ${doc.id}: ${e.message}", e)
                    null
                }
            }
            isLoading = false
            Log.d("FutureScreen", "Fetched ${forums.size} forums")
        } catch (e: Exception) {
            Log.e("FutureScreen", "Error fetching forums: ${e.message}", e)
            errorMessage = "Failed to load forums: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Future Forums") },
                navController = navController,
                showBackButton = true
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
            forums.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No forums found",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(forums) { forum ->
                        ForumCard(
                            forum = forum,
                            navController = navController,
                            category = category,
                            forumId = forum.id
                        )
                    }
                }
            }
        }
    }
}