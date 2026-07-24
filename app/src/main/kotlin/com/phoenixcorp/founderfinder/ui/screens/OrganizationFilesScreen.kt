package com.phoenixcorp.founderfinder.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.phoenixcorp.founderfinder.domain.model.File
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.utils.fetchCurrentUserRole
import com.phoenixcorp.founderfinder.ui.utils.permissionsFor
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Composable
fun OrganizationFilesScreen(navController: NavHostController, orgId: String?) {
    val context = LocalContext.current

    // Spectator permissions
    var role by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        role = fetchCurrentUserRole()
    }
    val perms = remember(role) { permissionsFor(role) }
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFileUri by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileType by remember { mutableStateOf("Business Plan") }
    var isFileTypeMenuExpanded by remember { mutableStateOf(false) }
    var shouldNavigate by remember { mutableStateOf(false) }

    val effectiveOrgId by remember(orgId) {
        mutableStateOf(
            orgId ?: context.getSharedPreferences("founderfinder", Context.MODE_PRIVATE)
                .getString("selectedOrgId", null)
        )
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedFileUri = it.toString() }
    }

    // Fetch files
    suspend fun fetchFiles(orgId: String) {
        try {
            val snapshot = firestore.collection("organizations")
                .document(orgId)
                .collection("files")
                .get()
                .await()

            files = snapshot.documents.mapNotNull { doc ->
                doc.toObject(File::class.java)?.copy(fileId = doc.id, orgId = orgId)
            }
            isLoading = false
        } catch (e: Exception) {
            Log.e("OrganizationFilesScreen", "Error fetching files", e)
            errorMessage = "Failed to load files: ${e.message}"
            isLoading = false
        }
    }

    LaunchedEffect(effectiveOrgId) {
        if (effectiveOrgId.isNullOrEmpty()) {
            errorMessage = "No organization selected."
            isLoading = false
            shouldNavigate = true
            return@LaunchedEffect
        }
        fetchFiles(effectiveOrgId!!)
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
            // Header
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
            } else if (errorMessage != null) {
                item {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { coroutineScope.launch { effectiveOrgId?.let { fetchFiles(it) } } }) {
                        Text("Retry")
                    }
                }
            } else {
                // Upload Section
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
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // File Type Dropdown
                            Box {
                                OutlinedTextField(
                                    value = fileType,
                                    onValueChange = {},
                                    label = { Text("File Type") },
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = { isFileTypeMenuExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, null)
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
                                enabled = perms.canCreate
                            ) {
                                Text(if (perms.canCreate) "Select File" else "View only")
                            }

                            if (selectedFileUri != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Selected: ${fileName.ifBlank { "Unnamed File" }}", style = MaterialTheme.typography.bodyMedium)

                                Button(
                                    onClick = {
                                        if (!perms.requireCreate(context, "upload a file")) return@Button
                                        /* Upload logic remains the same */
                                        // (Keep your existing upload coroutine here)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = perms.canCreate
                                ) {
                                    Text("Upload File")
                                }
                            }
                        }
                    }
                }

                // Uploaded Files Section
                item {
                    Text(
                        text = "Uploaded Files (${files.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                if (files.isEmpty()) {
                    item {
                        Text("No files uploaded yet. Start building credibility with investors!", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    items(files) { file ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.name, style = MaterialTheme.typography.titleMedium)
                                    Text(file.type, style = MaterialTheme.typography.bodyMedium)
                                }

                                if (perms.canDelete) {
                                    IconButton(onClick = {
                                        if (!perms.requireUpdate(context, "delete a file")) return@IconButton
                                        /* Delete logic */
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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