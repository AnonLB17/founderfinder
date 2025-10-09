package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.data.Forum
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ForumCard
import com.phoenixcorp.founderfinder.navigation.Screen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Data class to hold location and category data
data class LocationCategoryData(
    val selectedGlobe: String?,
    val selectedNation: String?,
    val selectedProvince: String?,
    val selectedCategory: String?,
    val buttonText: String,
    val selectedCategoryName: String?,
    val selectedCategoryDisplayName: String?
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IdeaGenerationScreen(
    navController: NavHostController,
    showAddButton: Boolean = true,
    category: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var selectedGlobe by remember { mutableStateOf<String?>(null) }
    var selectedNation by remember { mutableStateOf<String?>(null) }
    var selectedProvince by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(category?.split("/")?.get(0)?.lowercase()?.replace(" ", "") ?: null) }
    var forums by remember { mutableStateOf<List<Forum>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var tempGlobe by remember { mutableStateOf<String?>(null) }
    var tempNation by remember { mutableStateOf<String?>(null) }
    var tempProvince by remember { mutableStateOf<String?>(null) }

    // Location options
    val globes = listOf("Earth", "Moon", "Mars")
    val nations = listOf("Canada", "USA", "Mexico")
    val provincesByNation = mapOf(
        "Canada" to listOf(
            "Alberta", "British Columbia", "Manitoba", "New Brunswick", "Newfoundland and Labrador",
            "Nova Scotia", "Ontario", "Prince Edward Island", "Quebec", "Saskatchewan",
            "Northwest Territories", "Nunavut", "Yukon"
        ),
        "USA" to listOf(
            "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut",
            "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa",
            "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan",
            "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire",
            "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio",
            "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota",
            "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia",
            "Wisconsin", "Wyoming"
        ),
        "Mexico" to listOf(
            "Aguascalientes", "Baja California", "Baja California Sur", "Campeche", "Chiapas",
            "Chihuahua", "Coahuila", "Colima", "Durango", "Guanajuato", "Guerrero", "Hidalgo",
            "Jalisco", "Mexico State", "Michoacán", "Morelos", "Nayarit", "Nuevo León", "Oaxaca",
            "Puebla", "Querétaro", "Quintana Roo", "San Luis Potosí", "Sinaloa", "Sonora",
            "Tabasco", "Tamaulipas", "Tlaxcala", "Veracruz", "Yucatán", "Zacatecas", "Mexico City"
        )
    )

    // Categories for buttons
    val categories = listOf(
        "Global Issues" to "globalissues",
        "National Issues" to "nationalissues",
        "Local Issues" to "localissues",
        "Future" to "future",
        "Market Potential" to "marketpotential",
        "Requested Solutions" to "requestedsolutions"
    )

    // Fetch forums based on location and category
    LaunchedEffect(selectedGlobe, selectedNation, selectedProvince, selectedCategory) {
        if (selectedGlobe != null && selectedCategory != null) {
            isLoading = true
            try {
                val globe = selectedGlobe
                val nation = selectedNation
                val province = selectedProvince
                val cat = selectedCategory
                val locations = mutableListOf<String>()
                locations.add(globe!!)
                nation?.let { locations.add(it) }
                province?.let { locations.add(it) }
                Log.d("IdeaGenerationScreen", "Fetching forums for category: $cat, locations: $locations")
                val query = firestore.collection("category")
                    .document(cat!!)
                    .collection("forum")
                    .whereIn("location", locations)

                val snapshot = query.get().await()
                forums = snapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        Forum(
                            id = doc.id,
                            title = data["name"] as? String ?: "Untitled",
                            description = data["description"] as? String ?: "",
                            creatorId = data["creatorId"] as? String ?: "",
                            creatorName = data["creatorName"] as? String ?: "Anonymous",
                            timestamp = (data["timestamp"] as? Long) ?: 0L,
                            imageUrl = data["imageUrl"] as? String,
                            likes = data["likes"]?.toString()?.toIntOrNull() ?: 0,
                            isFavorited = data["isFavorited"] as? Boolean ?: false,
                            hasLiked = if (currentUser != null) {
                                firestore.collection("category")
                                    .document(cat!!)
                                    .collection("forum")
                                    .document(doc.id)
                                    .collection("likes")
                                    .document(currentUser.uid)
                                    .get()
                                    .await()
                                    .exists()
                            } else false,
                            category = cat!!,
                            location = data["location"] as? String
                        )
                    } catch (e: Exception) {
                        Log.e("IdeaGenerationScreen", "Error parsing forum ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                Log.d("IdeaGenerationScreen", "Fetched ${forums.size} forums for category: $selectedCategory, locations: $locations")
                isLoading = false
            } catch (e: Exception) {
                Log.e("IdeaGenerationScreen", "Error fetching forums: ${e.message}", e)
                errorMessage = "Failed to load forums: ${e.message}"
                isLoading = false
            }
        }
    }

    // Location and category data
    val locationCategoryData = when {
        selectedGlobe == null -> LocationCategoryData(
            selectedGlobe = null,
            selectedNation = null,
            selectedProvince = null,
            selectedCategory = null,
            buttonText = "Select Location",
            selectedCategoryName = null,
            selectedCategoryDisplayName = null
        )
        else -> {
            val displayName = categories.find { it.second == selectedCategory }?.first ?: "Select Category"
            val buttonText = when {
                selectedProvince != null -> "$selectedGlobe -> $selectedNation -> $selectedProvince"
                selectedNation != null -> "$selectedGlobe -> $selectedNation"
                else -> selectedGlobe!!
            }
            LocationCategoryData(
                selectedGlobe = selectedGlobe,
                selectedNation = selectedNation,
                selectedProvince = selectedProvince,
                selectedCategory = selectedCategory,
                buttonText = buttonText,
                selectedCategoryName = selectedCategory,
                selectedCategoryDisplayName = displayName
            )
        }
    }

    // Destructure the data
    val (selectedGlobeData, selectedNationData, selectedProvinceData, selectedCategoryData, buttonText, selectedCategoryName, selectedCategoryDisplayName) = locationCategoryData

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Idea Generation") },
                subtitle = buttonText,
                navController = navController,
                showAddButton = showAddButton && selectedGlobe != null,
                categoryButtonText = buttonText,
                onCategoryButtonClick = {
                    Log.d("IdeaGenerationScreen", "Opening location selection dialog")
                    showLocationDialog = true
                },
                onAddClick = {
                    Log.d("IdeaGenerationScreen", "Navigating to ForumCreationScreen with category=$selectedCategoryName, location=${selectedProvinceData ?: selectedNationData ?: selectedGlobeData}")
                    navController.navigate(
                        Screen.ForumCreation.createRoute(
                            category = selectedCategoryName,
                            location = selectedProvinceData ?: selectedNationData ?: selectedGlobeData ?: "Earth"
                        )
                    )
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedGlobe == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Please select a location to view forums",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = { showLocationDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Select Location")
                        }
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Categories for $buttonText",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        categories.forEach { (displayName, category) ->
                            Button(
                                onClick = {
                                    Log.d("IdeaGenerationScreen", "Selected category: $category")
                                    selectedCategory = category
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Text(displayName)
                            }
                        }
                    }
                }

                // Forum Display
                when {
                    isLoading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    errorMessage != null -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    selectedCategory == null -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Please select a category to view forums",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    forums.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No forums found for $buttonText in $selectedCategoryDisplayName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        items(forums) { forum ->
                            ForumCard(
                                forum = forum,
                                navController = navController,
                                category = selectedCategory!!,
                                forumId = forum.id
                            )
                        }
                    }
                }
            }
        }
    }

    // Location Selection Dialog
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = {
                Log.d("IdeaGenerationScreen", "Dismissing location selection dialog")
                showLocationDialog = false
            },
            title = { Text("Select Location") },
            text = {
                Column {
                    // Globe Selection
                    Text("Select Globe", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomDropdownMenuItem(
                        selectedItem = tempGlobe ?: "Select Globe",
                        items = globes,
                        onItemSelected = { globe ->
                            tempGlobe = globe
                            tempNation = null
                            tempProvince = null
                            Log.d("IdeaGenerationScreen", "Globe selected: $globe")
                        },
                        label = "Globe"
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Nation Selection (only if globe is Earth)
                    if (tempGlobe == "Earth") {
                        Text("Select Nation", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        CustomDropdownMenuItem(
                            selectedItem = tempNation ?: "Select Nation",
                            items = nations,
                            onItemSelected = { nation ->
                                tempNation = nation
                                tempProvince = null
                                Log.d("IdeaGenerationScreen", "Nation selected: $nation")
                            },
                            label = "Nation"
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Province/State Selection
                    if (tempNation != null) {
                        Text("Select Province/State", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        CustomDropdownMenuItem(
                            selectedItem = tempProvince ?: "Select Province/State",
                            items = provincesByNation[tempNation] ?: emptyList(),
                            onItemSelected = { province ->
                                tempProvince = province
                                Log.d("IdeaGenerationScreen", "Province selected: $province")
                            },
                            label = "Province/State"
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempGlobe != null && (tempGlobe != "Earth" || (tempNation != null && tempProvince != null))) {
                            selectedGlobe = tempGlobe
                            selectedNation = tempNation
                            selectedProvince = tempProvince
                            showLocationDialog = false
                            Log.d("IdeaGenerationScreen", "Location selected: globe=$selectedGlobe, nation=$selectedNation, province=$selectedProvince")
                        } else {
                            Toast.makeText(context, "Please complete location selection", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = tempGlobe != null && (tempGlobe != "Earth" || (tempNation != null && tempProvince != null))
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    Log.d("IdeaGenerationScreen", "Cancel button clicked")
                    showLocationDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CustomDropdownMenuItem(
    selectedItem: String,
    items: List<String>,
    onItemSelected: (String) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedItem,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = {
                    expanded = !expanded
                    Log.d("IdeaGenerationScreen", "Dropdown toggled: $label, expanded=$expanded")
                }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                Log.d("IdeaGenerationScreen", "Dropdown dismissed: $label")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}