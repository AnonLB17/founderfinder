package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.tasks.await

@Composable
fun SignInScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Sign In", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Please fill in all fields."
                    return@Button
                }

                isLoading = true
                errorMessage = null

                authViewModel.signInUser(email, password) { success: Boolean, message: String? ->
                    isLoading = false
                    if (success) {
                        // FCM Setup on successful login
                        setupFcmNotifications()

                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SignIn.route) { inclusive = true }
                        }
                    } else {
                        errorMessage = message ?: "Sign in failed. Please try again."
                        Toast.makeText(context, message ?: "Sign in failed", Toast.LENGTH_SHORT).show()
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
                Text("Sign In")
            }
        }
    }
}

private fun setupFcmNotifications() {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val firestore = FirebaseFirestore.getInstance()

    // Get and save FCM Token
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val token = task.result
            firestore.collection("users").document(currentUser.uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM", "✅ FCM Token saved for user ${currentUser.uid}")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM", "Failed to save FCM token", e)
                }
        } else {
            Log.e("FCM", "Failed to get FCM token", task.exception)
        }
    }

    // Subscribe to personal topic
    val topic = "user_${currentUser.uid}"
    FirebaseMessaging.getInstance().subscribeToTopic(topic)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM", "✅ Subscribed to topic: $topic")
            } else {
                Log.e("FCM", "Failed to subscribe to topic", task.exception)
            }
        }
}

@Preview(showBackground = true)
@Composable
fun SignInScreenPreview() {
    SignInScreen(navController = rememberNavController())
}