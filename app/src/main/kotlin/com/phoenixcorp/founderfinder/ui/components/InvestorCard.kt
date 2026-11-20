package com.phoenixcorp.founderfinder.ui.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
    onSwipe: () -> Unit = {}
) {
    val auth = Firebase.auth
    val firestore = Firebase.firestore
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Robust name fallback
    val displayName = investor.name.ifBlank { "Investor" }

    // Handle both possible field names for stage
    val preferredStage = investor.investmentStage.ifBlank {
        (investor as? Any)?.let { (it as? Map<*, *>)?.get("preferredStages")?.toString() } ?: "Any Stage"
    }.ifBlank { "Any Stage" }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clickable {
                navController.navigate(Screen.UserProfile.createRoute(investor.userId))
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header - Photo + Name
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

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

                                firestore.collection("conversations")
                                    .document(conversationId)
                                    .set(
                                        mapOf(
                                            "participantIds" to listOf(currentUser.uid, investor.userId),
                                            "lastUpdated" to System.currentTimeMillis()
                                        ),
                                        com.google.firebase.firestore.SetOptions.merge()
                                    )
                                    .await()

                                navController.navigate(Screen.PrivateChat.createRoute(conversationId))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to start chat", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Mail, contentDescription = "Message", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Preferred Industries
            if (investor.preferredIndustries.isNotEmpty()) {
                item {
                    Text(
                        text = "Preferred Industries",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        investor.preferredIndustries.forEach { industry ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(industry) }
                            )
                        }
                    }
                }
            }

            // Investment Philosophy
            if (investor.philosophy.isNotBlank()) {
                item {
                    Text("Investment Philosophy", style = MaterialTheme.typography.titleMedium)
                    Text(investor.philosophy, style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Investment Range & Preferred Stage
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Investment Range", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${investor.investmentRangeMin.ifBlank { "Any" }} - ${investor.investmentRangeMax.ifBlank { "Any" }}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Preferred Stage", style = MaterialTheme.typography.titleSmall)
                        Text(
                            investor.investmentStage.ifBlank { "Any Stage" },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Portfolio Companies
            if (investor.portfolioCompanies.isNotEmpty()) {
                item {
                    Text("Portfolio Companies", style = MaterialTheme.typography.titleMedium)
                    investor.portfolioCompanies.take(4).forEach { company ->
                        Text("• $company", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Testimonials
            if (investor.testimonials.isNotEmpty()) {
                item {
                    Text("Testimonials", style = MaterialTheme.typography.titleMedium)
                    investor.testimonials.take(2).forEach { testimonial ->
                        Text("\"$testimonial\"", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Investment Terms
            if (investor.equityTerms.isNotBlank() || investor.boardRole.isNotBlank() || investor.returnTimeline.isNotBlank()) {
                item {
                    Text("Investment Terms", style = MaterialTheme.typography.titleMedium)
                    if (investor.equityTerms.isNotBlank()) Text("Equity: ${investor.equityTerms}")
                    if (investor.boardRole.isNotBlank()) Text("Board Role: ${investor.boardRole}")
                    if (investor.returnTimeline.isNotBlank()) Text("Return Timeline: ${investor.returnTimeline}")
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}