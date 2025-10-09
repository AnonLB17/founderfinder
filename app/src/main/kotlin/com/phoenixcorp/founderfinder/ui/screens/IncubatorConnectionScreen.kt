package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.data.Incubator
import com.phoenixcorp.founderfinder.ui.components.IncubatorCard
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun IncubatorConnectionScreen(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()
    var incubators by remember { mutableStateOf<List<Incubator>>(emptyList()) }
    var filteredIncubators by remember { mutableStateOf<List<Incubator>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch incubators
    LaunchedEffect(Unit) {
        try {
            Log.d("IncubatorConnectionScreen", "Fetching incubators")
            val snapshot = firestore.collection("incubators")
                .get()
                .await()
            incubators = snapshot.documents.mapNotNull { doc ->
                try {
                    Incubator(
                        incubatorId = doc.id,
                        name = doc.getString("name") ?: "",
                        websiteUrl = doc.getString("websiteUrl") ?: "",
                        location = doc.getString("location") ?: "",
                        imageUri = doc.getString("imageUri"),
                        creatorId = doc.getString("creatorId") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } catch (e: Exception) {
                    Log.e("IncubatorConnectionScreen", "Error parsing incubator ${doc.id}: ${e.message}", e)
                    null
                }
            }
            filteredIncubators = incubators
            isLoading = false
            Log.d("IncubatorConnectionScreen", "Fetched ${incubators.size} incubators")
        } catch (e: Exception) {
            Log.e("IncubatorConnectionScreen", "Error fetching incubators: ${e.message}", e)
            errorMessage = "Failed to load incubators: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Incubator Connection") },
                navController = navController,
                showBackButton = true,
                // Other parameters...
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Incubators in the Calgary Area",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar and Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by location") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        filteredIncubators = if (searchQuery.isBlank()) {
                            incubators
                        } else {
                            incubators.filter {
                                it.location.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    })
                )
                Button(
                    onClick = {
                        filteredIncubators = if (searchQuery.isBlank()) {
                            incubators
                        } else {
                            incubators.filter {
                                it.location.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }
                ) {
                    Text("Search")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (filteredIncubators.isEmpty()) {
                Text(
                    text = if (searchQuery.isBlank()) "No incubators found." else "No incubators found for \"$searchQuery\".",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredIncubators) { incubator ->
                        IncubatorCard(incubator)
                    }
                }
            }
        }
    }
}