package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.AdvisorCard
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AdvisorSearchFeatureScreen(navController: NavHostController) {
    val firestore = Firebase.firestore
    var searchQuery by remember { mutableStateOf("") }
    var advisors by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Real-time Firestore listener
    DisposableEffect(Unit) {
        Log.d("AdvisorSearch", "Setting up Firestore listener for advisors")

        val listener = firestore.collection("profiles")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = "Failed to load advisors: ${error.message}"
                    Log.e("AdvisorSearch", "Snapshot error", error)
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    coroutineScope.launch {
                        val advisorList = mutableListOf<UserProfile>()
                        for (doc in snapshot.documents) {
                            try {
                                val baseProfile = doc.toObject(UserProfile::class.java)
                                    ?: continue

                                // Try to fetch additional advisor-specific data (if exists)
                                val advisorDoc = try {
                                    firestore.collection("profiles")
                                        .document(doc.id)
                                        .collection("advisor")
                                        .document("data")
                                        .get()
                                        .await()
                                } catch (e: Exception) {
                                    null
                                }

                                val advisorProfile = if (advisorDoc?.exists() == true) {
                                    baseProfile.copy(
                                        // Add advisor-specific fields safely using copy with defaults
                                        isFounder = baseProfile.isFounder,
                                        // You can extend UserProfile later with expertise, etc.
                                        ambitionStatement = advisorDoc.getString("expertise")
                                            ?: baseProfile.ambitionStatement
                                    )
                                } else {
                                    baseProfile
                                }

                                advisorList.add(advisorProfile)
                            } catch (e: Exception) {
                                Log.e("AdvisorSearch", "Error parsing advisor ${doc.id}", e)
                            }
                        }
                        advisors = advisorList
                        isLoading = false
                        Log.d("AdvisorSearch", "Fetched ${advisors.size} advisors")
                    }
                } else {
                    isLoading = false
                }
            }

        onDispose {
            Log.d("AdvisorSearch", "Removing Firestore listener")
            listener.remove()
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Advisor Search") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Search Bar
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
                    keyboardActions = KeyboardActions(onSearch = { }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { /* Live filtering */ }) {
                    Text("Search")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate(Screen.AdvisorSignUp.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Become an Advisor")
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
                                    advisor.ambitionStatement?.contains(searchQuery, ignoreCase = true) == true
                        }
                        items(filteredAdvisors) { advisor ->
                            AdvisorCard(
                                profile = advisor,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}