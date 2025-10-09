package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioCompaniesScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var notableStartupInput by remember { mutableStateOf("") }
    var testimonialInput by remember { mutableStateOf("") }
    var notableStartups by remember { mutableStateOf(listOf<String>()) }
    var testimonials by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { ScreenBanner(title = { Text("Portfolio Companies") }, navController = navController, showBackButton = true) }
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
                label = { Text("Notable Startups Funded (e.g., Company Name, Exit)") },
                modifier = Modifier.fillMaxWidth(),
                isError = notableStartups.isEmpty() && errorMessage != null
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
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = testimonialInput,
                onValueChange = { testimonialInput = it },
                label = { Text("Testimonials from Entrepreneurs") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                isError = testimonials.isEmpty() && errorMessage != null
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
                    if (currentUser == null) {
                        errorMessage = "You must be logged in."
                        navController.navigate(Screen.SignIn.route)
                        return@Button
                    }
                    if (notableStartups.isEmpty() || testimonials.isEmpty()) {
                        errorMessage = "At least one startup and testimonial are required."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val data = mapOf(
                                "portfolioCompanies" to notableStartups,
                                "testimonials" to testimonials
                            )
                            firestore.collection("investors")
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
                    Text("Next")
                }
            }
        }
    }
}