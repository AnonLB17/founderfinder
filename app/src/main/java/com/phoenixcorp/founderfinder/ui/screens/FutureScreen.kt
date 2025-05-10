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
fun FutureScreen(navController: NavHostController) {
    Scaffold(
        topBar = { ScreenBanner(
            title = "Future",
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


data class Forum(val title: String, val description: String, val creatorName: String)

val sampleForums = listOf(
    Forum("Tech Innovations", "Discussion on latest tech trends", "Alice Johnson"),
    Forum("Startup Funding", "Talk about investment strategies", "John Doe"),
    Forum("Marketing Tactics", "Best practices for digital marketing", "Jane Smith")
)

