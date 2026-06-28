package com.phoenixcorp.founderfinder.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.viewmodel.notifications.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenBanner(
    title: @Composable () -> Unit,
    subtitle: String? = null,
    profilePicture: String? = null,
    navController: NavHostController? = null,
    showBackButton: Boolean = false,
    showAddButton: Boolean = false,
    showMailButton: Boolean = false,
    showInvestorAddButton: Boolean = false,
    showNotifications: Boolean = false,
    showLogout: Boolean = false,
    categoryButtonText: String? = null,
    onCategoryButtonClick: (() -> Unit)? = null,
    onMailClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
    onAddClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null
) {
    val notificationsViewModel: NotificationsViewModel = hiltViewModel()
    val unreadCount by notificationsViewModel.unreadCount.collectAsState()

    TopAppBar(
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .clickable(enabled = onProfileClick != null) { onProfileClick?.invoke() }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (profilePicture != null && profilePicture.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = profilePicture,
                                    placeholder = painterResource(R.drawable.ic_profile_placeholder),
                                    error = painterResource(R.drawable.ic_profile_placeholder)
                                ),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else if (onProfileClick != null) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_profile_placeholder),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (onProfileClick != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (categoryButtonText != null && onCategoryButtonClick != null) {
                            Button(onClick = { onCategoryButtonClick.invoke() }) {
                                Text(categoryButtonText)
                            }
                        } else {
                            title()
                        }
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (showBackButton && navController != null) {
                IconButton(onClick = { onBackClick?.invoke() ?: navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            } else if (showLogout) {
                IconButton(onClick = { onLogoutClick?.invoke() }) {
                    Icon(Icons.Default.Logout, contentDescription = "Logout")
                }
            }
        },
        actions = {
            // Notifications Bell
            if (showNotifications && navController != null) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge { Text(unreadCount.toString()) }
                        }
                    }
                ) {
                    IconButton(onClick = {
                        navController.navigate(Screen.Notifications.route)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                }
            }

            if (showAddButton && navController != null) {
                IconButton(onClick = {
                    onAddClick?.invoke() ?: navController.navigate(Screen.ForumCreation.route)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
            if (showMailButton && onMailClick != null) {
                IconButton(onClick = onMailClick) {
                    Icon(Icons.Default.Mail, contentDescription = "Messages")
                }
            }
            if (showInvestorAddButton && navController != null) {
                IconButton(onClick = { navController.navigate(Screen.SelectUserType.route) }) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Add Investor")
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )
}

// Previews
@Preview(showBackground = true)
@Composable
fun ScreenBannerPreview() {
    ScreenBanner(
        title = { Text("Sample Screen") },
        subtitle = null,
        showBackButton = true
    )
}