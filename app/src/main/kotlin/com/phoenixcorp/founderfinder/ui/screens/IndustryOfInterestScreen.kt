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
import com.phoenixcorp.founderfinder.ui.viewmodel.IndustriesOfInterestViewModel

@Composable
fun IndustriesOfInterestScreen(
    navController: NavHostController,
    industriesViewModel: IndustriesOfInterestViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val keyword by industriesViewModel.keyword.collectAsState()
    val industries by industriesViewModel.industries.collectAsState()
    val isLoading by industriesViewModel.isLoading.collectAsState()
    val errorMessage by industriesViewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    val currentUser = authViewModel.getCurrentUser()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Industries of Interest",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = keyword,
            onValueChange = { industriesViewModel.updateKeyword(it) },
            label = { Text("Search Industry") },
            placeholder = { Text("e.g. FinTech, AI, HealthTech") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                industriesViewModel.addIndustry()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("+ Add")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (industries.isNotEmpty()) {
            Text("Selected Industries:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            industries.forEach { industry ->
                Text(text = "• $industry", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
            }
        } else {
            Text(
                text = "No industries added yet.",
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
                if (currentUser == null) {
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                industriesViewModel.saveIndustries(currentUser.uid) { success ->
                    if (success) {
                        navController.navigate(Screen.OrganizationsOfInterest.route) {
                            popUpTo(Screen.IndustriesOfInterest.route) { inclusive = true }
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
fun IndustriesOfInterestScreenPreview() {
    IndustriesOfInterestScreen(navController = rememberNavController())
}