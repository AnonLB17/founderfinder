package com.phoenixcorp.founderfinder.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.viewmodel.AuthViewModel

@Composable
fun WorkExperienceScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()   // ← Fixed: Use Hilt
) {
    var jobTitle by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var yearsOfExperience by remember { mutableStateOf("") }
    var workExperiences by remember { mutableStateOf(listOf<String>()) }
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
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Work Experience",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = jobTitle,
            onValueChange = { jobTitle = it },
            label = { Text("Job Title") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = company,
            onValueChange = { company = it },
            label = { Text("Company Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = yearsOfExperience,
            onValueChange = { yearsOfExperience = it },
            label = { Text("Years of Experience") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (jobTitle.isBlank() || company.isBlank() || yearsOfExperience.isBlank()) {
                    errorMessage = "Please fill in all work experience fields."
                    return@Button
                }
                val years = yearsOfExperience.toIntOrNull()
                if (years == null || years < 0) {
                    errorMessage = "Years of experience must be a valid non-negative number."
                    return@Button
                }

                val entry = "$jobTitle at $company ($years years)"
                workExperiences = workExperiences + entry

                jobTitle = ""
                company = ""
                yearsOfExperience = ""
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("+ Add")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (workExperiences.isNotEmpty()) {
            Text("Added Work Experiences:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            workExperiences.forEach { experience ->
                Text(text = "• $experience", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
            }
        } else {
            Text(
                text = "No work experiences added yet.",
                style = MaterialTheme.typography.bodySmall
            )
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
                if (workExperiences.isEmpty()) {
                    errorMessage = "Please add at least one work experience."
                    return@Button
                }

                isLoading = true
                errorMessage = null

                authViewModel.saveWorkExperience(userId, workExperiences) { success ->
                    isLoading = false
                    if (success) {
                        navController.navigate(Screen.FounderStatus.route) {
                            popUpTo(Screen.WorkExperience.route) { inclusive = true }
                        }
                    } else {
                        errorMessage = "Failed to save work experience. Please try again."
                        Toast.makeText(context, "Failed to save work experience", Toast.LENGTH_SHORT).show()
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
fun WorkExperienceScreenPreview() {
    WorkExperienceScreen(navController = rememberNavController())
}