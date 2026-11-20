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
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.PartnerCard
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.ui.viewmodel.PartnerSearchUiState
import com.phoenixcorp.founderfinder.ui.viewmodel.PartnerSearchViewModel

@Composable
fun PartnerSearchFeatureScreen(
    navController: NavHostController,
    viewModel: PartnerSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        viewModel.searchPartners(searchQuery)
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Partner Search") },
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Partners") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate(Screen.PartnerSignUp.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Become a Partner")
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState) {
                is PartnerSearchUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is PartnerSearchUiState.Success -> {
                    val partners = (uiState as PartnerSearchUiState.Success).partners
                    if (partners.isEmpty()) {
                        Text(
                            text = "No partners found yet.\nBe the first!",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        LazyColumn {
                            items(partners) { partner ->
                                PartnerCard(
                                    profile = partner,
                                    navController = navController
                                )
                            }
                        }
                    }
                }
                is PartnerSearchUiState.Error -> {
                    Text(
                        text = (uiState as PartnerSearchUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                else -> {}
            }
        }
    }
}