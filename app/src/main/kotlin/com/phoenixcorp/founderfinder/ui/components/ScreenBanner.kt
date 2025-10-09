package com.phoenixcorp.founderfinder.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.navigation.Screen

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
    categoryButtonText: String? = null,
    onCategoryButtonClick: (() -> Unit)? = null,
    onMailClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
    onAddClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
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
                        // Profile Picture
                        if (profilePicture != null && profilePicture.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = profilePicture,
                                    placeholder = painterResource(R.drawable.ic_profile_placeholder),
                                    error = painterResource(R.drawable.ic_profile_placeholder)
                                ),
                                contentDescription = "Recipient Profile Picture",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else if (onProfileClick != null) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_profile_placeholder),
                                contentDescription = "Recipient Profile Picture",
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
                            Button(
                                onClick = { onCategoryButtonClick.invoke() },
                                modifier = Modifier.wrapContentWidth()
                            ) {
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            if (showAddButton && navController != null) {
                IconButton(onClick = {
                    Log.d("ScreenBanner", "Navigating to ForumCreationScreen")
                    onAddClick?.invoke() ?: navController.navigate(Screen.ForumCreation.route)
                }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Forum"
                    )
                }
            }
            if (showMailButton && onMailClick != null) {
                IconButton(onClick = {
                    Log.d("ScreenBanner", "Invoking onMailClick")
                    onMailClick.invoke()
                }) {
                    Icon(
                        imageVector = Icons.Default.Mail,
                        contentDescription = "Private Messages"
                    )
                }
            }
            if (showInvestorAddButton && navController != null) {
                IconButton(onClick = {
                    Log.d("ScreenBanner", "Navigating to SelectUserTypeScreen for investor profile creation")
                    navController.navigate(Screen.SelectUserType.route)
                }) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add Investor Profile"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Preview(showBackground = true)
@Composable
fun ScreenBannerPreview() {
    ScreenBanner(
        title = { Text("Sample Screen") },
        subtitle = null,
        showBackButton = true
    )
}

@Preview(showBackground = true)
@Composable
fun ScreenBannerWithProfilePreview() {
    ScreenBanner(
        title = { Text("John Doe") },
        subtitle = "Canada -> Alberta",
        profilePicture = "https://example.com/profile.jpg",
        showBackButton = true,
        onProfileClick = { /* Preview click */ }
    )
}

@Preview(showBackground = true)
@Composable
fun ScreenBannerWithAddButtonPreview() {
    ScreenBanner(
        title = { Text("Idea Generation") },
        subtitle = null,
        showAddButton = true
    )
}

@Preview(showBackground = true)
@Composable
fun ScreenBannerWithInvestorAddButtonPreview() {
    ScreenBanner(
        title = { Text("Investor Search") },
        subtitle = null,
        showInvestorAddButton = true
    )
}

@Preview(showBackground = true)
@Composable
fun ScreenBannerWithCategoryButtonPreview() {
    ScreenBanner(
        title = { Text("Placeholder") },
        subtitle = "Canada -> Alberta",
        categoryButtonText = "National: Canada",
        onCategoryButtonClick = { /* Preview click */ },
        showBackButton = true
    )
}