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
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.PartnerCard
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

@Composable
fun PartnerSearchFeatureScreen(navController: NavHostController) {
    val firestore: FirebaseFirestore = Firebase.firestore
    var searchQuery by remember { mutableStateOf("") }
    var partners by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch partners from Firestore
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val snapshot = firestore.collection("profiles")
                    .whereEqualTo("isPartner", true)
                    .get()
                    .await()
                partners = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserProfile::class.java)?.copy(userId = doc.id)
                }
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load partners: ${e.message}"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = "Find Partners",
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
                            onCardClick = {
                                val userId = partner.userId
                                if (userId != null && userId.isNotEmpty()) {
                                    Log.d("PartnerSearch", "Navigating to UserProfile with userId: $userId")
                                    navController.navigate(Screen.UserProfile.createRoute(userId))
                                } else {
                                    Log.e("PartnerSearch", "Invalid userId for profile navigation")
                                }
                            },
                            onMessageClick = {
                                val userId = partner.userId
                                if (userId != null && userId.isNotEmpty()) {
                                    Log.d("PartnerSearch", "Navigating to PrivateMessages with recipientId: $userId")
                                    navController.navigate(Screen.PrivateMessages.createRoute(userId))
                                } else {
                                    Log.e("PartnerSearch", "Invalid userId for messaging")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}