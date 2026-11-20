package com.phoenixcorp.founderfinder.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
fun FounderStatusScreen(
    navController: NavHostController,
    onboardingViewModel: OnboardingViewModel
) {
    val profile by onboardingViewModel.profile.collectAsState()
    val isLoading by onboardingViewModel.isLoading.collectAsState()
    val errorMessage by onboardingViewModel.errorMessage.collectAsState()
    val isInitialized by onboardingViewModel.isInitialized.collectAsState()

    var isFounder by remember { mutableStateOf(false) }
    var startupName by remember { mutableStateOf("") }
    var startupStage by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<String>>(emptyList()) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(profile, isInitialized) {
        if (isInitialized) {
            isFounder = profile.isFounder || profile.founderEntries.isNotEmpty()
            entries = profile.founderEntries
        }
    }

    val context = LocalContext.current

    fun clearFields() {
        startupName = ""; startupStage = ""; editingIndex = null
    }

    fun parseEntry(entry: String) {
        // "{name} - {stage}"
        val dash = entry.indexOf(" - ")
        if (dash >= 0) {
            startupName = entry.substring(0, dash)
            startupStage = entry.substring(dash + 3)
        } else {
            startupName = entry
            startupStage = ""
        }
    }

    OnboardingScaffold(
        navController = navController,
        title = "Founder Status",
        showBack = true,
        currentStep = 5,
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
            Text("Are you currently a founder?", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

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
                Spacer(modifier = Modifier.width(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !isFounder,
                        onClick = {
                            isFounder = false
                            entries = emptyList()
                            clearFields()
                        },
                        enabled = !isLoading
                    )
                    Text("No")
                }
            }

            if (isFounder) {
                OutlinedTextField(
                    value = startupName,
                    onValueChange = { startupName = it },
                    label = { Text("Startup Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
                OutlinedTextField(
                    value = startupStage,
                    onValueChange = { startupStage = it },
                    label = { Text("Startup Stage (e.g. Pre-seed, Seed, Series A)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
                OutlinedButton(
                    onClick = {
                        if (startupName.isBlank() || startupStage.isBlank()) {
                            Toast.makeText(context, "Name and stage required", Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }
                        val formatted = "$startupName - $startupStage"
                        entries = if (editingIndex != null) {
                            entries.toMutableList().also { it[editingIndex!!] = formatted }
                        } else entries + formatted
                        clearFields()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text(if (editingIndex != null) "Update Startup" else "+ Add Startup")
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
                    emptyMessage = "No founder entries yet.",
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
            } else {
                Text("No founder details required.", style = MaterialTheme.typography.bodyMedium)
            }

            errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isFounder && entries.isEmpty()) {
                        Toast.makeText(context, "Add at least one founder entry or select No", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onboardingViewModel.updateFounderStatus(isFounder, if (isFounder) entries else emptyList())
                    onboardingViewModel.saveDraft { success ->
                        if (success) navController.navigate(Screen.AmbitionStatement.route)
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