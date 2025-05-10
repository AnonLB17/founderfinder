package com.phoenixcorp.founderfinder.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.ui.screens.UserProfile

@Composable
fun AdvisorCard(profile: UserProfile, onCardClick: () -> Unit, onMessageClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            val profilePicture = profile.profilePicture
            if (profilePicture != null && profilePicture.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(profilePicture)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .build(),
                        onError = { error -> println("Coil Error: ${error.result.throwable.message}") }
                    ),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 8.dp)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_profile_placeholder),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Name
                Text(
                    text = "${profile.firstName ?: "Unknown"} ${profile.lastName ?: "User"}",
                    style = MaterialTheme.typography.titleMedium
                )
                // Expertise (from educationEntries)
                Text(
                    text = profile.expertise ?: "Not specified",
                    color = Color.Gray
                )
            }

            // Message Icon Button
            IconButton(onClick = onMessageClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mail),
                    contentDescription = "Message Advisor"
                )
            }
        }
    }
}