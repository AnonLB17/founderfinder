package com.phoenixcorp.founderfinder.ui.screens

import android.content.Context
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
import com.phoenixcorp.founderfinder.workers.ReminderWorker
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Data
import java.util.concurrent.TimeUnit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.phoenixcorp.founderfinder.workers.ActivityReminderWorker
import androidx.hilt.navigation.compose.hiltViewModel
import com.phoenixcorp.founderfinder.domain.repository.NotificationRepository
import com.phoenixcorp.founderfinder.ui.viewmodel.notifications.NotificationsViewModel

@Composable
fun PartnersScreen(
    navController: NavHostController,
    notificationsViewModel: NotificationsViewModel = hiltViewModel()   // ← Changed to ViewModel
) {
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
                    doc.toObject(Organization::class.java)?.copy(id = doc.id)
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
                            doc.toObject(Organization::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e("PartnersScreen", "Error parsing invited organization ${doc.id}: ${e.message}")
                            null
                        }
                    }
            } else {
                emptyList()
            }

            // Merge organizations
            organizations = (createdOrgs + invitedOrgs).distinctBy { it.id }
            Log.d("PartnersScreen", "Total fetched ${organizations.size} organizations")

            // Fetch partners for each organization
            val partnersMap = mutableMapOf<String, List<String>>()
            organizations.forEach { org ->
                try {
                    Log.d("PartnersScreen", "Fetching partners for organization: ${org.id}")
                    val partnerSnapshot = firestore.collection("organizations")
                        .document(org.id)
                        .collection("partners")
                        .get()
                        .await()
                    val partnerIds = partnerSnapshot.documents.mapNotNull { it.id }
                    val collaboratorSnapshot = firestore.collection("organizations")
                        .document(org.id)
                        .collection("collaborators")
                        .get()
                        .await()
                    val collaboratorIds = collaboratorSnapshot.documents.mapNotNull { it.id }
                    val allPartnerIds = (partnerIds + collaboratorIds + listOf(org.creatorId)).distinct()
                    partnersMap[org.id] = allPartnerIds
                    Log.d("PartnersScreen", "Org ${org.id} collaborators: $allPartnerIds")
                } catch (e: Exception) {
                    Log.e("PartnersScreen", "Error fetching partners for ${org.id}: ${e.message}")
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

    // Handle deep link from notification / reminder
    LaunchedEffect(navController) {
        val highlightActivityId = navController.currentBackStackEntry
            ?.arguments
            ?.getString("highlightActivity")

        if (highlightActivityId != null) {
            Log.d("PartnersScreen", "Highlighting activity: $highlightActivityId")

            // Find the activity
            val targetActivity = activities.find { it.id == highlightActivityId }

            if (targetActivity != null) {
                // Switch to correct context (org or personal)
                if (targetActivity.isOrganizationActivity && targetActivity.organizationId != null) {
                    selectedOrgId = targetActivity.organizationId
                    selectedPartnerId = null
                } else {
                    selectedPartnerId = targetActivity.creatorId
                    selectedOrgId = null
                }

                // Auto-select the day of the activity
                val activityCal = Calendar.getInstance().apply {
                    timeInMillis = targetActivity.date
                }
                selectedDay = activityCal.get(Calendar.DAY_OF_MONTH)

                Log.d("PartnersScreen", "Auto-selected day: $selectedDay for activity ${targetActivity.id}")
            }
        }
    }

    // Real-time activity listeners
    LaunchedEffect(organizations, currentUser) {
        if (currentUser == null) return@LaunchedEffect
        val orgIds = organizations.map { it.id }
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
            selectedOrgId = organizations.first().id
            Log.d("PartnersScreen", "Auto-selected organization: ${organizations.first().id}")
        }
    }

    // Calendar title
    val calendarTitle = when {
        selectedOrgId != null -> {
            val orgName = organizations.find { it.id == selectedOrgId }?.name ?: "Organization"
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
                                    .pointerInput(org.id) {
                                        detectTapGestures(
                                            onTap = {
                                                selectedOrgId = org.id
                                                selectedPartnerId = null
                                                Log.d("PartnersScreen", "Tapped organization: ${org.id}, showing calendar")
                                            },
                                            onLongPress = {
                                                if (lastNavigatedOrgId != org.id) {
                                                    lastNavigatedOrgId = org.id
                                                    Log.d("PartnersScreen", "Long pressed organization: ${org.id}, navigating to group chat")
                                                    navController.navigate(Screen.GroupChat.createRoute(org.id)) {
                                                        launchSingleTop = true
                                                        restoreState = true
                                                        popUpTo(Screen.Partners.route) { inclusive = false }
                                                    }
                                                } else {
                                                    Log.d("PartnersScreen", "Skipping redundant navigation to group chat for orgId: ${org.id}")
                                                }
                                            }
                                        )
                                    }
                            )

                            // Partners Profile Pictures
                            partnersByOrg[org.id]?.forEach { partnerId ->
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
                            selectedPartnerId != null -> activity.creatorId == selectedPartnerId
                            selectedOrgId != null -> activity.organizationId == selectedOrgId || activity.orgId == selectedOrgId
                            currentUser != null -> activity.creatorId == currentUser.uid
                            else -> false
                        }
                    },
                    timeSlots = timeSlots,
                    calendarTitle = calendarTitle,
                    selectedDay = selectedDay,                    // ← Make sure this is passed
                    onDayTap = { day ->
                        selectedDay = day
                        showActivityInput = false
                        Log.d("PartnersScreen", "Tapped day: $day")
                    },
                    onDayLongPress = { day ->
                        selectedDay = day

                        if (selectedOrgId == null && selectedPartnerId == null && organizations.isNotEmpty()) {
                            selectedOrgId = organizations.first().id
                            Toast.makeText(context, "Selected organization: ${organizations.first().name}", Toast.LENGTH_SHORT).show()
                        }

                        showActivityInput = true
                        Log.d("PartnersScreen", "Long pressed day: $day | Org: $selectedOrgId | Partner: $selectedPartnerId")
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
                    val dayActivities = activities.filter { activity ->
                        val activityDate = Calendar.getInstance().apply { timeInMillis = activity.date }
                        val matchesDate = activityDate.get(Calendar.DAY_OF_MONTH) == selectedDay &&
                                activityDate.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) &&
                                activityDate.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR)

                        val matchesContext = when {
                            selectedOrgId != null -> activity.isOrganizationActivity &&
                                    (activity.organizationId == selectedOrgId || activity.orgId == selectedOrgId)
                            selectedPartnerId != null -> activity.creatorId == selectedPartnerId && !activity.isOrganizationActivity
                            else -> !activity.isOrganizationActivity && activity.creatorId == currentUser?.uid
                        }

                        matchesDate && matchesContext
                    }.sortedBy { it.time }

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
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // === TIME PICKER (AM/PM Dial) ===
                        Text("Select Time", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        var selectedHour by remember { mutableStateOf(12) }
                        var selectedMinute by remember { mutableStateOf(0) }
                        var isAm by remember { mutableStateOf(true) }

                        TimePicker(
                            hour = selectedHour,
                            minute = selectedMinute,
                            isAm = isAm,
                            onHourChange = { selectedHour = it },
                            onMinuteChange = { selectedMinute = it },
                            onAmPmChange = { isAm = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (currentUser != null && activityTitle.isNotBlank()) {
                                    coroutineScope.launch {
                                        try {
                                            // === CONVERT LOCAL TIME PICKER TO UTC MILLIS ===
                                            val calendar = Calendar.getInstance().apply {
                                                // Set the date from currentMonth + selectedDay
                                                time = currentMonth.time
                                                set(Calendar.DAY_OF_MONTH, selectedDay!!)

                                                // Set the time from picker (local time)
                                                set(Calendar.HOUR_OF_DAY, if (isAm) selectedHour % 12 else (selectedHour % 12) + 12)
                                                set(Calendar.MINUTE, selectedMinute)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }

                                            val activityDateUtc = calendar.timeInMillis   // This is now correct UTC

                                            val isOrgActivity = selectedOrgId != null

                                            val newActivity = Activity(
                                                title = activityTitle,
                                                description = activityDescription,
                                                partnerId = selectedPartnerId ?: currentUser.uid,
                                                date = activityDateUtc,                    // Store as UTC
                                                time = String.format("%02d:%02d %s",
                                                    if (selectedHour == 12) 12 else selectedHour % 12,
                                                    selectedMinute,
                                                    if (isAm) "AM" else "PM"),
                                                creatorId = currentUser.uid,
                                                orgId = selectedOrgId,
                                                organizationId = selectedOrgId,
                                                organizationName = if (isOrgActivity) organizations.find { it.id == selectedOrgId }?.name else null,
                                                activityType = if (isOrgActivity) "organization" else "personal"
                                            )

                                            // Save to Firestore
                                            val activityRef = if (isOrgActivity) {
                                                firestore.collection("organizations")
                                                    .document(selectedOrgId!!)
                                                    .collection("activities")
                                                    .add(newActivity)
                                                    .await()
                                            } else {
                                                firestore.collection("profiles")
                                                    .document(currentUser.uid)
                                                    .collection("activities")
                                                    .add(newActivity)
                                                    .await()
                                            }

                                            // Create notification & schedule reminders using UTC time
                                            createActivityReminders(
                                                context = context,
                                                activityId = activityRef.id,
                                                title = activityTitle,
                                                startTimeMillis = activityDateUtc,   // Pass correct UTC time
                                                isOrganizationActivity = isOrgActivity,
                                                organizationId = selectedOrgId
                                            )

                                            Toast.makeText(context, "Activity added successfully!", Toast.LENGTH_SHORT).show()

                                            // Clear form
                                            activityTitle = ""
                                            activityDescription = ""
                                            selectedHour = 12
                                            selectedMinute = 0
                                            isAm = true
                                            showActivityInput = false

                                        } catch (e: Exception) {
                                            Log.e("PartnersScreen", "Failed to add activity", e)
                                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = activityTitle.isNotBlank()
                        ) {
                            Text("Add Activity")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        // Existing day activities display code (unchanged)
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
                                val isOrg = activity.isOrganizationActivity

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isOrg)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isOrg) Icons.Default.Business else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isOrg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(28.dp)
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = activity.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = activity.displayType,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isOrg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                            )
                                            // Use local time display
                                            Text(
                                                text = activity.getLocalTimeOnly() ?: "No time specified",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (!activity.description.isNullOrBlank()) {
                                                Text(
                                                    text = activity.description!!,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No activities for this day",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
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

@Composable
fun TimePicker(
    hour: Int,
    minute: Int,
    isAm: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onAmPmChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hour
        NumberPicker(
            value = hour,
            onValueChange = onHourChange,
            range = 1..12
        )

        Text(":", style = MaterialTheme.typography.headlineMedium)

        // Minute
        NumberPicker(
            value = minute,
            onValueChange = onMinuteChange,
            range = 0..59,
            step = 5
        )

        Spacer(modifier = Modifier.width(16.dp))

        // AM/PM Toggle
        Row {
            TextButton(onClick = { onAmPmChange(true) }) {
                Text("AM", color = if (isAm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
            TextButton(onClick = { onAmPmChange(false) }) {
                Text("PM", color = if (!isAm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    step: Int = 1
) {
    val displayValue = if (value < 10) "0$value" else value.toString()
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            val newValue = (value - step).coerceIn(range)
            onValueChange(newValue)
        }) {
            Text("-")
        }
        Text(displayValue, style = MaterialTheme.typography.headlineMedium)
        IconButton(onClick = {
            val newValue = (value + step).coerceIn(range)
            onValueChange(newValue)
        }) {
            Text("+")
        }
    }
}

// ==================== HELPER FUNCTIONS (OUTSIDE COMPOSABLE) ====================

private fun createActivityReminders(
    context: Context,
    activityId: String,
    title: String,
    startTimeMillis: Long,           // This is the scheduled UTC start time
    isOrganizationActivity: Boolean,
    organizationId: String? = null
) {
    val firestore = FirebaseFirestore.getInstance()
    val workManager = androidx.work.WorkManager.getInstance(context)

    val reminders = listOf(
        24 * 60 * 60 * 1000L to "1 day before",
        60 * 60 * 1000L to "1 hour before",
        10 * 60 * 1000L to "10 minutes before"
    )

    reminders.forEach { (offset, label) ->
        val reminderTime = startTimeMillis - offset

        if (reminderTime > System.currentTimeMillis()) {
            // Save to activityReminders collection
            val reminderData = mapOf(
                "activityId" to activityId,
                "title" to title,
                "reminderTime" to reminderTime,
                "eventTime" to startTimeMillis,           // ← Important: pass the actual event time
                "label" to label,
                "isOrganizationActivity" to isOrganizationActivity,
                "organizationId" to organizationId,
                "createdAt" to System.currentTimeMillis(),
                "triggered" to false
            )

            firestore.collection("activityReminders")
                .add(reminderData)
                .addOnSuccessListener { docRef ->
                    Log.d("PartnersScreen", "✅ Created $label reminder: ${docRef.id}")

                    // Schedule WorkManager
                    val data = androidx.work.Data.Builder()
                        .putString("reminderId", docRef.id)
                        .putString("activityId", activityId)
                        .putString("title", title)
                        .putString("message", "Starting soon!")
                        .putLong("eventTime", startTimeMillis)        // ← Pass to worker
                        .build()

                    val request = androidx.work.OneTimeWorkRequestBuilder<ActivityReminderWorker>()
                        .setInitialDelay(reminderTime - System.currentTimeMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                        .setInputData(data)
                        .build()

                    workManager.enqueue(request)
                }
                .addOnFailureListener { e ->
                    Log.e("PartnersScreen", "Failed to create reminder", e)
                }
        }
    }
}

private fun getTimeInMillis(timeSlot: String): Long {
    val parts = timeSlot.split(":")
    val hours = parts[0].toIntOrNull() ?: 0
    val minutes = parts[1].toIntOrNull() ?: 0
    return (hours * 60L * 60 * 1000) + (minutes * 60L * 1000)
}