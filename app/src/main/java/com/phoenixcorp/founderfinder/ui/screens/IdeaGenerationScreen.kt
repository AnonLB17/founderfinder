package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.navigation.Screen

@Composable
fun IdeaGenerationScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            ScreenBanner(
                title = "Idea Generation",
                navController = navController,
                showAddButton = true // ✅ Enables the "+" button
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), // Ensures spacing
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Each button should expand to fill available vertical space
            CategoryButton("Global Issues", navController, Screen.GlobalIssues, Modifier.weight(1f))
            CategoryButton("National Issues", navController, Screen.NationalIssues, Modifier.weight(1f))
            CategoryButton("Local Issues", navController, Screen.LocalIssues, Modifier.weight(1f))
            CategoryButton("Future", navController, Screen.Future, Modifier.weight(1f))
            CategoryButton("Market Potential", navController, Screen.MarketPotential, Modifier.weight(1f))
            CategoryButton("Requested Solutions", navController, Screen.RequestedSolutions, Modifier.weight(1f))
        }
    }
}

@Composable
fun CategoryButton(category: String, navController: NavHostController, destination: Screen, modifier: Modifier) {
    Button(
        onClick = { navController.navigate(destination.route) },
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.headlineMedium // ✅ Makes text larger
        )
    }
}