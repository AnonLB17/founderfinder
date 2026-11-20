package com.phoenixcorp.founderfinder.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.ui.components.CreateInvitation
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner

@Composable
fun OrganizationManagementScreen(
    navController: NavHostController,
    orgId: String
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Manage Organization") },
                navController = navController,
                showBackButton = true
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Invite Users to Organization",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            CreateInvitation(
                orgId = orgId,
                onSuccess = { invitationId ->
                    Toast.makeText(context, "Invitation sent: $invitationId", Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}