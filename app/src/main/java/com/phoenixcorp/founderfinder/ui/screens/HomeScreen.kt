package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.navigation.Screen

@Composable
fun HomeScreen(navController: NavHostController) {
    Scaffold(
        topBar = { ScreenBanner(title = "Home") },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Philosophy of the Day (Placeholder)
            Text(
                text = "Philosophy of the Day: \"Success is not final, failure is not fatal: It is the courage to continue that counts.\"",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Trending Threads Section
            Text("Trending Threads", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow {
                items(getTrendingThreads()) { thread ->
                    TrendingThreadItem(thread, navController)
                }
            }
        }
    }
}

// Sample Data for Threads
data class TrendingThread(val title: String, val route: String)

// Function to retrieve trending threads (Placeholder)
fun getTrendingThreads(): List<TrendingThread> {
    return listOf(
        TrendingThread("How to pitch to investors", Screen.Future.route),
        TrendingThread("Best startup incubators", Screen.GlobalIssues.route),
        TrendingThread("Tech industry trends", Screen.NationalIssues.route),
        TrendingThread("Legal aspects of startups", Screen.LocalIssues.route)
    )
}

@Composable
fun TrendingThreadItem(thread: TrendingThread, navController: NavHostController) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .width(200.dp)
            .clickable { navController.navigate(thread.route) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(thread.title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
