package com.phoenixcorp.founderfinder.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.navigation.Screen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicAppearanceScreen(navController: NavHostController) {
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        profileImageUri = uri
    }
    val coroutineScope = rememberCoroutineScope()
    val firestore: FirebaseFirestore = Firebase.firestore
    val storage = Firebase.storage
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userData by remember { mutableStateOf<UserProfile?>(null) }

    // Fetch user data when screen loads
    LaunchedEffect(Unit) {
        if (currentUser != null) {
            firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userData = UserProfile(
                            profilePicture = document.getString("profilePicture"),
                            firstName = document.getString("firstName"),
                            lastName = document.getString("lastName"),
                            linkedin = document.getString("linkedin"),
                            twitter = document.getString("twitter"),
                            facebook = document.getString("facebook"),
                            instagram = document.getString("instagram"),
                            ambitionStatement = document.getString("ambitionStatement"),
                            founderStatus = document.getBoolean("founderStatus"),
                            founderEntries = document.get("founderEntries") as? List<String>,
                            educationEntries = document.get("educationEntries") as? List<String>,
                            workExperiences = document.get("workExperiences") as? List<String>,
                            industries = document.get("industries") as? List<String>,
                            organizations = document.get("organizations") as? List<String>,
                            hasInvestorProfile = document.getBoolean("hasInvestorProfile"),
                            investmentFirmName = document.getString("investmentFirmName"),
                            professionalBackground = document.getString("professionalBackground"),
                            notableAchievements = document.getString("notableAchievements"),
                            preferredIndustries = document.get("preferredIndustries") as? List<String>,
                            investmentStage = document.getString("investmentStage"),
                            investmentRangeMin = document.getString("investmentRangeMin"),
                            investmentRangeMax = document.getString("investmentRangeMax"),
                            investmentApproach = document.getString("investmentApproach"),
                            strategicInvolvement = document.getString("strategicInvolvement"),
                            roiExpectations = document.getString("roiExpectations"),
                            portfolioCompanies = document.get("portfolioCompanies") as? List<String>,
                            successStories = document.get("successStories") as? List<String>,
                            testimonials = document.get("testimonials") as? List<String>,
                            equityTerms = document.getString("equityTerms"),
                            boardRole = document.getString("boardRole"),
                            returnTimeline = document.getString("returnTimeline"),
                            firmLogo = document.getString("firmLogo")
                        )
                    } else {
                        errorMessage = "No profile data found."
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    errorMessage = "Failed to load data: ${e.message}"
                    isLoading = false
                }
        } else {
            errorMessage = "You must be logged in to view your profile."
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("Public Appearance") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else if (userData != null) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(text = "Choose your profile picture", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileImageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(profileImageUri),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val savedUri = userData!!.profilePicture
                            if (savedUri != null && savedUri.isNotEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(savedUri)
                                            .crossfade(true)
                                            .placeholder(R.drawable.ic_profile_placeholder)
                                            .error(R.drawable.ic_profile_placeholder)
                                            .build(),
                                        onError = { error -> println("Coil Error: ${error.result.throwable.message}") }
                                    ),
                                    contentDescription = "Saved Profile Picture",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_profile_placeholder),
                                    contentDescription = "Default Profile Picture",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
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
                    Text("Profile Summary", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    val firstName = userData!!.firstName ?: "Not provided"
                    val lastName = userData!!.lastName ?: "Not provided"
                    Text(
                        text = "Name: $firstName $lastName",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Education", style = MaterialTheme.typography.titleMedium)
                    val educationEntries = userData!!.educationEntries ?: emptyList()
                    if (educationEntries.isNotEmpty()) {
                        educationEntries.filter { it.isNotEmpty() }.forEach { entry ->
                            Text(text = entry, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Text(text = "Not provided", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Work Experience", style = MaterialTheme.typography.titleMedium)
                    val workExperiences = userData!!.workExperiences ?: emptyList()
                    if (workExperiences.isNotEmpty()) {
                        workExperiences.filter { it.isNotEmpty() }.forEach { entry ->
                            Text(text = entry, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Text(text = "Not provided", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Founder Status", style = MaterialTheme.typography.titleMedium)
                    val founderStatus = userData!!.founderStatus ?: false
                    Text(
                        text = if (founderStatus) "Founder" else "Not a Founder",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (founderStatus) {
                        val founderEntries = userData!!.founderEntries ?: emptyList()
                        founderEntries.filter { it.isNotEmpty() }.forEach { entry ->
                            Text(text = entry, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ambition Statement", style = MaterialTheme.typography.titleMedium)
                    val ambition = userData!!.ambitionStatement ?: "Not provided"
                    Text(
                        text = ambition,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Social Links", style = MaterialTheme.typography.titleMedium)
                    val linkedin = userData!!.linkedin ?: "Not provided"
                    val twitter = userData!!.twitter ?: "Not provided"
                    val facebook = userData!!.facebook ?: "Not provided"
                    val instagram = userData!!.instagram ?: "Not provided"
                    Text(text = "LinkedIn: $linkedin", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Twitter: $twitter", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Facebook: $facebook", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Instagram: $instagram", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Industries of Interest", style = MaterialTheme.typography.titleMedium)
                    val industries = userData!!.industries ?: emptyList()
                    if (industries.isNotEmpty()) {
                        industries.filter { it.isNotEmpty() }.forEach { entry ->
                            Text(text = entry, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Text(text = "Not provided", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Organizations of Interest", style = MaterialTheme.typography.titleMedium)
                    val organizations = userData!!.organizations ?: emptyList()
                    if (organizations.isNotEmpty()) {
                        organizations.filter { it.isNotEmpty() }.forEach { entry ->
                            Text(text = entry, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Text(text = "Not provided", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button (for saving profile picture)
            Button(
                onClick = {
                    if (currentUser == null) {
                        errorMessage = "You must be logged in to save your profile."
                        return@Button
                    }
                    if (profileImageUri == null && (userData!!.profilePicture ?: "").isEmpty()) {
                        errorMessage = "Please select a profile image."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null
                    val userId = currentUser.uid

                    coroutineScope.launch {
                        try {
                            // If a new image is selected, upload it to Firebase Storage
                            val downloadUrl = if (profileImageUri != null) {
                                val storageRef = storage.reference
                                    .child("profilePictures/$userId/profile.jpg")
                                storageRef.putFile(profileImageUri!!).await()
                                storageRef.downloadUrl.await().toString()
                            } else {
                                // Use existing URL if no new image is selected
                                userData!!.profilePicture ?: ""
                            }

                            // Update Firestore with the download URL
                            val updatedData = mapOf("profilePicture" to downloadUrl)
                            firestore.collection("users")
                                .document(userId)
                                .update(updatedData)
                                .addOnSuccessListener {
                                    isLoading = false
                                    navController.navigate(Screen.Profile.route) {
                                        popUpTo(Screen.PublicAppearance.route) { inclusive = true }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = "Failed to save: ${e.message}"
                                }
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Failed to upload image: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
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

            Spacer(modifier = Modifier.height(8.dp))

            // Publish Profile Button
            Button(
                onClick = {
                    if (currentUser == null) {
                        errorMessage = "You must be logged in to publish your profile."
                        return@Button
                    }
                    if (userData!!.profilePicture == null || userData!!.profilePicture!!.isEmpty()) {
                        errorMessage = "A profile picture is required to publish your profile."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    val userId = currentUser.uid
                    coroutineScope.launch {
                        try {
                            // Save to profiles collection
                            val profileData = mapOf(
                                "userId" to userId,
                                "profilePicture" to (userData!!.profilePicture ?: ""),
                                "firstName" to (userData!!.firstName ?: ""),
                                "lastName" to (userData!!.lastName ?: ""),
                                "linkedin" to (userData!!.linkedin ?: ""),
                                "twitter" to (userData!!.twitter ?: ""),
                                "facebook" to (userData!!.facebook ?: ""),
                                "instagram" to (userData!!.instagram ?: ""),
                                "ambitionStatement" to (userData!!.ambitionStatement ?: ""),
                                "founderStatus" to (userData!!.founderStatus ?: false),
                                "founderEntries" to (userData!!.founderEntries ?: emptyList()),
                                "educationEntries" to (userData!!.educationEntries ?: emptyList()),
                                "workExperiences" to (userData!!.workExperiences ?: emptyList()),
                                "industries" to (userData!!.industries ?: emptyList()),
                                "organizations" to (userData!!.organizations ?: emptyList()),
                                "hasInvestorProfile" to (userData!!.hasInvestorProfile ?: false),
                                "investmentFirmName" to (userData!!.investmentFirmName ?: ""),
                                "professionalBackground" to (userData!!.professionalBackground ?: ""),
                                "notableAchievements" to (userData!!.notableAchievements ?: ""),
                                "preferredIndustries" to (userData!!.preferredIndustries ?: emptyList()),
                                "investmentStage" to (userData!!.investmentStage ?: ""),
                                "investmentRangeMin" to (userData!!.investmentRangeMin ?: ""),
                                "investmentRangeMax" to (userData!!.investmentRangeMax ?: ""),
                                "investmentApproach" to (userData!!.investmentApproach ?: ""),
                                "strategicInvolvement" to (userData!!.strategicInvolvement ?: ""),
                                "roiExpectations" to (userData!!.roiExpectations ?: ""),
                                "portfolioCompanies" to (userData!!.portfolioCompanies ?: emptyList()),
                                "successStories" to (userData!!.successStories ?: emptyList()),
                                "testimonials" to (userData!!.testimonials ?: emptyList()),
                                "equityTerms" to (userData!!.equityTerms ?: ""),
                                "boardRole" to (userData!!.boardRole ?: ""),
                                "returnTimeline" to (userData!!.returnTimeline ?: ""),
                                "firmLogo" to (userData!!.firmLogo ?: "")
                            )
                            firestore.collection("profiles")
                                .document(userId)
                                .set(profileData)
                                .addOnSuccessListener {
                                    isLoading = false
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.PublicAppearance.route) { inclusive = true }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = "Failed to publish profile: ${e.message}"
                                }
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Error: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && (profileImageUri != null || (userData!!.profilePicture?.isNotEmpty() == true))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Publish Profile")
                }
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PublicAppearanceScreenPreview() {
    PublicAppearanceScreen(navController = rememberNavController())
}