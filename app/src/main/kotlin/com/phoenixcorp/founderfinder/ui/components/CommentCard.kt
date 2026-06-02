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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.phoenixcorp.founderfinder.domain.model.Comment
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.R
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CommentCard(
    comment: Comment,
    depth: Int,
    showReplies: Boolean,
    showRepliesMap: Map<String, Boolean>,
    comments: List<Comment>,
    onCommentClick: (String) -> Unit,
    onToggleReplies: (String, Boolean) -> Unit,
    onFavorite: (String, Boolean) -> Unit,
    onLike: (String) -> Unit,
    navController: NavHostController
) {
    Log.d("CommentCard", "Rendering comment: id=${comment.id}, creatorName=${comment.creatorName}, parentId=${comment.parentId}, depth=${comment.depth}, hasReplies=${comments.any { it.parentId == comment.id }}")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 0.25.dp, end = 0.25.dp, top = 1.dp) // Match provided padding
            .wrapContentHeight()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onCommentClick(comment.id)
                    }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
        ) {
            // Top row: profile picture, creator's name, favorite star
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Profile picture
                Image(
                    painter = rememberAsyncImagePainter(
                        model = comment.creatorProfilePicture,
                        placeholder = painterResource(R.drawable.ic_profile_placeholder),
                        error = painterResource(R.drawable.ic_profile_placeholder)
                    ),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            Log.d("CommentCard", "Navigating to user profile with userId=${comment.creatorId}")
                            navController.navigate(Screen.UserProfile.createRoute(comment.creatorId))
                        }
                )

                Spacer(modifier = Modifier.width(2.dp))

                // Creator's name
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (comment.creatorName.isNullOrBlank()) "Anonymous" else comment.creatorName,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 30.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            Log.d("CommentCard", "Navigating to user profile with userId=${comment.creatorId}")
                            navController.navigate(Screen.UserProfile.createRoute(comment.creatorId))
                        }
                    )
                    // Timestamp below name
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(comment.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                // Favorite star
                IconButton(onClick = { onFavorite(comment.id, !(comment.isFavorited ?: false)) }) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite comment",
                        tint = if (comment.isFavorited == true) Color.Yellow else Color.Gray
                    )
                }
            }

            // Like heart below favorite star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { onLike(comment.id) }) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Like comment",
                        tint = if ((comment.likes ?: 0) > 0) Color.Red else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Comment content below profile picture
            Text(
                text = comment.message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 2.dp, top = 2.dp), // Align under profile picture
                maxLines = 10,
                overflow = TextOverflow.Ellipsis
            )

            // Down arrow for replies
            if (comments.any { it.parentId == comment.id }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { onToggleReplies(comment.id, !showReplies) }) {
                        Icon(
                            imageVector = if (showReplies) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = if (showReplies) "Collapse replies" else "Expand replies",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Render replies if expanded
            if (showReplies) {
                comments.filter { it.parentId == comment.id }.forEach { reply ->
                    CommentCard(
                        comment = reply,
                        depth = reply.depth,
                        showReplies = showRepliesMap[reply.id] ?: false,
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