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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.Investor
import com.phoenixcorp.founderfinder.data.RoleProfile
import com.phoenixcorp.founderfinder.data.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(navController: NavHostController, userId: String) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val firestore = FirebaseFirestore.getInstance()
    var userData by remember { mutableStateOf<UserProfile?>(null) }
    var investorData by remember { mutableStateOf<Investor?>(null) }
    var advisorProfile by remember { mutableStateOf<RoleProfile?>(null) }
    var partnerProfile by remember { mutableStateOf<RoleProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch user and investor data
    LaunchedEffect(userId) {
        Log.d("UserProfileScreen", "Navigated to UserProfileScreen with userId: $userId")
        if (userId.isEmpty()) {
            errorMessage = "Invalid user ID provided."
            isLoading = false
            Log.e("UserProfileScreen", "Empty userId")
            return@LaunchedEffect
        }
        coroutineScope.launch {
            try {
                Log.d("UserProfileScreen", "Fetching user profile for userId: $userId")
                // Fetch user profile
                val profileDoc = firestore.collection("profiles")
                    .document(userId)
                    .get()
                    .await()
                if (profileDoc.exists()) {
                    userData = profileDoc.toObject(UserProfile::class.java)?.copy(userId = userId)
                    Log.d("UserProfileScreen", "Profile data: $userData")
                } else {
                    errorMessage = "Profile not found for this user."
                    Log.e("UserProfileScreen", "Profile document not found for userId: $userId")
                }

                // Fetch investor profile
                val investorDoc = firestore.collection("investors")
                    .document(userId)
                    .get()
                    .await()
                if (investorDoc.exists()) {
                    investorData = investorDoc.toObject(Investor::class.java)
                    Log.d("UserProfileScreen", "Investor data: $investorData")
                }

                // Fetch advisor profile
                val advisorDoc = firestore.collection("profiles")
                    .document(userId)
                    .collection("advisor")
                    .document("data")
                    .get()
                    .await()
                if (advisorDoc.exists()) {
                    advisorProfile = advisorDoc.toObject(RoleProfile::class.java)
                    Log.d("UserProfileScreen", "Advisor profile: $advisorProfile")
                }

                // Fetch partner profile
                val partnerDoc = firestore.collection("profiles")
                    .document(userId)
                    .collection("partner")
                    .document("data")
                    .get()
                    .await()
                if (partnerDoc.exists()) {
                    partnerProfile = partnerDoc.toObject(RoleProfile::class.java)
                    Log.d("UserProfileScreen", "Partner profile: $partnerProfile")
                }

                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load profile: ${e.message}"
                isLoading = false
                Log.e("UserProfileScreen", "Error fetching profile: ${e.stackTraceToString()}", e)
            }
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text(userData?.let { "${it.firstName ?: "Unknown"} ${it.lastName ?: "User"}" } ?: "Profile") },
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
                CircularProgressIndicator()
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                if (currentUser != null && currentUser.uid == userId) {
                    Button(
                        onClick = { navController.navigate(Screen.PublicAppearance.route) },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Create Your Profile")
                    }
                } else {
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Go Back")
                    }
                }
            } else {
                userData?.let { user ->
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            // Profile Image
                            user.profilePicture?.takeIf { it.isNotEmpty() }?.let { picture ->
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(picture)
                                            .crossfade(true)
                                            .placeholder(R.drawable.ic_profile_placeholder)
                                            .error(R.drawable.ic_profile_placeholder)
                                            .build(),
                                        onError = { error -> Log.e("UserProfileScreen", "Coil Error: ${error.result.throwable.message}") }
                                    ),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } ?: Image(
                                painter = painterResource(id = R.drawable.ic_profile_placeholder),
                                contentDescription = "Default Profile Picture",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Name
                            Text(
                                text = "${user.firstName ?: "Unknown"} ${user.lastName ?: "User"}",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Social Links
                            user.twitter?.takeIf { it.isNotEmpty() }?.let {
                                Text(
                                    text = "@$it",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            user.linkedin?.takeIf { it.isNotEmpty() }?.let {
                                Text(
                                    text = "LinkedIn: $it",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            user.facebook?.takeIf { it.isNotEmpty() }?.let {
                                Text(
                                    text = "Facebook: $it",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            user.instagram?.takeIf { it.isNotEmpty() }?.let {
                                Text(
                                    text = "Instagram: $it",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Bio
                            user.bio?.takeIf { it.isNotEmpty() }?.let {
                                Text(
                                    text = "Bio",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Ambition Statement
                            user.ambitionStatement?.takeIf { it.isNotEmpty() }?.let {
                                Text(
                                    text = "Ambition Statement",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Founder Status
                            Text(
                                text = "Founder Status",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            val founderStatus = user.founderStatus ?: false
                            if (founderStatus) {
                                user.founderEntries?.filter { it.isNotBlank() }?.forEach { entry ->
                                    Text(
                                        text = entry,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            } else {
                                Text(
                                    text = "Not a Founder",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Education
                            Text(
                                text = "Education",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            user.educationEntries?.takeIf { it.isNotEmpty() }?.filter { it.isNotEmpty() }?.forEach { entry ->
                                Text(
                                    text = entry,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            } ?: Text(
                                text = "Not provided",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Work Experience
                            Text(
                                text = "Work Experience",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            user.workExperiences?.takeIf { it.isNotEmpty() }?.filter { it.isNotEmpty() }?.forEach { entry ->
                                Text(
                                    text = entry,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            } ?: Text(
                                text = "Not provided",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Industries of Interest
                            Text(
                                text = "Industries of Interest",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            user.industries?.takeIf { it.isNotEmpty() }?.filter { it.isNotEmpty() }?.forEach { entry ->
                                Text(
                                    text = entry,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            } ?: Text(
                                text = "Not provided",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Organizations of Interest
                            Text(
                                text = "Organizations of Interest",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            user.organizations?.takeIf { it.isNotEmpty() }?.filter { it.isNotEmpty() }?.forEach { entry ->
                                Text(
                                    text = entry,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            } ?: Text(
                                text = "Not provided",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Advisor Profile
                            advisorProfile?.let { advisor ->
                                Text(
                                    text = "Advisor Profile",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                advisor.expertise?.let {
                                    Text(
                                        text = "Expertise: $it",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                advisor.experienceYears?.let {
                                    Text(
                                        text = "Experience: $it years",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Partner Profile
                            partnerProfile?.let { partner ->
                                Text(
                                    text = "Partner Profile",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                partner.expertise?.let {
                                    Text(
                                        text = "Expertise: $it",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                partner.experienceYears?.let {
                                    Text(
                                        text = "Experience: $it years",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Investor Information
                            investorData?.let { investor ->
                                Text(
                                    text = "Investor Information",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Primary Industry",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = investor.industry.ifEmpty { "Not provided" },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Philosophy",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = investor.philosophy.ifEmpty { "Not provided" },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                if (investor.preferredIndustries.isNotEmpty()) {
                                    Text(
                                        text = "Preferred Industries",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = investor.preferredIndustries.joinToString(", "),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                Text(
                                    text = "Investment Stage",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = investor.investmentStage.ifEmpty { "Not provided" },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Investment Range",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "\$${investor.investmentRangeMin.ifEmpty { "N/A" }} - \$${investor.investmentRangeMax.ifEmpty { "N/A" }}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Approach",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = investor.approachAndInvolvement.ifEmpty { "Not provided" },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "ROI Expectations",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = investor.roiExpectations.ifEmpty { "Not provided" },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                if (investor.portfolioCompanies.isNotEmpty()) {
                                    Text(
                                        text = "Portfolio Companies",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = investor.portfolioCompanies.joinToString(", "),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                if (investor.testimonials.isNotEmpty()) {
                                    Text(
                                        text = "Testimonials",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = investor.testimonials.joinToString("; "),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                Text(
                                    text = "Equity Terms",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = investor.equityTerms.ifEmpty { "Not provided" },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Board Role",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = investor.boardRole.ifEmpty { "Not provided" },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Return Timeline",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = investor.returnTimeline.ifEmpty { "Not provided" },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}