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
fun AdvisorSignUpScreen(navController: NavHostController) {
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
                Log.d("AdvisorSignUp", "Fetching user profile from users/${currentUser.uid}")
                val userDoc = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                if (userDoc.exists()) {
                    existingProfile = userDoc.toObject(UserProfile::class.java)
                    firstName = TextFieldValue(existingProfile?.firstName ?: "")
                    lastName = TextFieldValue(existingProfile?.lastName ?: "")
                    expertise = TextFieldValue(
                        existingProfile?.educationEntries?.firstOrNull() ?: ""
                    )
                    profilePictureUrl = existingProfile?.profilePicture
                    Log.d("AdvisorSignUp", "Fetched profile: ${existingProfile?.firstName} ${existingProfile?.lastName}, expertise: ${expertise.text}, educationEntries: ${existingProfile?.educationEntries}")
                } else {
                    Log.w("AdvisorSignUp", "No user profile found in users/${currentUser.uid}")
                }
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load profile: ${e.message}"
                isLoading = false
                Log.e("AdvisorSignUp", "Error fetching profile: ${e.message}", e)
            }
        } else {
            errorMessage = "You must be logged in."
            isLoading = false
            Log.e("AdvisorSignUp", "No user logged in")
        }
    }

    Scaffold(
        topBar = { ScreenBanner(title = "Advisor Sign-Up", navController = navController) },
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
            AdvisorInputField(label = "First Name", value = firstName) { firstName = it }
            AdvisorInputField(label = "Last Name", value = lastName) { lastName = it }
            AdvisorInputField(label = "Expertise (from Education)", value = expertise) { expertise = it }
            AdvisorInputField(label = "Years of Experience", value = experienceYears) { experienceYears = it }

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Submit Button
            Button(
                onClick = {
                    if (firstName.text.isBlank() || lastName.text.isBlank() || expertise.text.isBlank() || experienceYears.text.isBlank()) {
                        errorMessage = "All fields are required."
                        Log.w("AdvisorSignUp", "Validation failed: Required fields are blank")
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
                                    Log.d("AdvisorSignUp", "Uploading new profile picture for user ${currentUser.uid}")
                                    val storageRef = storage.reference
                                        .child("profilePictures/${currentUser.uid}/profile.jpg")
                                    storageRef.putFile(imageUri!!).await()
                                    storageRef.downloadUrl.await().toString()
                                } else {
                                    profilePictureUrl ?: ""
                                }

                                // Create advisor profile by merging existing user profile with advisor-specific fields
                                val advisorData = existingProfile?.copy(
                                    firstName = firstName.text,
                                    lastName = lastName.text,
                                    expertise = expertise.text,
                                    experienceYears = experienceYears.text.toIntOrNull() ?: 0,
                                    profilePicture = downloadUrl,
                                    isAdvisor = true, // Use isAdvisor for Kotlin property
                                    userId = currentUser.uid
                                ) ?: UserProfile(
                                    firstName = firstName.text,
                                    lastName = lastName.text,
                                    expertise = expertise.text,
                                    experienceYears = experienceYears.text.toIntOrNull() ?: 0,
                                    profilePicture = downloadUrl,
                                    isAdvisor = true, // Use isAdvisor for Kotlin property
                                    userId = currentUser.uid
                                )

                                // Log the data being saved
                                Log.d("AdvisorSignUp", "Saving advisor profile to profiles/${currentUser.uid}: isAdvisor=${advisorData.isAdvisor}, userId=${advisorData.userId}, firstName=${advisorData.firstName}")

                                // Save to profiles collection
                                firestore.collection("profiles")
                                    .document(currentUser.uid)
                                    .set(advisorData)
                                    .await()

                                // Verify the save
                                val savedDoc = firestore.collection("profiles")
                                    .document(currentUser.uid)
                                    .get()
                                    .await()
                                if (savedDoc.exists()) {
                                    val savedData = savedDoc.data
                                    Log.d("AdvisorSignUp", "Profile saved successfully: advisor=${savedData?.get("advisor")}, userId=${savedData?.get("userId")}, firstName=${savedData?.get("firstName")}")
                                } else {
                                    Log.e("AdvisorSignUp", "Profile not found after save attempt for user ${currentUser.uid}")
                                }

                                // Update users collection with advisor-specific fields
                                Log.d("AdvisorSignUp", "Updating users/${currentUser.uid} with advisor fields")
                                firestore.collection("users")
                                    .document(currentUser.uid)
                                    .update(
                                        mapOf(
                                            "firstName" to firstName.text,
                                            "lastName" to lastName.text,
                                            "expertise" to expertise.text,
                                            "profilePicture" to downloadUrl,
                                            "advisor" to true // Maps to advisor in Firestore
                                        ) as Map<String, Any>
                                    )
                                    .await()

                                isLoading = false
                                errorMessage = "Advisor profile created successfully!"
                                Log.d("AdvisorSignUp", "Advisor sign-up successful, navigating to AdvisorSearchFeature")
                                navController.navigate(Screen.AdvisorSearchFeature.route) {
                                    popUpTo(Screen.AdvisorSignUp.route) { inclusive = true }
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = "Failed to sign up: ${e.message}"
                                Log.e("AdvisorSignUp", "Error during sign-up: ${e.message}", e)
                            }
                        }
                    } else {
                        isLoading = false
                        errorMessage = "You must be logged in."
                        Log.e("AdvisorSignUp", "No user logged in during submit")
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
                    Text("Submit")
                }
            }
        }
    }
}

@Composable
fun AdvisorInputField(label: String, value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}