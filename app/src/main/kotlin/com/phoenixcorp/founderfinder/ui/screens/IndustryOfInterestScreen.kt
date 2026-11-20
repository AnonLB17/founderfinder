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
fun IndustriesOfInterestScreen(
    navController: NavHostController,
    onboardingViewModel: OnboardingViewModel
) {
    val profile by onboardingViewModel.profile.collectAsState()
    val isLoading by onboardingViewModel.isLoading.collectAsState()
    val errorMessage by onboardingViewModel.errorMessage.collectAsState()
    val isInitialized by onboardingViewModel.isInitialized.collectAsState()

    var keyword by remember { mutableStateOf("") }
    var industries by remember { mutableStateOf<List<String>>(emptyList()) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(profile, isInitialized) {
        if (isInitialized) industries = profile.industriesOfInterest
    }

    val context = LocalContext.current

    OnboardingScaffold(
        navController = navController,
        title = "Industries of Interest",
        showBack = true,
        currentStep = 8,
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
            Text("What industries interest you?", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("Industry") },
                placeholder = { Text("e.g. Fintech, Healthtech, AI") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            OutlinedButton(
                onClick = {
                    val trimmed = keyword.trim()
                    if (trimmed.isBlank()) {
                        Toast.makeText(context, "Enter an industry", Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    industries = if (editingIndex != null) {
                        industries.toMutableList().also { it[editingIndex!!] = trimmed }
                    } else {
                        if (trimmed in industries) {
                            Toast.makeText(context, "Already added", Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }
                        industries + trimmed
                    }
                    keyword = ""
                    editingIndex = null
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (editingIndex != null) "Update Industry" else "+ Add")
            }

            if (editingIndex != null) {
                OutlinedButton(
                    onClick = { keyword = ""; editingIndex = null },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) { Text("Cancel Edit") }
            }

            EditableEntryList(
                entries = industries,
                enabled = !isLoading,
                emptyMessage = "No industries added yet.",
                onEdit = { index ->
                    keyword = industries[index]
                    editingIndex = index
                },
                onRemove = { index ->
                    industries = industries.toMutableList().also { it.removeAt(index) }
                    if (editingIndex == index) {
                        keyword = ""; editingIndex = null
                    } else if (editingIndex != null && editingIndex!! > index) {
                        editingIndex = editingIndex!! - 1
                    }
                }
            )

            errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onboardingViewModel.updateIndustries(industries)
                    onboardingViewModel.saveDraft { success ->
                        if (success) navController.navigate(Screen.OrganizationsOfInterest.route)
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