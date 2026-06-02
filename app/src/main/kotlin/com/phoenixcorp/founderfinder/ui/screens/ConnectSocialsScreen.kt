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

@Composable
fun ConnectSocialsScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()   // ← Fixed: Use Hilt
) {
    var linkedin by remember { mutableStateOf("") }
    var twitter by remember { mutableStateOf("") }
    var facebook by remember { mutableStateOf("") }
    var instagram by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // Use ViewModel instead of direct Firebase call
    val currentUser = authViewModel.getCurrentUser()
    val userId = currentUser?.uid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Connect Socials",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = linkedin,
            onValueChange = { linkedin = it },
            label = { Text("LinkedIn Profile URL") },
            placeholder = { Text("https://linkedin.com/in/yourprofile") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = twitter,
            onValueChange = { twitter = it },
            label = { Text("Twitter / X Profile URL") },
            placeholder = { Text("https://twitter.com/yourprofile") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = facebook,
            onValueChange = { facebook = it },
            label = { Text("Facebook Profile URL") },
            placeholder = { Text("https://facebook.com/yourprofile") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = instagram,
            onValueChange = { instagram = it },
            label = { Text("Instagram Profile URL") },
            placeholder = { Text("https://instagram.com/yourprofile") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = website,
            onValueChange = { website = it },
            label = { Text("Personal Website or Portfolio") },
            placeholder = { Text("https://yourwebsite.com") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (userId == null) {
                    errorMessage = "You must be logged in."
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                errorMessage = null

                authViewModel.saveSocials(
                    userId = userId,
                    linkedin = linkedin.trim(),
                    twitter = twitter.trim(),
                    facebook = facebook.trim(),
                    instagram = instagram.trim(),
                    website = website.trim()
                ) { success ->
                    isLoading = false
                    if (success) {
                        navController.navigate(Screen.IndustriesOfInterest.route) {
                            popUpTo(Screen.ConnectSocials.route) { inclusive = true }
                        }
                    } else {
                        errorMessage = "Failed to save social links. Please try again."
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
fun ConnectSocialsScreenPreview() {
    ConnectSocialsScreen(navController = rememberNavController())
}