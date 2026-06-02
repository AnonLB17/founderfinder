package com.phoenixcorp.founderfinder.ui.screens

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
                            // Profile Image, Name, and all sections...
                            // (same as previous version)
                            user.profilePicture?.takeIf { it.isNotEmpty() }?.let { picture ->
                                Image(
                                    painter = rememberAsyncImagePainter(picture),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } ?: Image(
                                painter = painterResource(id = R.drawable.ic_profile_placeholder),
                                contentDescription = "Default",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "${user.firstName ?: "Unknown"} ${user.lastName ?: "User"}",
                                style = MaterialTheme.typography.headlineMedium
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            ProfileSection(title = "Ambition Statement") {
                                Text(user.ambitionStatement ?: "Not provided")
                            }

                            ProfileSection(title = "Founder Status") {
                                val isFounder = user.founderStatus ?: false
                                Text(if (isFounder) "Founder" else "Not a Founder")
                                if (isFounder) user.founderEntries?.forEach { Text("• $it") }
                            }

                            ProfileSection(title = "Education") {
                                user.educationEntries?.forEach { Text("• $it") } ?: Text("Not provided")
                            }

                            ProfileSection(title = "Work Experience") {
                                user.workExperiences?.forEach { Text("• $it") } ?: Text("Not provided")
                            }

                            ProfileSection(title = "Industries of Interest") {
                                user.industries?.forEach { Text("• $it") } ?: Text("Not provided")
                            }

                            ProfileSection(title = "Organizations of Interest") {
                                user.organizations?.forEach { Text("• $it") } ?: Text("Not provided")
                            }
                        }
                    }
                } ?: Text("Profile not found")
            }
        }
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        content()
        Spacer(modifier = Modifier.height(20.dp))
    }
}