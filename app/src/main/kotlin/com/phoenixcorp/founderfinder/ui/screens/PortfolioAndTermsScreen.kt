package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.SetOptions
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioAndTermsScreen(navController: NavHostController) {
    val firestore = Firebase.firestore
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    var notableStartupInput by remember { mutableStateOf("") }
    var notableStartups by remember { mutableStateOf(listOf<String>()) }

    var testimonialInput by remember { mutableStateOf("") }
    var testimonials by remember { mutableStateOf(listOf<String>()) }

    var equityTerms by remember { mutableStateOf("") }
    var boardRole by remember { mutableStateOf("") }
    var returnTimeline by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Portfolio & Terms") },
                navController = navController,
                showBackButton = true
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "Showcase Your Track Record",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Portfolio Companies
            item {
                Card(elevation = CardDefaults.cardElevation(6.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Portfolio Companies", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = notableStartupInput,
                            onValueChange = { notableStartupInput = it },
                            label = { Text("Add Company (e.g. Stripe - $100M Exit)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            if (notableStartupInput.isNotBlank()) {
                                notableStartups = notableStartups + notableStartupInput
                                notableStartupInput = ""
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("+ Add Company")
                        }

                        notableStartups.forEach { company ->
                            Text("• $company", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Testimonials
            item {
                Card(elevation = CardDefaults.cardElevation(6.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testimonials", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = testimonialInput,
                            onValueChange = { testimonialInput = it },
                            label = { Text("Add Testimonial") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            if (testimonialInput.isNotBlank()) {
                                testimonials = testimonials + testimonialInput
                                testimonialInput = ""
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("+ Add Testimonial")
                        }

                        testimonials.forEach { testimonial ->
                            Text("• \"$testimonial\"", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Terms & Expectations
            item {
                Card(elevation = CardDefaults.cardElevation(6.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Investment Terms & Expectations", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = equityTerms,
                            onValueChange = { equityTerms = it },
                            label = { Text("Equity / Convertible Terms") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = boardRole,
                            onValueChange = { boardRole = it },
                            label = { Text("Board / Advisory Role") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = returnTimeline,
                            onValueChange = { returnTimeline = it },
                            label = { Text("Expected Return Timeline") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        if (currentUser == null) {
                            errorMessage = "Not logged in"
                            return@Button
                        }

                        if (notableStartups.isEmpty() || testimonials.isEmpty() ||
                            equityTerms.isBlank() || boardRole.isBlank() || returnTimeline.isBlank()) {
                            errorMessage = "Please fill all required fields."
                            return@Button
                        }

                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val data = mapOf(
                                    "portfolioCompanies" to notableStartups,
                                    "testimonials" to testimonials,
                                    "equityTerms" to equityTerms,
                                    "boardRole" to boardRole,
                                    "returnTimeline" to returnTimeline,
                                    "updatedAt" to System.currentTimeMillis()
                                )

                                // Write to the SAME location as InvestorInfoScreen
                                firestore.collection("profiles")
                                    .document(currentUser.uid)
                                    .collection("investor")
                                    .document("data")
                                    .set(data, SetOptions.merge())
                                    .await()

                                navController.navigate(Screen.InvestorSearch.route) {
                                    popUpTo(Screen.SelectUserType.route) { inclusive = true }
                                }
                            } catch (e: Exception) {
                                errorMessage = "Failed to save: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Complete Investor Profile")
                    }
                }
            }
        }
    }
}