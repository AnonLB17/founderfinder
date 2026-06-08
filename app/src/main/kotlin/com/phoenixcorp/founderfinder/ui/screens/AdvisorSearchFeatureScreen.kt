package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.AdvisorCard
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.ui.viewmodel.AdvisorSearchUiState
import com.phoenixcorp.founderfinder.ui.viewmodel.AdvisorSearchViewModel

@Composable
fun AdvisorSearchFeatureScreen(
    navController: NavHostController,
    viewModel: AdvisorSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Trigger search whenever the user types
    LaunchedEffect(searchQuery) {
        viewModel.searchAdvisors(searchQuery)
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Advisor Search") },
                navController = navController,
                showBackButton = true
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Advisors") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate(Screen.AdvisorSignUp.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Become an Advisor")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content
            when (uiState) {
                is AdvisorSearchUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                is AdvisorSearchUiState.Success -> {
                    val advisors = (uiState as AdvisorSearchUiState.Success).advisors

                    if (advisors.isEmpty()) {
                        Text(
                            text = "No advisors found yet.\nBe the first to join!",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        LazyColumn {
                            items(advisors) { advisor ->
                                AdvisorCard(
                                    profile = advisor,
                                    navController = navController
                                )
                            }
                        }
                    }
                }
                is AdvisorSearchUiState.Error -> {
                    Text(
                        text = (uiState as AdvisorSearchUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                else -> {
                    Text(
                        text = "Start searching for advisors...",
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}