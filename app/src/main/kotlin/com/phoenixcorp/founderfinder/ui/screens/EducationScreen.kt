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
fun EducationScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    var highestEducation by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var areaOfStudy by remember { mutableStateOf("") }
    var educationEntries by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val currentUser = Firebase.auth.currentUser
    val userId = currentUser?.uid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Education", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = highestEducation,
            onValueChange = { highestEducation = it },
            label = { Text("Highest Level of Education") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = areaOfStudy,
            onValueChange = { areaOfStudy = it },
            label = { Text("Area of Study / Major") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = institution,
            onValueChange = { institution = it },
            label = { Text("Institution Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (highestEducation.isBlank() || areaOfStudy.isBlank() || institution.isBlank()) {
                    errorMessage = "Please fill in all education fields."
                    return@Button
                }
                educationEntries = educationEntries + "$highestEducation in $areaOfStudy from $institution"
                highestEducation = ""
                areaOfStudy = ""
                institution = ""
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("+ Add Education")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (educationEntries.isNotEmpty()) {
            educationEntries.forEach { entry ->
                Text(text = entry, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
            }
        } else {
            Text(text = "No education entries added yet.", style = MaterialTheme.typography.bodySmall)
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
                if (educationEntries.isEmpty()) {
                    errorMessage = "Please add at least one education entry."
                    return@Button
                }

                isLoading = true
                errorMessage = null

                authViewModel.saveEducation(userId, educationEntries) { success ->
                    isLoading = false
                    if (success) {
                        navController.navigate(Screen.WorkExperience.route) {
                            popUpTo(Screen.Education.route) { inclusive = true }
                        }
                    } else {
                        errorMessage = "Failed to save education details. Please try again."
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
fun EducationScreenPreview() {
    EducationScreen(navController = rememberNavController())
}