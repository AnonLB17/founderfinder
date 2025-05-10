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
fun ProposalForFinancingScreen(navController: NavHostController) {
    Scaffold(
        topBar = { ScreenBanner(
            title = "Proposal for Financing",
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
            Text("By following this guide, you can draft a proposal for financing and submit the file at the bottom of the screen.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Essential Sections of a Proposal for Financing", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            val sections = listOf(
                "Executive Summary - Overview of the business and financing request.",
                "Business Description & Background - Mission, operations, and structure.",
                "Market Opportunity & Competitive Analysis - Growth potential and competitors.",
                "Financial Request & Purpose of Funding - Exact amount and fund allocation.",
                "Revenue Model & Financial Projections - Profitability analysis.",
                "Repayment Plan & Exit Strategy - Investor repayment details.",
                "Risk Analysis & Mitigation Strategies - Addressing potential risks.",
                "Supporting Documents & Appendices - Additional proof of credibility."
            )

            sections.forEach { section ->
                Text("- $section", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* File picker logic for uploading */ }) {
                Text("Upload Proposal Document")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { navController.navigate(Screen.PitchReviewSubmit.route) }) {
                Text("Next")
            }
        }
    }
}
