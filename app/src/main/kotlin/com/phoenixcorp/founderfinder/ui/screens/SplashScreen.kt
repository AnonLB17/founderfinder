package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

/**
 * Splash / Landing screen.
 *
 * - Always the startDestination of the NavGraph.
 * - Checks Firebase Auth via AuthViewModel (MVVM).
 * - If already signed in → auto-navigate to Home (no flash of buttons).
 * - If not signed in → show logo + "Get Started" / "Sign In" choice.
 */
@Composable
fun SplashScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var isCheckingAuth by remember { mutableStateOf(true) }

    // Clean MVVM auth check on first composition
    LaunchedEffect(Unit) {
        // Small delay so the splash logo is visible (optional branding)
        delay(900)

        val currentUser = authViewModel.getCurrentUser()
        if (currentUser != null) {
            // Already authenticated → go straight to main app
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            // Not signed in → show the choice buttons
            isCheckingAuth = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ff_logo),
            contentDescription = "Founder Finder Logo",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome to Founder Finder",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isCheckingAuth) {
            // Show spinner while we decide
            CircularProgressIndicator()
        } else {
            // Unauthenticated → show the choice the user wanted
            Button(
                onClick = {
                    navController.navigate(Screen.SignUp.route)
                }
            ) {
                Text("Get Started")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    navController.navigate(Screen.SignIn.route)
                }
            ) {
                Text("Already have an account? Sign In")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(navController = rememberNavController())
}