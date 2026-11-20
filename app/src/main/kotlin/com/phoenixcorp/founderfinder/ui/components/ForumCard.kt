package com.phoenixcorp.founderfinder.ui.components

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.phoenixcorp.founderfinder.ui.utils.fetchCurrentUserRole
import com.phoenixcorp.founderfinder.ui.utils.permissionsFor
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
    val currentUser = FirebaseAuth.getInstance().currentUser
    val coroutineScope = rememberCoroutineScope()
    val categoryKey = category?.lowercase()?.replace(" ", "") ?: forum.category
    var likes by remember(forumId) { mutableIntStateOf(forum.likes) }
    var hasLiked by remember(forumId) { mutableStateOf(forum.hasLiked) }
    var liking by remember(forumId) { mutableStateOf(false) }

    LaunchedEffect(currentUser?.uid, forumId, categoryKey) {
        if (currentUser == null) return@LaunchedEffect
        try {
            val likeDoc = firestore.collection("category")
                .document(categoryKey)
                .collection("forum")
                .document(forumId)
                .collection("likes")
                .document(currentUser.uid)
                .get()
                .await()
            hasLiked = likeDoc.exists()
            Log.d("ForumCard", "User ${currentUser.uid} hasLiked: $hasLiked for forum $forumId")
        } catch (e: Exception) {
            Log.e("ForumCard", "Error checking like status: ${e.message}", e)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                navController.navigate(Screen.InstitutionForum.createRoute(categoryKey, forumId))
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = forum.imageUrl?.let { rememberAsyncImagePainter(it) }
                        ?: painterResource(id = R.drawable.ic_placeholder),
                    contentDescription = "Forum Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                )
                IconButton(
                    onClick = {
                        if (liking) return@IconButton
                        if (currentUser == null) {
                            Toast.makeText(context, "Please sign in to like", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.SignIn.route)
                            return@IconButton
                        }
                        val removing = hasLiked
                        liking = true
                        coroutineScope.launch {
                            val role = fetchCurrentUserRole()
                            if (!permissionsFor(role).requireEngage(
                                    context,
                                    if (removing) "unlike a forum" else "like a forum"
                                )
                            ) {
                                liking = false
                                return@launch
                            }
                            try {
                                val forumRef = firestore.collection("category")
                                    .document(categoryKey)
                                    .collection("forum")
                                    .document(forumId)
                                val likeRef = forumRef.collection("likes").document(currentUser.uid)
                                firestore.runTransaction { transaction ->
                                    val forumDoc = transaction.get(forumRef)
                                    val currentLikes = when (val raw = forumDoc.get("likes")) {
                                        is Number -> raw.toInt()
                                        else -> 0
                                    }
                                    if (removing) {
                                        transaction.delete(likeRef)
                                        transaction.update(forumRef, "likes", (currentLikes - 1).coerceAtLeast(0))
                                    } else {
                                        transaction.set(likeRef, mapOf("timestamp" to System.currentTimeMillis()))
                                        transaction.update(forumRef, "likes", currentLikes + 1)
                                    }
                                }.await()
                                if (removing) {
                                    likes = (likes - 1).coerceAtLeast(0)
                                    hasLiked = false
                                    Log.d("ForumCard", "Unliked forum $forumId, likes: $likes")
                                } else {
                                    likes += 1
                                    hasLiked = true
                                    Log.d("ForumCard", "Liked forum $forumId, likes: $likes")
                                }
                            } catch (e: Exception) {
                                Log.e("ForumCard", "Transaction error toggling like: ${e.message}", e)
                                Toast.makeText(context, "Error updating like: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                liking = false
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (hasLiked) "Unlike" else "Like",
                            tint = if (hasLiked) Color(0xFFFF4D6D) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = forum.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = forum.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Created by: ${forum.creatorName}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$likes likes",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
