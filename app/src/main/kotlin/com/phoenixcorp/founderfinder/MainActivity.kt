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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import com.phoenixcorp.founderfinder.navigation.AppNavGraph
import com.phoenixcorp.founderfinder.ui.FounderfinderTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var messageListener: ListenerRegistration? = null
    private var invitationListener: ListenerRegistration? = null
    private var threadListener: ListenerRegistration? = null
    private var commentListener: ListenerRegistration? = null
    private var activityListeners: List<ListenerRegistration> = emptyList()
    private lateinit var navController: NavHostController
    private lateinit var snackbarHostState: SnackbarHostState

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.w("MainActivity", "Notification permission denied")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        Log.d("MainActivity", "Using DebugAppCheckProviderFactory")

        setContent {
            FounderfinderTheme {
                snackbarHostState = remember { SnackbarHostState() }
                navController = rememberNavController()
                AppNavGraph(navController = navController, snackbarHostState = snackbarHostState)
            }
        }
        fetchFcmToken()
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            setupFirestoreListeners()
        }
    }

    override fun onStop() {
        super.onStop()
        messageListener?.remove()
        invitationListener?.remove()
        threadListener?.remove()
        commentListener?.remove()
        activityListeners.forEach { it.remove() }
        Log.d("MainActivity", "Firestore listeners removed")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "Handling new intent: ${intent.extras?.toString() ?: intent.data?.toString()}")
        handleIntent(intent)
        if (auth.currentUser != null) {
            fetchFcmToken()
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.extras?.let { extras ->
            val screen = extras.getString("screen")
            val id = extras.getString("id")
            val type = extras.getString("type")
            Log.d("MainActivity", "Handling intent: screen=$screen, id=$id, type=$type")
            when (screen) {
                "PrivateChat" -> {
                    navController.navigate("private_chat/$id")
                }
                "GroupChat" -> {
                    navController.navigate("group_chat/$id")
                }
                "OrganizationDetails" -> {
                    navController.navigate("organization_details/$id")
                }
                "ForumTemplate" -> {
                    navController.navigate("institution_forum/${id?.split("/")[0]}/${id?.split("/")[1]}")
                }
                "Partners" -> {
                    navController.navigate("partners/$id")
                }
            }
        }
    }

    private fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "Manual token fetch: $token")
                val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        firestore.collection("users")
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
                        Log.e("FCM", "Failed to save token: ${e.message}", e)
                    }
                }
            } else {
                Log.e("FCM", "Token fetch failed: ${task.exception?.message}")
            }
        }
    }

    private suspend fun fetchOrganizations(userId: String): List<String> {
        val createdOrgSnapshot = firestore.collection("organizations")
            .whereEqualTo("creatorId", userId)
            .get()
            .await()
        val createdOrgIds = createdOrgSnapshot.documents.mapNotNull { it.id }

        val invitationSnapshot = firestore.collection("invitations")
            .whereEqualTo("inviteeId", userId)
            .get()
            .await()
        val invitedOrgIds = invitationSnapshot.documents.mapNotNull { it.getString("orgId") }.distinct()

        val collaboratorOrgIds = mutableListOf<String>()
        val orgSnapshot = firestore.collection("organizations").get().await()
        for (orgDoc in orgSnapshot.documents) {
            val orgId = orgDoc.id
            val collaboratorDoc = firestore.collection("organizations")
                .document(orgId)
                .collection("collaborators")
                .document(userId)
                .get()
                .await()
            if (collaboratorDoc.exists()) {
                collaboratorOrgIds.add(orgId)
            }
        }

        return (createdOrgIds + invitedOrgIds + collaboratorOrgIds).distinct()
    }

    private suspend fun fetchForums(userId: String): List<Triple<String, String, String>> {
        val forumSnapshot = firestore.collectionGroup("forum")
            .whereEqualTo("creatorId", userId)
            .get()
            .await()
        return forumSnapshot.documents.mapNotNull { doc ->
            val category = doc.reference.parent.parent?.id ?: return@mapNotNull null
            val forumId = doc.id
            val name = doc.getString("name") ?: "Untitled Forum"
            Triple(category, forumId, name)
        }
    }

    private suspend fun fetchThreads(userId: String): List<Triple<String, String, String>> {
        val threadSnapshot = firestore.collectionGroup("threads")
            .whereEqualTo("creatorId", userId)
            .get()
            .await()
        return threadSnapshot.documents.mapNotNull { doc ->
            val threadId = doc.id
            val forumDoc = doc.reference.parent.parent?.get()?.await() ?: return@mapNotNull null
            val category = forumDoc.reference.parent.parent?.id ?: return@mapNotNull null
            val forumId = forumDoc.id
            Triple(category, forumId, threadId)
        }
    }

    private fun setupFirestoreListeners() {
        val userId = auth.currentUser?.uid ?: return
        Log.d("MainActivity", "Setting up listeners for user: $userId")

        // Message listener
        messageListener?.remove()
        messageListener = firestore.collectionGroup("messages")
            .whereEqualTo("recipientId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MainActivity", "Message listener error: ${error.message}", error)
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.let { changes ->
                    val seenMessageIds = mutableSetOf<String>()
                    val uniqueMessages = changes
                        .filter {
                            it.type == com.google.firebase.firestore.DocumentChange.Type.ADDED &&
                                    it.document.id !in seenMessageIds && seenMessageIds.add(it.document.id)
                        }
                    uniqueMessages.forEach { change ->
                        val message = change.document.data
                        val senderId = message["senderId"] as? String ?: return@forEach
                        val content = message["content"] as? String ?: ""
                        Log.d("MainActivity", "Message listener triggered, change: ${change.type}, content: $content from $senderId, messageId: ${change.document.id}")
                    }
                }
            }

        // Invitation listener
        invitationListener?.remove()
        invitationListener = firestore.collection("invitations")
            .whereEqualTo("inviteeId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MainActivity", "Invitation listener error: ${error.message}", error)
                    return@addSnapshotListener
                }
                Log.d("MainActivity", "Invitation listener triggered, changes: ${snapshot?.documentChanges?.size ?: 0}")
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val invitation = change.document.data
                        val inviterId = invitation["inviterId"] as? String ?: return@forEach
                        val orgId = invitation["orgId"] as? String ?: return@forEach
                    }
                }
            }

        // Thread listener for forums created by the user
        threadListener?.remove()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val forums = fetchForums(userId)
                threadListener = firestore.collectionGroup("threads")
                    .whereIn("institutionName", forums.map { it.second })
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("MainActivity", "Thread listener error: ${error.message}", error)
                            return@addSnapshotListener
                        }
                        snapshot?.documentChanges?.forEach { change ->
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                val thread = change.document.data
                                val creatorId = thread["creatorId"] as? String ?: return@forEach
                                val message = thread["message"] as? String ?: ""
                                val forumId = thread["institutionName"] as? String ?: ""
                                val forumName = forums.find { it.second == forumId }?.third ?: "a forum"
                                if (creatorId != userId) {
                                    Log.d("MainActivity", "Thread listener triggered, change: ${change.type}, message: $message, creator: $creatorId")
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to fetch forums for thread listener: ${e.message}", e)
            }
        }

        // Comment listener for threads created by the user
        commentListener?.remove()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val threads = fetchThreads(userId)
                commentListener = firestore.collectionGroup("comments")
                    .whereIn("parentId", threads.map { it.third }.ifEmpty { listOf("none") })
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("MainActivity", "Comment listener error: ${error.message}", error)
                            return@addSnapshotListener
                        }
                        snapshot?.documentChanges?.forEach { change ->
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                val comment = change.document.data
                                val creatorId = comment["creatorId"] as? String ?: return@forEach
                                val message = comment["message"] as? String ?: ""
                                val parentId = comment["parentId"] as? String
                                val threadId = change.document.reference.parent.parent?.id ?: ""
                                val forumId = threads.find { it.third == threadId }?.second ?: ""
                                val category = threads.find { it.third == threadId }?.first ?: ""
                                if (creatorId != userId) {
                                    Log.d("MainActivity", "Comment listener triggered, change: ${change.type}, message: $message, creator: $creatorId, parentId: $parentId")
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to fetch threads for comment listener: ${e.message}", e)
            }
        }

        // Activity listeners
        CoroutineScope(Dispatchers.Main).launch {
            try {
                activityListeners.forEach { it.remove() }
                val orgIds = fetchOrganizations(userId)
                val newListeners = mutableListOf<ListenerRegistration>()
                orgIds.forEach { orgId ->
                    val listener = firestore.collection("organizations")
                        .document(orgId)
                        .collection("activities")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.e("MainActivity", "Activity listener error for org $orgId: ${error.message}", error)
                                return@addSnapshotListener
                            }
                            snapshot?.documentChanges?.forEach { change ->
                                if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                    val activity = change.document.data
                                    val title = activity["title"] as? String ?: "New Activity"
                                    val creatorId = activity["creatorId"] as? String ?: ""
                                    if (creatorId != userId) {
                                        Log.d("MainActivity", "Activity listener triggered for org $orgId, change: ${change.type}, title: $title")
                                    }
                                }
                            }
                        }
                    newListeners.add(listener)
                }
                // User activities listener
                val userListener = firestore.collectionGroup("activities")
                    .whereEqualTo("creatorId", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("MainActivity", "User activity listener error: ${error.message}", error)
                            return@addSnapshotListener
                        }
                        snapshot?.documentChanges?.forEach { change ->
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                val activity = change.document.data
                                val title = activity["title"] as? String ?: "New Activity"
                                Log.d("MainActivity", "User activity listener triggered, change: ${change.type}, title: $title")
                            }
                        }
                    }
                newListeners.add(userListener)
                activityListeners = newListeners
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to fetch organizations: ${e.message}", e)
            }
        }
    }
}