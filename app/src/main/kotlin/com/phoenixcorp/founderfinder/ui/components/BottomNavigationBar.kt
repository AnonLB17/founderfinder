package com.phoenixcorp.founderfinder.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import com.phoenixcorp.founderfinder.navigation.Screen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    val navItems = listOf(
        BottomNavItem(Screen.Home.route, Icons.Filled.Home, ""),
        BottomNavItem(Screen.UserProfile.route, Icons.Filled.Person, ""),
        BottomNavItem(Screen.Partners.route, Icons.Filled.People, ""),
        BottomNavItem(Screen.FindPartners.route, Icons.Filled.Search, ""),
        BottomNavItem(Screen.IdeaDevelopment.route, Icons.Filled.Lightbulb, "")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = MaterialTheme.colorScheme.primary) {
        navItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route || (item.route == Screen.UserProfile.route && currentRoute?.startsWith("user_profile/") == true),
                onClick = {
                    if (item.route == Screen.UserProfile.route) {
                        if (userId.isNotEmpty()) {
                            navController.navigate(Screen.UserProfile.createRoute(userId)) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(Screen.SignIn.route)
                        }
                    } else {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (currentRoute == item.route || (item.route == Screen.UserProfile.route && currentRoute?.startsWith("user_profile/") == true))
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSecondary
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}

data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)