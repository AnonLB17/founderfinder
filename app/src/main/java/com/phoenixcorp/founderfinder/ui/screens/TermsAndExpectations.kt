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
fun TermsAndExpectationsScreen(navController: NavHostController) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = Firebase.firestore
    var equityTerms by remember { mutableStateOf("") }
    var boardRole by remember { mutableStateOf("") }
    var returnTimeline by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { ScreenBanner(title = "Terms and Expectations", navController = navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Provide your investment terms and expectations.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = equityTerms,
                onValueChange = { equityTerms = it },
                label = { Text("Equity stake or convertible note terms") },
                modifier = Modifier.fillMaxWidth(),
                isError = equityTerms.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = boardRole,
                onValueChange = { boardRole = it },
                label = { Text("Board seat or advisory role requirements") },
                modifier = Modifier.fillMaxWidth(),
                isError = boardRole.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = returnTimeline,
                onValueChange = { returnTimeline = it },
                label = { Text("Expected timeline for financial returns") },
                modifier = Modifier.fillMaxWidth(),
                isError = returnTimeline.isBlank() && errorMessage != null
            )
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
                    if (equityTerms.isBlank() || boardRole.isBlank() || returnTimeline.isBlank()) {
                        errorMessage = "All fields are required."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        coroutineScope.launch {
                            try {
                                val data = mapOf(
                                    "equityTerms" to equityTerms,
                                    "boardRole" to boardRole,
                                    "returnTimeline" to returnTimeline
                                )
                                firestore.collection("users")
                                    .document(currentUser.uid)
                                    .update(data)
                                    .await()
                                isLoading = false
                                navController.navigate(Screen.Profile.route) {
                                    popUpTo(Screen.SelectUserType.route) { inclusive = true }
                                }
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
fun TermsAndExpectationsScreenPreview() {
    TermsAndExpectationsScreen(navController = rememberNavController())
}