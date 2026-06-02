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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.viewmodel.AuthViewModel

@Composable
fun OrganizationsOfInterestScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()   // ← Fixed: Use Hilt
) {
    var keyword by remember { mutableStateOf("") }
    var organizations by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // Use ViewModel instead of direct Firebase call
    val currentUser = authViewModel.getCurrentUser()
    val userId = currentUser?.uid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Organizations of Interest",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            label = { Text("Search Organization") },
            placeholder = { Text("e.g. Y Combinator, a16z, Google") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (keyword.isBlank()) {
                    errorMessage = "Please enter an organization."
                    return@Button
                }
                val trimmed = keyword.trim()
                if (organizations.contains(trimmed)) {
                    errorMessage = "This organization is already added."
                    return@Button
                }
                organizations = organizations + trimmed
                keyword = ""
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("+ Add")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (organizations.isNotEmpty()) {
            Text("Selected Organizations:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            organizations.forEach { org ->
                Text(text = "• $org", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
            }
        } else {
            Text(
                text = "No organizations added yet.",
                style = MaterialTheme.typography.bodySmall
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
                if (organizations.isEmpty()) {
                    errorMessage = "Please add at least one organization."
                    return@Button
                }

                isLoading = true
                errorMessage = null

                authViewModel.saveOrganizationsOfInterest(userId, organizations) { success ->
                    isLoading = false
                    if (success) {
                        navController.navigate(Screen.PublicAppearance.route) {
                            popUpTo(Screen.OrganizationsOfInterest.route) { inclusive = true }
                        }
                    } else {
                        errorMessage = "Failed to save organizations. Please try again."
                        Toast.makeText(context, "Failed to save organizations", Toast.LENGTH_SHORT).show()
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
fun OrganizationsOfInterestScreenPreview() {
    OrganizationsOfInterestScreen(navController = rememberNavController())
}