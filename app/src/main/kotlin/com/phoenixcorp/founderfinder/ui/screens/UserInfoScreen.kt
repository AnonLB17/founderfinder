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
fun UserInfoScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()   // ← Fixed: Use Hilt
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // Get current user ID safely
    val currentUser = authViewModel.getCurrentUser() // We'll add this method
    val userId = currentUser?.uid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tell us about yourself",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = birthDate,
            onValueChange = { birthDate = it },
            label = { Text("Birth Date (MM/DD/YYYY)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            placeholder = { Text("05/15/1995") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (userId == null) {
                    errorMessage = "You must be logged in to continue."
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (firstName.isBlank() || lastName.isBlank() || birthDate.isBlank()) {
                    errorMessage = "Please fill in all fields."
                    return@Button
                }

                // Birth date format validation
                val birthDatePattern = Regex("^\\d{2}/\\d{2}/\\d{4}$")
                if (!birthDate.matches(birthDatePattern)) {
                    errorMessage = "Birth date must be in MM/DD/YYYY format."
                    return@Button
                }

                isLoading = true
                errorMessage = null

                authViewModel.saveUserInfo(userId, firstName, lastName, birthDate) { success ->
                    isLoading = false
                    if (success) {
                        navController.navigate(Screen.Education.route) {
                            popUpTo(Screen.UserInfo.route) { inclusive = true }
                        }
                    } else {
                        errorMessage = "Failed to save information. Please try again."
                        Toast.makeText(context, "Failed to save user info", Toast.LENGTH_SHORT).show()
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