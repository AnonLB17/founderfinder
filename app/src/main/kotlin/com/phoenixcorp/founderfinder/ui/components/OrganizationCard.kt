package com.phoenixcorp.founderfinder.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.File
import com.phoenixcorp.founderfinder.domain.model.Organization
import com.phoenixcorp.founderfinder.navigation.Screen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

private const val TAG = "OrganizationCard"
private const val TYPE_BUSINESS_PLAN = "Business Plan"
private const val TYPE_PITCH_DECK = "Pitch Deck"

@Composable
fun OrganizationCard(
    organization: Organization? = null,
    orgId: String? = null,
    invitationId: String = "",
    navController: NavHostController,
    onSwipe: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val firestore = FirebaseFirestore.getInstance()
    var fetchedOrganization by remember { mutableStateOf<Organization?>(null) }
    var isLoading by remember { mutableStateOf(organization == null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var orgFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(orgId) {
        if (organization != null) {
            isLoading = false
            return@LaunchedEffect
        }
        if (orgId != null) {
            try {
                val doc = firestore.collection("organizations")
                    .document(orgId)
                    .get()
                    .await()
                fetchedOrganization = doc.toObject(Organization::class.java)?.copy(id = doc.id)
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load organization: ${e.message}"
                isLoading = false
            }
        }
    }

    val currentOrg = organization ?: fetchedOrganization
    val resolvedOrgId = currentOrg?.id ?: orgId

    LaunchedEffect(resolvedOrgId) {
        val id = resolvedOrgId ?: return@LaunchedEffect
        orgFiles = fetchOrgFiles(firestore, id)
    }

    val businessPlan = remember(orgFiles) { latestFileOfType(orgFiles, TYPE_BUSINESS_PLAN) }
    val pitchDeck = remember(orgFiles) { latestFileOfType(orgFiles, TYPE_PITCH_DECK) }

    Card(
        modifier = modifier
            .then(
                if (onSwipe != null) {
                    Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.9f)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                }
            )
            .then(
                if (onSwipe != null) {
                    Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (abs(dragAmount) > 50) {
                                Log.d(TAG, "Swiped organization: ${currentOrg?.name}")
                                onSwipe.invoke()
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
            .clickable(enabled = onSwipe == null) {
                val id = resolvedOrgId ?: return@clickable
                navController.navigate(Screen.OrganizationDetails.createRoute(id, invitationId.ifBlank { "none" }))
            },
        elevation = CardDefaults.cardElevation(defaultElevation = if (onSwipe != null) 8.dp else 4.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            )
        } else if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        } else if (currentOrg != null) {
            if (onSwipe != null) {
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
                                    navController.navigate(
                                        Screen.OrganizationDetails.createRoute(
                                            currentOrg.id,
                                            invitationId.ifBlank { "none" }
                                        )
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            currentOrg.imageUri?.takeIf { it.isNotEmpty() }?.let { uri ->
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(uri)
                                            .crossfade(true)
                                            .placeholder(R.drawable.ic_profile_placeholder)
                                            .error(R.drawable.ic_profile_placeholder)
                                            .build(),
                                        onError = { error ->
                                            Log.e(TAG, "Coil Error: ${error.result.throwable.message}")
                                        }
                                    ),
                                    contentDescription = "Organization Logo",
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } ?: Image(
                                painter = painterResource(id = R.drawable.ic_profile_placeholder),
                                contentDescription = "Organization Logo",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentOrg.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentOrg.description.ifEmpty { "Not provided" },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        Text(
                            text = "Key Documents",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OrgDocumentRow(
                            label = TYPE_BUSINESS_PLAN,
                            file = businessPlan,
                            onOpen = { openOrgFile(context, it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OrgDocumentRow(
                            label = TYPE_PITCH_DECK,
                            file = pitchDeck,
                            onOpen = { openOrgFile(context, it) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (currentOrg.partnerIds.isNotEmpty()) {
                        item {
                            Text(
                                text = "Partners",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentOrg.partnerIds.joinToString(", "),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    if (currentOrg.financingDocuments.isNotEmpty()) {
                        item {
                            Text(
                                text = "Financing Documents",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentOrg.financingDocuments.joinToString(", "),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                val currentUser = auth.currentUser
                                if (currentUser == null) {
                                    Log.e(TAG, "No authenticated user found, navigating to SignIn")
                                    navController.navigate(Screen.SignIn.route)
                                } else {
                                    coroutineScope.launch {
                                        try {
                                            startConversationWithCreator(
                                                firestore = firestore,
                                                currentUid = currentUser.uid,
                                                recipientId = currentOrg.creatorId,
                                                requestMessage = "I would like to request the financing documents for your organization ${currentOrg.name}: ${currentOrg.financingDocuments.joinToString(", ")}."
                                            )
                                            val conversationId = conversationIdFor(currentUser.uid, currentOrg.creatorId)
                                            navController.navigate(Screen.PrivateChat.createRoute(conversationId))
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error handling request: ${e.message}", e)
                                            Toast.makeText(
                                                context,
                                                "Failed to send request: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }) {
                                Text("Request Documents")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(onClick = {
                                val currentUser = auth.currentUser
                                if (currentUser == null) {
                                    Log.e(TAG, "No authenticated user found, navigating to SignIn")
                                    navController.navigate(Screen.SignIn.route)
                                } else {
                                    coroutineScope.launch {
                                        try {
                                            startConversationWithCreator(
                                                firestore = firestore,
                                                currentUid = currentUser.uid,
                                                recipientId = currentOrg.creatorId,
                                                requestMessage = null
                                            )
                                            val conversationId = conversationIdFor(currentUser.uid, currentOrg.creatorId)
                                            navController.navigate(Screen.PrivateChat.createRoute(conversationId))
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error creating conversation: ${e.message}", e)
                                            Toast.makeText(
                                                context,
                                                "Failed to start conversation: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.Mail, contentDescription = "Message Organization Creator")
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = currentOrg.imageUri?.let { rememberAsyncImagePainter(it) }
                            ?: painterResource(id = R.drawable.ic_profile_placeholder),
                        contentDescription = "Organization Logo",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentOrg.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentOrg.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (businessPlan != null || pitchDeck != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = listOfNotNull(
                                    businessPlan?.let { TYPE_BUSINESS_PLAN },
                                    pitchDeck?.let { TYPE_PITCH_DECK }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrgDocumentRow(
    label: String,
    file: File?,
    onOpen: (File) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (file != null && file.url.isNotBlank()) {
                        Modifier.clickable { onOpen(file) }
                    } else {
                        Modifier
                    }
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = if (file != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = file?.name ?: "Not uploaded yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (file != null && file.url.isNotBlank()) {
                TextButton(onClick = { onOpen(file) }) {
                    Text("Open")
                }
            }
        }
    }
}

private suspend fun fetchOrgFiles(
    firestore: FirebaseFirestore,
    orgId: String
): List<File> {
    return try {
        val snapshot = firestore.collection("organizations")
            .document(orgId)
            .collection("files")
            .get()
            .await()
        snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(File::class.java)?.copy(fileId = doc.id, orgId = orgId)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing file ${doc.id}: ${e.message}")
                null
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching files for $orgId", e)
        emptyList()
    }
}

private fun latestFileOfType(files: List<File>, type: String): File? {
    return files
        .filter { it.type.equals(type, ignoreCase = true) }
        .maxByOrNull { it.uploadedAt }
}

private fun openOrgFile(context: android.content.Context, file: File) {
    if (file.url.isBlank()) {
        Toast.makeText(context, "File URL not available", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(file.url)))
    } catch (e: Exception) {
        Log.e(TAG, "Error opening file", e)
        Toast.makeText(context, "Failed to open file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun conversationIdFor(uidA: String, uidB: String): String {
    return if (uidA < uidB) "${uidA}_$uidB" else "${uidB}_$uidA"
}

private suspend fun startConversationWithCreator(
    firestore: FirebaseFirestore,
    currentUid: String,
    recipientId: String,
    requestMessage: String?
) {
    val conversationId = conversationIdFor(currentUid, recipientId)
    val conversationData = hashMapOf(
        "senderId" to currentUid,
        "recipientId" to recipientId,
        "participantIds" to listOf(currentUid, recipientId),
        "lastUpdated" to System.currentTimeMillis()
    )
    firestore.collection("conversations")
        .document(conversationId)
        .set(conversationData, com.google.firebase.firestore.SetOptions.merge())
        .await()

    if (!requestMessage.isNullOrBlank()) {
        val messageData = hashMapOf(
            "senderId" to currentUid,
            "content" to requestMessage,
            "timestamp" to System.currentTimeMillis(),
            "read" to false
        )
        firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .add(messageData)
            .await()
        firestore.collection("conversations")
            .document(conversationId)
            .update(
                "lastMessage", requestMessage,
                "lastUpdated", System.currentTimeMillis()
            )
            .await()
    }
}
