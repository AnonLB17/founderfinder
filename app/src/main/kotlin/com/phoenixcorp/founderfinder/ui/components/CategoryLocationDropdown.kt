package com.phoenixcorp.founderfinder.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CategoryLocationDropdown(
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var currentMenuLevel by remember { mutableStateOf("root") } // root, global, national, local
    var selectedGlobal by remember { mutableStateOf<String?>(null) }
    var selectedNational by remember { mutableStateOf<String?>(null) }

    // Define the hierarchical location data
    val categories = listOf(
        "Global Issues", "National Issues", "Local Issues",
        "Future", "Market Potential", "Requested Solutions"
    )
    val locations = mapOf(
        "Global Issues" to mapOf(
            "Earth" to emptyMap<String, List<String>>(),
            "Moon" to emptyMap(),
            "Mars" to emptyMap()
        ),
        "National Issues" to mapOf(
            "Canada" to emptyMap(),
            "USA" to emptyMap(),
            "Mexico" to emptyMap()
        ),
        "Local Issues" to mapOf(
            "Canada" to mapOf(
                "Provinces" to listOf(
                    "Alberta", "British Columbia", "Manitoba", "New Brunswick",
                    "Newfoundland and Labrador", "Nova Scotia", "Ontario",
                    "Prince Edward Island", "Quebec", "Saskatchewan"
                )
            ),
            "USA" to mapOf(
                "States" to listOf(
                    "Alabama", "Alaska", "Arizona", "Arkansas", "California",
                    "Colorado", "Connecticut", "Delaware", "Florida", "Georgia",
                    "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa", "Kansas",
                    "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts",
                    "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana",
                    "Nebraska", "Nevada", "New Hampshire", "New Jersey",
                    "New Mexico", "New York", "North Carolina", "North Dakota",
                    "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island",
                    "South Carolina", "South Dakota", "Tennessee", "Texas",
                    "Utah", "Vermnt", "Virginia", "Washington", "West Virginia",
                    "Wisconsin", "Wyoming"
                )
            ),
            "Mexico" to mapOf(
                "States" to listOf(
                    "Aguascalientes", "Baja California", "Baja California Sur",
                    "Campeche", "Chiapas", "Chihuahua", "Coahuila", "Colima",
                    "Durango", "Guanajuato", "Guerrero", "Hidalgo", "Jalisco",
                    "Mexico", "Michoacán", "Morelos", "Nayarit", "Nuevo León",
                    "Oaxaca", "Puebla", "Querétaro", "Quintana Roo", "San Luis Potosí",
                    "Sinaloa", "Sonora", "Tabasco", "Tamaulipas", "Tlaxcala",
                    "Veracruz", "Yucatán", "Zacatecas"
                )
            )
        )
    )

    // Display text for the dropdown
    val displayText = selectedCategory?.split("/")?.let { parts ->
        if (parts.size > 1) parts[0] else parts[0]
    } ?: "Idea Generation"

    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleLarge
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                currentMenuLevel = "root"
                selectedGlobal = null
                selectedNational = null
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            when (currentMenuLevel) {
                "root" -> {
                    // Show main categories
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                if (category in listOf("Global Issues", "National Issues", "Local Issues")) {
                                    currentMenuLevel = "global"
                                } else {
                                    onCategorySelected(category)
                                    expanded = false
                                    currentMenuLevel = "root"
                                    selectedGlobal = null
                                    selectedNational = null
                                }
                            }
                        )
                    }
                }
                "global" -> {
                    // Show global/national categories
                    locations[selectedCategory?.split("/")?.get(0) ?: return@DropdownMenu]?.keys?.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                selectedGlobal = item
                                if (selectedCategory?.startsWith("Global Issues") == true || selectedCategory?.startsWith("National Issues") == true) {
                                    onCategorySelected("${selectedCategory?.split("/")?.get(0)}/${item}")
                                    expanded = false
                                    currentMenuLevel = "root"
                                    selectedGlobal = null
                                    selectedNational = null
                                } else {
                                    currentMenuLevel = "national"
                                }
                            }
                        )
                    }
                }
                "national" -> {
                    // Show local categories (provinces/states) for the selected nation
                    locations[selectedCategory?.split("/")?.get(0) ?: return@DropdownMenu]?.get(selectedGlobal)?.keys?.forEach { subCategory ->
                        DropdownMenuItem(
                            text = { Text(subCategory) },
                            onClick = {
                                selectedNational = subCategory
                                currentMenuLevel = "local"
                            }
                        )
                    }
                }
                "local" -> {
                    // Show specific provinces/states
                    locations[selectedCategory?.split("/")?.get(0) ?: return@DropdownMenu]?.get(selectedGlobal)?.get(selectedNational)?.forEach { location ->
                        DropdownMenuItem(
                            text = { Text(location) },
                            onClick = {
                                onCategorySelected("${selectedCategory?.split("/")?.get(0)}/${selectedGlobal} -> ${location}")
                                expanded = false
                                currentMenuLevel = "root"
                                selectedGlobal = null
                                selectedNational = null
                            }
                        )
                    }
                }
            }
        }
    }
}