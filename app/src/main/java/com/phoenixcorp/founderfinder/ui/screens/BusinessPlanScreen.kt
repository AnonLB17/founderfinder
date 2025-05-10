package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.navigation.Screen

@Composable
fun BusinessPlanScreen(navController: NavHostController) {
    Scaffold(
        topBar = { ScreenBanner(
            title = "Business Plan",
            navController = navController,
            showBackButton = true
        ) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Following the instructions provided you can develop a simple business plan and submit the file at the bottom of the screen.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Business Plan Sections with Descriptions", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("1. Executive Summary", style = MaterialTheme.typography.bodyMedium)
            Text("2. Company Description", style = MaterialTheme.typography.bodyMedium)
            Text("3. Market Research & Industry Analysis", style = MaterialTheme.typography.bodyMedium)
            Text("4. Products & Services", style = MaterialTheme.typography.bodyMedium)
            Text("5. Marketing & Sales Strategy", style = MaterialTheme.typography.bodyMedium)
            Text("6. Operations & Management", style = MaterialTheme.typography.bodyMedium)
            Text("7. Financial Plan", style = MaterialTheme.typography.bodyMedium)
            Text("8. Risk Analysis & Contingency Plan", style = MaterialTheme.typography.bodyMedium)
            Text("9. Supporting Documents & Appendices", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Implement file upload logic */ }) {
                Text("Upload Business Plan")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { navController.navigate(Screen.PartnershipAgreement.route) }) {
                Text("Next")
            }
        }
    }
}
