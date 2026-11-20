package com.phoenixcorp.founderfinder.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.File
import com.phoenixcorp.founderfinder.domain.model.Organization
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.utils.fetchCurrentUserRole
import com.phoenixcorp.founderfinder.ui.utils.permissionsFor
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import coil.request.ImageRequest as CoilImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationDetailsScreen(
    navController: NavHostController,
    orgId: String,
    invitationId: String
) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Spectator permissions
    var role by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        role = fetchCurrentUserRole()
    }
    val perms = remember(role) { permissionsFor(role) }

    var organization by remember { mutableStateOf<Organization?>(null) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var filesError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var invitedType by remember { mutableStateOf<String?>(null) }
    var invitationStatus by remember { mutableStateOf<String?>(null) }
    var invitationError by remember { mutableStateOf<String?>(null) }
    var orgInvitationMissing by remember { mutableStateOf(false) }

    // Fetch organization details and files
    LaunchedEffect(orgId) {
        if (currentUser == null) {
            errorMessage = "Please sign in to view this page."
            isLoading = false
            Toast.makeText(context, "Please sign in", Toast.LENGTH_SHORT).show()
            navController.navigate(Screen.SignIn.route) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
            return@LaunchedEffect
        }
        try {
            Log.d("OrganizationDetailsScreen", "Fetching organization: $orgId, User: ${currentUser.uid}")
            // Fetch organization
            val orgDoc = firestore.collection("organizations")
                .document(orgId)
                .get()
                .await()
            if (!orgDoc.exists()) {
                Log.e("OrganizationDetailsScreen", "Organization $orgId does not exist")
                errorMessage = "Organization not found."
                isLoading = false
                return@LaunchedEffect
            }
            organization = orgDoc.toObject(Organization::class.java)?.copy(id = orgDoc.id)
            Log.d("OrganizationDetailsScreen", "Organization data: ${orgDoc.data}")

            // Fetch files
            try {
                Log.d("OrganizationDetailsScreen", "Fetching files for organization: $orgId")
                val fileSnapshot = firestore.collection("organizations")
                    .document(orgId)
                    .collection("files")
                    .get()
                    .await()
                files = fileSnapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(File::class.java)?.copy(fileId = doc.id, orgId = orgId)
                    } catch (e: Exception) {
                        Log.e("OrganizationDetailsScreen", "Error parsing file ${doc.id}: ${e.message}")
                        null
                    }
                }
                Log.d("OrganizationDetailsScreen", "Fetched ${files.size} files")
            } catch (e: Exception) {
                Log.e("OrganizationDetailsScreen", "Error fetching files: ${e.message}", e)
                filesError = "Unable to load files: ${e.message}"
            }

            // Debug organization invitations
            try {
                Log.d("OrganizationDetailsScreen", "Checking organization invitations for user: ${currentUser.uid}")
                val invitationSnapshot = firestore.collection("organizations")
                    .document(orgId)
                    .collection("invitations")
                    .document(currentUser.uid)
                    .get()
                    .await()
                if (invitationSnapshot.exists()) {
                    Log.d("OrganizationDetailsScreen", "Organization invitation found: ${invitationSnapshot.data}")
                } else {
                    Log.w("OrganizationDetailsScreen", "No organization invitation found for user: ${currentUser.uid}")
                    orgInvitationMissing = true
                }
            } catch (e: Exception) {
                Log.e("OrganizationDetailsScreen", "Error checking organization invitations: ${e.message}")
                orgInvitationMissing = true
            }

            isLoading = false
        } catch (e: Exception) {
            Log.e("OrganizationDetailsScreen", "Error fetching organization: ${e.message}", e)
            errorMessage = "Failed to load organization: ${e.message}"
            isLoading = false
        }
    }

    // Fetch and debug invitation details
    LaunchedEffect(currentUser, invitationId) {
        if (currentUser == null || invitationId.isBlank()) {
            invitationError = "Invalid invitation or user not signed in."
            Log.e("OrganizationDetailsScreen", "Invalid invitationId: $invitationId or no user")
            return@LaunchedEffect
        }
        try {
            Log.d("OrganizationDetailsScreen", "Fetching invitation: $invitationId")
            val invitationDoc = firestore.collection("invitations")
                .document(invitationId)
                .get()
                .await()
            if (!invitationDoc.exists()) {
                Log.e("OrganizationDetailsScreen", "Invitation $invitationId does not exist")
                invitationError = "Invitation not found."
                return@LaunchedEffect
            }
            val inviteeId = invitationDoc.getString("inviteeId")
            val orgIdFromInvitation = invitationDoc.getString("orgId")
            invitationStatus = invitationDoc.getString("status")
            invitedType = invitationDoc.getString("type")?.lowercase() ?: "collaborator"
            Log.d("OrganizationDetailsScreen", "Invitation details: inviteeId=$inviteeId, orgId=$orgIdFromInvitation, status=$invitationStatus, type=$invitedType")
            if (inviteeId != currentUser.uid) {
                invitationError = "Invitation is not for this user."
                Log.e("OrganizationDetailsScreen", "Mismatched inviteeId: expected ${currentUser.uid}, got $inviteeId")
            }
            if (orgIdFromInvitation != orgId) {
                invitationError = "Invitation is for a different organization."
                Log.e("OrganizationDetailsScreen", "Mismatched orgId: expected $orgId, got $orgIdFromInvitation")
            }
        } catch (e: Exception) {
            Log.e("OrganizationDetailsScreen", "Error fetching invitation: ${e.message}", e)
            invitationError = "Failed to load invitation details: ${e.message}"
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Organization Details") },
                navController = navController,
                showBackButton = true
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (organization != null) {
                    Image(
                        painter = organization!!.imageUri?.let {
                            rememberAsyncImagePainter(
                                model = CoilImageRequest.Builder(LocalContext.current)
                                    .data(it)
                                    .crossfade(true)
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .error(R.drawable.ic_profile_placeholder)
                                    .build()
                            )
                        } ?: painterResource(id = R.drawable.ic_profile_placeholder),
                        contentDescription = "Organization Logo",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = organization!!.name,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = organization!!.description,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!isLoading && errorMessage == null && organization != null) {
                // Creator management button (commented out if not supported in reverted navigation)
                /* if (currentUser != null && currentUser.uid == organization!!.creatorId) {
                    item {
                        Button(
                            onClick = { navController.navigate(Screen.OrganizationManagement.createRoute(orgId)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Manage Organization")
                        }
                    }
                } */

                // Invitation Section
                if (invitationError != null) {
                    item {
                        Text(
                            text = invitationError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (currentUser != null && currentUser.uid != organization!!.creatorId && invitationStatus == "pending") {
                    item {
                        Text(
                            text = "You've been invited to collaborate as a ${invitedType?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Collaborator"}.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(onClick = {
                                if (!perms.requireUpdate(context, "accept an invitation")) return@Button
                                coroutineScope.launch {
                                    try {
                                        // Update invitation status
                                        firestore.collection("invitations")
                                            .document(invitationId)
                                            .update("status", "accepted")
                                            .await()
                                        // Update organization invitation
                                        firestore.collection("organizations")
                                            .document(orgId)
                                            .collection("invitations")
                                            .document(currentUser.uid)
                                            .update("status", "accepted")
                                            .await()
                                        // Add collaborator or partner based on type
                                        val typeData = mapOf(
                                            "type" to (invitedType ?: "collaborator"),
                                            "joinedAt" to System.currentTimeMillis()
                                        )
                                        firestore.collection("organizations")
                                            .document(orgId)
                                            .collection(if (invitedType == "partner") "partners" else "collaborators")
                                            .document(currentUser.uid)
                                            .set(typeData)
                                            .await()
                                        // Update SharedPreferences
                                        context.getSharedPreferences("founderfinder", Context.MODE_PRIVATE)
                                            .edit()
                                            .putString("selectedOrgId", orgId)
                                            .apply()
                                        invitationStatus = "accepted"
                                        Toast.makeText(context, "Collaboration accepted as ${invitedType?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Collaborator"}", Toast.LENGTH_SHORT).show()
                                        navController.navigate(Screen.OrganizationFiles.route)
                                    } catch (e: Exception) {
                                        Log.e("OrganizationDetailsScreen", "Error accepting invitation: ${e.message}", e)
                                        errorMessage = "Failed to accept invitation: ${e.message}"
                                    }
                                }
                            }) {
                                Text("Accept")
                            }
                            Button(onClick = {
                                if (!perms.requireUpdate(context, "respond to an invitation")) return@Button
                                coroutineScope.launch {
                                    try {
                                        firestore.collection("invitations")
                                            .document(invitationId)
                                            .update("status", "denied")
                                            .await()
                                        firestore.collection("organizations")
                                            .document(orgId)
                                            .collection("invitations")
                                            .document(currentUser.uid)
                                            .update("status", "denied")
                                            .await()
                                        invitationStatus = "denied"
                                        Toast.makeText(context, "Collaboration denied", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Log.e("OrganizationDetailsScreen", "Error denying invitation: ${e.message}", e)
                                        errorMessage = "Failed to deny invitation: ${e.message}"
                                    }
                                }
                            }) {
                                Text("Deny")
                            }
                        }
                    }
                } else if (invitationStatus != null && invitationStatus != "pending") {
                    item {
                        Text(
                            text = "Invitation status: ${invitationStatus?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Files Section
                item {
                    Text(
                        text = "Shared Files",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (orgInvitationMissing) {
                    item {
                        Text(
                            text = "Invitation data incomplete. Please ask the organization creator to re-send the invitation.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = {
                            if (!perms.requireSendMessage(context)) return@Button
                            coroutineScope.launch {
                                try {
                                    val conversationId = firestore.collection("conversations").add(
                                        mapOf(
                                            "senderId" to currentUser?.uid,
                                            "recipientIds" to listOf(organization?.creatorId),
                                            "message" to "Please re-send the invitation for ${organization?.name}",
                                            "isGroup" to false,
                                            "createdAt" to System.currentTimeMillis()
                                        )
                                    ).await().id
                                    Toast.makeText(context, "Re-send request sent to creator", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Log.e("OrganizationDetailsScreen", "Error sending re-send request: ${e.message}")
                                    Toast.makeText(context, "Failed to send re-send request", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Text("Request Re-send")
                        }
                    }
                } else if (filesError != null) {
                    item {
                        Text(
                            text = filesError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (files.isEmpty() && filesError == null && !orgInvitationMissing) {
                    item {
                        Text(
                            text = "No files shared yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (filesError == null && !orgInvitationMissing) {
                    items(files) { file ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (file.url.isNotBlank()) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(file.url))
                                            context.startActivity(intent)
                                            Log.d("OrganizationDetailsScreen", "Opening file: ${file.name}, URL: ${file.url}")
                                        } catch (e: Exception) {
                                            Log.e("OrganizationDetailsScreen", "Error opening file: ${e.message}")
                                            Toast.makeText(context, "Failed to open file: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Log.e("OrganizationDetailsScreen", "File URL is empty for ${file.name}")
                                        Toast.makeText(context, "File URL not available", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(file.uploadedAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            context.getSharedPreferences("founderfinder", Context.MODE_PRIVATE)
                                .edit()
                                .putString("selectedOrgId", orgId)
                                .apply()
                            navController.navigate(Screen.OrganizationFiles.route)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Files")
                    }
                }
            }
        }
    }
}