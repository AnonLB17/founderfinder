package com.phoenixcorp.founderfinder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.phoenixcorp.founderfinder.navigation.AppNavGraph
import com.phoenixcorp.founderfinder.ui.theme.FounderfinderTheme
import com.phoenixcorp.founderfinder.ui.viewmodel.notifications.NotificationsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var navController: NavHostController
    private lateinit var snackbarHostState: SnackbarHostState
    private lateinit var notificationsViewModel: NotificationsViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) Log.d("MainActivity", "Notification permission granted")
        else Log.w("MainActivity", "Notification permission denied")
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            Log.d("MainActivity", "✅ Location permission granted")
        } else {
            Log.w("MainActivity", "⚠️ Location permission denied")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupPersistentAuth()
        requestPermissionsIfNeeded()

        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())

        setContent {
            FounderfinderTheme {
                snackbarHostState = remember { SnackbarHostState() }
                navController = rememberNavController()
                notificationsViewModel = hiltViewModel()

                // NavGraph now correctly starts at Splash
                AppNavGraph(
                    navController = navController,
                    snackbarHostState = snackbarHostState
                )
            }
        }

        // Refresh notifications after a short delay (non-blocking)
        lifecycleScope.launch {
            delay(800)
            notificationsViewModel.refreshNotifications()
        }

        // Handle deep links / FCM after composition is ready
        // NOTE: We NO LONGER force navigate based on auth here.
        // That responsibility moved to SplashScreen (MVVM + clean).
        // This prevents the race that was skipping Splash.
        window.decorView.post {
            handleIntent(intent)
            if (auth.currentUser != null) {
                fetchFcmToken()
            }
            createNotificationChannel()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "FounderFinder Notifications"
            val descriptionText = "Notifications for new threads, comments, and replies"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel(
                "founderfinder_notifications",
                name,
                importance
            ).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager =
                getSystemService(android.content.Context.NOTIFICATION_SERVICE)
                        as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermissionLauncher.launch(locationPermissions)
        }
    }

    private fun setupPersistentAuth() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d("Auth", "✅ User signed in persistently: ${user.uid}")
            } else {
                Log.d("Auth", "User signed out")
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
        if (auth.currentUser != null) fetchFcmToken()
    }

    private fun handleIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        val screen = extras.getString("screen") ?: extras.getString("type") ?: ""
        val threadId = extras.getString("threadId")
        val forumId = extras.getString("forumId")
        val category = extras.getString("category")
        val chatId = extras.getString("chatId")
        val activityId = extras.getString("activityId")

        Log.d("MainActivity", "handleIntent → screen=$screen, threadId=$threadId, forumId=$forumId")

        // Mark notification as read if present
        extras.getString("notificationId")?.let {
            notificationsViewModel.markAsRead(it)
        }

        // Only navigate if we have a valid navController and user is authenticated
        // (deep links from notifications usually require auth)
        if (!::navController.isInitialized) return

        when {
            threadId != null -> {
                val route = "thread/${category.orEmpty()}/$forumId/$threadId"
                Log.d("MainActivity", "Navigating to Thread: $route")
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }

            forumId != null -> {
                val route = "institution_forum/${category.orEmpty()}/$forumId"
                Log.d("MainActivity", "Navigating to Forum: $route")
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }

            chatId != null -> {
                navController.navigate("private_chat/$chatId") {
                    launchSingleTop = true
                }
            }

            activityId != null -> {
                navController.navigate("partners?highlightActivity=$activityId") {
                    launchSingleTop = true
                }
            }

            else -> {
                Log.w("MainActivity", "No specific route for screen: $screen")
            }
        }
    }

    private fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        firestore.collection("profiles")
                            .document(userId)
                            .set(
                                mapOf(
                                    "fcmToken" to token,
                                    "updatedAt" to System.currentTimeMillis()
                                ),
                                com.google.firebase.firestore.SetOptions.merge()
                            )
                            .await()
                        Log.d("FCM", "Token saved for user: $userId")
                    } catch (e: Exception) {
                        Log.e("FCM", "Failed to save token", e)
                    }
                }
            }
        }
    }
}