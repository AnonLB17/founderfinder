package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.navigation.Screen

@Composable
fun AddIncubatorScreen(navController: NavHostController) {
    var incubatorName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = { ScreenBanner(
            title = "Add Incubator",
            navController = navController,
            showBackButton = true
        ) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("To add an incubator, pick an image that represents the incubator, enter the name of the incubator, add the location, and provide the URL to the incubator's main website.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Image Picker (Placeholder)
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .padding(8.dp)
            ) {
                if (selectedImage == null) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_placeholder), // Placeholder image
                        contentDescription = "Pick Image"
                    )
                } else {
                    Image(
                        painter = painterResource(id = selectedImage!!),
                        contentDescription = "Selected Image"
                    )
                }
            }
            Button(onClick = { /* TODO: Implement image picker logic */ }) {
                Text("Pick Image")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Incubator Name Input
            OutlinedTextField(
                value = incubatorName,
                onValueChange = { incubatorName = it },
                label = { Text("Incubator Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Location Input
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Website URL Input
            OutlinedTextField(
                value = websiteUrl,
                onValueChange = { websiteUrl = it },
                label = { Text("Website URL") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Button(
                onClick = {
                    // TODO: Save incubator details & navigate back to IncubatorConnectScreen
                    navController.navigate(Screen.IncubatorConnection.route)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Submit")
            }
        }
    }
}
