package com.phoenixcorp.founderfinder.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestorInfoScreen(navController: NavHostController) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = Firebase.firestore
    val storage = Firebase.storage
    var investmentFirmName by remember { mutableStateOf("") }
    var professionalBackground by remember { mutableStateOf("") }
    var notableAchievements by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { ScreenBanner(title = "Investor Info", navController = navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Fill out all fields in all sections of the investor user sign-up process, choose an image that represents the investment firm or its logo.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            imageUri?.let {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(it)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .build(),
                        onError = { error -> println("Coil Error: ${error.result.throwable.message}") }
                    ),
                    contentDescription = "Selected Investment Firm Logo",
                    modifier = Modifier.size(120.dp)
                )
            } ?: Image(
                painter = painterResource(id = R.drawable.ic_profile_placeholder),
                contentDescription = "Investment Firm Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text("Pick Image")
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = investmentFirmName,
                onValueChange = { investmentFirmName = it },
                label = { Text("Investment Firm Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = investmentFirmName.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = professionalBackground,
                onValueChange = { professionalBackground = it },
                label = { Text("Professional Background and Expertise") },
                modifier = Modifier.fillMaxWidth(),
                isError = professionalBackground.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notableAchievements,
                onValueChange = { notableAchievements = it },
                label = { Text("Notable Past Achievements") },
                modifier = Modifier.fillMaxWidth(),
                isError = notableAchievements.isBlank() && errorMessage != null
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    // Validate inputs
                    if (investmentFirmName.isBlank() || professionalBackground.isBlank() || notableAchievements.isBlank() || imageUri == null) {
                        errorMessage = "All fields are required, including an image."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        coroutineScope.launch {
                            try {
                                // Upload image to Firebase Storage
                                val storageRef = storage.reference
                                    .child("firmLogos/${currentUser.uid}/logo.jpg")
                                storageRef.putFile(imageUri!!).await()
                                val downloadUrl = storageRef.downloadUrl.await().toString()

                                // Save data to Firestore
                                val investorData = mapOf(
                                    "investmentFirmName" to investmentFirmName,
                                    "professionalBackground" to professionalBackground,
                                    "notableAchievements" to notableAchievements,
                                    "firmLogo" to downloadUrl,
                                    "hasInvestorProfile" to true
                                )

                                // Save to users collection (main document)
                                firestore.collection("users")
                                    .document(currentUser.uid)
                                    .update(investorData)
                                    .await()

                                isLoading = false
                                navController.navigate(Screen.IndustryPreferences.route)
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = "Failed to save data: ${e.message}"
                            }
                        }
                    } else {
                        isLoading = false
                        errorMessage = "You must be logged in."
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

@Preview(showBackground = true)
@Composable
fun InvestorInfoScreenPreview() {
    InvestorInfoScreen(navController = rememberNavController())
}