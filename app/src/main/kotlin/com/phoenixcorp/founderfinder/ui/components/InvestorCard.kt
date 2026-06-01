package com.phoenixcorp.founderfinder.ui.components

import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
    val auth = FirebaseAuth.getInstance()
    val firestore = Firebase.firestore
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Log.d("InvestorCard", "Rendering investor: ${investor.name}, Industries: ${investor.preferredIndustries}, Stage: ${investor.investmentStage}")
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.9f)
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 50 || dragAmount < -50) {
                        Log.d("InvestorCard", "Swiped investor: ${investor.name}")
                        onSwipe()
                    }
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
                            val route = Screen.UserProfile.createRoute(investor.userId)
                            Log.d("InvestorCard", "Navigating to profile: $route")
                            navController.navigate(route)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    investor.profilePicture?.takeIf { it.isNotEmpty() }?.let { picture ->
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(picture)
                                    .crossfade(true)
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .error(R.drawable.ic_profile_placeholder)
                                    .build(),
                                onError = { error -> Log.e("InvestorCard", "Coil Error: ${error.result.throwable.message}") }
                            ),
                            contentDescription = "Investor Profile Picture",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } ?: Image(
                        painter = painterResource(id = R.drawable.ic_profile_placeholder),
                        contentDescription = "Investor Profile Picture",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
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
            item {
                Text(
                    text = "Primary Industry",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = investor.industry.ifEmpty { "Not provided" },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text(
                    text = "Philosophy",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = investor.philosophy.ifEmpty { "Not provided" },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                if (investor.preferredIndustries.isNotEmpty()) {
                    Text(
                        text = "Preferred Industries",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = investor.preferredIndustries.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            item {
                Text(
                    text = "Investment Stage",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = investor.investmentStage.ifEmpty { "Not provided" },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text(
                    text = "Investment Range",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\$${investor.investmentRangeMin.ifEmpty { "N/A" }} - \$${investor.investmentRangeMax.ifEmpty { "N/A" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text(
                    text = "Approach",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = investor.approachAndInvolvement.ifEmpty { "Not provided" },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text(
                    text = "ROI Expectations",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = investor.roiExpectations.ifEmpty { "Not provided" },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                if (investor.portfolioCompanies.isNotEmpty()) {
                    Text(
                        text = "Portfolio Companies",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = investor.portfolioCompanies.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            item {
                if (investor.testimonials.isNotEmpty()) {
                    Text(
                        text = "Testimonials",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = investor.testimonials.joinToString("; "),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            item {
                Text(
                    text = "Equity Terms",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = investor.equityTerms.ifEmpty { "Not provided" },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text(
                    text = "Board Role",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = investor.boardRole.ifEmpty { "Not provided" },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text(
                    text = "Return Timeline",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = investor.returnTimeline.ifEmpty { "Not provided" },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Spacer(modifier = Modifier.weight(1f)) // Push mail icon to bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = {
                        val currentUser = auth.currentUser
                        if (currentUser == null) {
                            Log.e("InvestorCard", "No authenticated user found, navigating to SignIn")
                            navController.navigate(Screen.SignIn.route)
                        } else {
                            coroutineScope.launch {
                                try {
                                    Log.d("InvestorCard", "Creating conversation for user ${currentUser.uid} with recipient ${investor.userId}")
                                    // Generate sorted conversation ID
                                    val conversationId = if (currentUser.uid < investor.userId) {
                                        "${currentUser.uid}_${investor.userId}"
                                    } else {
                                        "${investor.userId}_${currentUser.uid}"
                                    }
                                    // Create conversation document
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
                                    Log.d("InvestorCard", "Conversation created: $conversationId")
                                    // Navigate to PrivateChatScreen
                                    navController.navigate(Screen.PrivateChat.createRoute(conversationId))
                                } catch (e: Exception) {
                                    Log.e("InvestorCard", "Error creating conversation: ${e.message}", e)
                                    android.widget.Toast.makeText(
                                        context,
                                        "Failed to start conversation: ${e.message}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Mail, contentDescription = "Message Investor")
                    }
                }
            }
        }
    }
}