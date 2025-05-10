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
fun IdeaDevelopmentScreen(navController: NavHostController) {
    Scaffold(
        topBar = { ScreenBanner(title = "Idea Development") },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly, // Evenly spaces buttons
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NavigationImage(
                imageResId = R.drawable.idea_generation,
                onClick = { navController.navigate(Screen.IdeaGeneration.route) }
            )
            NavigationImage(
                imageResId = R.drawable.criteria_for_concept,
                onClick = { navController.navigate(Screen.CriteriaForConcept.route) }
            )
            NavigationImage(
                imageResId = R.drawable.incubator_connection,
                onClick = { navController.navigate(Screen.IncubatorConnection.route) }
            )
            NavigationImage(
                imageResId = R.drawable.investor_search,
                onClick = { navController.navigate(Screen.InvestorSearch.route) }
            )
        }
    }
}


