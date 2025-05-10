package com.phoenixcorp.founderfinder.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Lightbulb
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.phoenixcorp.founderfinder.navigation.Screen

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navItems = listOf(
        BottomNavItem(Screen.Home.route, Icons.Filled.Home),
        BottomNavItem(Screen.Profile.route, Icons.Filled.Person),
        BottomNavItem(Screen.Partners.route, Icons.Filled.Group),
        BottomNavItem(Screen.FindPartners.route, Icons.Filled.Search),
        BottomNavItem(Screen.IdeaDevelopment.route, Icons.Filled.Lightbulb)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = MaterialTheme.colorScheme.primary) {
        navItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { navController.navigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.route,
                        tint = if (currentRoute == item.route) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSecondary
                    )
                }
            )
        }
    }
}

data class BottomNavItem(val route: String, val icon: ImageVector)