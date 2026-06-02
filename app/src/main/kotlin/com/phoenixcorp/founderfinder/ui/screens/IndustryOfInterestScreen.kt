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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.viewmodel.AuthViewModel

@Composable
fun IndustriesOfInterestScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    var keyword by remember { mutableStateOf("") }
    var industries by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val currentUser = Firebase.auth.currentUser
    val userId = currentUser?.uid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Industries of Interest", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            label = { Text("Search Industry") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (keyword.isBlank()) {
                    errorMessage = "Please enter an industry."
                    return@Button
                }
                val trimmed = keyword.trim()
                if (industries.contains(trimmed)) {
                    errorMessage = "This industry is already added."
                    return@Button
                }
                industries = industries + trimmed
                keyword = ""
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("+ Add")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (industries.isNotEmpty()) {
            industries.forEach { industry ->
                Text(text = industry, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
            }
        } else {
            Text(text = "No industries added yet.", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                if (industries.isEmpty()) {
                    errorMessage = "Please add at least one industry."
                    return@Button
                }

                isLoading = true
                errorMessage = null

                authViewModel.saveIndustriesOfInterest(userId, industries) { success ->
                    isLoading = false
                    if (success) {
                        navController.navigate(Screen.OrganizationsOfInterest.route) {
                            popUpTo(Screen.IndustriesOfInterest.route) { inclusive = true }
                        }
                    } else {
                        errorMessage = "Failed to save industries. Please try again."
                        Toast.makeText(context, "Failed to save industries", Toast.LENGTH_SHORT).show()
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

@Preview(showBackground = true)
@Composable
fun IndustriesOfInterestScreenPreview() {
    IndustriesOfInterestScreen(navController = rememberNavController())
}