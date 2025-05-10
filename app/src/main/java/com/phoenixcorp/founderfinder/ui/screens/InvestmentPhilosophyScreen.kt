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
fun InvestmentPhilosophyScreen(navController: NavHostController) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = Firebase.firestore
    var investmentApproach by remember { mutableStateOf("") }
    var strategicInvolvement by remember { mutableStateOf("") }
    var roiExpectations by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { ScreenBanner(title = "Investment Philosophy", navController = navController) }
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
                value = investmentApproach,
                onValueChange = { investmentApproach = it },
                label = { Text("Hands-on vs. hands-off approach") },
                modifier = Modifier.fillMaxWidth(),
                isError = investmentApproach.isBlank() && errorMessage != null
            )

            OutlinedTextField(
                value = strategicInvolvement,
                onValueChange = { strategicInvolvement = it },
                label = { Text("Level of involvement in strategic decision-making") },
                modifier = Modifier.fillMaxWidth(),
                isError = strategicInvolvement.isBlank() && errorMessage != null
            )

            OutlinedTextField(
                value = roiExpectations,
                onValueChange = { roiExpectations = it },
                label = { Text("Expectations regarding return on investment (ROI)") },
                modifier = Modifier.fillMaxWidth(),
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
                    if (investmentApproach.isBlank() || strategicInvolvement.isBlank() || roiExpectations.isBlank()) {
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
                                    "investmentApproach" to investmentApproach,
                                    "strategicInvolvement" to strategicInvolvement,
                                    "roiExpectations" to roiExpectations
                                )
                                firestore.collection("users")
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
fun InvestmentPhilosophyScreenPreview() {
    InvestmentPhilosophyScreen(navController = rememberNavController())
}