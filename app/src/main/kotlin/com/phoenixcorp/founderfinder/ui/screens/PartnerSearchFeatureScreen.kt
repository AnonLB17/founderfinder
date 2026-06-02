package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
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
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.phoenixcorp.founderfinder.data.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
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
    var refreshTrigger by remember { mutableStateOf(0) }

    // Real-time Firestore listener
    DisposableEffect(Unit) {
        Log.d("PartnerSearch", "Setting up Firestore listener for partners")
        val listener = firestore.collection("profiles")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = "Failed to load partners: ${error.message}"
                    Log.e("PartnerSearch", "Snapshot error: ${error.message}", error)
                    isLoading = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    Log.d("PartnerSearch", "Snapshot received: ${snapshot.documents.size} documents")
                    coroutineScope.launch {
                        val partnerList = mutableListOf<UserProfile>()
                        for (doc in snapshot.documents) {
                            try {
                                val partnerDoc = firestore.collection("profiles")
                                    .document(doc.id)
                                    .collection("partner")
                                    .document("data")
                                    .get()
                                    .await()
                                if (partnerDoc.exists()) {
                                    val partner = doc.toObject(UserProfile::class.java)?.copy(
                                        expertise = partnerDoc.getString("expertise"),
                                        experienceYears = partnerDoc.getLong("experienceYears")?.toInt()
                                    )
                                    partner?.let { partnerList.add(it) }
                                }
                            } catch (e: Exception) {
                                Log.e("PartnerSearch", "Error parsing partner ${doc.id}: ${e.message}", e)
                            }
                        }
                        partners = partnerList
                        isLoading = false
                        Log.d("PartnerSearch", "Fetched ${partners.size} partners")
                    }
                } else {
                    Log.w("PartnerSearch", "Snapshot is null")
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
                    keyboardActions = KeyboardActions(onSearch = { /* Search handled via state */ }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { /* Search handled via state */ }) {
                    Text("Enter")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Partner Sign-Up Button
            Button(
                onClick = { navController.navigate(Screen.PartnerSignUp.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Partner Sign Up")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Refresh Button
            Button(
                onClick = {
                    Log.d("PartnerSearch", "Manual refresh button clicked")
                    refreshTrigger++
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh Partners")
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
                                    partner.expertise?.contains(searchQuery, ignoreCase = true) == true
                        }
                        items(filteredPartners.size) { index ->
                            val partner = filteredPartners[index]
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