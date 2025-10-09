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
    var existingPartnerProfile by remember { mutableStateOf<RoleProfile?>(null) }

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
                Log.d("PartnerSignUp", "Fetching user profile from profiles/${currentUser.uid}")
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
                        Log.d("PartnerSignUp", "Fetched user profile: firstName=${existingProfile?.firstName}, lastName=${existingProfile?.lastName}, profilePicture=${existingProfile?.profilePicture}")
                    } else {
                        Log.w("PartnerSignUp", "Failed to deserialize user profile from profiles/${currentUser.uid}")
                    }
                } else {
                    Log.w("PartnerSignUp", "No user profile found in profiles/${currentUser.uid}")
                }

                // Fetch partner-specific data
                Log.d("PartnerSignUp", "Fetching partner profile from profiles/${currentUser.uid}/partner/data")
                val partnerDoc = firestore.collection("profiles")
                    .document(currentUser.uid)
                    .collection("partner")
                    .document("data")
                    .get()
                    .await()
                if (partnerDoc.exists()) {
                    existingPartnerProfile = partnerDoc.toObject(RoleProfile::class.java)
                    if (existingPartnerProfile != null) {
                        expertise = TextFieldValue(existingPartnerProfile?.expertise ?: "")
                        experienceYears = TextFieldValue(existingPartnerProfile?.experienceYears?.toString() ?: "")
                        Log.d("PartnerSignUp", "Fetched partner profile: expertise=${existingPartnerProfile?.expertise}, experienceYears=${existingPartnerProfile?.experienceYears}")
                    } else {
                        Log.w("PartnerSignUp", "Failed to deserialize partner profile from profiles/${currentUser.uid}/partner/data")
                    }
                } else {
                    Log.w("PartnerSignUp", "No partner profile found in profiles/${currentUser.uid}/partner/data")
                }
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load profile: ${e.message}"
                isLoading = false
                Log.e("PartnerSignUp", "Error fetching profile: ${e.message}", e)
            }
        } else {
            errorMessage = "You must be logged in."
            isLoading = false
            Log.e("PartnerSignUp", "No user logged in")
        }
    }

    Scaffold(
        topBar = { ScreenBanner(title = { Text("Partner Sign-Up") }, navController = navController) },
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
                        onError = { error -> Log.e("PartnerSignUp", "Coil Error: ${error.result.throwable.message}") }
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
            PartnerInputField(
                label = "First Name",
                value = firstName,
                errorMessage = errorMessage,
                onValueChange = { firstName = it }
            )
            PartnerInputField(
                label = "Last Name",
                value = lastName,
                errorMessage = errorMessage,
                onValueChange = { lastName = it }
            )
            PartnerInputField(
                label = "Expertise",
                value = expertise,
                errorMessage = errorMessage,
                onValueChange = { expertise = it }
            )
            PartnerInputField(
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
                                    Log.d("PartnerSignUp", "Uploading new profile picture for user ${currentUser.uid}")
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

                                // Create partner profile
                                val partnerProfile = existingPartnerProfile?.copy(
                                    expertise = expertise.text,
                                    experienceYears = experienceYears.text.toIntOrNull() ?: 0
                                ) ?: RoleProfile(
                                    expertise = expertise.text,
                                    experienceYears = experienceYears.text.toIntOrNull() ?: 0
                                )

                                // Log the data being saved
                                Log.d("PartnerSignUp", "Saving user profile to profiles/${currentUser.uid}: firstName=${userProfile.firstName}")
                                Log.d("PartnerSignUp", "Saving partner profile to profiles/${currentUser.uid}/partner/data: expertise=${partnerProfile.expertise}")

                                // Save to profiles collection
                                firestore.collection("profiles")
                                    .document(currentUser.uid)
                                    .set(userProfile)
                                    .await()

                                // Save to partner sub-collection
                                firestore.collection("profiles")
                                    .document(currentUser.uid)
                                    .collection("partner")
                                    .document("data")
                                    .set(partnerProfile)
                                    .await()

                                // Verify the saves
                                val savedProfileDoc = firestore.collection("profiles")
                                    .document(currentUser.uid)
                                    .get()
                                    .await()
                                if (savedProfileDoc.exists()) {
                                    val savedData = savedProfileDoc.data
                                    Log.d("PartnerSignUp", "User profile saved successfully: firstName=${savedData?.get("firstName")}")
                                } else {
                                    Log.e("PartnerSignUp", "User profile not found after save attempt for user ${currentUser.uid}")
                                }

                                val savedPartnerDoc = firestore.collection("profiles")
                                    .document(currentUser.uid)
                                    .collection("partner")
                                    .document("data")
                                    .get()
                                    .await()
                                if (savedPartnerDoc.exists()) {
                                    val savedData = savedPartnerDoc.data
                                    Log.d("PartnerSignUp", "Partner profile saved successfully: expertise=${savedData?.get("expertise")}")
                                } else {
                                    Log.e("PartnerSignUp", "Partner profile not found after save attempt for user ${currentUser.uid}")
                                }

                                // Update users collection
                                Log.d("PartnerSignUp", "Updating users/${currentUser.uid} with user fields")
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
                                errorMessage = "Partner profile created successfully!"
                                Log.d("PartnerSignUp", "Partner sign-up successful, navigating to PartnerSearchFeature")
                                navController.navigate(Screen.PartnerSearchFeature.route) {
                                    popUpTo(Screen.PartnerSignUp.route) { inclusive = true }
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = "Failed to sign up: ${e.message}"
                                Log.e("PartnerSignUp", "Error during sign-up: ${e.message}", e)
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

@Composable
fun PartnerInputField(
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