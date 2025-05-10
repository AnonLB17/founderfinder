package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.navigation.Screen

@Composable
fun PartnersScreen(navController: NavHostController) {
    Scaffold(
        topBar = { ScreenBanner(title = "Partners") },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Horizontal Scroll View with Organization and Partner Profiles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Organization Photo
                Image(
                    painter = painterResource(id = R.drawable.ic_placeholder),
                    contentDescription = "Organization",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .clickable { /* Show Organization Activities */ }
                        .padding(8.dp)
                )

                // Partners Profile Pictures
                repeat(5) { // Example placeholders for partners
                    Image(
                        painter = painterResource(id = R.drawable.ic_profile_placeholder),
                        contentDescription = "Partner Profile",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .clickable { /* Show Partner Calendar */ }
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calendar Section
            CalendarSection()
        }
    }
}

@Composable
fun CalendarSection() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Partners' Activities Calendar", style = MaterialTheme.typography.titleMedium)

        // Placeholder for 28-day calendar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Calendar Placeholder")
        }

        // Navigation Arrows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { /* Navigate back 28 days */ }) { Text("<") }
            Text("Current Month", style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = { /* Navigate forward 28 days */ }) { Text(">") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Activity Input or Details Section
        Text("Tap a date to see activities or add new ones", style = MaterialTheme.typography.bodyMedium)
    }
}
