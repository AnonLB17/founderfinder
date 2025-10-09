package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner

@Composable
fun CriteriaForConceptScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Advisor Search") },
                navController = navController,
                showBackButton = true
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
                text = "To create a concept in FounderFinder, complete the following steps:",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "- Idea Creation: Define your organization by uploading a photo, entering a business name, and describing your idea or goal.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "- Organization Files: Upload and manage files (e.g., Business Plan, Proposal for Financing) associated with your organization to share with potential partners or investors.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "After completing these steps, your concept will be ready to share with potential partners or investors via the Organization Files screen.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { navController.navigate(Screen.IdeaCreation.route) }) {
                Text("Start")
            }
        }
    }
}