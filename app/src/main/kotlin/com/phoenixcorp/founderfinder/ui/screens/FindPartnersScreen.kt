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
fun FindPartnersScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Find Partners") },
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
                onClick = { navController.navigate(Screen.AdvisorSearchFeature.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Advisor Search",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Button(
                onClick = { navController.navigate(Screen.PartnerSearchFeature.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Partner Search",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Button(
                onClick = { navController.navigate(Screen.SchoolForums.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "School Forum",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}