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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.navigation.OnboardingSteps
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.EditableEntryList
import com.phoenixcorp.founderfinder.ui.components.OnboardingScaffold
import com.phoenixcorp.founderfinder.ui.viewmodel.OnboardingViewModel

@Composable
fun EducationScreen(
    navController: NavHostController,
    onboardingViewModel: OnboardingViewModel
) {
    val profile by onboardingViewModel.profile.collectAsState()
    val isLoading by onboardingViewModel.isLoading.collectAsState()
    val errorMessage by onboardingViewModel.errorMessage.collectAsState()
    val isInitialized by onboardingViewModel.isInitialized.collectAsState()

    var highestEducation by remember { mutableStateOf("") }
    var areaOfStudy by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<String>>(emptyList()) }
    /** null = adding new; non-null = replacing entries[index] */
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(profile, isInitialized) {
        if (isInitialized) entries = profile.educationEntries
    }

    val context = LocalContext.current

    fun clearFields() {
        highestEducation = ""
        areaOfStudy = ""
        institution = ""
        editingIndex = null
    }

    fun parseEducation(entry: String) {
        // Format: "{degree} in {area} from {institution}"
        val inIdx = entry.indexOf(" in ")
        val fromIdx = entry.indexOf(" from ")
        if (inIdx >= 0 && fromIdx > inIdx) {
            highestEducation = entry.substring(0, inIdx)
            areaOfStudy = entry.substring(inIdx + 4, fromIdx)
            institution = entry.substring(fromIdx + 6)
        } else {
            highestEducation = entry
            areaOfStudy = ""
            institution = ""
        }
    }

    OnboardingScaffold(
        navController = navController,
        title = "Education",
        showBack = true,
        currentStep = 3,
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
            Text("Add your education", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = highestEducation,
                onValueChange = { highestEducation = it },
                label = { Text("Highest Education (e.g. Bachelor's)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )
            OutlinedTextField(
                value = areaOfStudy,
                onValueChange = { areaOfStudy = it },
                label = { Text("Area of Study / Major") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )
            OutlinedTextField(
                value = institution,
                onValueChange = { institution = it },
                label = { Text("Institution Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            OutlinedButton(
                onClick = {
                    if (highestEducation.isBlank() || areaOfStudy.isBlank() || institution.isBlank()) {
                        Toast.makeText(context, "Fill all education fields first", Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    val formatted = "$highestEducation in $areaOfStudy from $institution"
                    entries = if (editingIndex != null) {
                        entries.toMutableList().also { it[editingIndex!!] = formatted }
                    } else {
                        entries + formatted
                    }
                    clearFields()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (editingIndex != null) "Update Education" else "+ Add Education")
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
                emptyMessage = "No education entries yet.",
                onEdit = { index ->
                    parseEducation(entries[index])
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
                    if (entries.isEmpty()) {
                        Toast.makeText(context, "Please add at least one education entry", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onboardingViewModel.updateEducation(entries)
                    onboardingViewModel.saveDraft { success ->
                        if (success) navController.navigate(Screen.WorkExperience.route)
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