package com.phoenixcorp.founderfinder.ui.screens

import android.net.Uri
import android.util.Log
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
import com.phoenixcorp.founderfinder.data.RoleProfile
import com.phoenixcorp.founderfinder.data.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    var existingAdvisorProfile by remember { mutableStateOf<RoleProfile?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Fetch existing profile data from profiles collection
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            try {
                Log.d("AdvisorSignUp", "Fetching user profile from profiles/${currentUser.uid}")
                val profileDoc = firestore.collection("profiles")
                    .document(currentUser.uid)
                    .get()
                    .await()
                if (profileDoc.exists()) {
                    existingProfile = profileDoc.toObject(UserProfile::class.java)
                    if (existingProfile != null) {
                        firstName = TextFieldValue(existingProfile?.firstName ?: "")
                        lastName = TextFieldValue(existingProfile?.lastName ?: "")
                        profilePictureUrl = existingProfile?.profilePicture
                        Log.d("AdvisorSignUp", "Fetched user profile: firstName=${existingProfile?.firstName}, lastName=${existingProfile?.lastName}, profilePicture=${existingProfile?.profilePicture}")
                    } else {
                        Log.w("AdvisorSignUp", "Failed to deserialize user profile from profiles/${currentUser.uid}")
                    }
                } else {
                    Log.w("AdvisorSignUp", "No user profile found in profiles/${currentUser.uid}")
                }

                // Fetch advisor-specific data
                Log.d("AdvisorSignUp", "Fetching advisor profile from profiles/${currentUser.uid}/advisor/data")
                val advisorDoc = firestore.collection("profiles")
                    .document(currentUser.uid)
                    .collection("advisor")
                    .document("data")
                    .get()
                    .await()
                if (advisorDoc.exists()) {
                    existingAdvisorProfile = advisorDoc.toObject(RoleProfile::class.java)
                    if (existingAdvisorProfile != null) {
                        expertise = TextFieldValue(existingAdvisorProfile?.expertise ?: "")
                        experienceYears = TextFieldValue(existingAdvisorProfile?.experienceYears?.toString() ?: "")
                        Log.d("AdvisorSignUp", "Fetched advisor profile: expertise=${existingAdvisorProfile?.expertise}, experienceYears=${existingAdvisorProfile?.experienceYears}")
                    } else {
                        Log.w("AdvisorSignUp", "Failed to deserialize advisor profile from profiles/${currentUser.uid}/advisor/data")
                    }
                } else {
                    Log.w("AdvisorSignUp", "No advisor profile found in profiles/${currentUser.uid}/advisor/data")
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
                        onError = { error -> Log.e("AdvisorSignUp", "Coil Error: ${error.result.throwable.message}") }
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
            AdvisorInputField(
                label = "First Name",
                value = firstName,
                errorMessage = errorMessage,
                onValueChange = { firstName = it }
            )
            AdvisorInputField(
                label = "Last Name",
                value = lastName,
                errorMessage = errorMessage,
                onValueChange = { lastName = it }
            )
            AdvisorInputField(
                label = "Expertise",
                value = expertise,
                errorMessage = errorMessage,
                onValueChange = { expertise = it }
            )
            AdvisorInputField(
                label = "Years of Experience",
                value = experienceYears,
                errorMessage = errorMessage,
                onValueChange = { experienceYears = it }
            )

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

                                // Create user profile
                                val userProfile = existingProfile?.copy(
                                    firstName = firstName.text,
                                    lastName = lastName.text,
                                    profilePicture = downloadUrl,
                                    userId = currentUser.uid
                                ) ?: UserProfile(
                                    firstName = firstName.text,
                                    lastName = lastName.text,
                                    profilePicture = downloadUrl,
                                    userId = currentUser.uid
                                )

                                // Create advisor profile
                                val advisorProfile = existingAdvisorProfile?.copy(
                                    expertise = expertise.text,
                                    experienceYears = experienceYears.text.toIntOrNull() ?: 0
                                ) ?: RoleProfile(
                                    expertise = expertise.text,
                                    experienceYears = experienceYears.text.toIntOrNull() ?: 0
                                )

                                // Log the data being saved
                                Log.d("AdvisorSignUp", "Saving user profile to profiles/${currentUser.uid}: firstName=${userProfile.firstName}")
                                Log.d("AdvisorSignUp", "Saving advisor profile to profiles/${currentUser.uid}/advisor/data: expertise=${advisorProfile.expertise}")

                                // Save to profiles collection
                                firestore.collection("profiles")
                                    .document(currentUser.uid)
                                    .set(userProfile)
                                    .await()

                                // Save to advisor sub-collection
                                firestore.collection("profiles")
                                    .document(currentUser.uid)
                                    .collection("advisor")
                                    .document("data")
                                    .set(advisorProfile)
                                    .await()

                                // Verify the saves
                                val savedProfileDoc = firestore.collection("profiles")
                                    .document(currentUser.uid)
                                    .get()
                                    .await()
                                if (savedProfileDoc.exists()) {
                                    val savedData = savedProfileDoc.data
                                    Log.d("AdvisorSignUp", "User profile saved successfully: firstName=${savedData?.get("firstName")}")
                                } else {
                                    Log.e("AdvisorSignUp", "User profile not found after save attempt for user ${currentUser.uid}")
                                }

                                val savedAdvisorDoc = firestore.collection("profiles")
                                    .document(currentUser.uid)
                                    .collection("advisor")
                                    .document("data")
                                    .get()
                                    .await()
                                if (savedAdvisorDoc.exists()) {
                                    val savedData = savedAdvisorDoc.data
                                    Log.d("AdvisorSignUp", "Advisor profile saved successfully: expertise=${savedData?.get("expertise")}")
                                } else {
                                    Log.e("AdvisorSignUp", "Advisor profile not found after save attempt for user ${currentUser.uid}")
                                }

                                // Update users collection
                                Log.d("AdvisorSignUp", "Updating users/${currentUser.uid} with user fields")
                                firestore.collection("users")
                                    .document(currentUser.uid)
                                    .update(
                                        mapOf(
                                            "firstName" to firstName.text,
                                            "lastName" to lastName.text,
                                            "profilePicture" to downloadUrl
                                        )
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
fun AdvisorInputField(
    label: String,
    value: TextFieldValue,
    errorMessage: String?,
    onValueChange: (TextFieldValue) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        isError = value.text.isBlank() && errorMessage != null
    )
}