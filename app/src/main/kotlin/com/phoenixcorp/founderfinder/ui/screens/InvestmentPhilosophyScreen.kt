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
fun InvestmentPhilosophyScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var approachAndInvolvement by remember { mutableStateOf("") }
    var roiExpectations by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { ScreenBanner(title = { Text("Investment Philosophy") }, navController = navController, showBackButton = true) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = approachAndInvolvement,
                onValueChange = { approachAndInvolvement = it },
                label = { Text("Approach and Involvement (e.g., Hands-on, Board Advisor)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                isError = approachAndInvolvement.isBlank() && errorMessage != null
            )

            OutlinedTextField(
                value = roiExpectations,
                onValueChange = { roiExpectations = it },
                label = { Text("ROI Expectations (e.g., 10x in 5 years)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                isError = roiExpectations.isBlank() && errorMessage != null
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                    if (approachAndInvolvement.isBlank() || roiExpectations.isBlank()) {
                        errorMessage = "All fields are required."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val data = mapOf(
                                "approachAndInvolvement" to approachAndInvolvement,
                                "roiExpectations" to roiExpectations
                            )
                            firestore.collection("investors")
                                .document(currentUser.uid)
                                .update(data)
                                .await()
                            isLoading = false
                            navController.navigate(Screen.PortfolioCompanies.route)
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