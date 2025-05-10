package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.ui.components.NavigationImage
import com.phoenixcorp.founderfinder.navigation.Screen

@Composable
fun FindPartnersScreen(navController: NavHostController) {
    Scaffold(
        topBar = { ScreenBanner(title = "Find Partners") },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Ensures content is not hidden behind bars
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly, // Evenly spaces out the buttons
            horizontalAlignment = Alignment.CenterHorizontally // Centers them horizontally
        ) {
            NavigationImage(
                imageResId = R.drawable.advisor_search,
                onClick = { navController.navigate(Screen.AdvisorSearchFeature.route) }
            )
            NavigationImage(
                imageResId = R.drawable.partner_search,
                onClick = { navController.navigate(Screen.PartnerSearchFeature.route) }
            )
            NavigationImage(
                imageResId = R.drawable.school_forums,
                onClick = { navController.navigate(Screen.SchoolForums.route) }
            )
        }
    }
}

