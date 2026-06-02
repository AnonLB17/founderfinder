package com.phoenixcorp.founderfinder.ui.components

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.Forum
import com.phoenixcorp.founderfinder.navigation.Screen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ForumCard(
    forum: Forum,
    navController: NavHostController,
    category: String?,
    forumId: String
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val coroutineScope = rememberCoroutineScope()
    var localForum by remember { mutableStateOf(forum) }
    var hasLiked by remember { mutableStateOf(forum.hasLiked) }

    // Check if user has liked the forum
    LaunchedEffect(currentUser, forumId) {
        if (currentUser != null) {
            try {
                val likeDoc = firestore.collection("category")
                    .document(category?.lowercase()?.replace(" ", "") ?: forum.category)
                    .collection("forum")
                    .document(forumId)
                    .collection("likes")
                    .document(currentUser.uid)
                    .get()
                    .await()
                hasLiked = likeDoc.exists()
                localForum = localForum.copy(hasLiked = hasLiked)
                Log.d("ForumCard", "User ${currentUser.uid} hasLiked: $hasLiked for forum $forumId")
            } catch (e: Exception) {
                Log.e("ForumCard", "Error checking like status: ${e.message}", e)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                navController.navigate(Screen.InstitutionForum.createRoute(category?.lowercase()?.replace(" ", "") ?: forum.category, forumId))
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Image and Text
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = localForum.imageUrl?.let { rememberAsyncImagePainter(it) }
                        ?: painterResource(id = R.drawable.ic_placeholder),
                    contentDescription = "Forum Image",
                    modifier = Modifier
                        .size(50.dp)
                        .padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = localForum.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = localForum.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Created by: ${localForum.creatorName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Right: Like and Favorite Buttons
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (currentUser == null) {
                                Toast.makeText(context, "Please sign in to like", Toast.LENGTH_SHORT).show()
                                navController.navigate(Screen.SignIn.route)
                                return@IconButton
                            }
                            if (!hasLiked) {
                                coroutineScope.launch {
                                    try {
                                        firestore.runTransaction { transaction ->
                                            val forumRef = firestore.collection("category")
                                                .document(category?.lowercase()?.replace(" ", "") ?: localForum.category)
                                                .collection("forum")
                                                .document(forumId)
                                            val likeRef = forumRef.collection("likes").document(currentUser.uid)
                                            val forumDoc = transaction.get(forumRef)
                                            val currentLikes = forumDoc.getLong("likes")?.toInt() ?: 0
                                            val newLikes = currentLikes + 1
                                            transaction.set(likeRef, mapOf("timestamp" to System.currentTimeMillis()))
                                            transaction.update(forumRef, "likes", newLikes)
                                            newLikes
                                        }.await()
                                        localForum = localForum.copy(likes = localForum.likes + 1, hasLiked = true)
                                        hasLiked = true
                                        Toast.makeText(context, "Liked!", Toast.LENGTH_SHORT).show()
                                        Log.d("ForumCard", "Successfully liked forum $forumId, new likes: ${localForum.likes}")
                                    } catch (e: Exception) {
                                        Log.e("ForumCard", "Transaction error liking forum: ${e.message}", e)
                                        Toast.makeText(context, "Error liking forum: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = !hasLiked
                    ) {
                        Icon(
                            imageVector = if (hasLiked) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Like"
                        )
                    }
                    Text(
                        text = "${localForum.likes} Likes",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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
                                    .document(category?.lowercase()?.replace(" ", "") ?: localForum.category)
                                    .collection("forum")
                                    .document(forumId)
                                    .update("isFavorited", newFavorited)
                                    .await()
                                localForum = localForum.copy(isFavorited = newFavorited)
                                Toast.makeText(context, if (newFavorited) "Favorited!" else "Unfavorited!", Toast.LENGTH_SHORT).show()
                                Log.d("ForumCard", "Favorited forum $forumId: $newFavorited")
                            } catch (e: Exception) {
                                Log.e("ForumCard", "Error favoriting forum: ${e.message}", e)
                                Toast.makeText(context, "Error favoriting forum", Toast.LENGTH_SHORT).show()
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