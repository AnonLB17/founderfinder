package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.navigation.Screen

@Composable
fun IdeaDevelopmentScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Idea Development") },
                navController = navController,
                showBackButton = false
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { navController.navigate(Screen.IdeaGeneration.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Idea Generation",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Button(
                onClick = { navController.navigate(Screen.CriteriaForConcept.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Criteria For Concept",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Button(
                onClick = { navController.navigate(Screen.IncubatorConnection.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Incubator Connection",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Button(
                onClick = { navController.navigate(Screen.InvestorSearch.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Investor Search",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}