package com.phoenixcorp.founderfinder.ui.components

import android.util.Log
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
import com.phoenixcorp.founderfinder.domain.model.Advisor
import com.phoenixcorp.founderfinder.navigation.Screen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AdvisorCard(
    profile: Advisor,
    navController: NavHostController
) {
    val auth = Firebase.auth
    val firestore = Firebase.firestore
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val user = profile.user
    val userId = user.uid

    Log.d("AdvisorCard", "Rendering card for userId: $userId, name: ${user.name}")

    val expertiseText = when {
        profile.expertise.isNotEmpty() -> profile.expertise.joinToString(", ")
        else -> "No expertise listed"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                Log.d("AdvisorCard", "Card clicked - navigating to UserProfile for: $userId")
                if (userId.isNotBlank()) {
                    try {
                        navController.navigate(Screen.UserProfile.createRoute(userId))
                    } catch (e: Exception) {
                        Log.e("AdvisorCard", "Navigation failed", e)
                        Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "User ID is missing", Toast.LENGTH_SHORT).show()
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
                    text = user.name.ifBlank { "Advisor" },
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = expertiseText,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (profile.experienceYears > 0) {
                    Text(
                        text = "${profile.experienceYears} years experience",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Message Button
            IconButton(onClick = {
                Log.d("AdvisorCard", "Message button clicked for: $userId")
                val currentUser = auth.currentUser ?: run {
                    navController.navigate(Screen.SignIn.route)
                    return@IconButton
                }

                if (userId.isBlank()) {
                    Toast.makeText(context, "User ID missing", Toast.LENGTH_SHORT).show()
                    return@IconButton
                }

                coroutineScope.launch {
                    try {
                        val conversationId = if (currentUser.uid < userId) {
                            "${currentUser.uid}_$userId"
                        } else {
                            "${userId}_${currentUser.uid}"
                        }

                        firestore.collection("conversations")
                            .document(conversationId)
                            .set(
                                mapOf(
                                    "participantIds" to listOf(currentUser.uid, userId),
                                    "lastUpdated" to System.currentTimeMillis()
                                ),
                                com.google.firebase.firestore.SetOptions.merge()
                            )
                            .await()

                        navController.navigate(Screen.PrivateChat.createRoute(conversationId))
                    } catch (e: Exception) {
                        Log.e("AdvisorCard", "Chat creation failed", e)
                        Toast.makeText(context, "Failed to start chat", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mail),
                    contentDescription = "Message Advisor"
                )
            }
        }
    }
}