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
                title = "Criteria for Concept",
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
                text = "To complete the criteria for a concept in FounderFinder you must fill out forms on the following screens:",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "- Idea Creation: Your idea in the simplest form with a relevant image.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "- Business Plan: Turning your idea into a business model.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "- Partnership Agreement: Required if not a solopreneur, ensuring IP protection.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "- Proposal for Financing: If financial backing is needed for your idea.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "After completing all necessary sections, you can review your full Criteria for Concept in the Pitch Review and Submit section, then submit it to potential partners or investors.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { navController.navigate(Screen.IdeaCreation.route) }) {
                Text("Next")
            }
        }
    }
}