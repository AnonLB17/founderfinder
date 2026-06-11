package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.phoenixcorp.founderfinder.navigation.Screen
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectUserTypeScreen(navController: NavHostController) {
    val auth = Firebase.auth
    val firestore = Firebase.firestore

    var hasBasicProfile by remember { mutableStateOf<Boolean?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Check if user already has a basic profile
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            hasBasicProfile = false
            isLoading = false
            return@LaunchedEffect
        }

        try {
            val profileDoc = firestore.collection("profiles")
                .document(currentUser.uid)
                .get()
                .await()

            hasBasicProfile = profileDoc.exists() &&
                    !profileDoc.getString("firstName").isNullOrBlank()
            Log.d("SelectUserType", "Has basic profile: $hasBasicProfile")
        } catch (e: Exception) {
            Log.e("SelectUserType", "Error checking profile", e)
            hasBasicProfile = false
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Choose Your Path",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Welcome to FounderFinder",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Every great journey starts with the right foundation.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                // Regular User / Founder Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Regular User / Founder",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Start building your profile and concepts",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.navigate(Screen.UserInfo.route) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continue as Regular User")
                        }
                    }
                }

                // Investor Card - Gold Theme
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF8E1)  // Soft gold background
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFFD4AF37)  // Gold color
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Investor",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFB8860B)  // Dark gold
                        )
                        Text(
                            text = "Finance promising ideas and entrepreneurs",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF8B5A00)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (hasBasicProfile == true) {
                            Button(
                                onClick = { navController.navigate(Screen.InvestorInfo.route) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD4AF37),   // Gold button
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Become an Investor", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = "You must complete your basic profile first before becoming an Investor.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { navController.navigate(Screen.UserInfo.route) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Complete Basic Profile First")
                            }
                        }
                    }
                }
            }
        }
    }
}