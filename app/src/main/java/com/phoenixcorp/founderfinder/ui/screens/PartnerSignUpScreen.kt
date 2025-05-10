package com.phoenixcorp.founderfinder.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

@Composable
fun PartnerSignUpScreen(navController: NavHostController) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = Firebase.firestore
    val storage = Firebase.storage
    var firstName by remember { mutableStateOf(TextFieldValue("")) }
    var lastName by remember { mutableStateOf(TextFieldValue("")) }
    var expertise by remember { mutableStateOf(TextFieldValue("")) }
    var experienceYears by remember { mutableStateOf(TextFieldValue("")) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var profilePictureUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var existingProfile by remember { mutableStateOf<UserProfile?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Fetch existing profile data from users collection
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            try {
                Log.d("PartnerSignUp", "Fetching user profile from users/${currentUser.uid}")
                val userDoc = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                if (userDoc.exists()) {
                    existingProfile = userDoc.toObject(UserProfile::class.java)
                    firstName = TextFieldValue(existingProfile?.firstName ?: "")
                    lastName = TextFieldValue(existingProfile?.lastName ?: "")
                    expertise = TextFieldValue(existingProfile?.expertise ?: "")
                    profilePictureUrl = existingProfile?.profilePicture
                    Log.d("PartnerSignUp", "Fetched profile: ${existingProfile?.firstName} ${existingProfile?.lastName}")
                } else {
                    Log.w("PartnerSignUp", "No user profile found in users/${currentUser.uid}")
                }
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load profile: ${e.message}"
                isLoading = false
                Log.e("PartnerSignUp", "Error fetching profile: ${e.message}")
            }
        } else {
            errorMessage = "You must be logged in."
            isLoading = false
            Log.e("PartnerSignUp", "No user logged in")
        }
    }

    Scaffold(
        topBar = { ScreenBanner(title = "Partner Sign-Up", navController = navController) },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Upload Profile Picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") }
                )
            } else if (profilePictureUrl != null && profilePictureUrl!!.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(profilePictureUrl)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .build(),
                        onError = { error -> println("Coil Error: ${error.result.throwable.message}") }
                    ),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") }
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_profile_placeholder),
                    contentDescription = "Upload Profile Picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Fields
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = firstName.text.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = lastName.text.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = expertise,
                onValueChange = { expertise = it },
                label = { Text("Area of Expertise") },
                modifier = Modifier.fillMaxWidth(),
                isError = expertise.text.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = experienceYears,
                onValueChange = { experienceYears = it },
                label = { Text("Years of Experience") },
                modifier = Modifier.fillMaxWidth(),
                isError = experienceYears.text.isBlank() && errorMessage != null
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Sign-Up Button
            Button(
                onClick = {
                    if (firstName.text.isBlank() || lastName.text.isBlank() || expertise.text.isBlank() || experienceYears.text.isBlank()) {
                        errorMessage = "All fields are required."
                        Log.w("PartnerSignUp", "Validation failed: Required fields are blank")
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        coroutineScope.launch {
                            try {
                                // Upload new profile picture if selected, otherwise use existing
                                val downloadUrl = if (imageUri != null) {
                                    Log.d("PartnerSignUp", "Uploading new profile picture")
                                    val storageRef = storage.reference
                                        .child("profilePictures/${currentUser.uid}/profile.jpg")
                                    storageRef.putFile(imageUri!!).await()
                                    storageRef.downloadUrl.await().toString()
                                } else {
                                    profilePictureUrl ?: ""
                                }

                                // Create partner profile by merging existing user profile with partner-specific fields
                                val partnerData = existingProfile?.copy(
                                    firstName = firstName.text,
                                    lastName = lastName.text,
                                    expertise = expertise.text,
                                    experienceYears = experienceYears.text.toIntOrNull() ?: 0,
                                    profilePicture = downloadUrl,
                                    isPartner = true,
                                    userId = currentUser.uid
                                ) ?: UserProfile(
                                    firstName = firstName.text,
                                    lastName = lastName.text,
                                    expertise = expertise.text,
                                    experienceYears = experienceYears.text.toIntOrNull() ?: 0,
                                    profilePicture = downloadUrl,
                                    isPartner = true,
                                    userId = currentUser.uid
                                )

                                // Save to profiles collection
                                Log.d("PartnerSignUp", "Saving partner profile to profiles/${currentUser.uid}")
                                firestore.collection("profiles")
                                    .document(currentUser.uid)
                                    .set(partnerData)
                                    .await()

                                // Update users collection with partner-specific fields
                                Log.d("PartnerSignUp", "Updating users/${currentUser.uid} with partner fields")
                                firestore.collection("users")
                                    .document(currentUser.uid)
                                    .update(
                                        mapOf(
                                            "firstName" to firstName.text,
                                            "lastName" to lastName.text,
                                            "expertise" to expertise.text,
                                            "profilePicture" to downloadUrl,
                                            "isPartner" to true
                                        ) as Map<String, Any>
                                    )
                                    .await()

                                isLoading = false
                                Log.d("PartnerSignUp", "Partner sign-up successful, navigating to Profile")
                                navController.navigate(Screen.Profile.route) {
                                    popUpTo(Screen.PartnerSignUp.route) { inclusive = true }
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = "Failed to sign up: ${e.message}"
                                Log.e("PartnerSignUp", "Error during sign-up: ${e.message}")
                            }
                        }
                    } else {
                        isLoading = false
                        errorMessage = "You must be logged in."
                        Log.e("PartnerSignUp", "No user logged in during submit")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Sign Up")
                }
            }
        }
    }
}