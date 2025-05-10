package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.AdvisorCard
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

@Composable
fun AdvisorSearchFeatureScreen(navController: NavHostController) {
    val firestore: FirebaseFirestore = Firebase.firestore
    var searchQuery by remember { mutableStateOf("") }
    var advisors by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableStateOf(0) } // Manual refresh trigger

    // Real-time Firestore listener
    DisposableEffect(Unit) {
        Log.d("AdvisorSearch", "Setting up Firestore listener for advisors")
        val listener = firestore.collection("profiles")
            .whereEqualTo("advisor", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = "Failed to load advisors: ${error.message}"
                    Log.e("AdvisorSearch", "Snapshot error: ${error.message}", error)
                    isLoading = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    Log.d("AdvisorSearch", "Snapshot received: ${snapshot.documents.size} documents")
                    advisors = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        Log.d("AdvisorSearch", "Document: id=${doc.id}, data=$data")
                        val profile = doc.toObject(UserProfile::class.java)?.copy(userId = doc.id)
                        if (profile == null) {
                            Log.w("AdvisorSearch", "Failed to deserialize document: ${doc.id}")
                        } else {
                            Log.d("AdvisorSearch", "Fetched profile: ${profile.firstName} ${profile.lastName}, userId: ${doc.id}")
                        }
                        profile
                    }
                    isLoading = false
                    Log.d("AdvisorSearch", "Snapshot updated: ${advisors.size} advisors")
                } else {
                    Log.w("AdvisorSearch", "Snapshot is null")
                }
            }

        onDispose {
            Log.d("AdvisorSearch", "Disposing Firestore listener")
            listener.remove()
        }
    }

    // Fallback one-time query on load and manual refresh
    LaunchedEffect(refreshTrigger) {
        coroutineScope.launch {
            try {
                Log.d("AdvisorSearch", "Executing fallback one-time query, refreshTrigger: $refreshTrigger")
                val snapshot = firestore.collection("profiles")
                    .whereEqualTo("advisor", true)
                    .get()
                    .await()
                Log.d("AdvisorSearch", "Fallback query snapshot: ${snapshot.documents.size} documents")
                advisors = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data
                    Log.d("AdvisorSearch", "Fallback document: id=${doc.id}, data=$data")
                    val profile = doc.toObject(UserProfile::class.java)?.copy(userId = doc.id)
                    if (profile == null) {
                        Log.w("AdvisorSearch", "Failed to deserialize fallback document: ${doc.id}")
                    } else {
                        Log.d("AdvisorSearch", "Fallback fetched profile: ${profile.firstName} ${profile.lastName}, userId: ${doc.id}")
                    }
                    profile
                }
                isLoading = false
                Log.d("AdvisorSearch", "Fallback query completed: ${advisors.size} advisors")
            } catch (e: Exception) {
                errorMessage = "Failed to load advisors (fallback): ${e.message}"
                isLoading = false
                Log.e("AdvisorSearch", "Fallback query error: ${e.message}", e)
            }
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = "Find Advisors",
                navController = navController,
                showMailButton = true
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Search Bar with Enter Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Advisors") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { /* Search handled via state */ }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { /* Search handled via state */ }) {
                    Text("Enter")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Advisor Sign-Up Button
            Button(
                onClick = { navController.navigate(Screen.AdvisorSignUp.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Advisor Sign Up")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Refresh Button
            Button(
                onClick = {
                    Log.d("AdvisorSearch", "Manual refresh button clicked")
                    refreshTrigger++
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh Advisors")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Advisor List
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                if (advisors.isEmpty()) {
                    Text(
                        text = "No advisors found.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    LazyColumn {
                        val filteredAdvisors = advisors.filter { advisor ->
                            advisor.firstName?.contains(searchQuery, ignoreCase = true) == true ||
                                    advisor.lastName?.contains(searchQuery, ignoreCase = true) == true ||
                                    advisor.expertise?.contains(searchQuery, ignoreCase = true) == true
                        }
                        items(filteredAdvisors.size) { index ->
                            val advisor = filteredAdvisors[index]
                            AdvisorCard(
                                profile = advisor,
                                onCardClick = {
                                    val userId = advisor.userId
                                    if (userId != null && userId.isNotEmpty()) {
                                        Log.d("AdvisorSearch", "Navigating to UserProfile with userId: $userId")
                                        navController.navigate(Screen.UserProfile.createRoute(userId))
                                    } else {
                                        Log.e("AdvisorSearch", "Invalid userId for profile navigation")
                                    }
                                },
                                onMessageClick = {
                                    val userId = advisor.userId
                                    if (userId != null && userId.isNotEmpty()) {
                                        Log.d("AdvisorSearch", "Navigating to PrivateMessages with recipientId: $userId")
                                        navController.navigate(Screen.PrivateMessages.createRoute(userId))
                                    } else {
                                        Log.e("AdvisorSearch", "Invalid userId for messaging")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}