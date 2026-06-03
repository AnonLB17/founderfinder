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
import com.phoenixcorp.founderfinder.ui.viewmodel.AmbitionStatementViewModel
import com.phoenixcorp.founderfinder.ui.viewmodel.AuthViewModel

@Composable
fun AmbitionStatementScreen(
    navController: NavHostController,
    ambitionViewModel: AmbitionStatementViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val ambitionStatement by ambitionViewModel.ambitionStatement.collectAsState()
    val isLoading by ambitionViewModel.isLoading.collectAsState()
    val errorMessage by ambitionViewModel.errorMessage.collectAsState()

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
            text = "Ambition Statement",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = ambitionStatement,
            onValueChange = { ambitionViewModel.updateAmbitionStatement(it) },
            label = { Text("Write about your ambition and goals") },
            placeholder = { Text("I want to build a platform that connects founders with advisors...") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            maxLines = 8,
            minLines = 5
        )

        Spacer(modifier = Modifier.height(24.dp))

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                if (currentUser == null) {
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                ambitionViewModel.saveAmbitionStatement(currentUser.uid) { success ->
                    if (success) {
                        navController.navigate(Screen.ConnectSocials.route) {
                            popUpTo(Screen.AmbitionStatement.route) { inclusive = true }
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
fun AmbitionStatementScreenPreview() {
    AmbitionStatementScreen(navController = rememberNavController())
}