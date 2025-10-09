package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.data.RoleProfile
import com.phoenixcorp.founderfinder.data.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AdvisorCard(profile: UserProfile, navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = Firebase.firestore
    val coroutineScope = rememberCoroutineScope()
    var advisorProfile by remember { mutableStateOf<RoleProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Fetch advisor-specific data
    LaunchedEffect(profile.userId) {
        if (profile.userId != null) {
            try {
                Log.d("AdvisorCard", "Fetching advisor profile from profiles/${profile.userId}/advisor/data")
                val advisorDoc = firestore.collection("profiles")
                    .document(profile.userId!!)
                    .collection("advisor")
                    .document("data")
                    .get()
                    .await()
                if (advisorDoc.exists()) {
                    advisorProfile = advisorDoc.toObject(RoleProfile::class.java)
                    Log.d("AdvisorCard", "Fetched advisor profile: expertise=${advisorProfile?.expertise}, experienceYears=${advisorProfile?.experienceYears}")
                } else {
                    Log.w("AdvisorCard", "No advisor profile found for user ${profile.userId}")
                }
                isLoading = false
            } catch (e: Exception) {
                Log.e("AdvisorCard", "Error fetching advisor profile: ${e.message}", e)
                isLoading = false
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                profile.userId?.let { userId ->
                    coroutineScope.launch {
                        try {
                            val profileDoc = firestore.collection("profiles")
                                .document(userId)
                                .get()
                                .await()
                            if (profileDoc.exists()) {
                                navController.navigate(Screen.UserProfile.createRoute(userId))
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "User profile not found",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                Log.e("AdvisorCard", "Profile not found for userId: $userId")
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                "Error checking profile: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            Log.e("AdvisorCard", "Error checking profile: ${e.message}", e)
                        }
                    }
                } ?: Log.e("AdvisorCard", "UserId is null")
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            if (profile.profilePicture != null && profile.profilePicture.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(profile.profilePicture)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .build()
                    ),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_profile_placeholder),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Profile Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${profile.firstName ?: "Unknown"} ${profile.lastName ?: "User"}",
                    style = MaterialTheme.typography.titleMedium
                )
                if (isLoading) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    advisorProfile?.expertise?.let { expertise ->
                        Text(
                            text = "Expertise: $expertise",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    advisorProfile?.experienceYears?.let { years ->
                        Text(
                            text = "Experience: $years years",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Mail Icon
            IconButton(onClick = {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Log.e("AdvisorCard", "No authenticated user found, navigating to SignIn")
                    navController.navigate(Screen.SignIn.route)
                } else {
                    profile.userId?.let { recipientId ->
                        coroutineScope.launch {
                            try {
                                Log.d("AdvisorCard", "Creating conversation for user ${currentUser.uid} with recipient $recipientId")
                                // Generate sorted conversation ID
                                val conversationId = if (currentUser.uid < recipientId) {
                                    "${currentUser.uid}_$recipientId"
                                } else {
                                    "${recipientId}_${currentUser.uid}"
                                }
                                // Create conversation document
                                val conversationData = hashMapOf(
                                    "senderId" to currentUser.uid,
                                    "recipientId" to recipientId,
                                    "participantIds" to listOf(currentUser.uid, recipientId),
                                    "lastUpdated" to System.currentTimeMillis()
                                )
                                firestore.collection("conversations")
                                    .document(conversationId)
                                    .set(conversationData, com.google.firebase.firestore.SetOptions.merge())
                                    .await()
                                Log.d("AdvisorCard", "Conversation created: $conversationId")
                                // Navigate to PrivateChatScreen
                                navController.navigate(Screen.PrivateChat.createRoute(conversationId))
                            } catch (e: Exception) {
                                Log.e("AdvisorCard", "Error creating conversation: ${e.message}", e)
                                android.widget.Toast.makeText(
                                    context,
                                    "Failed to start conversation: ${e.message}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } ?: Log.e("AdvisorCard", "Recipient userId is null")
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