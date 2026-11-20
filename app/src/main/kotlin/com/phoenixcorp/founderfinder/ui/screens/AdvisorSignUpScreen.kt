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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.storage
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.RoleProfile
import com.phoenixcorp.founderfinder.domain.model.UserProfile
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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    // Load existing data
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser ?: return@LaunchedEffect
        try {
            val profileDoc = firestore.collection("profiles").document(currentUser.uid).get().await()
            if (profileDoc.exists()) {
                val profile = profileDoc.toObject(UserProfile::class.java)
                profile?.let {
                    firstName = TextFieldValue(it.firstName ?: "")
                    lastName = TextFieldValue(it.lastName ?: "")
                    profilePictureUrl = it.profilePicture
                }
            }

            val advisorDoc = firestore.collection("profiles")
                .document(currentUser.uid)
                .collection("advisor")
                .document("data")
                .get()
                .await()

            if (advisorDoc.exists()) {
                val advisor = advisorDoc.toObject(RoleProfile::class.java)
                advisor?.let {
                    expertise = TextFieldValue(it.expertise ?: "")
                    experienceYears = TextFieldValue(it.experienceYears?.toString() ?: "")
                }
            }
        } catch (e: Exception) {
            Log.e("AdvisorSignUp", "Load error", e)
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
                val imageToShow = imageUri ?: profilePictureUrl
                if (imageToShow != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageToShow),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_profile_placeholder),
                        contentDescription = "Default",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AdvisorInputField("First Name", firstName) { firstName = it }
            AdvisorInputField("Last Name", lastName) { lastName = it }
            AdvisorInputField("Expertise (comma separated)", expertise) { expertise = it }
            AdvisorInputField("Years of Experience", experienceYears) { experienceYears = it }

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (firstName.text.isBlank() || lastName.text.isBlank()) {
                        errorMessage = "First and last name are required."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    val currentUser = auth.currentUser
                    if (currentUser == null) {
                        errorMessage = "Not logged in."
                        isLoading = false
                        return@Button
                    }

                    coroutineScope.launch {
                        try {
                            // Upload new image if selected
                            val downloadUrl = if (imageUri != null) {
                                val ref = storage.reference.child("profilePictures/${currentUser.uid}/profile.jpg")
                                ref.putFile(imageUri!!).await()
                                ref.downloadUrl.await().toString()
                            } else profilePictureUrl

                            // Update main profile - MERGE only changed fields
                            val updates = mapOf<String, Any?>(
                                "firstName" to firstName.text,
                                "lastName" to lastName.text,
                                "profilePicture" to downloadUrl
                            )

                            firestore.collection("profiles")
                                .document(currentUser.uid)
                                .set(updates, SetOptions.merge())
                                .await()

                            // Save Advisor data in subcollection
                            val advisorData = mapOf<String, Any>(
                                "expertise" to expertise.text,
                                "experienceYears" to (experienceYears.text.toIntOrNull() ?: 0)
                            )

                            firestore.collection("profiles")
                                .document(currentUser.uid)
                                .collection("advisor")
                                .document("data")
                                .set(advisorData, SetOptions.merge())
                                .await()

                            Toast.makeText(context, "Advisor profile saved successfully!", Toast.LENGTH_LONG).show()

                            navController.navigate(Screen.AdvisorSearchFeature.route) {
                                popUpTo(Screen.OnboardingGraph.route) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            errorMessage = "Save failed: ${e.message}"
                            Log.e("AdvisorSignUp", "Error", e)
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