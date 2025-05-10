package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun IndustryPreferencesScreen(navController: NavHostController) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = Firebase.firestore
    var industryInput by remember { mutableStateOf("") }
    var industryPreferences by remember { mutableStateOf(listOf<String>()) }
    var investmentStage by remember { mutableStateOf("") }
    var minRange by remember { mutableStateOf("") }
    var maxRange by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { ScreenBanner(title = "Industry Preferences", navController = navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Enter all industries that interest you and your investment preferences.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = industryInput,
                onValueChange = { industryInput = it },
                label = { Text("Industry of Interest (e.g., Tech, Healthcare)") },
                modifier = Modifier.fillMaxWidth(),
                isError = industryInput.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (industryInput.isNotBlank()) {
                        industryPreferences = industryPreferences + industryInput
                        industryInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add")
            }
            Spacer(modifier = Modifier.height(8.dp))

            industryPreferences.forEach { industry ->
                Text(text = industry, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = investmentStage,
                onValueChange = { investmentStage = it },
                label = { Text("Stage of Business (e.g., Seed, Early-stage, Growth)") },
                modifier = Modifier.fillMaxWidth(),
                isError = investmentStage.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Pick a range of the typical deal size and funding range you’re looking to meet",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = minRange,
                    onValueChange = { minRange = it },
                    label = { Text("Minimum Range") },
                    modifier = Modifier.weight(1f),
                    isError = minRange.isBlank() && errorMessage != null
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = maxRange,
                    onValueChange = { maxRange = it },
                    label = { Text("Maximum Range") },
                    modifier = Modifier.weight(1f),
                    isError = maxRange.isBlank() && errorMessage != null
                )
            }

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
                    if (industryPreferences.isEmpty() || investmentStage.isBlank() || minRange.isBlank() || maxRange.isBlank()) {
                        errorMessage = "All fields are required."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        coroutineScope.launch {
                            try {
                                val data = hashMapOf(
                                    "preferredIndustries" to industryPreferences,
                                    "investmentStage" to investmentStage,
                                    "investmentRangeMin" to minRange,
                                    "investmentRangeMax" to maxRange
                                )
                                firestore.collection("users")
                                    .document(currentUser.uid)
                                    .update(data)
                                    .await()
                                isLoading = false
                                navController.navigate(Screen.InvestmentPhilosophy.route)
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
fun IndustryPreferencesScreenPreview() {
    IndustryPreferencesScreen(navController = rememberNavController())
}