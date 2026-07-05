package com.phoenixcorp.founderfinder.ui.screens

import android.text.format.DateUtils
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.phoenixcorp.founderfinder.domain.model.Notification
import com.phoenixcorp.founderfinder.navigation.ScreenWithHeader
import com.phoenixcorp.founderfinder.ui.viewmodel.notifications.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavHostController,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAllNotifications()
    }

    ScreenWithHeader(navController = navController, title = "Activity") {
        Column {
            if (unreadCount > 0) {
                Text(
                    text = "$unreadCount unread",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            when {
                isLoading && notifications.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                notifications.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No activity yet.\nYour notifications and updates will appear here.")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = notifications,
                            key = { it.id }
                        ) { notification ->
                            NotificationItemCard(
                                notification = notification,
                                navController = navController,
                                onMarkAsRead = { id -> viewModel.markAsRead(id) },
                                onDelete = { id -> viewModel.deleteNotification(id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationItemCard(
    notification: Notification,
    navController: NavHostController,
    onMarkAsRead: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Mark as read immediately when tapped
                onMarkAsRead(notification.id)
                handleNotificationNavigation(navController, notification)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = getNotificationIcon(notification.type)
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Inside NotificationItemCard, in the Column:
            Column(modifier = Modifier.weight(1f)) {
                // Show sender name + title
                Text(
                    text = "${notification.displaySenderName} • ${notification.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = notification.body.ifBlank { "" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Time
                Text(
                    text = notification.getRelativeTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete Button (works on unread notifications)
            IconButton(onClick = { onDelete(notification.id) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            if (!notification.read) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}

// === NAVIGATION HANDLER ===
private fun handleNotificationNavigation(
    navController: NavHostController,
    notification: Notification
) {
    Log.d("Notifications", "Navigating from notification: type=${notification.type}, threadId=${notification.threadId}, forumId=${notification.forumId}")

    try {
        when (notification.type) {
            "new_comment", "comment_reply" -> {
                val forumId = notification.forumId ?: return
                val threadId = notification.threadId ?: return
                val category = notification.category ?: "requestedsolutions"

                navController.navigate("thread/$category/$forumId/$threadId")
            }

            "chat_message", "new_message", "group_message" -> {
                notification.chatId?.let { chatId ->
                    navController.navigate("private_chat/$chatId")
                }
            }

            "new_thread", "forum" -> {
                val forumId = notification.forumId ?: return
                val category = notification.category ?: "marketpotential"

                Log.d("Notifications", "Navigating to forum: $category/$forumId")

                navController.navigate("institution_forum/$category/$forumId")   // Make sure this route matches your NavGraph
            }

            "activity_reminder" -> {                    // ← ADD THIS
                notification.activityId?.let { activityId ->
                    Log.d("Notifications", "Navigating to activity: $activityId")
                    navController.navigate("partners?highlightActivity=$activityId")
                } ?: run {
                    navController.navigate("partners")
                }
            }

            else -> {
                Log.w("Notifications", "Unknown notification type: ${notification.type}")
            }
        }
    } catch (e: Exception) {
        Log.e("Notifications", "Failed to navigate", e)
    }
}

@Composable
private fun getRelativeTime(timestamp: Any?): String {
    if (timestamp == null) return "Just now"

    val millis = when (timestamp) {
        is Timestamp -> timestamp.toDate().time
        is com.google.firebase.Timestamp -> timestamp.toDate().time
        is Long -> timestamp
        else -> System.currentTimeMillis()
    }

    return DateUtils.getRelativeTimeSpanString(
        LocalContext.current,
        millis,
        true
    ).toString()
}

private fun getNotificationIcon(type: String): ImageVector {
    return when (type) {
        "chat_message", "new_message", "group_message" -> Icons.Default.ChatBubbleOutline
        "new_thread" -> Icons.Default.Forum
        "new_comment", "comment_reply" -> Icons.Default.Comment
        "file_share" -> Icons.Default.AttachFile
        else -> Icons.Default.Notifications
    }
}