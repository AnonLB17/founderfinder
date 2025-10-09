package com.phoenixcorp.founderfinder.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.data.Comment
import com.phoenixcorp.founderfinder.data.Thread
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.R
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ThreadCard(
    thread: Thread,
    showComments: Boolean,
    comments: List<Comment>,
    showRepliesMap: Map<String, Boolean>,
    onToggleComments: (String, Boolean) -> Unit,
    onToggleReplies: (String, Boolean) -> Unit,
    onCommentClick: (String) -> Unit,
    onFavorite: (String, Boolean) -> Unit,
    onLike: (String) -> Unit,
    navController: NavHostController
) {
    Log.d("ThreadCard", "Rendering thread: id=${thread.id}, creatorName=${thread.creatorName}, creatorProfilePicture=${thread.creatorProfilePicture}")

    var creatorFullName by remember { mutableStateOf(thread.creatorName) }
    var creatorProfilePicture by remember { mutableStateOf<String?>(null) }

    // Fetch creator's full name and profile picture if it's "Anonymous"
    LaunchedEffect(thread.creatorId) {
        if (thread.creatorName == "Anonymous" && thread.creatorId.isNotEmpty()) {
            try {
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
                }
            } catch (e: Exception) {
                Log.e("ThreadCard", "Error fetching creator details: ${e.message}", e)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 1.dp)
            .wrapContentHeight()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onToggleComments(thread.id, !showComments)
                    }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Image(
                    painter = creatorProfilePicture?.let { rememberAsyncImagePainter(it) } ?: painterResource(id = R.drawable.ic_profile_placeholder),
                    contentDescription = "Creator Profile Picture",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            thread.creatorId.let { userId ->
                                navController.navigate(Screen.UserProfile.createRoute(userId))
                            }
                        },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = creatorFullName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(thread.timestamp)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = { onFavorite(thread.id, !thread.isFavorited) }) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = if (thread.isFavorited) Color.Yellow else Color.Gray
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { onLike(thread.id) }) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Like",
                        tint = if (thread.likes > 0) Color.Red else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = thread.message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, end = 2.dp, top = 2.dp),
                maxLines = 10,
                overflow = TextOverflow.Ellipsis
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { onToggleComments(thread.id, !showComments) }) {
                    Icon(
                        imageVector = if (showComments) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (showComments) "Collapse comments" else "Expand comments",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (showComments) {
                comments.filter { it.parentId == null }.forEach { comment ->
                    CommentCard(
                        comment = comment,
                        depth = comment.depth,
                        showReplies = showRepliesMap[comment.id] ?: false,
                        showRepliesMap = showRepliesMap,
                        comments = comments,
                        onCommentClick = onCommentClick,
                        onToggleReplies = onToggleReplies,
                        onFavorite = onFavorite,
                        onLike = onLike,
                        navController = navController
                    )
                }
            }
        }
    }
}