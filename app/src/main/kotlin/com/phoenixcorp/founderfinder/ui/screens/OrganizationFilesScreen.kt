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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.phoenixcorp.founderfinder.domain.model.File
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Composable
fun OrganizationFilesScreen(navController: NavHostController, orgId: String?) {
    val context = LocalContext.current
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

    // Fallback to SharedPreferences if orgId is null
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
            Log.d("OrganizationFilesScreen", "Fetching files for orgId: $orgId")
            val snapshot = firestore.collection("organizations")
                .document(orgId)
                .collection("files")
                .get()
                .await()
            files = snapshot.documents.mapNotNull { doc ->
                try {
                    val file = doc.toObject(File::class.java)?.copy(fileId = doc.id, orgId = orgId)
                    Log.d("OrganizationFilesScreen", "File fetched: ${file?.fileId} - ${file?.name}")
                    file
                } catch (e: Exception) {
                    Log.e("OrganizationFilesScreen", "Error parsing file ${doc.id}: ${e.message}", e)
                    null
                }
            }
            isLoading = false
            errorMessage = null
            Log.d("OrganizationFilesScreen", "Fetched ${files.size} files")
        } catch (e: Exception) {
            Log.e("OrganizationFilesScreen", "Error fetching files: ${e.message}", e)
            errorMessage = "Failed to load files: ${e.message}"
            isLoading = false
        }
        Log.d("OrganizationFilesScreen", "State: isLoading=$isLoading, errorMessage=$errorMessage, files=${files.size}")
    }

    // Validate orgId and fetch files
    LaunchedEffect(effectiveOrgId) {
        Log.d("OrganizationFilesScreen", "Received orgId: $orgId, effectiveOrgId: $effectiveOrgId")
        if (effectiveOrgId.isNullOrEmpty()) {
            errorMessage = "No organization selected. Please select an organization."
            isLoading = false
            Log.e("OrganizationFilesScreen", "Invalid orgId, showing error")
            Toast.makeText(context, "Please select an organization", Toast.LENGTH_LONG).show()
            shouldNavigate = true
            return@LaunchedEffect
        }
        fetchFiles(effectiveOrgId!!)
    }

    // Handle navigation after error display
    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate) {
            delay(2000) // Allow time for toast and error to be visible
            Log.d("OrganizationFilesScreen", "Navigating to IdeaCreationScreen")
            navController.navigate(Screen.IdeaCreation.route) {
                popUpTo(navController.graph.startDestinationId) // Clear back stack
                launchSingleTop = true
            }
            shouldNavigate = false
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (isLoading) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading files...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Log.d("OrganizationFilesScreen", "Rendering: Loading indicator")
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch { effectiveOrgId?.let { fetchFiles(it) } }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Retry")
                    }
                    Log.d("OrganizationFilesScreen", "Rendering: Error message - $errorMessage")
                } else {
                    Text(
                        text = "Upload and manage files for your organization",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Log.d("OrganizationFilesScreen", "Rendering: Main content")
                }
            }

            if (!isLoading && errorMessage == null && effectiveOrgId != null) {
                item {
                    // File upload section
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("File Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Box {
                        OutlinedTextField(
                            value = fileType,
                            onValueChange = {},
                            label = { Text("File Type") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { isFileTypeMenuExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select File Type")
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = isFileTypeMenuExpanded,
                            onDismissRequest = { isFileTypeMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Business Plan", "Proposal for Financing", "Other").forEach { type ->
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
                }
                item {
                    Button(onClick = { filePickerLauncher.launch("*/*") }) {
                        Text("Pick File")
                    }
                }

                if (selectedFileUri != null) {
                    item {
                        Text(
                            text = "Selected: ${fileName.ifBlank { "Unnamed File" }}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    item {
                        Button(onClick = {
                            coroutineScope.launch {
                                if (fileName.isBlank()) {
                                    errorMessage = "Please enter a file name."
                                    return@launch
                                }
                                try {
                                    val fileId = UUID.randomUUID().toString()
                                    val storageRef = storage.reference.child("organization_files/$effectiveOrgId/$fileId")
                                    storageRef.putFile(Uri.parse(selectedFileUri)).await()
                                    val fileUrl = storageRef.downloadUrl.await().toString()
                                    val fileData = File(
                                        fileId = fileId,
                                        name = fileName,
                                        url = fileUrl,
                                        uploadedAt = System.currentTimeMillis(),
                                        uploaderId = auth.currentUser?.uid ?: "",
                                        type = fileType,
                                        sharedWith = emptyList(),
                                        orgId = effectiveOrgId!!
                                    )
                                    firestore.collection("organizations")
                                        .document(effectiveOrgId!!)
                                        .collection("files")
                                        .document(fileId)
                                        .set(fileData)
                                        .await()
                                    files = files + fileData
                                    selectedFileUri = null
                                    fileName = ""
                                    fileType = "Business Plan"
                                    errorMessage = null
                                    Toast.makeText(context, "File uploaded successfully", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Log.e("OrganizationFilesScreen", "Error uploading file: ${e.message}", e)
                                    errorMessage = "Failed to upload file: ${e.message}"
                                }
                            }
                        }) {
                            Text("Upload File")
                        }
                    }
                }

                item {
                    // File list
                    Text(
                        text = "Uploaded Files (${files.size})",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                if (files.isEmpty()) {
                    item {
                        Text(
                            text = "No files uploaded yet.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(files) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${file.name} (${file.type})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = TextDecoration.Underline
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { /* Open file URL (e.g., via Intent) */ }
                            )
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    try {
                                        firestore.collection("organizations")
                                            .document(file.orgId)
                                            .collection("files")
                                            .document(file.fileId)
                                            .delete()
                                            .await()
                                        files = files.filter { it.fileId != file.fileId }
                                        Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Log.e("OrganizationFilesScreen", "Error deleting file: ${e.message}", e)
                                        errorMessage = "Failed to delete file: ${e.message}"
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete File")
                            }
                        }
                    }
                }
                item {
                    Button(onClick = {
                        Toast.makeText(context, "Files saved for organization", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}