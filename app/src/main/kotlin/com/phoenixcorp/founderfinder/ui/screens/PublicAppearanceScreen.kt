package com.phoenixcorp.founderfinder.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicAppearanceScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val firestore: FirebaseFirestore = Firebase.firestore
    val storage: FirebaseStorage = Firebase.storage

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userData by remember { mutableStateOf<UserProfile?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val currentUser = authViewModel.getCurrentUser()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        profileImageUri = uri
    }

    // Fetch user data
    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            errorMessage = "You must be logged in to view your profile."
            isLoading = false
            return@LaunchedEffect
        }

        coroutineScope.launch {
            try {
                val document = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                userData = if (document.exists()) {
                    document.toObject(UserProfile::class.java)?.copy(userId = document.id)
                } else {
                    UserProfile(userId = currentUser.uid)
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load profile: ${e.message}"
                Log.e("PublicAppearanceScreen", "Error fetching profile", e)
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Public Appearance") },
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
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                userData?.let { user ->
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            // Profile Picture
                            Text("Choose your profile picture", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier.size(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (profileImageUri != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(profileImageUri),
                                        contentDescription = "Selected Profile Picture",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (!user.profilePicture.isNullOrEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = ImageRequest.Builder(context)
                                                .data(user.profilePicture)
                                                .crossfade(true)
                                                .placeholder(R.drawable.ic_profile_placeholder)
                                                .error(R.drawable.ic_profile_placeholder)
                                                .build()
                                        ),
                                        contentDescription = "Saved Profile Picture",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_profile_placeholder),
                                        contentDescription = "Default Profile Picture",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Pick Image")
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Profile Summary
                            Text("Profile Summary", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Name: ${user.firstName ?: "Not provided"} ${user.lastName ?: "Not provided"}")

                            // You can expand this summary with more fields as needed

                            Spacer(modifier = Modifier.height(24.dp))

                            // Submit Button
                            Button(
                                onClick = {
                                    if (currentUser == null) {
                                        errorMessage = "You must be logged in."
                                        return@Button
                                    }

                                    coroutineScope.launch {
                                        isLoading = true
                                        errorMessage = null

                                        try {
                                            val userId = currentUser.uid
                                            val downloadUrl = if (profileImageUri != null) {
                                                val storageRef = storage.reference
                                                    .child("profilePictures/$userId/profile.jpg")
                                                storageRef.putFile(profileImageUri!!).await()
                                                storageRef.downloadUrl.await().toString()
                                            } else {
                                                user.profilePicture ?: ""
                                            }

                                            val profileData = mapOf(
                                                "userId" to userId,
                                                "profilePicture" to downloadUrl,
                                                "firstName" to (user.firstName ?: ""),
                                                "lastName" to (user.lastName ?: ""),
                                                "ambitionStatement" to (user.ambitionStatement ?: ""),
                                                // Add other fields from UserProfile as needed
                                            )

                                            firestore.collection("profiles")
                                                .document(userId)
                                                .set(profileData)
                                                .await()

                                            navController.navigate(Screen.UserProfile.createRoute(userId)) {
                                                popUpTo(Screen.PublicAppearance.route) { inclusive = true }
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Failed to submit profile: ${e.message}"
                                            Log.e("PublicAppearanceScreen", "Submit error", e)
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Submit Profile")
                                }
                            }
                        }
                    }
                } ?: Text("No profile data available.")
            }
        }
    }
}