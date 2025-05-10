package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioCompaniesScreen(navController: NavHostController) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = Firebase.firestore
    var notableStartupInput by remember { mutableStateOf("") }
    var successStoryInput by remember { mutableStateOf("") }
    var testimonialInput by remember { mutableStateOf("") }
    var notableStartups by remember { mutableStateOf(listOf<String>()) }
    var successStories by remember { mutableStateOf(listOf<String>()) }
    var testimonials by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { ScreenBanner(title = "Portfolio Companies", navController = navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = notableStartupInput,
                onValueChange = { notableStartupInput = it },
                label = { Text("Notable startups or businesses funded") },
                modifier = Modifier.fillMaxWidth(),
                isError = notableStartupInput.isBlank() && errorMessage != null
            )
            Button(
                onClick = {
                    if (notableStartupInput.isNotBlank()) {
                        notableStartups = notableStartups + notableStartupInput
                        notableStartupInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add")
            }
            notableStartups.forEach { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = successStoryInput,
                onValueChange = { successStoryInput = it },
                label = { Text("Success stories and exits (acquisitions, IPOs)") },
                modifier = Modifier.fillMaxWidth(),
                isError = successStoryInput.isBlank() && errorMessage != null
            )
            Button(
                onClick = {
                    if (successStoryInput.isNotBlank()) {
                        successStories = successStories + successStoryInput
                        successStoryInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add")
            }
            successStories.forEach { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = testimonialInput,
                onValueChange = { testimonialInput = it },
                label = { Text("Testimonials or feedback from previous entrepreneurs") },
                modifier = Modifier.fillMaxWidth(),
                isError = testimonialInput.isBlank() && errorMessage != null
            )
            Button(
                onClick = {
                    if (testimonialInput.isNotBlank()) {
                        testimonials = testimonials + testimonialInput
                        testimonialInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add")
            }
            testimonials.forEach { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (notableStartups.isEmpty() || successStories.isEmpty() || testimonials.isEmpty()) {
                        errorMessage = "At least one entry is required for each field."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        coroutineScope.launch {
                            try {
                                val data = mapOf(
                                    "portfolioCompanies" to notableStartups,
                                    "successStories" to successStories,
                                    "testimonials" to testimonials
                                )
                                firestore.collection("users")
                                    .document(currentUser.uid)
                                    .update(data)
                                    .await()
                                isLoading = false
                                navController.navigate(Screen.TermsAndExpectations.route)
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = "Failed to save data: ${e.message}"
                            }
                        }
                    } else {
                        isLoading = false
                        errorMessage = "You must be logged in."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Submit")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PortfolioCompaniesScreenPreview() {
    PortfolioCompaniesScreen(navController = rememberNavController())
}