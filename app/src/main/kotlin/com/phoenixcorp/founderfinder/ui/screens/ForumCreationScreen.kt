package com.phoenixcorp.founderfinder.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.utils.fetchCurrentUserRole
import com.phoenixcorp.founderfinder.ui.utils.permissionsFor
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Composable
fun ForumCreationScreen(
    navController: NavHostController,
    initialCategory: String? = null,
    initialLocation: String? = null
) {
    val context = LocalContext.current

    // Spectator permissions
    var role by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        role = fetchCurrentUserRole()
    }
    val perms = remember(role) { permissionsFor(role) }
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()

    // State for form fields
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var category by remember { mutableStateOf(initialCategory?.let { mapCategoryToDisplayName(it) } ?: "Select Category") }
    var locationType by remember { mutableStateOf(initialLocation?.let { inferLocationType(it) } ?: "Select Location Type") }
    var location by remember { mutableStateOf(initialLocation ?: "Select Location") }
    var forumName by remember { mutableStateOf("") }
    var forumDescription by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedLocationType by remember { mutableStateOf(false) }
    var expandedLocation by remember { mutableStateOf(false) }

    // Location options
    val locationTypes = listOf("Global", "National", "Local")
    val locationsByType = mapOf(
        "Global" to listOf("Earth", "Moon", "Mars"),
        "National" to listOf("Canada", "USA", "Mexico"),
        "Local" to listOf(
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
    )

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Forum Creation") },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create a new forum by selecting a category, location, uploading an image, and providing a name and description.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Image Upload
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clickable {
                        if (!perms.requireCreate(context, "upload an image")) return@clickable
                        imagePickerLauncher.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = imageUri?.let { rememberAsyncImagePainter(it) }
                        ?: painterResource(id = R.drawable.ic_placeholder),
                    contentDescription = "Forum Image",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Category Dropdown
            DropdownMenuItem(
                selectedItem = category,
                items = listOf(
                    "Global Issues", "National Issues", "Local Issues",
                    "Future", "Market Potential", "Requested Solutions"
                ),
                onItemSelected = { selectedCategory -> category = selectedCategory },
                label = "Select Category",
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = it }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Location Type Dropdown
            DropdownMenuItem(
                selectedItem = locationType,
                items = locationTypes,
                onItemSelected = {
                        selectedLocationType ->
                    locationType = selectedLocationType
                    location = "Select Location" // Reset location when type changes
                },
                label = "Select Location Type",
                expanded = expandedLocationType,
                onExpandedChange = { expandedLocationType = it }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Location Dropdown
            if (locationType != "Select Location Type") {
                DropdownMenuItem(
                    selectedItem = location,
                    items = locationsByType[locationType] ?: emptyList(),
                    onItemSelected = { selectedLocation -> location = selectedLocation },
                    label = "Select Location",
                    expanded = expandedLocation,
                    onExpandedChange = { expandedLocation = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Forum Name Input
            OutlinedTextField(
                value = forumName,
                onValueChange = { forumName = it },
                label = { Text("Forum Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Forum Description Input
            OutlinedTextField(
                value = forumDescription,
                onValueChange = { forumDescription = it },
                label = { Text("Forum Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button — blocked for spectators
            Button(
                onClick = {
                    if (!perms.requireCreate(context, "create a forum")) return@Button
                    coroutineScope.launch {
                        val currentUser = auth.currentUser
                        if (currentUser == null) {
                            Toast.makeText(context, "Please sign in to create a forum", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.SignIn.route)
                            return@launch
                        }

                        if (imageUri == null) {
                            Toast.makeText(context, "Please select an image", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (category == "Select Category") {
                            Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (locationType == "Select Location Type") {
                            Toast.makeText(context, "Please select a location type", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (location == "Select Location") {
                            Toast.makeText(context, "Please select a location", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (forumName.isBlank()) {
                            Toast.makeText(context, "Forum name cannot be empty", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (forumDescription.isBlank()) {
                            Toast.makeText(context, "Forum description cannot be empty", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        try {
                            // Fetch UserProfile
                            val profileDoc = firestore.collection("profiles").document(currentUser.uid).get().await()
                            val profile = profileDoc.toObject(UserProfile::class.java)
                            val creatorName = when {
                                profile?.firstName.isNullOrBlank() && profile?.lastName.isNullOrBlank() -> "Anonymous"
                                profile?.firstName.isNullOrBlank() -> profile?.lastName ?: "Anonymous"
                                profile?.lastName.isNullOrBlank() -> profile?.firstName ?: "Anonymous"
                                else -> "${profile?.firstName} ${profile?.lastName}"
                            }

                            // Upload Image
                            val imageRef = storage.reference.child("forum_images/${UUID.randomUUID()}.jpg")
                            Log.d("ForumCreationScreen", "Uploading image to: ${imageRef.path}")
                            imageRef.putFile(imageUri!!).await()
                            val imageUrl = imageRef.downloadUrl.await().toString()
                            Log.d("ForumCreationScreen", "Image uploaded: $imageUrl")

                            // Create Forum
                            val forumId = UUID.randomUUID().toString()
                            val forumData = mapOf(
                                "name" to forumName,
                                "description" to forumDescription,
                                "imageUrl" to imageUrl,
                                "creatorId" to currentUser.uid,
                                "creatorName" to creatorName,
                                "timestamp" to System.currentTimeMillis(),
                                "likes" to 0,
                                "isFavorited" to false,
                                "category" to category.lowercase().replace(" ", ""),
                                "location" to location
                            )
                            Log.d("ForumCreationScreen", "Saving forum to: /category/${category.lowercase().replace(" ", "")}/forum/$forumId with location: $location")
                            firestore.collection("category")
                                .document(category.lowercase().replace(" ", ""))
                                .collection("forum")
                                .document(forumId)
                                .set(forumData)
                                .await()

                            Toast.makeText(context, "Forum created successfully", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } catch (e: Exception) {
                            Log.e("ForumCreationScreen", "Error creating forum: ${e.message}", e)
                            Toast.makeText(context, "Error creating forum: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = perms.canCreate
            ) {
                Text(if (perms.canCreate) "Submit" else "View only — cannot create")
            }
        }
    }
}

@Composable
fun DropdownMenuItem(
    selectedItem: String,
    items: List<String>,
    onItemSelected: (String) -> Unit,
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedItem,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { onExpandedChange(!expanded) }) {
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
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onItemSelected(item)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

// Helper function to map Firestore category to display name
private fun mapCategoryToDisplayName(category: String): String {
    return when (category.lowercase()) {
        "globalissues" -> "Global Issues"
        "nationalissues" -> "National Issues"
        "localissues" -> "Local Issues"
        "future" -> "Future"
        "marketpotential" -> "Market Potential"
        "requestedsolutions" -> "Requested Solutions"
        else -> "Select Category"
    }
}

// Helper function to infer location type from location
private fun inferLocationType(location: String): String {
    return when (location) {
        in listOf("Earth", "Moon", "Mars") -> "Global"
        in listOf("Canada", "USA", "Mexico") -> "National"
        else -> "Local"
    }
}