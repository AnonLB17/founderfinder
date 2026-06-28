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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
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
    navController: NavHostController
) {
    var creatorFullName by remember { mutableStateOf(thread.creatorName) }
    var creatorProfilePicture by remember { mutableStateOf<String?>(null) }

    // Fetch user profile if name is Anonymous
    LaunchedEffect(thread.creatorId) {
        if ((thread.creatorName.isNullOrBlank() || thread.creatorName == "Anonymous") &&
            thread.creatorId.isNotEmpty()) {

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
                    creatorFullName = "$firstName $lastName".trim().ifEmpty { "Anonymous" }
                    creatorProfilePicture = profileDoc.getString("profilePicture")

                    Log.d("ThreadCard", "Profile loaded: $creatorFullName")
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
            // Header: Avatar + Name + Time
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
                IconButton(onClick = { /* TODO: Favorite */ }) {
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

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { /* TODO: Like */ }) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Like",
                            tint = if (thread.likes > 0) Color.Red else Color.Gray
                        )
                    }
                    Text("${thread.likes}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}