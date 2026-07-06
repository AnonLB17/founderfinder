package com.phoenixcorp.founderfinder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.phoenixcorp.founderfinder.navigation.Screen
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

    // Permission Launchers
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

        // Request permissions
        requestPermissionsIfNeeded()

        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        Log.d("MainActivity", "Using DebugAppCheckProviderFactory")

        setContent {
            FounderfinderTheme {
                snackbarHostState = remember { SnackbarHostState() }
                navController = rememberNavController()
                notificationsViewModel = hiltViewModel()
                AppNavGraph(navController = navController, snackbarHostState = snackbarHostState)
            }
        }

        // Force start notification listener on physical device
        lifecycleScope.launch {
            delay(800)
            notificationsViewModel.refreshNotifications()
            Log.d("MainActivity", "Forced notification listener start on physical device")
        }

        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthAndNavigate()
            handleIntent(intent)
            fetchFcmToken()
            createNotificationChannel()
        }, 400)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "FounderFinder Notifications"
            val descriptionText = "Notifications for new threads, comments, and replies"
            val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
            val channel = android.app.NotificationChannel("default_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: android.app.NotificationManager =
                getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("MainActivity", "Notification channel created")
        }
    }

    private fun requestPermissionsIfNeeded() {
        // Notification Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Location Permissions
        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissionLauncher.launch(locationPermissions)
        }
    }

    private fun checkAuthAndNavigate() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("MainActivity", "✅ User already signed in: ${currentUser.uid}")
            try {
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { saveState = true }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Navigation to home failed", e)
            }
        }
    }

    private fun setupPersistentAuth() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d("Auth", "✅ User signed in persistently: ${user.uid}")
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
        val notificationId = extras.getString("notificationId")

        Log.d("MainActivity", "handleIntent → screen=$screen, category=$category, threadId=$threadId, forumId=$forumId")

        // Mark notification as read
        if (!notificationId.isNullOrEmpty()) {
            notificationsViewModel.markAsRead(notificationId)
        }

        when {
            // === THREAD / COMMENT NAVIGATION ===
            threadId != null && forumId != null -> {
                val cat = category?.ifBlank { "" } ?: ""
                val route = "thread/$cat/$forumId/$threadId"

                Log.d("MainActivity", "✅ Navigating to Thread: $route")
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }

            // === FORUM NAVIGATION ===
            forumId != null -> {
                val cat = category?.ifBlank { "" } ?: ""
                val route = "institution_forum/$cat/$forumId"

                Log.d("MainActivity", "✅ Navigating to Forum: $route")
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }

            // === CHAT NAVIGATION ===
            chatId != null -> {
                navController.navigate("private_chat/$chatId") {
                    launchSingleTop = true
                }
            }

            // === ACTIVITY NAVIGATION ===
            activityId != null -> {
                navController.navigate("partners?highlightActivity=$activityId") {
                    launchSingleTop = true
                }
            }

            else -> {
                Log.w("MainActivity", "Unknown navigation type: $screen")
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
                            .set(mapOf("fcmToken" to token, "updatedAt" to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge())
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