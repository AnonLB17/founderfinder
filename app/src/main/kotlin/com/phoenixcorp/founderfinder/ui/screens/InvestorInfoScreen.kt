package com.phoenixcorp.founderfinder.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.SetOptions
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.ui.viewmodel.InvestorUiState
import com.phoenixcorp.founderfinder.ui.viewmodel.InvestorViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestorInfoScreen(
    navController: NavHostController,
    viewModel: InvestorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val firestore = Firebase.firestore
    val auth = Firebase.auth
    val currentUser = auth.currentUser
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    var industryInput by remember { mutableStateOf("") }
    var preferredIndustries by remember { mutableStateOf(listOf<String>()) }
    var philosophy by remember { mutableStateOf("") }
    var investmentRangeMin by remember { mutableStateOf("") }
    var investmentRangeMax by remember { mutableStateOf("") }
    var preferredStages by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        when (uiState) {
            is InvestorUiState.Success -> {
                Toast.makeText(context, "Investor profile created successfully!", Toast.LENGTH_LONG).show()
                navController.navigate(Screen.PortfolioAndTerms.route) {
                    popUpTo(Screen.SelectUserType.route) { inclusive = true }
                }
            }
            is InvestorUiState.Error -> {
                val error = (uiState as InvestorUiState.Error).message
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Investor Profile") },
                navController = navController,
                showBackButton = true
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )

            Text(
                text = "Tell us about your investment focus",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Preferred Industries
            OutlinedTextField(
                value = industryInput,
                onValueChange = { industryInput = it },
                label = { Text("Add Industry (e.g. Tech, Healthcare, Fintech)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (industryInput.isNotBlank()) {
                        preferredIndustries = preferredIndustries + industryInput.trim()
                        industryInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add Industry")
            }

            if (preferredIndustries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Selected Industries:", style = MaterialTheme.typography.titleSmall)
                preferredIndustries.forEach { industry ->
                    Text("• $industry", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Investment Philosophy
            OutlinedTextField(
                value = philosophy,
                onValueChange = { philosophy = it },
                label = { Text("Investment Philosophy *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Investment Range
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = investmentRangeMin,
                    onValueChange = { investmentRangeMin = it },
                    label = { Text("Min Investment ($)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = investmentRangeMax,
                    onValueChange = { investmentRangeMax = it },
                    label = { Text("Max Investment ($)") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = preferredStages,
                onValueChange = { preferredStages = it },
                label = { Text("Preferred Stages (e.g. Seed, Series A)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (preferredIndustries.isEmpty() || philosophy.isBlank()) {
                        Toast.makeText(context, "Please add at least one industry and investment philosophy.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (currentUser == null) {
                        Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val investorData = mapOf(
                        "preferredIndustries" to preferredIndustries,
                        "philosophy" to philosophy,
                        "investmentRangeMin" to investmentRangeMin,
                        "investmentRangeMax" to investmentRangeMax,
                        "investmentStage" to preferredStages.split(",").map { it.trim() }.filter { it.isNotBlank() },  // Changed to match InvestorCard
                        "updatedAt" to System.currentTimeMillis()
                    )

                    coroutineScope.launch {
                        try {
                            val uid = currentUser.uid

                            // Update main profile role
                            firestore.collection("profiles")
                                .document(uid)
                                .set(mapOf("role" to "INVESTOR"), SetOptions.merge())
                                .await()

                            // Save / update investor data in the same document
                            firestore.collection("profiles")
                                .document(uid)
                                .collection("investor")
                                .document("data")
                                .set(investorData, SetOptions.merge())
                                .await()

                            viewModel.createInvestorProfile(mapOf("userId" to uid))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is InvestorUiState.Loading
            ) {
                if (uiState is InvestorUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save & Continue")
                }
            }
        }
    }
}