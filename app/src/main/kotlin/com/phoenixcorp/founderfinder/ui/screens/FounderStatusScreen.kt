package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun FounderStatusScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var founderStatus by remember { mutableStateOf(false) }
    var startupName by remember { mutableStateOf("") }
    var startupStage by remember { mutableStateOf("") }
    var founderEntries by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentUser = authViewModel.getCurrentUser()
    val userId = currentUser?.uid

    // Fetch existing founder status
    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect

        coroutineScope.launch {
            try {
                val firestore: FirebaseFirestore = Firebase.firestore
                val document = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                if (document.exists()) {
                    founderStatus = document.getBoolean("isFounder") ?: false
                    founderEntries = document.get("founderEntries") as? List<String> ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("FounderStatusScreen", "Error fetching founder status", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Founder Status",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Are you currently a founder?")
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = founderStatus,
                    onClick = { founderStatus = true },
                    enabled = !isLoading
                )
                Text("Yes")
            }
            Spacer(modifier = Modifier.width(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = !founderStatus,
                    onClick = { founderStatus = false },
                    enabled = !isLoading
                )
                Text("No")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (founderStatus) {
            OutlinedTextField(
                value = startupName,
                onValueChange = { startupName = it },
                label = { Text("Startup Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = startupStage,
                onValueChange = { startupStage = it },
                label = { Text("Startup Stage (e.g. Pre-seed, Seed, Series A)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (startupName.isNotBlank() && startupStage.isNotBlank()) {
                        founderEntries = founderEntries + "$startupName - $startupStage"
                        startupName = ""
                        startupStage = ""
                        errorMessage = null
                    } else {
                        errorMessage = "Please fill in both startup name and stage."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("+ Add Startup")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (founderEntries.isNotEmpty()) {
                Text("Added Founder Entries:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                founderEntries.forEach { entry ->
                    Text(text = "• $entry", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            } else {
                Text(text = "No founder entries added yet.", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Text(
                text = "No founder details required.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (userId == null) {
                    errorMessage = "You must be logged in."
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (founderStatus && founderEntries.isEmpty()) {
                    errorMessage = "Please add at least one founder entry if you are a founder."
                    return@Button
                }

                isLoading = true
                errorMessage = null

                authViewModel.saveFounderStatus(
                    userId = userId,
                    isFounder = founderStatus,
                    founderEntries = founderEntries
                ) { success ->
                    isLoading = false
                    if (success) {
                        navController.navigate(Screen.AmbitionStatement.route) {
                            popUpTo(Screen.FounderStatus.route) { inclusive = true }
                        }
                    } else {
                        errorMessage = "Failed to save founder status. Please try again."
                        Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
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

@Preview(showBackground = true)
@Composable
fun FounderStatusScreenPreview() {
    FounderStatusScreen(navController = rememberNavController())
}