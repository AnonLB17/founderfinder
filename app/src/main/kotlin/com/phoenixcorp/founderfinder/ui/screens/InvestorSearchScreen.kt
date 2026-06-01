package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.Investor
import com.phoenixcorp.founderfinder.data.Organizations
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.InvestorCard
import com.phoenixcorp.founderfinder.ui.components.OrganizationCard
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestorSearchScreen(navController: NavHostController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val coroutineScope = rememberCoroutineScope()
    var investors by remember { mutableStateOf<List<Investor>>(emptyList()) }
    var organizations by remember { mutableStateOf<List<Organizations>>(emptyList()) }
    var isInvestorSearch by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch data based on mode
    LaunchedEffect(isInvestorSearch) {
        if (currentUser == null) {
            Log.e("InvestorSearchScreen", "User not authenticated")
            errorMessage = "Please sign in to view content."
            isLoading = false
            Toast.makeText(context, "Please sign in", Toast.LENGTH_SHORT).show()
            navController.navigate(Screen.SignIn.route) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
            return@LaunchedEffect
        }
        try {
            if (isInvestorSearch) {
                Log.d("InvestorSearchScreen", "Fetching investors for user: ${currentUser.uid}")
                val snapshot = firestore.collection("investors")
                    .get()
                    .await()
                investors = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Fetch profile picture from profiles collection
                        val profileDoc = firestore.collection("profiles")
                            .document(doc.getString("userId") ?: "")
                            .get()
                            .await()
                        val profilePicture = profileDoc.getString("profilePicture")

                        val investor = Investor(
                            name = doc.getString("name") ?: "",
                            email = doc.getString("email") ?: "",
                            industry = doc.getString("industry") ?: "",
                            philosophy = doc.getString("philosophy") ?: "",
                            userId = doc.getString("userId") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            preferredIndustries = doc.get("preferredIndustries")?.let { field ->
                                if (field is List<*> && field.all { it is String }) field as List<String> else emptyList()
                            } ?: emptyList(),
                            investmentStage = doc.getString("investmentStage") ?: "",
                            investmentRangeMin = doc.getString("investmentRangeMin") ?: "",
                            investmentRangeMax = doc.getString("investmentRangeMax") ?: "",
                            approachAndInvolvement = doc.getString("approachAndInvolvement") ?: "",
                            roiExpectations = doc.getString("roiExpectations") ?: "",
                            portfolioCompanies = doc.get("portfolioCompanies")?.let { field ->
                                if (field is List<*> && field.all { it is String }) field as List<String> else emptyList()
                            } ?: emptyList(),
                            testimonials = doc.get("testimonials")?.let { field ->
                                if (field is List<*> && field.all { it is String }) field as List<String> else emptyList()
                            } ?: emptyList(),
                            equityTerms = doc.getString("equityTerms") ?: "",
                            boardRole = doc.getString("boardRole") ?: "",
                            returnTimeline = doc.getString("returnTimeline") ?: "",
                            profilePicture = profilePicture
                        )
                        Log.d("InvestorSearchScreen", "Fetched investor: ${investor.name}, Industries: ${investor.preferredIndustries}, Stage: ${investor.investmentStage}")
                        investor
                    } catch (e: Exception) {
                        Log.e("InvestorSearchScreen", "Error parsing investor ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                if (investors.isEmpty()) {
                    Log.w("InvestorSearchScreen", "No investors found in Firestore")
                    errorMessage = "No investors available."
                }
                Log.d("InvestorSearchScreen", "Fetched ${investors.size} investors")
            } else {
                Log.d("InvestorSearchScreen", "Fetching organizations for user: ${currentUser.uid}")
                val snapshot = firestore.collection("organizations")
                    .get()
                    .await()
                organizations = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Organizations::class.java)?.copy(orgId = doc.id)
                    } catch (e: Exception) {
                        Log.e("InvestorSearchScreen", "Error parsing organization ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                if (organizations.isEmpty()) {
                    Log.w("InvestorSearchScreen", "No organizations found in Firestore")
                    errorMessage = "No organizations available."
                }
                Log.d("InvestorSearchScreen", "Fetched ${organizations.size} organizations")
            }
            isLoading = false
        } catch (e: Exception) {
            Log.e("InvestorSearchScreen", "Error fetching data: ${e.message}", e)
            errorMessage = "Failed to load data: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Investor Search") },
                navController = navController,
                showBackButton = true,
                showInvestorAddButton = true,
                showMailButton = true,
                onMailClick = {
                    Log.d("InvestorSearchScreen", "Navigating to PrivateMessagesScreen")
                    navController.navigate(Screen.PrivateMessages.route)
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Button(
                onClick = { isInvestorSearch = !isInvestorSearch },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(bottom = 16.dp)
            ) {
                Text(if (isInvestorSearch) "Organization Search" else "Investor Search")
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search") },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(bottom = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (isInvestorSearch) {
                    val lowerQuery = searchQuery.lowercase()
                    val filteredInvestors = investors.filter { investor ->
                        investor.name.lowercase().contains(lowerQuery) ||
                                investor.industry.lowercase().contains(lowerQuery) ||
                                investor.philosophy.lowercase().contains(lowerQuery) ||
                                investor.preferredIndustries.any { it.lowercase().contains(lowerQuery) } ||
                                investor.investmentStage.lowercase().contains(lowerQuery) ||
                                investor.approachAndInvolvement.lowercase().contains(lowerQuery) ||
                                investor.roiExpectations.lowercase().contains(lowerQuery) ||
                                investor.portfolioCompanies.any { it.lowercase().contains(lowerQuery) } ||
                                investor.testimonials.any { it.lowercase().contains(lowerQuery) } ||
                                investor.equityTerms.lowercase().contains(lowerQuery) ||
                                investor.boardRole.lowercase().contains(lowerQuery) ||
                                investor.returnTimeline.lowercase().contains(lowerQuery)
                    }
                    if (filteredInvestors.isNotEmpty()) {
                        val investor = filteredInvestors.first()
                        InvestorCard(
                            investor = investor,
                            navController = navController,
                            onSwipe = {
                                coroutineScope.launch {
                                    investors = investors.filter { it != investor }
                                }
                            }
                        )
                    } else {
                        Text(
                            if (investors.isEmpty()) "No more investors to show" else "No matching investors",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    val lowerQuery = searchQuery.lowercase()
                    val filteredOrganizations = organizations.filter { org ->
                        org.name.lowercase().contains(lowerQuery) ||
                                org.description.lowercase().contains(lowerQuery)
                    }
                    if (filteredOrganizations.isNotEmpty()) {
                        val organization = filteredOrganizations.first()
                        OrganizationCard(
                            organization = organization,
                            invitationId = "",
                            navController = navController,
                            onSwipe = {
                                coroutineScope.launch {
                                    organizations = organizations.filter { it != organization }
                                }
                            }
                        )
                    } else {
                        Text(
                            if (organizations.isEmpty()) "No more organizations to show" else "No matching organizations",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}