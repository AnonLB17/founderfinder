package com.phoenixcorp.founderfinder.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.Organization
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Composable
fun IdeaCreationScreen(navController: NavHostController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var organizations by remember { mutableStateOf<List<Organization>>(emptyList()) }
    var selectedOrganization by remember { mutableStateOf<Organization?>(null) }
    var isCreatingOrganization by remember { mutableStateOf(false) }
    var businessName by remember { mutableStateOf("") }
    var ideaDescription by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedImageUri = it.toString() }
    }

    // Fetch organizations
    LaunchedEffect(Unit) {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                errorMessage = "Please sign in to access this feature."
                isLoading = false
                navController.navigate(Screen.SignIn.route) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
                return@LaunchedEffect
            }
            Log.d("IdeaCreationScreen", "Fetching organizations for user: ${currentUser.uid}")
            val snapshot = firestore.collection("organizations")
                .whereEqualTo("creatorId", currentUser.uid)
                .get()
                .await()
            organizations = snapshot.documents.mapNotNull { doc ->
                try {
                    Organization(
                        orgId = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        imageUri = doc.getString("imageUri"),
                        creatorId = doc.getString("creatorId") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e("IdeaCreationScreen", "Error parsing organization ${doc.id}: ${e.message}", e)
                    null
                }
            }
            isLoading = false
            Log.d("IdeaCreationScreen", "Fetched ${organizations.size} organizations")
        } catch (e: Exception) {
            Log.e("IdeaCreationScreen", "Error fetching organizations: ${e.message}", e)
            errorMessage = "Failed to load organizations: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Idea Creation") },
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
            if (isLoading) {
                CircularProgressIndicator()
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                // Organization list
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    organizations.forEach { org ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Image(
                                painter = org.imageUri?.let {
                                    rememberAsyncImagePainter(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(it)
                                            .crossfade(true)
                                            .placeholder(R.drawable.ic_profile_placeholder)
                                            .error(R.drawable.ic_profile_placeholder)
                                            .build(),
                                        onError = { error -> Log.e("IdeaCreationScreen", "Coil Error: ${error.result.throwable.message}") }
                                    )
                                } ?: painterResource(id = R.drawable.ic_profile_placeholder),
                                contentDescription = "Organization Image",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        selectedOrganization = org
                                        isCreatingOrganization = false
                                        businessName = org.name
                                        ideaDescription = org.description
                                        selectedImageUri = org.imageUri
                                    }
                            )
                            Text(org.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(50.dp)
                            .clickable {
                                isCreatingOrganization = true
                                selectedOrganization = null
                                businessName = ""
                                ideaDescription = ""
                                selectedImageUri = null
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "+",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isCreatingOrganization || selectedOrganization != null) {
                    Text(
                        text = if (isCreatingOrganization) "Create New Organization" else "Edit Organization",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Image picker
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = selectedImageUri?.let {
                                rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(it)
                                        .crossfade(true)
                                        .placeholder(R.drawable.ic_profile_placeholder)
                                        .error(R.drawable.ic_profile_placeholder)
                                        .build(),
                                    onError = { error -> Log.e("IdeaCreationScreen", "Coil Error: ${error.result.throwable.message}") }
                                )
                            } ?: painterResource(id = R.drawable.ic_profile_placeholder),
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text("Pick Image")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = ideaDescription,
                        onValueChange = { ideaDescription = it },
                        label = { Text("Idea Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        coroutineScope.launch {
                            if (businessName.isBlank() || ideaDescription.isBlank()) {
                                errorMessage = "Please fill all fields."
                                return@launch
                            }
                            try {
                                val currentUser = auth.currentUser ?: throw Exception("User not signed in")
                                var imageUri: String? = selectedOrganization?.imageUri
                                if (selectedImageUri != null && selectedImageUri != selectedOrganization?.imageUri) {
                                    val storageRef = storage.reference.child("organization_images/${UUID.randomUUID()}.jpg")
                                    storageRef.putFile(Uri.parse(selectedImageUri)).await()
                                    imageUri = storageRef.downloadUrl.await().toString()
                                }
                                val orgData = Organization(
                                    orgId = selectedOrganization?.orgId ?: UUID.randomUUID().toString(),
                                    name = businessName,
                                    description = ideaDescription,
                                    imageUri = imageUri,
                                    creatorId = currentUser.uid
                                )
                                firestore.collection("organizations")
                                    .document(orgData.orgId)
                                    .set(orgData)
                                    .await()
                                organizations = if (selectedOrganization == null) {
                                    organizations + orgData
                                } else {
                                    organizations.map { if (it.orgId == orgData.orgId) orgData else it }
                                }
                                selectedOrganization = orgData
                                // Save selected orgId to SharedPreferences
                                context.getSharedPreferences("founderfinder", Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("selectedOrgId", orgData.orgId)
                                    .apply()
                                isCreatingOrganization = false
                                errorMessage = null
                                Toast.makeText(context, "Organization saved successfully", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("IdeaCreationScreen", "Error saving organization: ${e.message}", e)
                                errorMessage = "Failed to save organization: ${e.message}"
                            }
                        }
                    }) {
                        Text("Submit")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    if (selectedOrganization != null) {
                        navController.navigate(Screen.OrganizationFiles.createRoute(selectedOrganization!!.orgId))
                    } else {
                        errorMessage = "Please select or create an organization first."
                    }
                }) {
                    Text("Next")
                }
            }
        }
    }
}