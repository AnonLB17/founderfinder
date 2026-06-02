package com.phoenixcorp.founderfinder.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.data.RoleProfile
import com.phoenixcorp.founderfinder.data.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AdvisorSignUpScreen(navController: NavHostController) {
    val auth = Firebase.auth
    val firestore = Firebase.firestore
    val storage = Firebase.storage

    var firstName by remember { mutableStateOf(TextFieldValue("")) }
    var lastName by remember { mutableStateOf(TextFieldValue("")) }
    var expertise by remember { mutableStateOf(TextFieldValue("")) }
    var experienceYears by remember { mutableStateOf(TextFieldValue("")) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var profilePictureUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var existingProfile by remember { mutableStateOf<UserProfile?>(null) }
    var existingAdvisorProfile by remember { mutableStateOf<RoleProfile?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    // Fetch existing profile data
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            try {
                // Fetch main user profile
                val profileDoc = firestore.collection("profiles")
                    .document(currentUser.uid)
                    .get()
                    .await()

                if (profileDoc.exists()) {
                    existingProfile = profileDoc.toObject(UserProfile::class.java)
                    existingProfile?.let {
                        firstName = TextFieldValue(it.firstName ?: "")
                        lastName = TextFieldValue(it.lastName ?: "")
                        profilePictureUrl = it.profilePicture
                    }
                }

                // Fetch advisor sub-profile
                val advisorDoc = firestore.collection("profiles")
                    .document(currentUser.uid)
                    .collection("advisor")
                    .document("data")
                    .get()
                    .await()

                if (advisorDoc.exists()) {
                    existingAdvisorProfile = advisorDoc.toObject(RoleProfile::class.java)
                    existingAdvisorProfile?.let {
                        expertise = TextFieldValue(it.expertise ?: "")
                        experienceYears = TextFieldValue(it.experienceYears?.toString() ?: "")
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load profile: ${e.message}"
                Log.e("AdvisorSignUp", "Error loading profile", e)
            }
        } else {
            errorMessage = "You must be logged in to continue."
        }
    }

    Scaffold(
        topBar = { ScreenBanner(title = { Text("Advisor Sign-Up") }, navController = navController) },
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
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .clickable { imagePickerLauncher.launch("image/*") }
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (!profilePictureUrl.isNullOrEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(profilePictureUrl),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_profile_placeholder),
                        contentDescription = "Default Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Fields
            AdvisorInputField("First Name", firstName) { firstName = it }
            AdvisorInputField("Last Name", lastName) { lastName = it }
            AdvisorInputField("Expertise (e.g., Tech, Finance)", expertise) { expertise = it }
            AdvisorInputField("Years of Experience", experienceYears) { experienceYears = it }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    if (firstName.text.isBlank() || lastName.text.isBlank() || expertise.text.isBlank()) {
                        errorMessage = "First name, last name and expertise are required."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    val currentUser = auth.currentUser
                    if (currentUser == null) {
                        errorMessage = "You must be logged in."
                        isLoading = false
                        return@Button
                    }

                    coroutineScope.launch {
                        try {
                            // Upload image if selected
                            val downloadUrl = if (imageUri != null) {
                                val storageRef = storage.reference.child("profilePictures/${currentUser.uid}/profile.jpg")
                                storageRef.putFile(imageUri!!).await()
                                storageRef.downloadUrl.await().toString()
                            } else {
                                profilePictureUrl ?: ""
                            }

                            // Save User Profile
                            val userProfile = UserProfile(
                                userId = currentUser.uid,
                                firstName = firstName.text,
                                lastName = lastName.text,
                                profilePicture = downloadUrl
                            )

                            firestore.collection("profiles")
                                .document(currentUser.uid)
                                .set(userProfile)
                                .await()

                            // Save Advisor Profile
                            val advisorProfile = RoleProfile(
                                expertise = expertise.text,
                                experienceYears = experienceYears.text.toIntOrNull() ?: 0
                            )

                            firestore.collection("profiles")
                                .document(currentUser.uid)
                                .collection("advisor")
                                .document("data")
                                .set(advisorProfile)
                                .await()

                            Toast.makeText(context, "Advisor profile created successfully!", Toast.LENGTH_LONG).show()
                            navController.navigate(Screen.AdvisorSearchFeature.route) {
                                popUpTo(Screen.AdvisorSignUp.route) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            errorMessage = "Failed to save profile: ${e.message}"
                            Log.e("AdvisorSignUp", "Save error", e)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Submit Advisor Profile")
                }
            }
        }
    }
}

@Composable
fun AdvisorInputField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
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