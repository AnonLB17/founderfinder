package com.phoenixcorp.founderfinder.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.phoenixcorp.founderfinder.domain.model.File
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import com.phoenixcorp.founderfinder.ui.utils.fetchCurrentUserRole
import com.phoenixcorp.founderfinder.ui.utils.permissionsFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "OrganizationFilesScreen"
private const val PREFS_NAME = "founderfinder"
private const val PREF_SELECTED_ORG = "selectedOrgId"

@Composable
fun OrganizationFilesScreen(navController: NavHostController, orgId: String?) {
    val context = LocalContext.current

    var role by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        role = fetchCurrentUserRole()
    }
    val perms = remember(role) { permissionsFor(role) }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    var deletingFileId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileType by remember { mutableStateOf("Business Plan") }
    var isFileTypeMenuExpanded by remember { mutableStateOf(false) }
    var shouldNavigate by remember { mutableStateOf(false) }

    val effectiveOrgId = remember(orgId) {
        resolveOrgId(orgId, context)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        selectedFileUri = uri
        if (fileName.isBlank()) {
            fileName = queryDisplayName(context, uri) ?: "Untitled"
        }
    }

    suspend fun fetchFiles(targetOrgId: String) {
        try {
            val snapshot = firestore.collection("organizations")
                .document(targetOrgId)
                .collection("files")
                .get()
                .await()

            files = snapshot.documents.mapNotNull { doc ->
                doc.toObject(File::class.java)?.copy(fileId = doc.id, orgId = targetOrgId)
            }.sortedByDescending { it.uploadedAt }
            errorMessage = null
            isLoading = false
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching files", e)
            errorMessage = "Failed to load files: ${e.message}"
            isLoading = false
        }
    }

    suspend fun uploadSelectedFile() {
        if (!perms.requireCreate(context, "upload a file")) return

        val targetOrgId = effectiveOrgId
        val uri = selectedFileUri
        val currentUser = auth.currentUser

        when {
            targetOrgId.isNullOrBlank() -> {
                Toast.makeText(context, "No organization selected.", Toast.LENGTH_SHORT).show()
                return
            }
            currentUser == null -> {
                Toast.makeText(context, "Please sign in to upload.", Toast.LENGTH_SHORT).show()
                return
            }
            uri == null -> {
                Toast.makeText(context, "Select a file first.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val displayName = fileName.trim().ifBlank {
            queryDisplayName(context, uri!!) ?: "Untitled"
        }
        val mimeType = context.contentResolver.getType(uri!!) ?: "application/octet-stream"
        val fileId = UUID.randomUUID().toString()
        // Must match Storage rules: organization_files/{orgId}/{fileId}
        val storagePath = "organization_files/$targetOrgId/$fileId"

        isUploading = true
        try {
            val storageRef = storage.reference.child(storagePath)
            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("orgId", targetOrgId)
                .setCustomMetadata("uploadedBy", currentUser!!.uid)
                .build()

            storageRef.putFile(uri, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val record = File(
                fileId = fileId,
                orgId = targetOrgId!!,
                name = displayName,
                type = fileType,
                url = downloadUrl,
                storagePath = storagePath,
                mimeType = mimeType,
                uploadedBy = currentUser.uid,
                uploadedAt = System.currentTimeMillis()
            )

            firestore.collection("organizations")
                .document(targetOrgId)
                .collection("files")
                .document(fileId)
                .set(record)
                .await()

            files = (listOf(record) + files).distinctBy { it.fileId }
            selectedFileUri = null
            fileName = ""
            Toast.makeText(context, "Uploaded \"$displayName\"", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isUploading = false
        }
    }

    suspend fun deleteFile(file: File) {
        if (!perms.requireUpdate(context, "delete a file")) return
        val targetOrgId = file.orgId.ifBlank { effectiveOrgId } ?: return
        deletingFileId = file.fileId
        try {
            if (file.storagePath.isNotBlank()) {
                try {
                    storage.reference.child(file.storagePath).delete().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Storage delete skipped for ${file.fileId}: ${e.message}")
                }
            }
            firestore.collection("organizations")
                .document(targetOrgId)
                .collection("files")
                .document(file.fileId)
                .delete()
                .await()
            files = files.filterNot { it.fileId == file.fileId }
            Toast.makeText(context, "Deleted \"${file.name}\"", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed", e)
            Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            deletingFileId = null
        }
    }

    LaunchedEffect(effectiveOrgId) {
        if (effectiveOrgId.isNullOrEmpty()) {
            errorMessage = "No organization selected."
            isLoading = false
            shouldNavigate = true
            return@LaunchedEffect
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_SELECTED_ORG, effectiveOrgId)
            .apply()
        fetchFiles(effectiveOrgId)
    }

    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate) {
            delay(1500)
            navController.navigate(Screen.IdeaCreation.route) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = { Text("Organization Files") },
                navController = navController,
                showBackButton = true
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Organization Files",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Share documents with potential partners and investors",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLoading) {
                item { CircularProgressIndicator() }
            } else if (errorMessage != null && effectiveOrgId.isNullOrEmpty()) {
                item {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { coroutineScope.launch { effectiveOrgId?.let { fetchFiles(it) } } }) {
                        Text("Retry")
                    }
                }
            } else {
                if (errorMessage != null) {
                    item {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Upload New File", style = MaterialTheme.typography.titleLarge)

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = fileName,
                                onValueChange = { fileName = it },
                                label = { Text("File Name") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = perms.canCreate && !isUploading
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Box {
                                OutlinedTextField(
                                    value = fileType,
                                    onValueChange = {},
                                    label = { Text("File Type") },
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    enabled = perms.canCreate && !isUploading,
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { isFileTypeMenuExpanded = true },
                                            enabled = perms.canCreate && !isUploading
                                        ) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose type")
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = isFileTypeMenuExpanded,
                                    onDismissRequest = { isFileTypeMenuExpanded = false }
                                ) {
                                    listOf(
                                        "Business Plan", "Pitch Deck", "Financial Projections",
                                        "Market Research", "Product Roadmap", "Marketing Materials",
                                        "Team Bios / Resumes", "Legal Documents", "Prototype Images/Video",
                                        "Letter of Intent", "Other"
                                    ).forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                fileType = type
                                                isFileTypeMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (!perms.requireCreate(context, "upload a file")) return@Button
                                    filePickerLauncher.launch("*/*")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = perms.canCreate && !isUploading
                            ) {
                                Text(if (perms.canCreate) "Select File" else "View only")
                            }

                            if (selectedFileUri != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Selected: ${fileName.ifBlank { "Unnamed File" }}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        coroutineScope.launch { uploadSelectedFile() }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = perms.canCreate && !isUploading
                                ) {
                                    if (isUploading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Uploading…")
                                    } else {
                                        Text("Upload File")
                                    }
                                }
                            }

                            if (isUploading) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Uploaded Files (${files.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                if (files.isEmpty()) {
                    item {
                        Text(
                            "No files uploaded yet. Start building credibility with investors!",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    items(files, key = { it.fileId }) { file ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = file.url.isNotBlank()) {
                                    openFileUrl(context, file)
                                },
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        file.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(file.type, style = MaterialTheme.typography.bodyMedium)
                                    if (file.uploadedAt > 0L) {
                                        Text(
                                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                                .format(Date(file.uploadedAt)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (perms.canDelete) {
                                    if (deletingFileId == file.fileId) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        IconButton(onClick = {
                                            coroutineScope.launch { deleteFile(file) }
                                        }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun resolveOrgId(orgId: String?, context: Context): String? {
    val fromArg = orgId?.takeIf { it.isNotBlank() && it != "{orgId}" }
    if (!fromArg.isNullOrBlank()) return fromArg
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(PREF_SELECTED_ORG, null)
        ?.takeIf { it.isNotBlank() }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            } ?: uri.lastPathSegment
    } catch (_: Exception) {
        uri.lastPathSegment
    }
}

private fun openFileUrl(context: Context, file: File) {
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
