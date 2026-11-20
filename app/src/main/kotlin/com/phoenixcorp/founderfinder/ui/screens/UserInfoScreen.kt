package com.phoenixcorp.founderfinder.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.navigation.OnboardingSteps
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.OnboardingScaffold
import com.phoenixcorp.founderfinder.ui.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Composable
fun UserInfoScreen(
    navController: NavHostController,
    onboardingViewModel: OnboardingViewModel
) {
    val profile by onboardingViewModel.profile.collectAsState()
    val isLoading by onboardingViewModel.isLoading.collectAsState()
    val errorMessage by onboardingViewModel.errorMessage.collectAsState()
    val isInitialized by onboardingViewModel.isInitialized.collectAsState()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()

    // Re-seed when shared profile loads or user navigates back
    LaunchedEffect(profile, isInitialized) {
        if (isInitialized) {
            firstName = profile.firstName.orEmpty()
            lastName = profile.lastName.orEmpty()
            birthDate = profile.birthDate.orEmpty()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    OnboardingScaffold(
        navController = navController,
        title = "About You",
        showBack = true,
        currentStep = 2,
        totalSteps = OnboardingSteps.TOTAL_FOUNDER,
        isLoading = isLoading && !isInitialized
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Tell us about yourself", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            // Profile photo picker
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isUploadingPhoto) {
                        imagePickerLauncher.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = imageUri?.let { rememberAsyncImagePainter(it) }
                        ?: profile.profilePicture?.let { rememberAsyncImagePainter(it) }
                        ?: painterResource(id = R.drawable.ic_placeholder),
                    contentDescription = "Profile photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (isUploadingPhoto) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
            Text(
                text = "Tap to add a profile photo (required)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )
            OutlinedTextField(
                value = birthDate,
                onValueChange = { birthDate = it },
                label = { Text("Birth Date (MM/DD/YYYY)") },
                placeholder = { Text("05/15/1995") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (firstName.isBlank() || lastName.isBlank() || birthDate.isBlank()) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Require a photo: either a newly selected one or an already-saved URL
                    if (imageUri == null && profile.profilePicture.isNullOrBlank()) {
                        Toast.makeText(context, "Please add a profile photo to continue", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        var photoUrl: String? = profile.profilePicture

                        // Upload photo if a new one was selected
                        if (imageUri != null) {
                            isUploadingPhoto = true
                            try {
                                val uid = auth.currentUser?.uid
                                if (uid.isNullOrBlank()) {
                                    Toast.makeText(context, "Please sign in again to upload a photo", Toast.LENGTH_LONG).show()
                                    isUploadingPhoto = false
                                    return@launch
                                }
                                val imageRef = storage.reference.child("profilePictures/$uid/${UUID.randomUUID()}.jpg")
                                Log.d("UserInfoScreen", "Uploading profile photo to ${imageRef.path}")
                                imageRef.putFile(imageUri!!).await()
                                photoUrl = imageRef.downloadUrl.await().toString()
                                onboardingViewModel.updateProfilePicture(photoUrl)
                                Log.d("UserInfoScreen", "Profile picture uploaded: $photoUrl")
                            } catch (e: Exception) {
                                Log.e("UserInfoScreen", "Photo upload failed", e)
                                Toast.makeText(
                                    context,
                                    "Failed to upload profile photo. Please try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                                isUploadingPhoto = false
                                return@launch   // Stop — photo is required
                            } finally {
                                isUploadingPhoto = false
                            }
                        }

                        // Must have a photo URL at this point
                        if (photoUrl.isNullOrBlank()) {
                            Toast.makeText(context, "A profile photo is required", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        onboardingViewModel.updateBasicInfo(firstName, lastName, birthDate)
                        onboardingViewModel.saveDraft { success ->
                            if (success) {
                                navController.navigate(Screen.Education.route)
                            } else {
                                Toast.makeText(context, "Failed to save profile", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && !isUploadingPhoto && (imageUri != null || !profile.profilePicture.isNullOrBlank())
            ) {
                if (isLoading || isUploadingPhoto) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Text("Next")
            }
        }
    }
}
