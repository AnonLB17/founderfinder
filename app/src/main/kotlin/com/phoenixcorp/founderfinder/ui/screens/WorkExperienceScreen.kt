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
import com.phoenixcorp.founderfinder.ui.viewmodel.WorkExperienceViewModel

@Composable
fun WorkExperienceScreen(
    navController: NavHostController,
    workExperienceViewModel: WorkExperienceViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val jobTitle by workExperienceViewModel.jobTitle.collectAsState()
    val company by workExperienceViewModel.company.collectAsState()
    val yearsOfExperience by workExperienceViewModel.yearsOfExperience.collectAsState()
    val workExperiences by workExperienceViewModel.workExperiences.collectAsState()
    val isLoading by workExperienceViewModel.isLoading.collectAsState()
    val errorMessage by workExperienceViewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    val currentUser = authViewModel.getCurrentUser()

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
            onValueChange = { workExperienceViewModel.updateJobTitle(it) },
            label = { Text("Job Title") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = company,
            onValueChange = { workExperienceViewModel.updateCompany(it) },
            label = { Text("Company Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = yearsOfExperience,
            onValueChange = { workExperienceViewModel.updateYearsOfExperience(it) },
            label = { Text("Years of Experience") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                workExperienceViewModel.addWorkExperience()
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
                if (currentUser == null) {
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                workExperienceViewModel.saveWorkExperience(currentUser.uid) { success ->
                    if (success) {
                        navController.navigate(Screen.FounderStatus.route) {
                            popUpTo(Screen.WorkExperience.route) { inclusive = true }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
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