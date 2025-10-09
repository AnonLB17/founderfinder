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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun FounderStatusScreen(navController: NavHostController, authViewModel: AuthViewModel = viewModel()) {
    val firestore = Firebase.firestore
    val coroutineScope = rememberCoroutineScope()
    var founderStatus by remember { mutableStateOf(false) }
    var startupName by remember { mutableStateOf("") }
    var startupStage by remember { mutableStateOf("") }
    var founderEntries by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    // Fetch existing founder status
    LaunchedEffect(Unit) {
        if (userId == null) return@LaunchedEffect
        coroutineScope.launch {
            try {
                val document = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()
                if (document.exists()) {
                    founderStatus = document.getBoolean("isFounder") ?: false
                    founderEntries = document.get("founderEntries") as? List<String> ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("FounderStatusScreen", "Error fetching founder status: ${e.message}", e)
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
        Text(text = "Founder Status", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Are you currently a founder?")
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
            Spacer(modifier = Modifier.width(16.dp))
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
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = startupStage,
                onValueChange = { startupStage = it },
                label = { Text("Startup Stage") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (startupName.isNotBlank() && startupStage.isNotBlank()) {
                        founderEntries = founderEntries + "$startupName - $startupStage"
                        startupName = ""
                        startupStage = ""
                    } else {
                        errorMessage = "Please fill in both startup name and stage."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("+ Add")
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (founderEntries.isNotEmpty()) {
                founderEntries.forEach { entry ->
                    Text(text = entry, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            } else {
                Text(text = "No founder entries added yet.", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Text(
                text = "No founder details required.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show error message if any
        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (userId == null) {
                    errorMessage = "You must be logged in to save your founder status."
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (founderStatus && founderEntries.isEmpty()) {
                    errorMessage = "Please add at least one founder entry if you are a founder."
                    return@Button
                }

                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    try {
                        val data = mapOf(
                            "isFounder" to founderStatus,
                            "founderEntries" to founderEntries
                        )
                        firestore.collection("users")
                            .document(userId)
                            .set(data)
                            .await()
                        isLoading = false
                        navController.navigate(Screen.AmbitionStatement.route) {
                            popUpTo(Screen.FounderStatus.route) { inclusive = true }
                        }
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = "Failed to save founder status: ${e.message}"
                        Toast.makeText(context, "Failed to save founder status", Toast.LENGTH_SHORT).show()
                        Log.e("FounderStatusScreen", "Error saving founder status: ${e.message}", e)
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