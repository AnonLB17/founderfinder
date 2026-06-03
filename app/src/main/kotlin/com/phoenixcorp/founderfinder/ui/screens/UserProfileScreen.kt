package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navController: NavHostController,
    userId: String,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userData by viewModel.userData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.loadUserProfile(userId)
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = {
                    Text(
                        text = userData?.let {
                            "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifBlank { "Profile" }
                        } ?: "Profile"
                    )
                },
                navController = navController,
                showBackButton = true
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(48.dp))
            } else if (errorMessage != null) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            } else {
                userData?.let { user ->
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            // Profile Picture
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = user.profilePicture ?: R.drawable.ic_profile_placeholder
                                ),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "${user.firstName ?: "Unknown"} ${user.lastName ?: "User"}",
                                style = MaterialTheme.typography.headlineMedium
                            )

                            user.birthDate?.let {
                                Text(text = "Born: $it", style = MaterialTheme.typography.bodyMedium)
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Matching Firestore field names
                            ProfileSection(title = "Ambition Statement") {
                                Text(user.ambitionStatement ?: "Not provided")
                            }

                            // === UPDATED FOUNDER STATUS SECTION ===
                            ProfileSection(title = "Founder Status") {
                                val isFounder = user.isFounder ?: false   // Force fallback

                                Log.d("DEBUG_PROFILE", "isFounder from model = $isFounder | founderEntries size = ${user.founderEntries.size}")

                                if (isFounder) {
                                    Text(
                                        text = "✅ Founder",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (user.founderEntries.isNotEmpty()) {
                                        user.founderEntries.forEach { entry ->
                                            Text(
                                                text = "• $entry",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    } else {
                                        Text("Founder (No startup details provided)")
                                    }
                                } else {
                                    Text(
                                        text = "Not a founder yet",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }

                            ProfileSection(title = "Education") {
                                if (user.educationEntries.isNotEmpty()) {
                                    user.educationEntries.forEach { entry ->
                                        Text("• $entry", style = MaterialTheme.typography.bodyMedium)
                                    }
                                } else {
                                    Text("No education added")
                                }
                            }

                            ProfileSection(title = "Work Experience") {
                                if (user.workExperiences.isNotEmpty()) {
                                    user.workExperiences.forEach { entry ->
                                        Text("• $entry", style = MaterialTheme.typography.bodyMedium)
                                    }
                                } else {
                                    Text("No work experience added")
                                }
                            }

                            ProfileSection(title = "Industries of Interest") {
                                if (user.industriesOfInterest.isNotEmpty()) {
                                    user.industriesOfInterest.forEach { industry ->
                                        Text("• $industry", style = MaterialTheme.typography.bodyMedium)
                                    }
                                } else {
                                    Text("No industries selected")
                                }
                            }

                            ProfileSection(title = "Organizations of Interest") {
                                if (user.organizationsOfInterest.isNotEmpty()) {
                                    user.organizationsOfInterest.forEach { org ->
                                        Text("• $org", style = MaterialTheme.typography.bodyMedium)
                                    }
                                } else {
                                    Text("No organizations selected")
                                }
                            }

                            ProfileSection(title = "Social Links") {
                                user.linkedinUrl?.takeIf { it.isNotBlank() }?.let { Text("LinkedIn: $it") }
                                user.twitterUrl?.takeIf { it.isNotBlank() }?.let { Text("Twitter/X: $it") }
                                user.facebookUrl?.takeIf { it.isNotBlank() }?.let { Text("Facebook: $it") }
                                user.instagramUrl?.takeIf { it.isNotBlank() }?.let { Text("Instagram: $it") }
                                user.websiteUrl?.takeIf { it.isNotBlank() }?.let { Text("Website: $it") }

                                if (listOfNotNull(
                                        user.linkedinUrl, user.twitterUrl, user.facebookUrl,
                                        user.instagramUrl, user.websiteUrl
                                    ).all { it.isNullOrBlank() }) {
                                    Text("No social links added")
                                }
                            }
                        }
                    }
                } ?: Text("Profile not found")
            }
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
        Spacer(modifier = Modifier.height(24.dp))
    }
}