package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.ui.components.ForumCard
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner

@Composable
fun GlobalIssuesScreen(navController: NavHostController) {
    Scaffold(
        topBar = { ScreenBanner(
            title = "Global Issues",
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