package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.Forum
import com.phoenixcorp.founderfinder.ui.components.SchoolForumCard
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolForumsScreen(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var forums by remember { mutableStateOf<List<Forum>>(emptyList()) }
    var filteredForums by remember { mutableStateOf<List<Forum>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val category = "institutions"

    // Load institution forums from Firestore only
    LaunchedEffect(Unit) {
        try {
            Log.d("SchoolForumsScreen", "Fetching forums from: /category/institutions/forum")
            val snapshot = firestore.collection("category")
                .document("institutions")
                .collection("forum")
                .get()
                .await()
            forums = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val title = data["name"] as? String ?: "Untitled"
                    val id = doc.id
                    if (title.isBlank() || id.isBlank()) return@mapNotNull null
                    Forum(
                        id = id,
                        title = title,
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
                    Log.e("SchoolForumsScreen", "Error parsing forum ${doc.id}: ${e.message}", e)
                    null
                }
            }

            // Sort by full name for nicer browsing
            forums = forums.sortedBy { it.title.lowercase() }
            filteredForums = forums
            isLoading = false
            Log.d("SchoolForumsScreen", "Fetched ${forums.size} forums")
        } catch (e: Exception) {
            Log.e("SchoolForumsScreen", "Error fetching forums: ${e.message}", e)
            errorMessage = "Failed to load forums: ${e.message}"
            isLoading = false
        }
    }

    val debouncedSearchQuery by produceState(initialValue = searchQuery, key1 = searchQuery) {
        isSearching = true
        delay(300)
        value = searchQuery.trim()
        isSearching = false
    }

    LaunchedEffect(debouncedSearchQuery, forums) {
        filteredForums = if (debouncedSearchQuery.isBlank()) {
            forums
        } else {
            val q = debouncedSearchQuery.lowercase()
            forums.filter { forum ->
                forum.title.lowercase().contains(q) || forum.id.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("School Forums") },
                navController = navController,
                showBackButton = true
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Find your campus community",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Universities and colleges across North America",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search institutions…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            if (!isLoading && errorMessage == null) {
                Text(
                    text = if (searchQuery.isBlank()) {
                        "${forums.size} institutions"
                    } else {
                        "${filteredForums.size} results"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                filteredForums.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "No institutions yet"
                            } else {
                                "No institutions match \"$searchQuery\""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = filteredForums,
                            key = { forum -> forum.id }
                        ) { forum ->
                            SchoolForumCard(
                                forum = forum,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}