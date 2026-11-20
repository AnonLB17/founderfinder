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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.OnboardingScaffold
import com.phoenixcorp.founderfinder.ui.viewmodel.OnboardingViewModel

/**
 * Final optional step. Finishes onboarding and goes to Home,
 * clearing the entire back stack so Back cannot return to onboarding.
 */
@Composable
fun PublicAppearanceScreen(
    navController: NavHostController,
    onboardingViewModel: OnboardingViewModel
) {
    val isLoading by onboardingViewModel.isLoading.collectAsState()
    val context = LocalContext.current

    OnboardingScaffold(
        navController = navController,
        title = "Public Appearance",
        showBack = true,
        isLoading = isLoading
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Almost done!", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Review your profile later from settings. Ready to explore FounderFinder?",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onboardingViewModel.completeOnboarding { success ->
                        if (success) {
                            navController.navigate(Screen.Home.route) {
                                // Clear entire stack — cannot go back into onboarding
                                popUpTo(0) { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, "Failed to complete", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Text("Finish & Go to Home")
            }
        }
    }
}