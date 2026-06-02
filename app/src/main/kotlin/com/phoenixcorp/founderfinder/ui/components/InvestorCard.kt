package com.phoenixcorp.founderfinder.ui.components

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.Investor
import com.phoenixcorp.founderfinder.navigation.Screen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun InvestorCard(
    investor: Investor,
    navController: NavHostController,
    onSwipe: () -> Unit
) {
    val auth = Firebase.auth
    val firestore = Firebase.firestore
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.9f)
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 50 || dragAmount < -50) onSwipe()
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate(Screen.UserProfile.createRoute(investor.userId))
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Profile Picture
                    if (!investor.profilePicture.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(context)
                                    .data(investor.profilePicture)
                                    .crossfade(true)
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .error(R.drawable.ic_profile_placeholder)
                                    .build()
                            ),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_profile_placeholder),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = investor.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Add the rest of your items here (Industry, Philosophy, etc.)
            // Example:
            item {
                Text("Primary Industry", style = MaterialTheme.typography.titleMedium)
                Text(investor.industry.ifEmpty { "Not provided" }, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ... keep adding your other sections

            item {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    val currentUser = auth.currentUser ?: run {
                        navController.navigate(Screen.SignIn.route)
                        return@IconButton
                    }

                    coroutineScope.launch {
                        try {
                            val conversationId = if (currentUser.uid < investor.userId) {
                                "${currentUser.uid}_${investor.userId}"
                            } else {
                                "${investor.userId}_${currentUser.uid}"
                            }

                            val conversationData = hashMapOf(
                                "senderId" to currentUser.uid,
                                "recipientId" to investor.userId,
                                "participantIds" to listOf(currentUser.uid, investor.userId),
                                "lastUpdated" to System.currentTimeMillis()
                            )

                            firestore.collection("conversations")
                                .document(conversationId)
                                .set(conversationData, com.google.firebase.firestore.SetOptions.merge())
                                .await()

                            navController.navigate(Screen.PrivateChat.createRoute(conversationId))
                        } catch (e: Exception) {
                            Log.e("InvestorCard", "Error creating conversation", e)
                            Toast.makeText(context, "Failed to start chat", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Icon(Icons.Filled.Mail, contentDescription = "Message Investor")
                }
            }
        }
    }
}