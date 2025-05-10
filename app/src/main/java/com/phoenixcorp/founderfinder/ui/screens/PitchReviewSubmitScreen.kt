package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner

@Composable
fun PitchReviewSubmitScreen(navController: NavHostController) {
    Scaffold(
        topBar = { ScreenBanner(
            title = "Pitch Review & Submit",
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
            Text("Here are your saved files for your Idea:", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))

            ClickableFileLink("Business Plan", onClick = { /* Open Business Plan */ })
            ClickableFileLink("Partnership Agreement", onClick = { /* Open Partnership Agreement */ })
            ClickableFileLink("Proposal for Financing", onClick = { /* Open Proposal for Financing */ })

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { /* Share with Investors */ }) {
                Text("Ready to Share")
            }
        }
    }
}

@Composable
fun ClickableFileLink(fileName: String, onClick: () -> Unit) {
    Text(
        text = fileName,
        style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    )
}
