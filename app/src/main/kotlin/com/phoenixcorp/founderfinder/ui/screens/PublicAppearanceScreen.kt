package com.phoenixcorp.founderfinder.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.data.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicAppearanceScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = Firebase.firestore
    val storage = Firebase.storage
    val currentUser = auth.currentUser
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userData by remember { mutableStateOf<UserProfile?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        profileImageUri = uri
    }

    // Fetch user data from users collection (onboarding data)
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            errorMessage = "You must be logged in to view your profile."
            isLoading = false
            Log.e("PublicAppearanceScreen", "No authenticated user")
            return@LaunchedEffect
        }
        coroutineScope.launch {
            try {
                Log.d("PublicAppearanceScreen", "Fetching profile for userId: ${currentUser.uid}")
                val document = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                if (document.exists()) {
                    userData = document.toObject(UserProfile::class.java)?.copy(userId = document.id)
                    Log.d("PublicAppearanceScreen", "Profile data: $userData")
                } else {
                    // Initialize with defaults if no profile exists
                    userData = UserProfile(userId = currentUser.uid)
                    Log.d("PublicAppearanceScreen", "No profile found, using default: $userData")
                }
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load profile: ${e.message}"
                isLoading = false
                Log.e("PublicAppearanceScreen", "Error fetching profile: ${e.message}", e)
            }
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Public Appearance") },
                navController = navController,
                showBackButton = true,
                // Other parameters...
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                userData?.let { user ->
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            // Profile Picture Picker
                            Text(
                                text = "Choose your profile picture",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier.size(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (profileImageUri != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(profileImageUri),
                                        contentDescription = "Selected Profile Picture",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    user.profilePicture?.takeIf { it.isNotEmpty() }?.let { savedUri ->
                                        Image(
                                            painter = rememberAsyncImagePainter(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(savedUri)
                                                    .crossfade(true)
                                                    .placeholder(R.drawable.ic_profile_placeholder)
                                                    .error(R.drawable.ic_profile_placeholder)
                                                    .build(),
                                                onError = { error -> Log.e("PublicAppearanceScreen", "Coil Error: ${error.result.throwable.message}") }
                                            ),
                                            contentDescription = "Saved Profile Picture",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } ?: Image(
                                        painter = painterResource(id = R.drawable.ic_profile_placeholder),
                                        contentDescription = "Default Profile Picture",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading
                            ) {
                                Text("Pick Image")
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Profile Summary
                            Text(
                                text = "Profile Summary",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Name: ${user.firstName ?: "Not provided"} ${user.lastName ?: "Not provided"}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Education",
                                style = MaterialTheme.typography.titleMedium
                            )
                            user.educationEntries?.takeIf { it.isNotEmpty() }?.filter { it.isNotEmpty() }?.forEach { entry ->
                                Text(
                                    text = entry,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } ?: Text(
                                text = "Not provided",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Work Experience",
                                style = MaterialTheme.typography.titleMedium
                            )
                            user.workExperiences?.takeIf { it.isNotEmpty() }?.filter { it.isNotEmpty() }?.forEach { entry ->
                                Text(
                                    text = entry,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } ?: Text(
                                text = "Not provided",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Founder Status",
                                style = MaterialTheme.typography.titleMedium
                            )
                            val isFounder = user.founderStatus ?: false
                            Text(
                                text = if (isFounder) "Founder" else "Not a Founder",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (isFounder) {
                                user.founderEntries?.filter { it.isNotBlank() }?.forEach { entry ->
                                    Text(
                                        text = entry,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ambition Statement",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = user.ambitionStatement ?: "Not provided",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Social Links",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "LinkedIn: ${if (user.linkedin != null && user.linkedin != "null") user.linkedin else "Not provided"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Twitter: ${if (user.twitter != null && user.twitter != "null") user.twitter else "Not provided"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Facebook: ${if (user.facebook != null && user.facebook != "null") user.facebook else "Not provided"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Instagram: ${if (user.instagram != null && user.instagram != "null") user.instagram else "Not provided"}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Industries of Interest",
                                style = MaterialTheme.typography.titleMedium
                            )
                            user.industries?.takeIf { it.isNotEmpty() }?.filter { it.isNotEmpty() }?.forEach { entry ->
                                Text(
                                    text = entry,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } ?: Text(
                                text = "Not provided",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Organizations of Interest",
                                style = MaterialTheme.typography.titleMedium
                            )
                            user.organizations?.takeIf { it.isNotEmpty() }?.filter { it.isNotEmpty() }?.forEach { entry ->
                                Text(
                                    text = entry,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } ?: Text(
                                text = "Not provided",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Submit Button
                            Button(
                                onClick = {
                                    if (currentUser == null) {
                                        errorMessage = "You must be logged in to submit your profile."
                                        Log.e("PublicAppearanceScreen", "No authenticated user")
                                        return@Button
                                    }
                                    if (profileImageUri == null && user.profilePicture.isNullOrEmpty()) {
                                        errorMessage = "Please select a profile picture."
                                        Log.e("PublicAppearanceScreen", "No profile picture selected")
                                        return@Button
                                    }

                                    isLoading = true
                                    errorMessage = null
                                    val userId = currentUser.uid

                                    coroutineScope.launch {
                                        try {
                                            // Upload profile picture if selected
                                            val downloadUrl = if (profileImageUri != null) {
                                                val storageRef = storage.reference
                                                    .child("profilePictures/$userId/profile.jpg")
                                                storageRef.putFile(profileImageUri!!).await()
                                                storageRef.downloadUrl.await().toString()
                                            } else {
                                                user.profilePicture ?: ""
                                            }

                                            // Save all profile fields to Firestore
                                            val profileData = mapOf(
                                                "userId" to userId,
                                                "firstName" to (user.firstName ?: ""),
                                                "lastName" to (user.lastName ?: ""),
                                                "bio" to (user.bio ?: ""),
                                                "profilePicture" to downloadUrl,
                                                "linkedin" to (if (user.linkedin != null && user.linkedin != "null") user.linkedin else "" ?: ""),
                                                "twitter" to (if (user.twitter != null && user.twitter != "null") user.twitter else "" ?: ""),
                                                "facebook" to (if (user.facebook != null && user.facebook != "null") user.facebook else "" ?: ""),
                                                "instagram" to (if (user.instagram != null && user.instagram != "null") user.instagram else "" ?: ""),
                                                "ambitionStatement" to (user.ambitionStatement ?: ""),
                                                "founderStatus" to (user.founderStatus ?: ""),
                                                "founderEntries" to (user.founderEntries ?: emptyList()),
                                                "educationEntries" to (user.educationEntries ?: emptyList()),
                                                "workExperiences" to (user.workExperiences ?: emptyList()),
                                                "industries" to (user.industries ?: emptyList()),
                                                "organizations" to (user.organizations ?: emptyList()),
                                                "hasInvestorProfile" to (user.hasInvestorProfile ?: false),
                                                "investmentFirmName" to (user.investmentFirmName ?: ""),
                                                "firmLogo" to (user.firmLogo ?: ""),
                                                "professionalBackground" to (user.professionalBackground ?: ""),
                                                "notableAchievements" to (user.notableAchievements ?: ""),
                                                "preferredIndustries" to (user.preferredIndustries ?: emptyList()),
                                                "investmentStage" to (user.investmentStage ?: ""),
                                                "investmentRangeMin" to (user.investmentRangeMin ?: ""),
                                                "investmentRangeMax" to (user.investmentRangeMax ?: ""),
                                                "investmentApproach" to (user.investmentApproach ?: ""),
                                                "strategicInvolvement" to (user.strategicInvolvement ?: ""),
                                                "roiExpectations" to (user.roiExpectations ?: ""),
                                                "portfolioCompanies" to (user.portfolioCompanies ?: emptyList()),
                                                "successStories" to (user.successStories ?: emptyList()),
                                                "testimonials" to (user.testimonials ?: emptyList()),
                                                "equityTerms" to (user.equityTerms ?: ""),
                                                "boardRole" to (user.boardRole ?: ""),
                                                "returnTimeline" to (user.returnTimeline ?: "")
                                            )
                                            firestore.collection("profiles")
                                                .document(userId)
                                                .set(profileData)
                                                .await()
                                            Log.d("PublicAppearanceScreen", "Profile saved successfully for userId: $userId")
                                            isLoading = false
                                            navController.navigate(Screen.UserProfile.createRoute(userId)) {
                                                popUpTo(Screen.PublicAppearance.route) { inclusive = true }
                                            }
                                        } catch (e: Exception) {
                                            isLoading = false
                                            errorMessage = "Failed to submit profile: ${e.message}"
                                            Log.e("PublicAppearanceScreen", "Error saving profile: ${e.message}", e)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading && (profileImageUri != null || user.profilePicture?.isNotEmpty() == true)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Submit")
                                }
                            }

                            errorMessage?.let { message ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } ?: run {
                    Text(
                        text = "No profile data available.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(
                        onClick = { navController.navigate(Screen.SignIn.route) },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Sign In to Create Profile")
                    }
                }
            }
        }
    }
}