package com.phoenixcorp.founderfinder.ui.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.Partner
import com.phoenixcorp.founderfinder.navigation.Screen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun PartnerCard(
    profile: Partner,
    navController: NavHostController
) {
    val auth = Firebase.auth
    val firestore = Firebase.firestore
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val user = profile.user

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                user.uid.let { userId ->
                    if (userId.isNotBlank()) {
                        navController.navigate(Screen.UserProfile.createRoute(userId))
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(user.profileImageUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .build()
                ),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name.ifBlank { "Partner" },
                    style = MaterialTheme.typography.titleMedium
                )

                // Skills / Expertise
                if (profile.skills.isNotEmpty()) {
                    Text(
                        text = profile.skills.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "No skills listed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Years of Experience
                if (profile.experienceYears > 0) {
                    Text(
                        text = "${profile.experienceYears} years experience",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Looking For
                profile.lookingFor?.let {
                    Text(
                        text = "Looking for: $it",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Message Button
            IconButton(onClick = {
                val currentUser = auth.currentUser ?: run {
                    navController.navigate(Screen.SignIn.route)
                    return@IconButton
                }

                user.uid.let { recipientId ->
                    if (recipientId.isBlank()) return@IconButton

                    coroutineScope.launch {
                        try {
                            val conversationId = if (currentUser.uid < recipientId) {
                                "${currentUser.uid}_$recipientId"
                            } else {
                                "${recipientId}_${currentUser.uid}"
                            }

                            firestore.collection("conversations")
                                .document(conversationId)
                                .set(
                                    mapOf(
                                        "participantIds" to listOf(currentUser.uid, recipientId),
                                        "lastUpdated" to System.currentTimeMillis()
                                    ),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )
                                .await()

                            navController.navigate(Screen.PrivateChat.createRoute(conversationId))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to start conversation", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mail),
                    contentDescription = "Message Partner"
                )
            }
        }
    }
}