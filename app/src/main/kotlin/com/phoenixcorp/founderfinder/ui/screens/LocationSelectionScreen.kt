package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionScreen(navController: NavHostController) {
    var expandedGlobal by remember { mutableStateOf(false) }
    var expandedNational by remember { mutableStateOf(false) }
    var expandedLocal by remember { mutableStateOf(false) }

    // Location options
    val globalLocations = listOf("Earth", "Moon", "Mars")
    val nationalLocations = listOf("Canada", "USA", "Mexico")
    val localLocations = listOf(
        // Canadian provinces/territories
        "Alberta", "British Columbia", "Manitoba", "New Brunswick", "Newfoundland and Labrador",
        "Nova Scotia", "Ontario", "Prince Edward Island", "Quebec", "Saskatchewan",
        "Northwest Territories", "Nunavut", "Yukon",
        // US states
        "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut",
        "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa",
        "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan",
        "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire",
        "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio",
        "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota",
        "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia",
        "Wisconsin", "Wyoming",
        // Mexican states
        "Aguascalientes", "Baja California", "Baja California Sur", "Campeche", "Chiapas",
        "Chihuahua", "Coahuila", "Colima", "Durango", "Guanajuato", "Guerrero", "Hidalgo",
        "Jalisco", "Mexico State", "Michoacán", "Morelos", "Nayarit", "Nuevo León", "Oaxaca",
        "Puebla", "Querétaro", "Quintana Roo", "San Luis Potosí", "Sinaloa", "Sonora",
        "Tabasco", "Tamaulipas", "Tlaxcala", "Veracruz", "Yucatán", "Zacatecas",
        "Mexico City"
    )

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Select Location") },
                navController = navController,
                showBackButton = true
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // All Categories Button
            Button(
                onClick = {
                    navController.navigate("idea_generation?category=null") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("All Categories")
            }

            // Global Locations Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = "Global Locations",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Global") },
                    trailingIcon = {
                        IconButton(onClick = { expandedGlobal = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = expandedGlobal,
                    onDismissRequest = { expandedGlobal = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    globalLocations.forEach { location ->
                        DropdownMenuItem(
                            text = { Text(location) },
                            onClick = {
                                navController.navigate("idea_generation?category=globalissues/$location") {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                                expandedGlobal = false
                            }
                        )
                    }
                }
            }

            // National Locations Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = "National Locations",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("National") },
                    trailingIcon = {
                        IconButton(onClick = { expandedNational = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = expandedNational,
                    onDismissRequest = { expandedNational = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    nationalLocations.forEach { location ->
                        DropdownMenuItem(
                            text = { Text(location) },
                            onClick = {
                                navController.navigate("idea_generation?category=nationalissues/$location") {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                                expandedNational = false
                            }
                        )
                    }
                }
            }

            // Local Locations Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = "Local Locations",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Local") },
                    trailingIcon = {
                        IconButton(onClick = { expandedLocal = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = expandedLocal,
                    onDismissRequest = { expandedLocal = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    localLocations.forEach { location ->
                        DropdownMenuItem(
                            text = { Text(location) },
                            onClick = {
                                navController.navigate("idea_generation?category=localissues/$location") {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                                expandedLocal = false
                            }
                        )
                    }
                }
            }
        }
    }
}