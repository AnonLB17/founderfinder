package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner

@Composable
fun SchoolForumsScreen(navController: NavHostController) {
    val institutions = listOf("Harvard University", "MIT", "Stanford University", "Oxford University")

    Scaffold(
        topBar = { ScreenBanner(
            title = "School Forums",
            navController = navController,
            showBackButton = true
        ) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Select your institution's forum", style = MaterialTheme.typography.bodyLarge)

            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                items(institutions.size) { index ->
                    InstitutionItem(institutions[index]) {
                        // Navigate to the institution's specific forum screen
                        navController.navigate("InstitutionForumScreen/${institutions[index]}")
                    }
                }
            }
        }
    }
}

@Composable
fun InstitutionItem(name: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
