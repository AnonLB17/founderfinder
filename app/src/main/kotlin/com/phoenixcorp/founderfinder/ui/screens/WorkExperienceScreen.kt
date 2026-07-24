package com.phoenixcorp.founderfinder.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.navigation.OnboardingSteps
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.EditableEntryList
import com.phoenixcorp.founderfinder.ui.components.OnboardingScaffold
import com.phoenixcorp.founderfinder.ui.viewmodel.OnboardingViewModel

@Composable
fun WorkExperienceScreen(
    navController: NavHostController,
    onboardingViewModel: OnboardingViewModel
) {
    val profile by onboardingViewModel.profile.collectAsState()
    val isLoading by onboardingViewModel.isLoading.collectAsState()
    val errorMessage by onboardingViewModel.errorMessage.collectAsState()
    val isInitialized by onboardingViewModel.isInitialized.collectAsState()

    var jobTitle by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var years by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<String>>(emptyList()) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(profile, isInitialized) {
        if (isInitialized) entries = profile.workExperiences
    }

    val context = LocalContext.current

    fun clearFields() {
        jobTitle = ""; company = ""; years = ""; editingIndex = null
    }

    fun parseEntry(entry: String) {
        // "{title} at {company}" or "{title} at {company} ({years} yrs)"
        val atIdx = entry.indexOf(" at ")
        if (atIdx < 0) {
            jobTitle = entry; company = ""; years = ""
            return
        }
        jobTitle = entry.substring(0, atIdx)
        val rest = entry.substring(atIdx + 4)
        val paren = rest.lastIndexOf(" (")
        if (paren >= 0 && rest.endsWith(" yrs)")) {
            company = rest.substring(0, paren)
            years = rest.substring(paren + 2, rest.length - 5)
        } else {
            company = rest
            years = ""
        }
    }

    OnboardingScaffold(
        navController = navController,
        title = "Work Experience",
        showBack = true,
        currentStep = 4,
        totalSteps = OnboardingSteps.TOTAL_FOUNDER,
        isLoading = isLoading && !isInitialized
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Add work experience", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = jobTitle,
                onValueChange = { jobTitle = it },
                label = { Text("Job Title") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )
            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                label = { Text("Company Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )
            OutlinedTextField(
                value = years,
                onValueChange = { years = it },
                label = { Text("Years of Experience") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            OutlinedButton(
                onClick = {
                    if (jobTitle.isBlank() || company.isBlank()) {
                        Toast.makeText(context, "Job title and company required", Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    val entry = "$jobTitle at $company" + if (years.isNotBlank()) " ($years yrs)" else ""
                    entries = if (editingIndex != null) {
                        entries.toMutableList().also { it[editingIndex!!] = entry }
                    } else entries + entry
                    clearFields()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (editingIndex != null) "Update Experience" else "+ Add")
            }

            if (editingIndex != null) {
                OutlinedButton(
                    onClick = { clearFields() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) { Text("Cancel Edit") }
            }

            EditableEntryList(
                entries = entries,
                enabled = !isLoading,
                emptyMessage = "No work experiences yet.",
                onEdit = { index ->
                    parseEntry(entries[index])
                    editingIndex = index
                },
                onRemove = { index ->
                    entries = entries.toMutableList().also { it.removeAt(index) }
                    if (editingIndex == index) clearFields()
                    else if (editingIndex != null && editingIndex!! > index) {
                        editingIndex = editingIndex!! - 1
                    }
                }
            )

            errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onboardingViewModel.updateWorkExperience(entries)
                    onboardingViewModel.saveDraft { success ->
                        if (success) navController.navigate(Screen.FounderStatus.route)
                        else Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Text("Next")
            }
        }
    }
}