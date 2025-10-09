package com.phoenixcorp.founderfinder.ui.screens

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.data.Forum
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.navigation.Screen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var forums by remember { mutableStateOf<List<Forum>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Philosophy of the Day
    val philosophy = getPhilosophyOfTheDay()

    // Fetch trending forums
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            Log.e("HomeScreen", "User not authenticated")
            errorMessage = "Please sign in to view forums."
            isLoading = false
            Toast.makeText(context, "Please sign in", Toast.LENGTH_SHORT).show()
            navController.navigate(Screen.SignIn.route) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
            return@LaunchedEffect
        }
        try {
            Log.d("HomeScreen", "Fetching trending forums for user: ${currentUser.uid}")
            val categories = listOf(
                "globalissues", "nationalissues", "localissues",
                "future", "marketpotential", "requestedsolutions"
            )
            val allForums = mutableListOf<Forum>()
            for (category in categories) {
                val snapshot = firestore.collection("category")
                    .document(category)
                    .collection("forum")
                    .get()
                    .await()
                val categoryForums = snapshot.documents.mapNotNull { doc ->
                    try {
                        Forum(
                            id = doc.id,
                            title = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            creatorId = doc.getString("creatorId") ?: "",
                            creatorName = doc.getString("creatorName") ?: "Anonymous",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            imageUrl = doc.getString("imageUrl"),
                            likes = doc.getLong("likes")?.toInt() ?: 0,
                            isFavorited = doc.getBoolean("isFavorited") ?: false,
                            hasLiked = currentUser?.let {
                                firestore.collection("category")
                                    .document(category)
                                    .collection("forum")
                                    .document(doc.id)
                                    .collection("likes")
                                    .document(it.uid)
                                    .get()
                                    .await()
                                    .exists()
                            } ?: false,
                            category = category
                        )
                    } catch (e: Exception) {
                        Log.e("HomeScreen", "Error parsing forum ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                allForums.addAll(categoryForums)
            }
            forums = allForums.sortedByDescending { it.likes }.take(10)
            isLoading = false
            Log.d("HomeScreen", "Fetched ${forums.size} trending forums")
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error fetching forums: ${e.message}", e)
            errorMessage = "Failed to load forums: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = { ScreenBanner(title = { Text("Home") }) },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Philosophy of the Day
            Text(
                text = "Philosophy of the Day",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = philosophy,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Trending Forums Section
            Text("Trending Forums", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (forums.isEmpty()) {
                Text(
                    text = "No trending forums available.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyRow {
                    items(forums) { forum ->
                        TrendingForumItem(forum, navController)
                    }
                }
            }
        }
    }
}

// Auto-generate Philosophy of the Day
@RequiresApi(Build.VERSION_CODES.O)
fun getPhilosophyOfTheDay(): String {
    val philosophies = listOf(
        "Success is not final, failure is not fatal: It is the courage to continue that counts. – Winston Churchill",
        "The only way to do great work is to love what you do. – Steve Jobs",
        "Innovation distinguishes between a leader and a follower. – Steve Jobs",
        "The best way to predict the future is to create it. – Peter Drucker",
        "Do not wait to strike till the iron is hot; but make it hot by striking. – William Butler Yeats",
        "The journey of a thousand miles begins with a single step. – Lao Tzu",
        "You miss 100% of the shots you don’t take. – Wayne Gretzky",
        "It’s not about ideas. It’s about making ideas happen. – Scott Belsky",
        "The only limit to our realization of tomorrow is our doubts of today. – Franklin D. Roosevelt",
        "Stay hungry, stay foolish. – Steve Jobs"
    )
    val dayOfYear = LocalDate.now(ZoneId.of("America/Edmonton")).dayOfYear
    return philosophies[dayOfYear % philosophies.size]
}

// Composable for trending forum items
@Composable
fun TrendingForumItem(forum: Forum, navController: NavHostController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val coroutineScope = rememberCoroutineScope()
    var localForum by remember { mutableStateOf(forum) }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .width(200.dp)
            .clickable {
                if (currentUser == null) {
                    Toast.makeText(context, "Please sign in to view forum", Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                    return@clickable
                }
                val route = Screen.InstitutionForum.createRoute(localForum.category, localForum.id)
                Log.d("TrendingForumItem", "Navigating to forum: $route")
                navController.navigate(route)
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = localForum.imageUrl?.let { rememberAsyncImagePainter(it) }
                    ?: painterResource(id = R.drawable.ic_placeholder),
                contentDescription = "Forum Image",
                modifier = Modifier
                    .size(50.dp)
                    .padding(bottom = 8.dp)
            )
            Text(
                text = localForum.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = localForum.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentUser == null) {
                            Toast.makeText(context, "Please sign in to like", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.SignIn.route)
                            return@IconButton
                        }
                        if (!localForum.hasLiked) {
                            coroutineScope.launch {
                                try {
                                    firestore.runTransaction { transaction ->
                                        val forumRef = firestore.collection("category")
                                            .document(localForum.category.lowercase())
                                            .collection("forum")
                                            .document(localForum.id)
                                        val likeRef = forumRef.collection("likes").document(currentUser.uid)
                                        val forumDoc = transaction.get(forumRef)
                                        val currentLikes = forumDoc.getLong("likes")?.toInt() ?: 0
                                        val newLikes = currentLikes + 1
                                        transaction.set(likeRef, mapOf("timestamp" to System.currentTimeMillis()))
                                        transaction.update(forumRef, "likes", newLikes)
                                        newLikes
                                    }.await()
                                    localForum = localForum.copy(likes = localForum.likes + 1, hasLiked = true)
                                    Toast.makeText(context, "Liked!", Toast.LENGTH_SHORT).show()
                                    Log.d("TrendingForumItem", "Successfully liked forum ${localForum.id}, new likes: ${localForum.likes}")
                                } catch (e: Exception) {
                                    Log.e("TrendingForumItem", "Transaction error liking forum: ${e.message}", e)
                                    Toast.makeText(context, "Error liking forum: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !localForum.hasLiked
                ) {
                    Icon(
                        imageVector = if (localForum.hasLiked) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like"
                    )
                }
                Text(
                    text = "${localForum.likes} Likes",
                    style = MaterialTheme.typography.bodySmall
                )
                IconButton(
                    onClick = {
                        if (currentUser == null) {
                            Toast.makeText(context, "Please sign in to favorite", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.SignIn.route)
                            return@IconButton
                        }
                        coroutineScope.launch {
                            try {
                                val newFavorited = !localForum.isFavorited
                                firestore.collection("category")
                                    .document(localForum.category.lowercase())
                                    .collection("forum")
                                    .document(localForum.id)
                                    .update("isFavorited", newFavorited)
                                    .await()
                                localForum = localForum.copy(isFavorited = newFavorited)
                                Toast.makeText(context, if (newFavorited) "Favorited!" else "Unfavorited!", Toast.LENGTH_SHORT).show()
                                Log.d("TrendingForumItem", "Favorited forum ${localForum.id}: $newFavorited")
                            } catch (e: Exception) {
                                Log.e("TrendingForumItem", "Error favoriting forum: ${e.message}", e)
                                Toast.makeText(context, "Error favoriting forum: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (localForum.isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite"
                    )
                }
            }
        }
    }
}