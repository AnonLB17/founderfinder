package com.phoenixcorp.founderfinder.ui.screens

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
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.viewmodel.AuthViewModel

@Composable
fun FounderStatusScreen(navController: NavHostController, authViewModel: AuthViewModel = viewModel()) {
    var isFounder by remember { mutableStateOf(false) }
    var startupName by remember { mutableStateOf("") }
    var startupStage by remember { mutableStateOf("") }
    var founderEntries by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid

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
                    selected = isFounder,
                    onClick = { isFounder = true },
                    enabled = !isLoading
                )
                Text("Yes")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = !isFounder,
                    onClick = { isFounder = false },
                    enabled = !isLoading
                )
                Text("No")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (isFounder) {
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
                    if (startupName.isBlank() || startupStage.isBlank()) {
                        errorMessage = "Please fill in both startup name and stage."
                        return@Button
                    }
                    founderEntries = founderEntries + "$startupName - $startupStage"
                    startupName = ""
                    startupStage = ""
                    errorMessage = null
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
                if (isFounder && founderEntries.isEmpty()) {
                    errorMessage = "Please add at least one founder entry if you are a founder."
                    return@Button
                }

                isLoading = true
                errorMessage = null
                val entriesToSave = if (isFounder) founderEntries else listOf("Not a founder")
                authViewModel.saveFounderStatus(userId, entriesToSave) { success ->
                    isLoading = false
                    if (success) {
                        navController.navigate(Screen.AmbitionStatement.route) {
                            popUpTo(Screen.FounderStatus.route) { inclusive = true }
                        }
                    } else {
                        errorMessage = "Failed to save founder status. Please try again."
                        Toast.makeText(context, "Failed to save founder status", Toast.LENGTH_SHORT).show()
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