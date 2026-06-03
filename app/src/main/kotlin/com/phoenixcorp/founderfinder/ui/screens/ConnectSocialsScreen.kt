package com.phoenixcorp.founderfinder.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.viewmodel.AuthViewModel
import com.phoenixcorp.founderfinder.ui.viewmodel.ConnectSocialsViewModel

@Composable
fun ConnectSocialsScreen(
    navController: NavHostController,
    socialsViewModel: ConnectSocialsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val linkedin by socialsViewModel.linkedin.collectAsState()
    val twitter by socialsViewModel.twitter.collectAsState()
    val facebook by socialsViewModel.facebook.collectAsState()
    val instagram by socialsViewModel.instagram.collectAsState()
    val website by socialsViewModel.website.collectAsState()
    val isLoading by socialsViewModel.isLoading.collectAsState()
    val errorMessage by socialsViewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    val currentUser = authViewModel.getCurrentUser()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Connect Socials",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = linkedin,
            onValueChange = { socialsViewModel.updateLinkedin(it) },
            label = { Text("LinkedIn Profile URL") },
            placeholder = { Text("https://linkedin.com/in/yourprofile") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = twitter,
            onValueChange = { socialsViewModel.updateTwitter(it) },
            label = { Text("Twitter / X Profile URL") },
            placeholder = { Text("https://twitter.com/yourprofile") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = facebook,
            onValueChange = { socialsViewModel.updateFacebook(it) },
            label = { Text("Facebook Profile URL") },
            placeholder = { Text("https://facebook.com/yourprofile") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = instagram,
            onValueChange = { socialsViewModel.updateInstagram(it) },
            label = { Text("Instagram Profile URL") },
            placeholder = { Text("https://instagram.com/yourprofile") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = website,
            onValueChange = { socialsViewModel.updateWebsite(it) },
            label = { Text("Personal Website or Portfolio") },
            placeholder = { Text("https://yourwebsite.com") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (currentUser == null) {
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                socialsViewModel.saveSocials(currentUser.uid) { success ->
                    if (success) {
                        navController.navigate(Screen.IndustriesOfInterest.route) {
                            popUpTo(Screen.ConnectSocials.route) { inclusive = true }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Next")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConnectSocialsScreenPreview() {
    ConnectSocialsScreen(navController = rememberNavController())
}