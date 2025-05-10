package com.phoenixcorp.founderfinder.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenBanner(
    title: String,
    navController: NavHostController? = null,
    showBackButton: Boolean = false,
    showAddButton: Boolean = false,
    showMailButton: Boolean = false
) {
    TopAppBar(
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(title)
            }
        },
        navigationIcon = {
            navController?.let {
                when {
                    showBackButton -> {
                        IconButton(onClick = { it.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                    showAddButton -> {
                        IconButton(onClick = {
                            println("🔍 Navigating to ForumCreationScreen")
                            it.navigate(Screen.ForumCreation.route)
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Forum")
                        }
                    }
                    showMailButton -> {
                        IconButton(onClick = {
                            println("📩 Navigating to PrivateMessagesScreen")
                            it.navigate(Screen.PrivateMessages.route)
                        }) {
                            Icon(Icons.Default.Mail, contentDescription = "Private Messages")
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun ScreenBannerPreview() {
    ScreenBanner(title = "Sample Screen")
}
