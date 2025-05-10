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
import com.google.firebase.auth.FirebaseAuth
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.viewmodel.AuthViewModel

@Composable
fun AmbitionStatementScreen(navController: NavHostController, authViewModel: AuthViewModel = viewModel()) {
    var ambitionStatement by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Ambition Statement", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = ambitionStatement,
            onValueChange = { ambitionStatement = it },
            label = { Text("Write about your ambition and goals") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            maxLines = 5 // Allow multi-line input for a statement
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Show error message if any
        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (userId == null) {
                    errorMessage = "You must be logged in to save your ambition statement."
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (ambitionStatement.isBlank()) {
                    errorMessage = "Please enter your ambition statement."
                    return@Button
                }

                isLoading = true
                errorMessage = null
                authViewModel.saveAmbitionStatement(userId, ambitionStatement) { success ->
                    isLoading = false
                    if (success) {
                        navController.navigate(Screen.ConnectSocials.route) {
                            popUpTo(Screen.AmbitionStatement.route) { inclusive = true }
                        }
                    } else {
                        errorMessage = "Failed to save ambition statement. Please try again."
                        Toast.makeText(context, "Failed to save ambition statement", Toast.LENGTH_SHORT).show()
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
fun AmbitionStatementScreenPreview() {
    AmbitionStatementScreen(navController = rememberNavController())
}