package com.phoenixcorp.founderfinder.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.domain.model.Activity
import com.phoenixcorp.founderfinder.domain.model.Organization
import com.phoenixcorp.founderfinder.domain.model.UserProfile
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.BottomNavigationBar
import com.phoenixcorp.founderfinder.ui.components.CalendarSection
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PartnersScreen(navController: NavHostController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val coroutineScope = rememberCoroutineScope()
    var organizations by remember { mutableStateOf<List<Organization>>(emptyList()) }
    var partnersByOrg by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var userProfiles by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
    var activities by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedOrgId by remember { mutableStateOf<String?>(null) }
    var selectedPartnerId by remember { mutableStateOf<String?>(null) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var showActivityInput by remember { mutableStateOf(false) }
    var activityTitle by remember { mutableStateOf("") }
    var activityDescription by remember { mutableStateOf("") }
    var selectedTimeSlot by remember { mutableStateOf<String?>(null) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    var activityListener: ListenerRegistration? by remember { mutableStateOf(null) }
    var orgActivityListeners by remember { mutableStateOf<Map<String, ListenerRegistration>>(emptyMap()) }
    var lastNavigatedOrgId by remember { mutableStateOf<String?>(null) }

    // Time slots
    val timeSlots = listOf(
        "00:00", "00:30", "01:00", "01:30", "02:00", "02:30", "03:00", "03:30",
        "04:00", "04:30", "05:00", "05:30", "06:00", "06:30", "07:00", "07:30",
        "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
        "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30",
        "16:00", "16:30", "17:00", "17:30", "18:00", "18:30", "19:00", "19:30",
        "20:00", "20:30", "21:00", "21:30", "22:00", "22:30", "23:00", "23:30"
    )

    // Fetch data
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            errorMessage = "Please sign in."
            isLoading = false
            Toast.makeText(context, "Please sign in", Toast.LENGTH_SHORT).show()
            navController.navigate(Screen.SignIn.route)
            return@LaunchedEffect
        }
        try {
            Log.d("PartnersScreen", "Fetching organizations for user: ${currentUser.uid}")
            // Fetch created organizations
            val createdOrgSnapshot = firestore.collection("organizations")
                .whereEqualTo("creatorId", currentUser.uid)
                .get()
                .await()
            val createdOrgs = createdOrgSnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Organization::class.java)?.copy(orgId = doc.id)
                } catch (e: Exception) {
                    Log.e("PartnersScreen", "Error parsing created organization ${doc.id}: ${e.message}")
                    null
                }
            }
            Log.d("PartnersScreen", "Fetched ${createdOrgs.size} created organizations")

            // Fetch invited organizations
            val invitationSnapshot = firestore.collection("invitations")
                .whereEqualTo("inviteeId", currentUser.uid)
                .get()
                .await()
            val invitedOrgIds = invitationSnapshot.documents.mapNotNull { doc ->
                doc.getString("orgId")
            }.distinct()

            // Fetch organizations where user is a collaborator
            val collaboratorOrgIds = mutableListOf<String>()
            val orgSnapshot = firestore.collection("organizations").get().await()
            for (orgDoc in orgSnapshot.documents) {
                val orgId = orgDoc.id
                val collaboratorDoc = firestore.collection("organizations")
                    .document(orgId)
                    .collection("collaborators")
                    .document(currentUser.uid)
                    .get()
                    .await()
                if (collaboratorDoc.exists()) {
                    collaboratorOrgIds.add(orgId)
                }
            }
            val allInvitedOrgIds = (invitedOrgIds + collaboratorOrgIds).distinct()
            Log.d("PartnersScreen", "Fetched ${allInvitedOrgIds.size} invited orgIds: $allInvitedOrgIds")

            val invitedOrgs = if (allInvitedOrgIds.isNotEmpty()) {
                firestore.collection("organizations")
                    .whereIn("orgId", allInvitedOrgIds.take(10))
                    .get()
                    .await()
                    .documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Organization::class.java)?.copy(orgId = doc.id)
                        } catch (e: Exception) {
                            Log.e("PartnersScreen", "Error parsing invited organization ${doc.id}: ${e.message}")
                            null
                        }
                    }
            } else {
                emptyList()
            }

            // Merge organizations
            organizations = (createdOrgs + invitedOrgs).distinctBy { it.orgId }
            Log.d("PartnersScreen", "Total fetched ${organizations.size} organizations")

            // Fetch partners for each organization
            val partnersMap = mutableMapOf<String, List<String>>()
            organizations.forEach { org ->
                try {
                    Log.d("PartnersScreen", "Fetching partners for organization: ${org.orgId}")
                    val partnerSnapshot = firestore.collection("organizations")
                        .document(org.orgId)
                        .collection("partners")
                        .get()
                        .await()
                    val partnerIds = partnerSnapshot.documents.mapNotNull { it.id }
                    val collaboratorSnapshot = firestore.collection("organizations")
                        .document(org.orgId)
                        .collection("collaborators")
                        .get()
                        .await()
                    val collaboratorIds = collaboratorSnapshot.documents.mapNotNull { it.id }
                    val allPartnerIds = (partnerIds + collaboratorIds + listOf(org.creatorId)).distinct()
                    partnersMap[org.orgId] = allPartnerIds
                    Log.d("PartnersScreen", "Org ${org.orgId} collaborators: $allPartnerIds")
                } catch (e: Exception) {
                    Log.e("PartnersScreen", "Error fetching partners for ${org.orgId}: ${e.message}")
                }
            }
            partnersByOrg = partnersMap
            Log.d("PartnersScreen", "Fetched partners: $partnersMap")

            // Fetch user profiles
            val partnerIds = partnersMap.values.flatten().distinct()
            val profiles = mutableMapOf<String, UserProfile>()
            partnerIds.forEach { id ->
                try {
                    Log.d("PartnersScreen", "Fetching profile for user: $id")
                    val profileDoc = firestore.collection("profiles").document(id).get().await()
                    profileDoc.toObject(UserProfile::class.java)?.let { profiles[id] = it }
                } catch (e: Exception) {
                    Log.e("PartnersScreen", "Error fetching profile $id: ${e.message}")
                }
            }
            userProfiles = profiles
            Log.d("PartnersScreen", "Fetched ${profiles.size} profiles")

            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Failed to load data: ${e.message}"
            isLoading = false
            Log.e("PartnersScreen", "General error: ${e.message}", e)
        }
    }

    // Real-time activity listeners
    LaunchedEffect(organizations, currentUser) {
        if (currentUser == null) return@LaunchedEffect
        val orgIds = organizations.map { it.orgId }
        activityListener?.remove()
        orgActivityListeners.values.forEach { it.remove() }
        val newListeners = mutableMapOf<String, ListenerRegistration>()

        // User activities listener
        activityListener = firestore.collectionGroup("activities")
            .whereEqualTo("creatorId", currentUser.uid)
            .addSnapshotListener { userSnapshot, userError ->
                if (userError != null) {
                    Log.e("PartnersScreen", "User activity listener error: ${userError.message}", userError)
                    errorMessage = "Failed to load user activities: ${userError.message}"
                    return@addSnapshotListener
                }
                if (userSnapshot == null) {
                    Log.w("PartnersScreen", "User activity snapshot is null")
                    return@addSnapshotListener
                }
                val userActivities = userSnapshot.documents.mapNotNull { doc ->
                    try {
                        val activity = doc.toObject(Activity::class.java)?.copy(id = doc.id)
                        Log.d("PartnersScreen", "Real-time user activity ${doc.id}: $activity")
                        activity
                    } catch (e: Exception) {
                        Log.e("PartnersScreen", "Error parsing user activity ${doc.id}: ${e.message}")
                        null
                    }
                }
                activities = (activities + userActivities).distinctBy { it.id }
                Log.d("PartnersScreen", "Updated activities: ${activities.size} activities")
            }

        // Organization activities listeners
        orgIds.forEach { orgId ->
            val listener = firestore.collection("organizations")
                .document(orgId)
                .collection("activities")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("PartnersScreen", "Org activity listener error for $orgId: ${error.message}", error)
                        errorMessage = "Failed to load org activities: ${error.message}"
                        return@addSnapshotListener
                    }
                    if (snapshot == null) {
                        Log.w("PartnersScreen", "Org activity snapshot is null for $orgId")
                        return@addSnapshotListener
                    }
                    val orgActivities = snapshot.documents.mapNotNull { doc ->
                        try {
                            val activity = doc.toObject(Activity::class.java)?.copy(id = doc.id)
                            Log.d("PartnersScreen", "Real-time org activity ${doc.id}: $activity")
                            activity
                        } catch (e: Exception) {
                            Log.e("PartnersScreen", "Error parsing org activity ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    activities = (activities + orgActivities).distinctBy { it.id }
                    Log.d("PartnersScreen", "Updated activities for org $orgId: ${activities.size} activities")
                }
            newListeners[orgId] = listener
        }
        orgActivityListeners = newListeners
    }

    // Default to current user's activities
    LaunchedEffect(currentUser, selectedOrgId, selectedPartnerId) {
        if (currentUser != null && selectedOrgId == null && selectedPartnerId == null) {
            selectedPartnerId = currentUser.uid
        }
    }

    // Auto-select organization
    LaunchedEffect(organizations) {
        if (selectedOrgId == null && organizations.isNotEmpty()) {
            selectedOrgId = organizations.first().orgId
            Log.d("PartnersScreen", "Auto-selected organization: ${organizations.first().orgId}")
        }
    }

    // Calendar title
    val calendarTitle = when {
        selectedOrgId != null -> {
            val orgName = organizations.find { it.orgId == selectedOrgId }?.name ?: "Organization"
            "$orgName Calendar"
        }
        selectedPartnerId != null -> {
            val userName = userProfiles[selectedPartnerId]?.firstName ?: "Anonymous"
            "$userName Calendar"
        }
        else -> {
            val userName = userProfiles[currentUser?.uid]?.firstName ?: "Your"
            "$userName Calendar"
        }
    }

    Scaffold(
        topBar = { ScreenBanner(title = { Text("Partners") }, navController = navController, showBackButton = true) },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 16.dp)
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 16.dp)
                )
            } else if (organizations.isEmpty()) {
                Text(
                    text = "No organizations found. Create or join an organization to see partners.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 16.dp)
                )
            } else {
                // Organizations and Partners
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(organizations) { org ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Organization Photo
                            Image(
                                painter = org.imageUri?.takeIf { it.isNotEmpty() }?.let {
                                    rememberAsyncImagePainter(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(it)
                                            .crossfade(true)
                                            .placeholder(R.drawable.ic_placeholder)
                                            .error(R.drawable.ic_placeholder)
                                            .build(),
                                        onError = { error -> Log.e("PartnersScreen", "Coil Error: ${error.result.throwable.message}") }
                                    )
                                } ?: painterResource(id = R.drawable.ic_placeholder),
                                contentDescription = "Organization",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .padding(8.dp)
                                    .pointerInput(org.orgId) {
                                        detectTapGestures(
                                            onTap = {
                                                selectedOrgId = org.orgId
                                                selectedPartnerId = null
                                                Log.d("PartnersScreen", "Tapped organization: ${org.orgId}, showing calendar")
                                            },
                                            onLongPress = {
                                                if (lastNavigatedOrgId != org.orgId) {
                                                    lastNavigatedOrgId = org.orgId
                                                    Log.d("PartnersScreen", "Long pressed organization: ${org.orgId}, navigating to group chat")
                                                    navController.navigate(Screen.GroupChat.createRoute(org.orgId)) {
                                                        launchSingleTop = true
                                                        restoreState = true
                                                        popUpTo(Screen.Partners.route) { inclusive = false }
                                                    }
                                                } else {
                                                    Log.d("PartnersScreen", "Skipping redundant navigation to group chat for orgId: ${org.orgId}")
                                                }
                                            }
                                        )
                                    }
                            )

                            // Partners Profile Pictures
                            partnersByOrg[org.orgId]?.forEach { partnerId ->
                                val profile = userProfiles[partnerId]
                                Image(
                                    painter = profile?.profilePicture?.takeIf { it.isNotEmpty() }?.let {
                                        rememberAsyncImagePainter(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(it)
                                                .crossfade(true)
                                                .placeholder(R.drawable.ic_profile_placeholder)
                                                .error(R.drawable.ic_profile_placeholder)
                                                .build(),
                                            onError = { error -> Log.e("PartnersScreen", "Coil Error: ${error.result.throwable.message}") }
                                        )
                                    } ?: painterResource(id = R.drawable.ic_profile_placeholder),
                                    contentDescription = "Partner Profile",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .padding(8.dp)
                                        .pointerInput(partnerId) {
                                            detectTapGestures(
                                                onTap = {
                                                    selectedPartnerId = partnerId
                                                    selectedOrgId = null
                                                    Log.d("PartnersScreen", "Tapped partner: $partnerId, showing calendar")
                                                },
                                                onLongPress = {
                                                    selectedPartnerId = partnerId
                                                    selectedOrgId = null
                                                    Log.d("PartnersScreen", "Long pressed partner: $partnerId, navigating to private chat")
                                                    val conversationId = if (currentUser?.uid != null && currentUser.uid < partnerId) {
                                                        "${currentUser.uid}_$partnerId"
                                                    } else {
                                                        "${partnerId}_${currentUser?.uid}"
                                                    }
                                                    navController.navigate(Screen.PrivateChat.createRoute(conversationId)) {
                                                        launchSingleTop = true
                                                        restoreState = true
                                                        popUpTo(Screen.Partners.route) { inclusive = false }
                                                    }
                                                }
                                            )
                                        }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Calendar Section
                CalendarSection(
                    currentMonth = currentMonth,
                    activities = activities.filter { activity ->
                        when {
                            selectedOrgId != null -> activity.organizationId == selectedOrgId
                            selectedPartnerId != null -> activity.creatorId == selectedPartnerId
                            currentUser != null -> activity.creatorId == currentUser.uid
                            else -> false
                        }
                    },
                    timeSlots = timeSlots,
                    calendarTitle = calendarTitle,
                    onDayTap = { day ->
                        selectedDay = day
                        showActivityInput = false
                        Log.d("PartnersScreen", "Tapped day: $day")
                    },
                    onDayLongPress = { day ->
                        selectedDay = day
                        if (selectedOrgId == null && organizations.isNotEmpty()) {
                            selectedOrgId = organizations.first().orgId
                            Toast.makeText(context, "Selected organization: ${organizations.first().name}", Toast.LENGTH_SHORT).show()
                        }
                        showActivityInput = true
                        Log.d("PartnersScreen", "Long pressed day: $day, showActivityInput: $showActivityInput")
                    },
                    onPreviousMonth = {
                        currentMonth = Calendar.getInstance().apply {
                            time = currentMonth.time
                            add(Calendar.MONTH, -1)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                    },
                    onNextMonth = {
                        currentMonth = Calendar.getInstance().apply {
                            time = currentMonth.time
                            add(Calendar.MONTH, 1)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Activity Input or Details
                if (selectedDay != null) {
                    var dayActivities = activities.filter { activity ->
                        val activityDate = Calendar.getInstance().apply { timeInMillis = activity.date }
                        activityDate.get(Calendar.DAY_OF_MONTH) == selectedDay &&
                                activityDate.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) &&
                                activityDate.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
                                (when {
                                    selectedOrgId != null -> activity.organizationId == selectedOrgId
                                    selectedPartnerId != null -> activity.creatorId == selectedPartnerId
                                    currentUser != null -> activity.creatorId == currentUser.uid
                                    else -> false
                                })
                    }.sortedBy { it.time } // Sort by time (earliest to latest)
                    if (showActivityInput) {
                        OutlinedTextField(
                            value = activityTitle,
                            onValueChange = { activityTitle = it },
                            label = { Text("Activity Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = activityDescription,
                            onValueChange = { activityDescription = it },
                            label = { Text("Activity Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select Time Slot",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (timeSlots.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(timeSlots) { slot ->
                                    val isBooked = dayActivities.any { it.time == slot }
                                    Button(
                                        onClick = { if (!isBooked) selectedTimeSlot = slot },
                                        enabled = !isBooked,
                                        modifier = Modifier.width(100.dp)
                                    ) {
                                        Text(slot)
                                    }
                                }
                            }
                        } else {
                            Text("No time slots available")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                Log.d("PartnersScreen", "Add Activity clicked: title=$activityTitle, description=$activityDescription, timeSlot=$selectedTimeSlot")
                                if (currentUser != null && activityTitle.isNotBlank() && selectedTimeSlot != null && selectedOrgId != null) {
                                    coroutineScope.launch {
                                        try {
                                            val activityDate = Calendar.getInstance().apply {
                                                time = currentMonth.time
                                                set(Calendar.DAY_OF_MONTH, selectedDay!!)
                                            }.timeInMillis
                                            val activity = Activity(
                                                title = activityTitle,
                                                description = activityDescription,
                                                partnerId = selectedPartnerId ?: currentUser.uid,
                                                date = activityDate,
                                                time = selectedTimeSlot,
                                                creatorId = currentUser.uid,
                                                orgId = selectedOrgId,
                                                organizationId = selectedOrgId
                                            )
                                            Log.d("PartnersScreen", "Adding activity: $activity to org: $selectedOrgId")
                                            val docRef = firestore.collection("organizations")
                                                .document(selectedOrgId!!)
                                                .collection("activities")
                                                .add(activity)
                                                .await()
                                            Log.d("PartnersScreen", "Activity added with ID: ${docRef.id}")
                                            activityTitle = ""
                                            activityDescription = ""
                                            selectedTimeSlot = null
                                            showActivityInput = false
                                            Toast.makeText(context, "Activity added", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Log.e("PartnersScreen", "Failed to add activity: ${e.message}", e)
                                            Toast.makeText(context, "Failed to add activity: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    val reason = when {
                                        activityTitle.isBlank() -> "Title is empty"
                                        selectedTimeSlot == null -> "No time slot selected"
                                        selectedOrgId == null -> "No organization selected"
                                        else -> "Unknown validation failure"
                                    }
                                    Log.w("PartnersScreen", "Validation failed: $reason")
                                    Toast.makeText(context, "Please select an organization, enter a title, and select a time slot", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = activityTitle.isNotBlank() && selectedTimeSlot != null && selectedOrgId != null
                        ) {
                            Text("Add Activity")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        if (dayActivities.isNotEmpty()) {
                            Text(
                                text = "Activities for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(
                                    Calendar.getInstance().apply {
                                        time = currentMonth.time
                                        set(Calendar.DAY_OF_MONTH, selectedDay!!)
                                    }.time
                                )}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            dayActivities.forEach { activity ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${activity.time ?: "No time"}: ${activity.title}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    activity.description?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        } else {
                            Text(
                                text = "No activities for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(
                                    Calendar.getInstance().apply {
                                        time = currentMonth.time
                                        set(Calendar.DAY_OF_MONTH, selectedDay!!)
                                    }.time
                                )}",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activityListener?.remove()
            orgActivityListeners.values.forEach { it.remove() }
            Log.d("PartnersScreen", "Firestore listeners removed")
        }
    }
}