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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.PartnerCard   // ← Fixed/Ensured Import
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun PartnerSearchFeatureScreen(navController: NavHostController) {
    val firestore: FirebaseFirestore = Firebase.firestore
    var searchQuery by remember { mutableStateOf("") }
    var partners by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Real-time Firestore listener
    DisposableEffect(Unit) {
        Log.d("PartnerSearch", "Setting up Firestore listener for partners")

        val listener = firestore.collection("profiles")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = "Failed to load partners: ${error.message}"
                    Log.e("PartnerSearch", "Snapshot error", error)
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    Log.d("PartnerSearch", "Snapshot received: ${snapshot.documents.size} documents")
                    coroutineScope.launch {
                        val partnerList = mutableListOf<UserProfile>()
                        for (doc in snapshot.documents) {
                            try {
                                val baseProfile = doc.toObject(UserProfile::class.java)
                                    ?: continue

                                // Try to fetch additional partner-specific data
                                val partnerDoc = try {
                                    firestore.collection("profiles")
                                        .document(doc.id)
                                        .collection("partner")
                                        .document("data")
                                        .get()
                                        .await()
                                } catch (e: Exception) {
                                    null
                                }

                                val partnerProfile = if (partnerDoc?.exists() == true) {
                                    baseProfile.copy(
                                        // Use ambitionStatement as fallback for expertise for now
                                        ambitionStatement = partnerDoc.getString("expertise")
                                            ?: baseProfile.ambitionStatement
                                    )
                                } else {
                                    baseProfile
                                }

                                partnerList.add(partnerProfile)
                            } catch (e: Exception) {
                                Log.e("PartnerSearch", "Error parsing partner ${doc.id}", e)
                            }
                        }
                        partners = partnerList
                        isLoading = false
                        Log.d("PartnerSearch", "Fetched ${partners.size} partners")
                    }
                } else {
                    isLoading = false
                }
            }

        onDispose {
            Log.d("PartnerSearch", "Removing Firestore listener")
            listener.remove()
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Partner Search") },
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
                    label = { Text("Search Partners") },
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
                onClick = { navController.navigate(Screen.PartnerSignUp.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Partner Sign Up")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Partner List
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                if (partners.isEmpty()) {
                    Text(
                        text = "No partners found.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    LazyColumn {
                        val filteredPartners = partners.filter { partner ->
                            partner.firstName?.contains(searchQuery, ignoreCase = true) == true ||
                                    partner.lastName?.contains(searchQuery, ignoreCase = true) == true ||
                                    partner.ambitionStatement?.contains(searchQuery, ignoreCase = true) == true
                        }
                        items(filteredPartners) { partner ->
                            PartnerCard(
                                profile = partner,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}