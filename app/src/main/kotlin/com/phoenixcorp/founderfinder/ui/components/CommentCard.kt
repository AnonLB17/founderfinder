package com.phoenixcorp.founderfinder.ui.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.R
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CommentCard(
    comment: Comment,
    depth: Int = 0,
    showReplies: Boolean,
    showRepliesMap: MutableMap<String, Boolean>,
    comments: List<Comment>,
    onCommentClick: (String) -> Unit,
    onToggleReplies: (String, Boolean) -> Unit,
    onReplyToComment: (String) -> Unit,
    onFavorite: (String, Boolean) -> Unit,
    onLike: (String) -> Unit,
    navController: NavHostController
) {
    var displayName by remember { mutableStateOf(comment.creatorName ?: "Anonymous") }
    var displayProfilePic by remember { mutableStateOf<String?>(comment.creatorProfilePicture) }

    LaunchedEffect(comment.creatorId) {
        if (comment.creatorId.isNotEmpty() && (displayName == "Anonymous" || displayName.isBlank())) {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("profiles")
                    .document(comment.creatorId)
                    .get()
                    .await()

                if (doc.exists()) {
                    val first = doc.getString("firstName") ?: ""
                    val last = doc.getString("lastName") ?: ""
                    displayName = "$first $last".trim().ifBlank { "Anonymous" }
                    displayProfilePic = doc.getString("profilePicture")
                }
            } catch (e: Exception) {
                Log.e("CommentCard", "Failed to load profile", e)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (depth == 0) 4.dp else 0.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (depth == 0) 6.dp else 3.dp),
        border = if (depth > 0 && showReplies)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(1.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Profile Picture
                Image(
                    painter = if (!displayProfilePic.isNullOrBlank()) {
                        rememberAsyncImagePainter(displayProfilePic)
                    } else {
                        painterResource(id = R.drawable.ic_profile_placeholder)
                    },
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable {
                            navController.navigate(Screen.UserProfile.createRoute(comment.creatorId))
                        },
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.UserProfile.createRoute(comment.creatorId))
                        }
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                            .format(Date(comment.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Favorite Button
                IconButton(onClick = {
                    val newState = !(comment.isActuallyFavorited)
                    onFavorite(comment.id, newState)
                }) {
                    Icon(
                        if (comment.isActuallyFavorited) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (comment.isActuallyFavorited) Color.Yellow else Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = comment.message,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isLikedByMe = comment.likedBy.contains(FirebaseAuth.getInstance().currentUser?.uid)

                    IconButton(onClick = { onLike(comment.id) }) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Like",
                            tint = if (isLikedByMe) Color.Red else Color.Gray
                        )
                    }
                    Text(
                        text = "${comment.likes ?: 0}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row {
                    TextButton(onClick = { onReplyToComment(comment.id) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Reply",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Reply")
                    }

                    val replyCount = comments.count { it.parentId == comment.id }
                    if (replyCount > 0) {
                        TextButton(onClick = { onToggleReplies(comment.id, !showReplies) }) {
                            Icon(
                                if (showReplies) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Toggle",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (showReplies) "Hide" else "Show $replyCount replies")
                        }
                    }
                }
            }

            // Replies Section
            if (showReplies) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))

                val directReplies = comments.filter { it.parentId == comment.id }
                    .sortedWith(   // ← Same sorting logic as top level
                        compareByDescending<Comment> { it.isActuallyFavorited }
                            .thenByDescending { it.timestamp }
                            .thenByDescending { it.likes ?: 0 }
                    )

                directReplies.forEach { reply ->
                    CommentCard(
                        comment = reply,
                        depth = depth + 1,
                        showReplies = showRepliesMap[reply.id] ?: false,
                        showRepliesMap = showRepliesMap,
                        comments = comments,
                        onCommentClick = onCommentClick,
                        onToggleReplies = onToggleReplies,
                        onReplyToComment = onReplyToComment,
                        onFavorite = onFavorite,
                        onLike = onLike,
                        navController = navController
                    )
                }
            }
        }
    }
}