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
import com.phoenixcorp.founderfinder.ui.viewmodel.FounderStatusViewModel

@Composable
fun FounderStatusScreen(
    navController: NavHostController,
    founderStatusViewModel: FounderStatusViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val isFounder by founderStatusViewModel.isFounder.collectAsState()
    val startupName by founderStatusViewModel.startupName.collectAsState()
    val startupStage by founderStatusViewModel.startupStage.collectAsState()
    val founderEntries by founderStatusViewModel.founderEntries.collectAsState()
    val isLoading by founderStatusViewModel.isLoading.collectAsState()
    val errorMessage by founderStatusViewModel.errorMessage.collectAsState()

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
            text = "Founder Status",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Are you currently a founder?")
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = isFounder,
                    onClick = { founderStatusViewModel.setFounderStatus(true) },
                    enabled = !isLoading
                )
                Text("Yes")
            }
            Spacer(modifier = Modifier.width(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = !isFounder,
                    onClick = { founderStatusViewModel.setFounderStatus(false) },
                    enabled = !isLoading
                )
                Text("No")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isFounder) {
            OutlinedTextField(
                value = startupName,
                onValueChange = { founderStatusViewModel.updateStartupName(it) },
                label = { Text("Startup Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = startupStage,
                onValueChange = { founderStatusViewModel.updateStartupStage(it) },
                label = { Text("Startup Stage (e.g. Pre-seed, Seed, Series A)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    founderStatusViewModel.addFounderEntry()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("+ Add Startup")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (founderEntries.isNotEmpty()) {
                Text("Added Founder Entries:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                founderEntries.forEach { entry ->
                    Text(text = "• $entry", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            } else {
                Text(text = "No founder entries added yet.", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Text(
                text = "No founder details required.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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

                founderStatusViewModel.saveFounderStatus(currentUser.uid) { success ->
                    if (success) {
                        navController.navigate(Screen.AmbitionStatement.route) {
                            popUpTo(Screen.FounderStatus.route) { inclusive = true }
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
fun FounderStatusScreenPreview() {
    FounderStatusScreen(navController = rememberNavController())
}