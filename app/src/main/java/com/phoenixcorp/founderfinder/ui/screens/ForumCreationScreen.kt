package com.phoenixcorp.founderfinder.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import coil.compose.rememberAsyncImagePainter

@Composable
fun ForumCreationScreen(navController: NavHostController) {
    Scaffold(
        topBar = { ScreenBanner(
            title = "Forum Creation",
            navController = navController,
            showBackButton = true,) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("To create a forum you must have an image that represents the topic of the forum, select the appropriate category, a topic header, and a brief description of the forum topic.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            var imageUri by remember { mutableStateOf<Uri?>(null) }
            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                imageUri = uri
            }

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = imageUri?.let { rememberAsyncImagePainter(it) } ?: painterResource(id = R.drawable.ic_placeholder),
                    contentDescription = "Forum Image"
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            var category by remember { mutableStateOf("Select Category") }
            DropdownMenuExample(category) { selectedCategory ->
                category = selectedCategory
            }
            Spacer(modifier = Modifier.height(16.dp))

            var topicHeader by remember { mutableStateOf("") }
            OutlinedTextField(
                value = topicHeader,
                onValueChange = { topicHeader = it },
                label = { Text("Topic Header") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            var topicDescription by remember { mutableStateOf("") }
            OutlinedTextField(
                value = topicDescription,
                onValueChange = { topicDescription = it },
                label = { Text("Topic Description") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* Save forum and navigate */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit")
            }
        }
    }
}

@Composable
fun DropdownMenuExample(selectedCategory: String, onCategorySelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Global Issue", "National Issue", "Local Issue", "Future", "Market Potential", "Requested Solutions")

    Box(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedCategory)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}
