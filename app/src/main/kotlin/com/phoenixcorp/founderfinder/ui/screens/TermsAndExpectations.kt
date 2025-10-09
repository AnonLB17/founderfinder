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
fun TermsAndExpectationsScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var equityTerms by remember { mutableStateOf("") }
    var boardRole by remember { mutableStateOf("") }
    var returnTimeline by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { ScreenBanner(title = { Text("Terms and Expectations") }, navController = navController, showBackButton = true) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Provide your investment terms and expectations.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = equityTerms,
                onValueChange = { equityTerms = it },
                label = { Text("Equity or Convertible Note Terms") },
                modifier = Modifier.fillMaxWidth(),
                isError = equityTerms.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = boardRole,
                onValueChange = { boardRole = it },
                label = { Text("Board or Advisory Role") },
                modifier = Modifier.fillMaxWidth(),
                isError = boardRole.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = returnTimeline,
                onValueChange = { returnTimeline = it },
                label = { Text("Expected Return Timeline") },
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
                    if (currentUser == null) {
                        errorMessage = "You must be logged in."
                        navController.navigate(Screen.SignIn.route)
                        return@Button
                    }
                    if (equityTerms.isBlank() || boardRole.isBlank() || returnTimeline.isBlank()) {
                        errorMessage = "All fields are required."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val data = mapOf(
                                "equityTerms" to equityTerms,
                                "boardRole" to boardRole,
                                "returnTimeline" to returnTimeline
                            )
                            firestore.collection("investors")
                                .document(currentUser.uid)
                                .update(data)
                                .await()
                            isLoading = false
                            navController.navigate(Screen.InvestorSearch.route) {
                                popUpTo(Screen.SelectUserType.route) { inclusive = true }
                            }
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
                    Text("Submit")
                }
            }
        }
    }
}