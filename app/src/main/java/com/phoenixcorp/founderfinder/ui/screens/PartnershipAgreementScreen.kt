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
fun PartnershipAgreementScreen(navController: NavHostController) {
    Scaffold(
        topBar = { ScreenBanner(
            title = "Partnership Agreement",
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
                "By following this guide, you can draft a partnership agreement and submit the file at the bottom of the screen. Note that it is recommended for partnership agreements to be reviewed by legal counsel, which you can find in the Advisor Search Feature before agreeing to anything.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Essential Sections of a Partnership Agreement with Descriptions",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "A Partnership Agreement is a legally binding document that outlines the roles, responsibilities, and rights of business partners. It ensures clarity, minimizes disputes, and protects the interests of all parties involved.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Placeholder for file upload
            Button(
                onClick = { /* TODO: Implement file upload functionality */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Upload Partnership Agreement")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { navController.navigate(Screen.ProposalForFinancing.route) }) {
                Text("Next")
            }
        }
    }
}
