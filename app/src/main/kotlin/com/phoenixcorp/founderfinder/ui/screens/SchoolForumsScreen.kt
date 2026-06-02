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
import com.phoenixcorp.founderfinder.ui.components.ForumCard
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()
    val institutions = listOf(
        // United States
        "stanford", "mit", "harvard", "ucla", "ucberkeley", "princeton", "yale", "columbia",
        "upenn", "uchicago", "caltech", "duke", "northwestern", "johnshopkins", "cornell",
        "nyu", "umich", "usc", "vanderbilt", "emory", "georgetown", "unc", "carnegiemellon",
        "uillinois", "uwashington", "uwmadison", "ucsd", "purdue", "umass", "northeastern",
        "bostonu", "rutgers", "brown", "dartmouth", "rice", "washu", "sdsu", "sfsu",
        "ccsf", "lacc", "santamonica", // Community colleges
        // Canada
        "utoronto", "mcgill", "ubc", "ualberta", "uottawa", "mcmaster", "waterloo", "uwestern",
        "queensu", "ubcokanagan", "sfu", "dalhousie", "uvic", "concordia", "ryerson",
        "carleton", "yorku", "uwinnipeg", "umanitoba", "uregina",
        // Mexico
        "unam", "tecmonterrey", "ipn", "udeg", "uam", "anahuac", "ibero", "upmx",
        "iteso", "cetys"
    )
    val category = "institutions"

    // Fetch forums
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
                    // Validate title and id
                    if (title.isBlank() || id.isBlank()) {
                        Log.w("SchoolForumsScreen", "Invalid forum data: id=$id, title=$title")
                        return@mapNotNull null
                    }
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
            // Ensure all institutions have a forum document
            val existingForumIds = forums.map { it.id }
            institutions.forEach { school ->
                if (school !in existingForumIds) {
                    try {
                        val forumData = mapOf(
                            "name" to school.uppercase(),
                            "description" to "Forum for $school",
                            "imageUrl" to "https://via.placeholder.com/150",
                            "creatorId" to "system",
                            "creatorName" to "System",
                            "timestamp" to System.currentTimeMillis(),
                            "likes" to 0,
                            "isFavorited" to false,
                            "category" to category,
                            "location" to null
                        )
                        firestore.collection("category")
                            .document("institutions")
                            .collection("forum")
                            .document(school)
                            .set(forumData)
                            .await()
                        forums = forums + Forum(
                            id = school,
                            title = school.uppercase(),
                            description = "Forum for $school",
                            creatorId = "system",
                            creatorName = "System",
                            timestamp = System.currentTimeMillis(),
                            imageUrl = "https://via.placeholder.com/150",
                            likes = 0,
                            isFavorited = false,
                            hasLiked = false,
                            category = category,
                            location = null
                        )
                        Log.d("SchoolForumsScreen", "Created forum for $school")
                    } catch (e: Exception) {
                        Log.e("SchoolForumsScreen", "Error creating forum for $school: ${e.message}", e)
                    }
                }
            }
            filteredForums = forums
            isLoading = false
            Log.d("SchoolForumsScreen", "Fetched ${forums.size} forums")
        } catch (e: Exception) {
            Log.e("SchoolForumsScreen", "Error fetching forums: ${e.message}", e)
            errorMessage = "Failed to load forums: ${e.message}"
            isLoading = false
        }
    }

    // Debounced search filtering
    val debouncedSearchQuery by produceState(initialValue = searchQuery, key1 = searchQuery) {
        isSearching = true
        delay(300) // Debounce for 300ms
        value = searchQuery.trim() // Trim whitespace
        isSearching = false
        Log.d("SchoolForumsScreen", "Debounced search query: $value")
    }

    // Filter forums based on debounced search query
    LaunchedEffect(debouncedSearchQuery) {
        filteredForums = if (debouncedSearchQuery.isBlank()) {
            forums
        } else {
            val queryLower = debouncedSearchQuery.lowercase()
            forums.filter { forum ->
                val titleMatch = forum.title.lowercase().contains(queryLower)
                val idMatch = forum.id.lowercase().contains(queryLower)
                Log.d("SchoolForumsScreen", "Forum ${forum.id}: title=${forum.title}, titleMatch=$titleMatch, idMatch=$idMatch")
                titleMatch || idMatch
            }
        }
        Log.d("SchoolForumsScreen", "Filtered ${filteredForums.size} forums for query: $debouncedSearchQuery")
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
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Institutions") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
                singleLine = true
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading forums...",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                isSearching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Searching...",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                filteredForums.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No forums found" else "No forums match \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredForums) { forum ->
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
}