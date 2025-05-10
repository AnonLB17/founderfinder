package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.ui.components.ForumCard

@Composable
fun LocalIssuesScreen(navController: NavHostController) {
    Scaffold(
        topBar = { ScreenBanner(
            title = "Local Issues",
            navController = navController,
            showBackButton = true
        ) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sampleForums) { forum ->
                    ForumCard(forum, navController)
                }
            }
        }
    }
}




