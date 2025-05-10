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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar

data class UserProfile(
    val profilePicture: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val linkedin: String? = null,
    val twitter: String? = null,
    val facebook: String? = null,
    val instagram: String? = null,
    val ambitionStatement: String? = null,
    val founderStatus: Boolean? = null,
    val founderEntries: List<String>? = null,
    val educationEntries: List<String>? = null,
    val workExperiences: List<String>? = null,
    val industries: List<String>? = null,
    val organizations: List<String>? = null,
    val hasInvestorProfile: Boolean? = null,
    // Investor Profile fields
    val investmentFirmName: String? = null,
    val professionalBackground: String? = null,
    val notableAchievements: String? = null,
    val preferredIndustries: List<String>? = null,
    val investmentStage: String? = null,
    val investmentRangeMin: String? = null,
    val investmentRangeMax: String? = null,
    val investmentApproach: String? = null,
    val strategicInvolvement: String? = null,
    val roiExpectations: String? = null,
    val portfolioCompanies: List<String>? = null,
    val successStories: List<String>? = null,
    val testimonials: List<String>? = null,
    val equityTerms: String? = null,
    val boardRole: String? = null,
    val returnTimeline: String? = null,
    val firmLogo: String? = null,
    // New fields
    val userId: String? = null,
    val isAdvisor: Boolean? = false,
    val isPartner: Boolean? = false,
    val expertise: String? = null,
    val experienceYears: Int? = 0,
    val email: String? = null,
    val bio: String? = null,
    val advisorPicture: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val firestore: FirebaseFirestore = Firebase.firestore
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
                .get(com.google.firebase.firestore.Source.SERVER)
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
                        println("Firestore Data: ${document.data}")
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

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopAppBar(
                title = { Text("Profile") },
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
                        // Profile Image and Name with Socials
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Profile Picture
                            val profilePicture = userData!!.profilePicture
                            println("Profile Picture URL: $profilePicture")
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
                            // Name and Socials
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                val firstName = userData!!.firstName ?: "Not provided"
                                val lastName = userData!!.lastName ?: "Not provided"
                                Text(
                                    text = "$firstName $lastName",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
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
                                println("Firm Logo URL: $firmLogo")
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

                        // Investor Button
                        if (!hasInvestorProfile) {
                            Button(
                                onClick = { navController.navigate(Screen.SelectUserType.route) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                Text("Sign Up as Investor")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}