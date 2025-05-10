package com.phoenixcorp.founderfinder.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.data.OrganizationRepositoryImpl
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner

@Composable
fun IdeaCreationScreen(navController: NavHostController) {
    val viewModel: IdeaCreationViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return IdeaCreationViewModel(OrganizationRepositoryImpl()) as T
            }
        }
    )

    val organizations by viewModel.organizations.collectAsState()
    val isCreatingOrganization by viewModel.isCreatingOrganization.collectAsState()
    val selectedOrganization by viewModel.selectedOrganization.collectAsState()
    val businessName by viewModel.businessName.collectAsState()
    val ideaDescription by viewModel.ideaDescription.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setSelectedImageUri(it.toString()) }
    }

    val selectedImageBitmap by remember(selectedImageUri) {
        mutableStateOf(
            viewModel.selectedImageUri.value?.let { uri ->
                try {
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, android.net.Uri.parse(uri))?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
        )
    }

    Scaffold(
        topBar = {
            ScreenBanner(
                title = "Idea Creation",
                navController = navController,
                showBackButton = true
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                organizations.forEach { org ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        val bitmap = org.getImageBitmap(context)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Organization Image",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clickable { viewModel.setSelectedOrganization(org) }
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_placeholder),
                                contentDescription = "Placeholder Image",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clickable { viewModel.setSelectedOrganization(org) }
                            )
                        }
                        Text(org.name, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(50.dp)
                        .clickable { viewModel.setCreatingOrganization(true) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "+",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            if (selectedOrganization != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Organization Details",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                val bitmap = selectedOrganization!!.getImageBitmap(context)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Selected Organization Image",
                        modifier = Modifier.size(100.dp)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_placeholder),
                        contentDescription = "Placeholder Image",
                        modifier = Modifier.size(100.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Text("Pick New Image")
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { viewModel.setBusinessName(it) },
                    label = { Text("Business Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = ideaDescription,
                    onValueChange = { viewModel.setIdeaDescription(it) },
                    label = { Text("Idea Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.updateOrganization() }) {
                    Text("Submit")
                }
            } else if (isCreatingOrganization) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Add an image that represents your idea or a business logo",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageBitmap != null) {
                        Image(
                            bitmap = selectedImageBitmap!!,
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_placeholder),
                            contentDescription = "Select Image",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Text("Pick Image")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Enter a title for your idea or a business name",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { viewModel.setBusinessName(it) },
                    label = { Text("Business Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Add a brief description of your idea which will be the first thing that potential partners will read.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = ideaDescription,
                    onValueChange = { viewModel.setIdeaDescription(it) },
                    label = { Text("Idea Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.submitOrganization() }) {
                    Text("Submit")
                }
            }

            // Display error message if any
            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate(Screen.BusinessPlan.route) }) {
                Text("Next")
            }
        }
    }
}