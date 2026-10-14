package com.hostelcare.app.ui.screens.student

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.data.model.Role
import com.hostelcare.app.ui.components.HostelCareLogo
import com.hostelcare.app.ui.components.StudentBottomNavigation
import com.hostelcare.app.ui.components.WardenBottomNavigation
import com.hostelcare.app.ui.components.StaffBottomNavigation
import com.hostelcare.app.ui.navigation.Routes
import com.hostelcare.app.ui.screens.auth.CustomTextField
import com.hostelcare.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf("all") } // "all" or "unread"

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Routes.STUDENT_HOME)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F172A)
                            )
                        }
                        Text(
                            text = "Notifications",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    IconButton(onClick = { viewModel.markAllInAppNotificationsRead() }) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Mark all read",
                            tint = Color(0xFF475569)
                        )
                    }
                }
            }
        },
        bottomBar = {
            StudentBottomNavigation(
                navController = navController,
                currentRoute = Routes.NOTIFICATIONS,
                unreadNotificationCount = unreadCount
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val (allCount, unreadTabCount) = when (val state = notificationsState) {
                is StudentNotificationsUiState.Success -> Pair(state.notifications.size, state.unreadCount)
                else -> Pair(0, 0)
            }

            // Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isAllSelected = selectedTab == "all"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAllSelected) Color(0xFF2563EB) else Color.White,
                    border = BorderStroke(1.dp, if (isAllSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = "all" }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All ($allCount)",
                            color = if (isAllSelected) Color.White else Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                val isUnreadSelected = selectedTab == "unread"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isUnreadSelected) Color(0xFF2563EB) else Color.White,
                    border = BorderStroke(1.dp, if (isUnreadSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = "unread" }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Unread ($unreadTabCount)",
                            color = if (isUnreadSelected) Color.White else Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontWeight = if (isUnreadSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Notifications List
            when (val state = notificationsState) {
                is StudentNotificationsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF2563EB))
                    }
                }
                is StudentNotificationsUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.NotificationsOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No notifications yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "You'll see complaint updates and important hostel announcements here.",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                is StudentNotificationsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = Color(0xFFDC2626), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadNotifications() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is StudentNotificationsUiState.Success -> {
                    val filtered = if (selectedTab == "unread") {
                        state.notifications.filter { !it.isRead }
                    } else {
                        state.notifications
                    }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedTab == "unread") "No unread notifications." else "No notifications.",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { item ->
                                StudentNotificationCard(
                                    item = item,
                                    onClick = {
                                        if (!item.isRead) {
                                            viewModel.markInAppNotificationRead(item.id)
                                        }
                                        if (!item.complaintId.isNullOrBlank()) {
                                            navController.navigate(Routes.complaintDetails(item.complaintId))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentNotificationCard(
    item: com.hostelcare.app.data.remote.InAppNotificationItem,
    onClick: () -> Unit
) {
    val (icon, iconTint, iconBg) = getStudentNotificationVisuals(item.type, item.title)
    val timeAgo = formatStudentTimeAgo(item.createdAt)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.message,
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )

                if (timeAgo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = timeAgo,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

fun getStudentNotificationVisuals(type: String, title: String): Triple<androidx.compose.ui.graphics.vector.ImageVector, Color, Color> {
    val t = type.lowercase()
    val tit = title.lowercase()
    return when {
        t == "complaint_created" || tit.contains("registered") ->
            Triple(Icons.Default.Description, Color(0xFF2563EB), Color(0xFFEFF6FF))
        t == "complaint_assigned" || tit.contains("assigned") ->
            Triple(Icons.Default.Engineering, Color(0xFF7C3AED), Color(0xFFF3E8FF))
        t == "work_started" || tit.contains("started") ->
            Triple(Icons.Default.Build, Color(0xFF0284C7), Color(0xFFE0F2FE))
        t == "work_submitted" || tit.contains("submitted") ->
            Triple(Icons.Default.HourglassTop, Color(0xFFD97706), Color(0xFFFEF3C7))
        t == "complaint_resolved" || tit.contains("resolved") ->
            Triple(Icons.Default.CheckCircle, Color(0xFF16A34A), Color(0xFFDCFCE7))
        t == "complaint_reopened" || tit.contains("reopened") ->
            Triple(Icons.Default.Replay, Color(0xFFEF4444), Color(0xFFFEE2E2))
        t == "status_updated" || tit.contains("status") ->
            Triple(Icons.Default.Settings, Color(0xFF64748B), Color(0xFFF1F5F9))
        else ->
            Triple(Icons.Default.Notifications, Color(0xFF64748B), Color(0xFFF1F5F9))
    }
}

fun formatStudentTimeAgo(isoString: String?): String {
    if (isoString.isNullOrBlank()) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(isoString) ?: return ""
        val now = System.currentTimeMillis()
        val diffMs = now - date.time
        val diffMins = diffMs / (1000 * 60)
        val diffHours = diffMins / 60
        val diffDays = diffHours / 24

        when {
            diffMins < 1 -> "Just now"
            diffMins < 60 -> "$diffMins min ago"
            diffHours < 24 -> "$diffHours hours ago"
            diffDays == 1L -> "1 day ago"
            else -> "$diffDays days ago"
        }
    } catch (e: Exception) {
        ""
    }
}

@Composable
fun UriImage(
    uriStr: String, 
    modifier: Modifier = Modifier, 
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Person
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bitmap by remember(uriStr) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var hasError by remember(uriStr) { mutableStateOf(false) }
    
    LaunchedEffect(uriStr) {
        if (uriStr.isNotEmpty()) {
            try {
                val androidBitmap = if (uriStr.startsWith("data:image")) {
                    val base64 = uriStr.substringAfter("base64,")
                    val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } else {
                    val uri = android.net.Uri.parse(uriStr)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val decoded = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    decoded
                }
                if (androidBitmap != null) {
                    bitmap = androidBitmap.asImageBitmap()
                } else {
                    hasError = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                hasError = true
            }
        } else {
            bitmap = null
            hasError = false
        }
    }
    
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier.clip(shape),
            contentScale = ContentScale.Crop
        )
    } else if (hasError) {
        Icon(androidx.compose.material.icons.Icons.Default.BrokenImage, null, modifier = modifier.padding(16.dp), tint = TextMuted)
    } else {
        Icon(fallbackIcon, null, modifier = modifier.padding(16.dp), tint = TextMuted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val user by viewModel.currentUser.collectAsStateWithLifecycle(null)
    val updateState by viewModel.updateProfileState.collectAsStateWithLifecycle()
    val isDarkMode by ThemeManager.isDarkMode.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showCollegeDetailsDialog by remember { mutableStateOf(false) }
    var showHostelDetailsDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(updateState) {
        if (updateState is UpdateProfileState.Success) {
            snackbarHostState.showSnackbar("Profile updated successfully")
            viewModel.resetUpdateState()
        } else if (updateState is UpdateProfileState.Error) {
            val err = (updateState as UpdateProfileState.Error).message
            snackbarHostState.showSnackbar(err)
            viewModel.resetUpdateState()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    val outStream = java.io.ByteArrayOutputStream()
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outStream)
                        val bytes = outStream.toByteArray()
                        if (bytes.size <= 5 * 1024 * 1024) {
                            viewModel.uploadProfilePhotoDirect(bytes, "image/jpeg")
                        } else {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Image too large (Max 5 MB)") }
                        }
                    }
                } catch (e: Exception) {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Failed to process photo") }
                }
            }
        }
    )

    // Dialogs
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    "Log out?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "Are you sure you want to log out of HostelCare? You can log back in at any time.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    navController.navigate(Routes.PORTAL_SELECTION) {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text("Log out", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showPhotoOptionsDialog && user != null) {
        val u = user!!
        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = { Text("Profile Photo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Choose an option for your profile photo:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showPhotoOptionsDialog = false
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose New Photo")
                    }
                    if (!u.profilePhotoUri.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                showPhotoOptionsDialog = false
                                viewModel.deleteProfilePhotoDirect()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, DangerRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = DangerRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove Photo", color = DangerRed)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoOptionsDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showCollegeDetailsDialog && user != null) {
        val u = user!!
        AlertDialog(
            onDismissRequest = { showCollegeDetailsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.School, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("College Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoItem(label = "University / College", value = u.collegeName.ifEmpty { "Not Assigned" })
                    InfoItem(label = "Course / Department", value = u.course.ifEmpty { "Not Assigned" })
                    val yr = u.currentYear.ifEmpty { "Not Specified" }
                    val sem = if (u.semester.isNotBlank()) " (Sem ${u.semester})" else ""
                    InfoItem(label = "Academic Year", value = "$yr$sem")
                    InfoItem(label = "Student ID", value = u.studentId.ifEmpty { "STU-XXXX" })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCollegeDetailsDialog = false
                        navController.navigate(Routes.EDIT_PROFILE)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Edit Details")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCollegeDetailsDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showHostelDetailsDialog && user != null) {
        val u = user!!
        AlertDialog(
            onDismissRequest = { showHostelDetailsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Apartment, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Hostel Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoItem(label = "Hostel Name", value = u.hostelName.ifEmpty { "Not Assigned" })
                    InfoItem(label = "Hostel Block", value = u.hostelBlock.ifEmpty { "Not Assigned" })
                    InfoItem(label = "Room Number", value = u.roomNumber.ifEmpty { "Not Assigned" })
                    InfoItem(label = "Helpdesk Desk", value = "Warden Office, Block A Ground Floor")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHostelDetailsDialog = false
                        navController.navigate(Routes.EDIT_PROFILE)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Edit Details")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHostelDetailsDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Translate, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Language", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("English", fontWeight = FontWeight.Bold, color = PrimaryBlue, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Check, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                    listOf("Hindi (हिंदी)", "Spanish (Español)", "French (Français)").forEach { lang ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lang, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                Text("Coming soon", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Text(
                        "HostelCare is currently available in English. Additional language localizations are underway.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLanguageDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Done")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Description, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Privacy Policy", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Last updated: October 2026", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    PolicySection(
                        title = "1. Information We Collect",
                        body = "HostelCare collects your name, university email, student ID, hostel block, room number, contact phone, and uploaded complaint photos for operational and facility management purposes."
                    )
                    PolicySection(
                        title = "2. How Information is Used",
                        body = "Your information is used solely to triage, assign, and resolve maintenance complaints, dispatch technicians, track resolution timelines, and issue push notifications."
                    )
                    PolicySection(
                        title = "3. AI Processing & Gemini",
                        body = "Complaint titles, descriptions, and uploaded images may be analyzed by Google Gemini AI models strictly to classify priority, assess hazard levels, and suggest actionable repair instructions."
                    )
                    PolicySection(
                        title = "4. Data Security",
                        body = "All network communication is encrypted over HTTPS. User tokens and sensitive credentials are encrypted using industry standard protocols and never exposed publicly."
                    )
                    PolicySection(
                        title = "5. Your Rights",
                        body = "You have the right to view, update, or request deletion of your profile photo and personal details at any time through the HostelCare profile management interface."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyPolicyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VerifiedUser, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Terms & Resident Code", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Hostel Residence Guidelines & Portal Rules", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    PolicySection(
                        title = "1. Resident Code of Conduct",
                        body = "All residents must observe quiet hours between 10:00 PM and 6:00 AM. Mutual respect among roommates and staff is strictly enforced."
                    )
                    PolicySection(
                        title = "2. Complaint Submission Rules",
                        body = "Submit authentic maintenance grievances only. Falsified, frivolous, or abusive complaints may result in suspension of portal privileges and administrative disciplinary review."
                    )
                    PolicySection(
                        title = "3. Property & Appliance Care",
                        body = "Residents are responsible for preserving hostel furnishings, plumbing fixtures, and electrical installations. Intentional damage is subject to replacement levies."
                    )
                    PolicySection(
                        title = "4. Urgent Safety Hazards",
                        body = "For life-safety emergencies, electrical short circuits, or gas leakages, report immediately to the physical Warden desk and activate the emergency contact channel."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTermsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("I Understand")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { StudentBottomNavigation(navController, Routes.PROFILE) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HostelCareLogo(size = 40.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("HOSTEL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, letterSpacing = 1.sp)
                                Text("CARE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue, letterSpacing = 1.sp)
                            }
                            Text("Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* filter/actions */ }) {
                        Icon(Icons.Default.Tune, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .background(PrimaryBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (user == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            val u = user!!
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Avatar, Name, Student ID
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.size(104.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(104.dp)
                                    .background(Color(0xFFE2E8F0), CircleShape)
                                    .clickable {
                                        if (u.profilePhotoUri.isNullOrEmpty()) {
                                            photoPickerLauncher.launch(
                                                androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        } else {
                                            showPhotoOptionsDialog = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!u.profilePhotoUri.isNullOrEmpty()) {
                                    UriImage(u.profilePhotoUri!!, modifier = Modifier.fillMaxSize())
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(60.dp))
                                }
                            }
                            // Camera badge overlapping bottom-right
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(34.dp)
                                    .background(PrimaryBlue, CircleShape)
                                    .border(2.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .clickable {
                                        if (u.profilePhotoUri.isNullOrEmpty()) {
                                            photoPickerLauncher.launch(
                                                androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        } else {
                                            showPhotoOptionsDialog = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Change photo", tint = Color.White, modifier = Modifier.size(17.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            u.name.ifEmpty { "Sahil Kumar Chaudhary" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            u.studentId.ifEmpty { "12305507" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Section 1: MY DETAILS
                item {
                    ProfileSectionCard(
                        icon = Icons.Default.Person,
                        title = "MY DETAILS",
                        subtitle = "View and manage your personal, college and hostel information."
                    ) {
                        ProfileNavigationRow(
                            icon = Icons.Default.Person,
                            title = "Personal details",
                            subtitle = "Name, email and phone",
                            onClick = { navController.navigate(Routes.EDIT_PROFILE) }
                        )
                        ProfileNavigationRow(
                            icon = Icons.Default.School,
                            title = "College details",
                            subtitle = "University, course and academic year",
                            onClick = { showCollegeDetailsDialog = true }
                        )
                        ProfileNavigationRow(
                            icon = Icons.Default.Apartment,
                            title = "Hostel details",
                            subtitle = "Hostel, block and room number",
                            showDivider = false,
                            onClick = { showHostelDetailsDialog = true }
                        )
                    }
                }

                // Section 2: APP PREFERENCES
                item {
                    ProfileSectionCard(
                        icon = Icons.Default.Settings,
                        title = "APP PREFERENCES",
                        subtitle = "Customize your app experience."
                    ) {
                        ProfileNavigationRow(
                            icon = Icons.Default.DarkMode,
                            title = "Appearance",
                            subtitle = "Light or dark theme",
                            onClick = { ThemeManager.setDarkMode(context, !isDarkMode) },
                            trailingContent = {
                                Switch(
                                    checked = isDarkMode,
                                    onCheckedChange = { checked ->
                                        ThemeManager.setDarkMode(context, checked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = PrimaryBlue,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFFCBD5E1)
                                    )
                                )
                            }
                        )
                        ProfileNavigationRow(
                            icon = Icons.Default.Translate,
                            title = "Language",
                            subtitle = "Choose app language",
                            showDivider = false,
                            onClick = { showLanguageDialog = true },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "English",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                // Section 3: ACCOUNT & SUPPORT
                item {
                    ProfileSectionCard(
                        icon = Icons.Default.Shield,
                        title = "ACCOUNT & SUPPORT",
                        subtitle = "Manage your account and get help."
                    ) {
                        ProfileNavigationRow(
                            icon = Icons.Default.Lock,
                            title = "Change password",
                            subtitle = "Update your account password",
                            onClick = { navController.navigate(Routes.CHANGE_PASSWORD) }
                        )
                        ProfileNavigationRow(
                            icon = Icons.Default.Description,
                            title = "Privacy policy",
                            subtitle = "How your information is used",
                            onClick = { showPrivacyPolicyDialog = true }
                        )
                        ProfileNavigationRow(
                            icon = Icons.Default.VerifiedUser,
                            title = "Terms & resident code",
                            subtitle = "Review app and hostel rules",
                            showDivider = false,
                            onClick = { showTermsDialog = true }
                        )
                    }
                }

                // Log out Danger Button
                item {
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            contentColor = DangerRed
                        ),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.35f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Log out",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DangerRed
                        )
                    }
                }

                // Version Footer
                item {
                    Text(
                        "HostelCare • Version 1.0.0",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )
            content()
        }
    }
}

@Composable
fun ProfileNavigationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp,
                modifier = Modifier.padding(start = 54.dp)
            )
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(2.dp))
        Text(body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
    }
}

@Composable
fun ProfileDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
    }
}

@Composable
fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, iconBg: Color, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(iconBg, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProfileFormField(label: String, required: Boolean = false, content: @Composable () -> Unit) {
    Column {
        Row {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            if (required) {
                Text(" *", fontSize = 13.sp, color = DangerRed)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val user by viewModel.currentUser.collectAsStateWithLifecycle(null)
    val updateState by viewModel.updateProfileState.collectAsStateWithLifecycle()
    
    var name by remember(user) { mutableStateOf(user?.name ?: "") }
    var email by remember(user) { mutableStateOf(user?.email ?: "") }
    var hostelName by remember(user) { mutableStateOf(user?.hostelName ?: "") }
    var hostel by remember(user) { mutableStateOf(user?.hostelBlock ?: "") }
    var room by remember(user) { mutableStateOf(user?.roomNumber ?: "") }
    var phone by remember(user) { mutableStateOf(user?.phone ?: "") }
    
    var photoUri by remember(user) { mutableStateOf(user?.profilePhotoUri ?: "") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    var hostelExpanded by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val isSubmitting = updateState is com.hostelcare.app.ui.screens.student.UpdateProfileState.Loading

    LaunchedEffect(updateState) {
        if (updateState is com.hostelcare.app.ui.screens.student.UpdateProfileState.Success) {
            viewModel.resetUpdateState()
            navController.popBackStack()
        } else if (updateState is com.hostelcare.app.ui.screens.student.UpdateProfileState.Error) {
            val err = (updateState as com.hostelcare.app.ui.screens.student.UpdateProfileState.Error).message
            snackbarHostState.showSnackbar(err)
            viewModel.resetUpdateState()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> 
            if (uri != null) {
                try {
                    val outStream = java.io.ByteArrayOutputStream()
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outStream)
                        val bytes = outStream.toByteArray()
                        if (bytes.size <= 5 * 1024 * 1024) {
                            selectedImageUri = uri
                            photoUri = uri.toString()
                            viewModel.profilePhotoBytes = bytes
                            viewModel.profilePhotoMimeType = "image/jpeg"
                            viewModel.removeProfilePhoto = false
                        } else {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Image too large (Max 5 MB)") }
                        }
                    }
                } catch (e: Exception) {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Failed to process image") }
                }
            }
        }
    )

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Edit Profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) } }, actions = { IconButton(onClick = {}) { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } ; IconButton(onClick = {}) { Box(modifier = Modifier.size(32.dp).background(PrimaryBlue, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp)) } } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Button(
                    onClick = { 
                        if (!com.hostelcare.app.utils.NetworkUtils.isNetworkAvailable(context)) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("No internet connection") }
                        } else if (name.isBlank() || email.isBlank() || hostelName.isBlank() || hostel.isBlank() || room.isBlank()) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Please fill all required fields") }
                        } else {
                            viewModel.updateProfile(
                                name = name.trim(),
                                email = email.trim(),
                                hostelBlock = hostel.trim(),
                                roomNumber = room.trim(),
                                phone = phone.trim(),
                                hostelName = hostelName.trim()
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmitting && name.isNotBlank() && email.isNotBlank() && hostelName.isNotBlank() && hostel.isNotBlank() && room.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) { 
                    if (isSubmitting) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.size(100.dp).clickable { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(96.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) { 
                        UriImage(photoUri, modifier = Modifier.fillMaxSize())
                    }
                    Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-4).dp).size(32.dp).background(PrimaryBlue, CircleShape).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tap to change photo", color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                if (photoUri.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Remove photo", color = DangerRed, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { 
                        photoUri = ""
                        selectedImageUri = null
                        viewModel.profilePhotoBytes = null
                        viewModel.profilePhotoMimeType = null
                        viewModel.removeProfilePhoto = true
                    })
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            item {
                ProfileFormField("Full Name", required = true) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = inputColors)
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            
            item {
                ProfileFormField("College Email", required = true) {
                    OutlinedTextField(value = email, onValueChange = { email = it }, leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = inputColors)
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            
            item {
                ProfileFormField("Hostel Name", required = true) {
                    OutlinedTextField(
                        value = hostelName,
                        onValueChange = { hostelName = it },
                        placeholder = { Text("e.g. University Hostel", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = inputColors
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }

            item {
                ProfileFormField("Block", required = true) {
                    OutlinedTextField(
                        value = hostel,
                        onValueChange = { hostel = it },
                        placeholder = { Text("e.g. Block B", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Default.Domain, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = inputColors
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            
            item {
                ProfileFormField("Room Number", required = true) {
                    OutlinedTextField(value = room, onValueChange = { room = it }, leadingIcon = { Icon(Icons.Default.MeetingRoom, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = inputColors)
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            
            item {
                ProfileFormField("Phone Number") {
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, leadingIcon = { Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = inputColors)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text("Change Password", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Button(
                    onClick = { 
                        if (newPassword == confirmPassword && newPassword.length >= 6) {
                            android.widget.Toast.makeText(context, "Password changed successfully", android.widget.Toast.LENGTH_SHORT).show()
                            navController.popBackStack() 
                        } else {
                            android.widget.Toast.makeText(context, "Invalid passwords", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = currentPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) { Text("Change Password", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp)) {
            ProfileFormField("Current Password") {
                CustomTextField(
                    value = currentPassword, onValueChange = { currentPassword = it },
                    placeholder = "Enter current password", isPassword = true
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            ProfileFormField("New Password") {
                CustomTextField(
                    value = newPassword, onValueChange = { newPassword = it },
                    placeholder = "Enter new password", isPassword = true
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            ProfileFormField("Confirm New Password") {
                CustomTextField(
                    value = confirmPassword, onValueChange = { confirmPassword = it },
                    placeholder = "Confirm new password", isPassword = true
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(navController: NavController, app: HostelCareApp, complaintId: String) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    
    var note by remember { mutableStateOf("") }
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    val complaint = complaints.find { it.id == complaintId }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Resolution Feedback", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(complaint?.title ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Resolved", color = SuccessGreen, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("How was the resolution?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(5) { Icon(Icons.Default.StarBorder, null, tint = WarningOrange, modifier = Modifier.size(48.dp)) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            CustomTextField(note, { note = it }, "Optional comment")
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { 
                    viewModel.submitFeedback(complaintId, 5, note)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) { Text("Submit Feedback") }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { navController.popBackStack() }) { Text("Skip for now", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

