package com.phoenixcorp.founderfinder.ui.screens

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.Forum
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current

    var forums by remember { mutableStateOf<List<Forum>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val philosophy = getPhilosophyOfTheDay()

    // Fetch trending forums
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            errorMessage = "Please sign in to view content."
            isLoading = false
            return@LaunchedEffect
        }
        try {
            val categories = listOf("globalissues", "nationalissues", "localissues", "future", "marketpotential", "requestedsolutions")
            val allForums = mutableListOf<Forum>()

            for (category in categories) {
                val snapshot = firestore.collection("category")
                    .document(category)
                    .collection("forum")
                    .get()
                    .await()

                val categoryForums = snapshot.documents.mapNotNull { doc ->
                    try {
                        Forum(
                            id = doc.id,
                            title = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            creatorId = doc.getString("creatorId") ?: "",
                            creatorName = doc.getString("creatorName") ?: "Anonymous",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            imageUrl = doc.getString("imageUrl"),
                            likes = doc.getLong("likes")?.toInt() ?: 0,
                            isFavorited = doc.getBoolean("isFavorited") ?: false,
                            hasLiked = false,
                            category = category
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                allForums.addAll(categoryForums)
            }

            forums = allForums.sortedByDescending { it.likes }
            isLoading = false
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error fetching forums", e)
            errorMessage = "Failed to load trending forums"
            isLoading = false
        }
    }

    Scaffold(
        topBar = { ScreenBanner(title = { Text("Home") }) },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Philosophy of the Day
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Philosophy of the Day", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(philosophy, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    }
                }
            }

            // Trending Forums
            item {
                Text("Trending Forums", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (errorMessage != null) {
                item {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
            } else {
                val groupedForums = forums.groupBy { it.category }

                groupedForums.forEach { (categoryKey, categoryForums) ->
                    item {
                        Column {
                            Text(
                                text = getCategoryDisplayName(categoryKey),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(categoryForums.take(6)) { forum ->
                                    TrendingForumItem(
                                        forum = forum,
                                        navController = navController,
                                        modifier = Modifier.width(180.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Opportunities Section
            item {
                Text("Opportunities For You", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Featured Incubators & Grants", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Apply to top Canadian startup programs and funding opportunities tailored for founders like you.")
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { /* TODO: Navigate to opportunities screen */ }, modifier = Modifier.fillMaxWidth()) {
                            Text("Explore Opportunities")
                        }
                    }
                }
            }
        }
    }
}

// Helper function for readable category names
private fun getCategoryDisplayName(category: String): String {
    return when (category.lowercase()) {
        "globalissues" -> "Global Issues"
        "nationalissues" -> "National Issues"
        "localissues" -> "Local Issues"
        "future" -> "Future Trends"
        "marketpotential" -> "Market Potential"
        "requestedsolutions" -> "Requested Solutions"
        else -> category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

// Trending Forum Item
@Composable
fun TrendingForumItem(
    forum: Forum,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(220.dp)
            .clickable {
                val route = Screen.InstitutionForum.createRoute(forum.category, forum.id)
                navController.navigate(route)
            }
    ) {
        Column {
            Image(
                painter = forum.imageUrl?.let { rememberAsyncImagePainter(it) }
                    ?: painterResource(id = R.drawable.ic_placeholder),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = forum.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = forum.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${forum.likes} likes", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// Philosophy of the Day
@RequiresApi(Build.VERSION_CODES.O)
fun getPhilosophyOfTheDay(): String {
    val philosophies = listOf(
        "Success is not final, failure is not fatal: It is the courage to continue that counts. – Winston Churchill",
        "The only way to do great work is to love what you do. – Steve Jobs",
        "Innovation distinguishes between a leader and a follower. – Steve Jobs",
        "The best way to predict the future is to create it. – Peter Drucker",
        "The journey of a thousand miles begins with a single step. – Lao Tzu",
        "You miss 100% of the shots you don’t take. – Wayne Gretzky",
        "It’s not about ideas. It’s about making ideas happen. – Scott Belsky",
        "Stay hungry, stay foolish. – Steve Jobs",
        "The only limit to our realization of tomorrow is our doubts of today. – Franklin D. Roosevelt",
        "Do not wait to strike till the iron is hot; but make it hot by striking. – William Butler Yeats",
        "Everything you want is on the other side of fear. – Jack Canfield",
        "The way to get started is to quit talking and begin doing. – Walt Disney",
        "Whether you think you can, or you think you can’t – you’re right. – Henry Ford",
        "The future belongs to those who believe in the beauty of their dreams. – Eleanor Roosevelt",
        "Opportunities don't happen. You create them. – Chris Grosser",
        "The only impossible journey is the one you never begin. – Tony Robbins",
        "Build something 100 people love, not something 1 million people kind of like. – Brian Chesky",
        "Your network is your net worth. – Porter Gale",
        "Done is better than perfect. – Sheryl Sandberg",
        "Fall seven times, stand up eight. – Japanese Proverb",
        "If you are not embarrassed by the first version of your product, you’ve launched too late. – Reid Hoffman",
        "The biggest risk is not taking any risk. – Mark Zuckerberg",
        "Make something people want. – Paul Graham",
        "Move fast and break things. – Mark Zuckerberg",
        "The best time to plant a tree was 20 years ago. The second best time is now. – Chinese Proverb",
        "Genius is 1% inspiration and 99% perspiration. – Thomas Edison",
        "Customers don’t care about your solution. They care about their problems. – Dave McClure",
        "Ideas are cheap. Execution is everything. – John Doerr",
        "The harder you work, the luckier you get. – Gary Player",
        "Surround yourself with people who lift you higher. – Oprah Winfrey",
        "Chase the vision, not the money. The money will follow. – Tony Hsieh"
    )

    val dayOfMonth = LocalDate.now(ZoneId.of("America/Edmonton")).dayOfMonth
    return philosophies[(dayOfMonth - 1) % philosophies.size]
}