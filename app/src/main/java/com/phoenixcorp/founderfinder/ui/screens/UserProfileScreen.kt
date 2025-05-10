package com.phoenixcorp.founderfinder.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(navController: NavHostController, userId: String) {
    val firestore: FirebaseFirestore = Firebase.firestore
    var userData by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch user profile data from profiles collection
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val document = firestore.collection("profiles")
                    .document(userId)
                    .get()
                    .await()
                if (document.exists()) {
                    userData = document.toObject(UserProfile::class.java)?.copy(userId = document.id)
                } else {
                    errorMessage = "Profile not found."
                }
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load profile: ${e.message}"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = "User Profile",
                navController = navController,
                showBackButton = true
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                        // Profile Image and Name with Socials
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val profilePicture = userData!!.profilePicture
                            if (profilePicture != null && profilePicture.isNotEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(profilePicture)
                                            .crossfade(true)
                                            .placeholder(R.drawable.ic_profile_placeholder)
                                            .error(R.drawable.ic_profile_placeholder)
                                            .build(),
                                        onError = { error -> println("Coil Error: ${error.result.throwable.message}") }
                                    ),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.size(80.dp)
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_profile_placeholder),
                                    contentDescription = "Default Profile Picture",
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "${userData!!.firstName ?: "Unknown"} ${userData!!.lastName ?: "User"}",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                val twitter = userData!!.twitter
                                if (twitter != null && twitter.isNotEmpty()) {
                                    Text(
                                        text = "@$twitter",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                val linkedin = userData!!.linkedin
                                if (linkedin != null && linkedin.isNotEmpty()) {
                                    Text(
                                        text = "LinkedIn: $linkedin",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                val facebook = userData!!.facebook
                                if (facebook != null && facebook.isNotEmpty()) {
                                    Text(
                                        text = "Facebook: $facebook",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                val instagram = userData!!.instagram
                                if (instagram != null && instagram.isNotEmpty()) {
                                    Text(
                                        text = "Instagram: $instagram",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Ambition Statement
                        Text(
                            text = "Ambition Statement",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        val ambition = userData!!.ambitionStatement ?: "Not provided"
                        Text(
                            text = ambition,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Founder Status
                        Text(
                            text = "Founder Status",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        val founderStatus = userData!!.founderStatus ?: false
                        Text(
                            text = if (founderStatus) "Founder" else "Not a Founder",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        if (founderStatus) {
                            val founderEntries = userData!!.founderEntries ?: emptyList()
                            founderEntries.filter { it.isNotEmpty() }.forEach { entry ->
                                Text(
                                    text = entry,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Education
                        Text(
                            text = "Education",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        val educationEntries = userData!!.educationEntries ?: emptyList()
                        if (educationEntries.isNotEmpty()) {
                            educationEntries.filter { it.isNotEmpty() }.forEach { entry ->
                                Text(
                                    text = entry,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        } else {
                            Text(
                                text = "Not provided",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Work Experience
                        Text(
                            text = "Work Experience",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        val workExperiences = userData!!.workExperiences ?: emptyList()
                        if (workExperiences.isNotEmpty()) {
                            workExperiences.filter { it.isNotEmpty() }.forEach { entry ->
                                Text(
                                    text = entry,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        } else {
                            Text(
                                text = "Not provided",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Industries of Interest
                        Text(
                            text = "Industries of Interest",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        val industries = userData!!.industries ?: emptyList()
                        if (industries.isNotEmpty()) {
                            industries.filter { it.isNotEmpty() }.forEach { entry ->
                                Text(
                                    text = entry,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        } else {
                            Text(
                                text = "Not provided",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Organizations of Interest
                        Text(
                            text = "Organizations of Interest",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        val organizations = userData!!.organizations ?: emptyList()
                        if (organizations.isNotEmpty()) {
                            organizations.filter { it.isNotEmpty() }.forEach { entry ->
                                Text(
                                    text = entry,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        } else {
                            Text(
                                text = "Not provided",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Investor Profile Section
                        val hasInvestorProfile = userData!!.hasInvestorProfile ?: false
                        if (hasInvestorProfile) {
                            Text(
                                text = "Investor Profile",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Firm Logo
                                val firmLogo = userData!!.firmLogo
                                if (firmLogo != null && firmLogo.isNotEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(firmLogo)
                                                .crossfade(true)
                                                .error(R.drawable.ic_profile_placeholder)
                                                .placeholder(R.drawable.ic_profile_placeholder)
                                                .build(),
                                            onError = { error -> println("Coil Error (Firm Logo): ${error.result.throwable.message}") }
                                        ),
                                        contentDescription = "Investment Firm Logo",
                                        modifier = Modifier.size(80.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                // Investment Firm Name
                                val investmentFirmName = userData!!.investmentFirmName ?: "Not provided"
                                Text(
                                    text = investmentFirmName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Professional Background
                            Text(
                                text = "Professional Background",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            val professionalBackground = userData!!.professionalBackground ?: "Not provided"
                            Text(
                                text = professionalBackground,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Notable Achievements
                            Text(
                                text = "Notable Achievements",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            val notableAchievements = userData!!.notableAchievements ?: "Not provided"
                            Text(
                                text = notableAchievements,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Preferred Industries
                            Text(
                                text = "Preferred Industries",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            val preferredIndustries = userData!!.preferredIndustries ?: emptyList()
                            if (preferredIndustries.isNotEmpty()) {
                                preferredIndustries.filter { it.isNotEmpty() }.forEach { entry ->
                                    Text(
                                        text = entry,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Not provided",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Investment Stage
                            Text(
                                text = "Investment Stage",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            val investmentStage = userData!!.investmentStage ?: "Not provided"
                            Text(
                                text = investmentStage,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Investment Range
                            Text(
                                text = "Investment Range",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            val minRange = userData!!.investmentRangeMin ?: "Not provided"
                            val maxRange = userData!!.investmentRangeMax ?: "Not provided"
                            Text(
                                text = "$minRange - $maxRange",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Investment Approach
                            Text(
                                text = "Investment Approach",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            val investmentApproach = userData!!.investmentApproach ?: "Not provided"
                            Text(
                                text = investmentApproach,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Strategic Involvement
                            Text(
                                text = "Strategic Involvement",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            val strategicInvolvement = userData!!.strategicInvolvement ?: "Not provided"
                            Text(
                                text = strategicInvolvement,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // ROI Expectations
                            Text(
                                text = "ROI Expectations",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            val roiExpectations = userData!!.roiExpectations ?: "Not provided"
                            Text(
                                text = roiExpectations,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Portfolio Companies
                            Text(
                                text = "Portfolio Companies",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            val portfolioCompanies = userData!!.portfolioCompanies ?: emptyList()
                            if (portfolioCompanies.isNotEmpty()) {
                                portfolioCompanies.filter { it.isNotEmpty() }.forEach { entry ->
                                    Text(
                                        text = entry,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Not provided",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Success Stories
                            Text(
                                text = "Success Stories",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            val successStories = userData!!.successStories ?: emptyList()
                            if (successStories.isNotEmpty()) {
                                successStories.filter { it.isNotEmpty() }.forEach { entry ->
                                    Text(
                                        text = entry,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Not provided",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Testimonials
                            Text(
                                text = "Testimonials",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            val testimonials = userData!!.testimonials ?: emptyList()
                            if (testimonials.isNotEmpty()) {
                                testimonials.filter { it.isNotEmpty() }.forEach { entry ->
                                    Text(
                                        text = entry,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Not provided",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Equity Terms
                            Text(
                                text = "Equity Terms",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            val equityTerms = userData!!.equityTerms ?: "Not provided"
                            Text(
                                text = equityTerms,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Board Role
                            Text(
                                text = "Board Role",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            val boardRole = userData!!.boardRole ?: "Not provided"
                            Text(
                                text = boardRole,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Return Timeline
                            Text(
                                text = "Return Timeline",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            val returnTimeline = userData!!.returnTimeline ?: "Not provided"
                            Text(
                                text = returnTimeline,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}