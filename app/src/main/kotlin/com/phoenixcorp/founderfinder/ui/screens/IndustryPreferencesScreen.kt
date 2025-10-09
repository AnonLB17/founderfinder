package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun IndustryPreferencesScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var industryInput by remember { mutableStateOf("") }
    var industryPreferences by remember { mutableStateOf(listOf<String>()) }
    var investmentStage by remember { mutableStateOf("") }
    var minRange by remember { mutableStateOf("") }
    var maxRange by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Pre-fill industry from investor profile
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                val investorDoc = firestore.collection("investors").document(currentUser.uid).get().await()
                val industry = investorDoc.getString("industry")
                if (!industry.isNullOrBlank() && industryPreferences.isEmpty()) {
                    industryPreferences = listOf(industry)
                    industryInput = ""
                    Log.d("IndustryPreferencesScreen", "Pre-filled industry: $industry")
                }
            } catch (e: Exception) {
                Log.e("IndustryPreferencesScreen", "Error fetching investor profile: ${e.message}", e)
            }
        }
    }

    Scaffold(
        topBar = { ScreenBanner(title = { Text("Industry Preferences") }, navController = navController, showBackButton = true) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enter industries and investment preferences.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = industryInput,
                onValueChange = { industryInput = it },
                label = { Text("Industry (e.g., Tech, Healthcare)") },
                modifier = Modifier.fillMaxWidth(),
                isError = industryPreferences.isEmpty() && errorMessage != null
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
                label = { Text("Investment Stage (e.g., Seed, Series A)") },
                modifier = Modifier.fillMaxWidth(),
                isError = investmentStage.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Typical deal size range",
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
                    label = { Text("Min ($)") },
                    modifier = Modifier.weight(1f),
                    isError = minRange.isBlank() && errorMessage != null
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = maxRange,
                    onValueChange = { maxRange = it },
                    label = { Text("Max ($)") },
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
                    if (currentUser == null) {
                        errorMessage = "You must be logged in."
                        navController.navigate(Screen.SignIn.route)
                        return@Button
                    }
                    if (industryPreferences.isEmpty() || investmentStage.isBlank() || minRange.isBlank() || maxRange.isBlank()) {
                        errorMessage = "All fields are required."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val data = mapOf(
                                "preferredIndustries" to industryPreferences,
                                "investmentStage" to investmentStage,
                                "investmentRangeMin" to minRange,
                                "investmentRangeMax" to maxRange
                            )
                            firestore.collection("investors")
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