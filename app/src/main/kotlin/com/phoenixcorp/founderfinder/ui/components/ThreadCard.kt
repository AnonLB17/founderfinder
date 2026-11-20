package com.phoenixcorp.founderfinder.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.Thread
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.R
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ThreadCard(
    thread: Thread,
    navController: NavHostController,
    onFavorite: (String, Boolean) -> Unit = { _, _ -> },
    onLike: (String) -> Unit = { }
) {
    var creatorFullName by remember { mutableStateOf(thread.creatorName) }
    var creatorProfilePicture by remember { mutableStateOf<String?>(null) }

    // Fetch user profile picture and name if missing
    LaunchedEffect(thread.creatorId) {
        if (thread.creatorId.isNotEmpty()) {
            try {
                Log.d("ThreadCard", "Fetching profile for user: ${thread.creatorId}")

                val firestore = FirebaseFirestore.getInstance()
                val profileDoc = firestore.collection("profiles")
                    .document(thread.creatorId)
                    .get()
                    .await()

                if (profileDoc.exists()) {
                    val firstName = profileDoc.getString("firstName") ?: ""
                    val lastName = profileDoc.getString("lastName") ?: ""
                    val fullName = "$firstName $lastName".trim()

                    if (fullName.isNotBlank() && creatorFullName != fullName) {
                        creatorFullName = fullName
                    }

                    val picUrl = profileDoc.getString("profilePicture")
                    if (!picUrl.isNullOrBlank() && creatorProfilePicture != picUrl) {
                        creatorProfilePicture = picUrl
                        Log.d("ThreadCard", "Profile picture loaded: $picUrl")
                    }
                }
            } catch (e: Exception) {
                Log.e("ThreadCard", "Error fetching creator details", e)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                if (thread.forumId.isNotBlank() && thread.id.isNotBlank()) {
                    val category = thread.category.ifBlank { "requestedsolutions" }
                    navController.navigate("thread/${category}/${thread.forumId}/${thread.id}")
                    Log.d("ThreadCard", "✅ Navigating with category=$category")
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Avatar + Name + Time + Favorite
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = creatorProfilePicture?.let { rememberAsyncImagePainter(it) }
                        ?: painterResource(id = R.drawable.ic_profile_placeholder),
                    contentDescription = "Creator",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            navController.navigate(Screen.UserProfile.createRoute(thread.creatorId))
                        },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = creatorFullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.UserProfile.createRoute(thread.creatorId))
                        }
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                            .format(Date(thread.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Favorite Button
                IconButton(onClick = {
                    onFavorite(thread.id, !thread.isFavorited)
                }) {
                    Icon(
                        imageVector = if (thread.isFavorited) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (thread.isFavorited) Color.Yellow else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Thread Content
            Text(
                text = thread.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Like Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isLikedByMe = thread.likedBy.contains(FirebaseAuth.getInstance().currentUser?.uid)

                IconButton(onClick = { onLike(thread.id) }) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Like",
                        tint = if (isLikedByMe) Color.Red else Color.Gray
                    )
                }
                Text("${thread.likes}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}