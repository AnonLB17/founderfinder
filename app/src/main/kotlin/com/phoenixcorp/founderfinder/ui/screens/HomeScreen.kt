package com.phoenixcorp.founderfinder.ui.screens

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.Forum
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.ui.viewmodel.notifications.NotificationsViewModel
import com.phoenixcorp.founderfinder.ui.utils.fetchCurrentUserRole
import com.phoenixcorp.founderfinder.ui.utils.permissionsFor
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    navController: NavHostController,
    notificationsViewModel: NotificationsViewModel = hiltViewModel()
) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current

    // Spectator / role permissions
    var role by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        role = fetchCurrentUserRole()
    }
    val perms = remember(role) { permissionsFor(role) }

    var forums by remember { mutableStateOf<List<Forum>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val philosophy = getPhilosophyOfTheDay()

    // Forum structure tip (shows until user opts out)
    val prefs = context.getSharedPreferences("founderfinder_tips", Context.MODE_PRIVATE)
    var showForumTip by remember { mutableStateOf(!prefs.getBoolean("hasSeenHomeForumTip", false)) }
    var dontShowForumTipAgain by remember { mutableStateOf(false) }

    // Load notifications (updated method)
    LaunchedEffect(Unit) {
        notificationsViewModel.refreshNotifications()
    }

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
                            likes = when (val raw = doc.get("likes")) {
                                is Number -> raw.toInt()
                                is String -> raw.toIntOrNull() ?: 0
                                else -> 0
                            },
                            isFavorited = doc.getBoolean("isFavorited") ?: false,
                            hasLiked = false,
                            category = category
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                // Highest likes first; newest breaks a tie. Home shows the top two per category.
                allForums.addAll(
                    categoryForums
                        .sortedWith(compareByDescending<Forum> { it.likes }.thenByDescending { it.timestamp })
                        .take(2)
                )
            }

            forums = allForums
            isLoading = false
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error fetching forums", e)
            errorMessage = "Failed to load trending forums"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Home") },
                navController = navController,
                showNotifications = true,
                showLogout = true,
                onLogoutClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                    Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                }
            )
        },
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
                        Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
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
                                items(categoryForums.take(2)) { forum ->
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
                        Button(
                            onClick = {
                                // Explore is view-only — OK for spectators.
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Explore Opportunities")
                        }
                    }
                }
            }
        }
    }

    // Tip dialog — persists until user checks "Don't show again"
    if (showForumTip) {
        AlertDialog(
            onDismissRequest = { showForumTip = false },
            title = { Text("Create Your First Idea") },
            text = {
                Column {
                    Text("A Forum is an Idea — a problem or opportunity you want to explore.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Inside a Forum you (or others) can add Threads. A Thread is a specific thought or angle on that Idea.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Comments under a Thread are the discussion that builds on that thought.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "To create a Forum, go to Idea Development tab then navigate to Idea Generation to open a category (Global Issues, Future, etc.) and look for the + to open the Create Forum screen.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = dontShowForumTipAgain,
                            onCheckedChange = { dontShowForumTipAgain = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Don't show this again")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (dontShowForumTipAgain) {
                        prefs.edit().putBoolean("hasSeenHomeForumTip", true).apply()
                    }
                    showForumTip = false
                }) {
                    Text("Got it")
                }
            }
        )
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
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val coroutineScope = rememberCoroutineScope()
    var likes by remember(forum.id) { mutableIntStateOf(forum.likes) }
    var hasLiked by remember(forum.id) { mutableStateOf(forum.hasLiked) }
    var liking by remember(forum.id) { mutableStateOf(false) }

    LaunchedEffect(currentUser?.uid, forum.id, forum.category) {
        if (currentUser == null) return@LaunchedEffect
        try {
            val likeDoc = firestore.collection("category")
                .document(forum.category)
                .collection("forum")
                .document(forum.id)
                .collection("likes")
                .document(currentUser.uid)
                .get()
                .await()
            hasLiked = likeDoc.exists()
        } catch (e: Exception) {
            Log.e("TrendingForumItem", "Error checking like status: ${e.message}", e)
        }
    }

    Card(
        modifier = modifier
            .height(252.dp)
            .clickable {
                val route = Screen.InstitutionForum.createRoute(forum.category, forum.id)
                navController.navigate(route)
            }
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = forum.imageUrl?.let { rememberAsyncImagePainter(it) }
                        ?: painterResource(id = R.drawable.ic_placeholder),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
                IconButton(
                    onClick = {
                        if (liking) return@IconButton
                        if (currentUser == null) {
                            Toast.makeText(context, "Please sign in to like", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.SignIn.route)
                            return@IconButton
                        }
                        val removing = hasLiked
                        liking = true
                        coroutineScope.launch {
                            val role = fetchCurrentUserRole()
                            if (!permissionsFor(role).requireEngage(
                                    context,
                                    if (removing) "unlike a forum" else "like a forum"
                                )
                            ) {
                                liking = false
                                return@launch
                            }
                            try {
                                val forumRef = firestore.collection("category")
                                    .document(forum.category)
                                    .collection("forum")
                                    .document(forum.id)
                                val likeRef = forumRef.collection("likes").document(currentUser.uid)
                                firestore.runTransaction { transaction ->
                                    val forumDoc = transaction.get(forumRef)
                                    val currentLikes = when (val raw = forumDoc.get("likes")) {
                                        is Number -> raw.toInt()
                                        else -> 0
                                    }
                                    if (removing) {
                                        transaction.delete(likeRef)
                                        transaction.update(forumRef, "likes", (currentLikes - 1).coerceAtLeast(0))
                                    } else {
                                        transaction.set(likeRef, mapOf("timestamp" to System.currentTimeMillis()))
                                        transaction.update(forumRef, "likes", currentLikes + 1)
                                    }
                                }.await()
                                if (removing) {
                                    likes = (likes - 1).coerceAtLeast(0)
                                    hasLiked = false
                                } else {
                                    likes += 1
                                    hasLiked = true
                                }
                            } catch (e: Exception) {
                                Log.e("TrendingForumItem", "Error toggling like: ${e.message}", e)
                                Toast.makeText(context, "Error updating like", Toast.LENGTH_SHORT).show()
                            } finally {
                                liking = false
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (hasLiked) "Unlike" else "Like",
                            tint = if (hasLiked) Color(0xFFFF4D6D) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
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
                Text(
                    text = "$likes likes",
                    style = MaterialTheme.typography.bodySmall
                )
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
