package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestorInfoScreen(navController: NavHostController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var industry by remember { mutableStateOf("") }
    var philosophy by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pre-fill name from Regular User Profile
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                val profileDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val profile = profileDoc.toObject(UserProfile::class.java)
                if (profile != null) {
                    name = when {
                        !profile.firstName.isNullOrBlank() && !profile.lastName.isNullOrBlank() -> "${profile.firstName} ${profile.lastName}"
                        !profile.firstName.isNullOrBlank() -> profile.firstName
                        !profile.lastName.isNullOrBlank() -> profile.lastName
                        else -> ""
                    }
                    Log.d("InvestorInfoScreen", "Pre-filled name: $name")
                }
            } catch (e: Exception) {
                Log.e("InvestorInfoScreen", "Error fetching user profile: ${e.message}", e)
            }
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Create Investor Profile") },
                navController = navController,
                showBackButton = true,
                // Other parameters...
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enter your investor profile details",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = name.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Contact Email") },
                modifier = Modifier.fillMaxWidth(),
                isError = email.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = industry,
                onValueChange = { industry = it },
                label = { Text("Primary Industry Focus") },
                modifier = Modifier.fillMaxWidth(),
                isError = industry.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = philosophy,
                onValueChange = { philosophy = it },
                label = { Text("Investment Philosophy") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                isError = philosophy.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (currentUser == null) {
                        Toast.makeText(context, "Please sign in", Toast.LENGTH_SHORT).show()
                        navController.navigate(Screen.SignIn.route)
                        return@Button
                    }
                    if (name.isBlank() || email.isBlank() || industry.isBlank() || philosophy.isBlank()) {
                        errorMessage = "Please fill all fields."
                        return@Button
                    }
                    coroutineScope.launch {
                        try {
                            val investorData = mapOf(
                                "name" to name,
                                "email" to email,
                                "industry" to industry,
                                "philosophy" to philosophy,
                                "userId" to currentUser.uid,
                                "createdAt" to System.currentTimeMillis()
                            )
                            firestore.collection("investors")
                                .document(currentUser.uid)
                                .set(investorData)
                                .await()
                            Toast.makeText(context, "Investor profile created", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.IndustryPreferences.route) {
                                popUpTo(Screen.SelectUserType.route) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Log.e("InvestorInfoScreen", "Error creating profile: ${e.message}", e)
                            errorMessage = "Failed to create profile: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Next")
            }
        }
    }
}