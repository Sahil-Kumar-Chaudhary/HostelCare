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
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle(null)
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    
    var filter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Updates", "Assigned", "Resolved", "System")
    
    val filtered = notifications.filter {
        when (filter) {
            "Updates" -> it.type == com.hostelcare.app.data.model.NotificationType.SUBMITTED || it.type == com.hostelcare.app.data.model.NotificationType.STATUS_UPDATED || it.type == com.hostelcare.app.data.model.NotificationType.AI_ANALYSIS_COMPLETED
            "Assigned" -> it.type == com.hostelcare.app.data.model.NotificationType.ASSIGNED
            "Resolved" -> it.type == com.hostelcare.app.data.model.NotificationType.RESOLVED
            "System" -> it.type == com.hostelcare.app.data.model.NotificationType.ADVISORY
            else -> true
        }
    }

    Scaffold(
        bottomBar = {
            when (currentUser?.role) {
                Role.WARDEN -> WardenBottomNavigation(navController, Routes.NOTIFICATIONS)
                Role.STAFF -> StaffBottomNavigation(navController, Routes.NOTIFICATIONS)
                else -> StudentBottomNavigation(navController, Routes.NOTIFICATIONS)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navController.popBackStack()) {
                            when (currentUser?.role) {
                                Role.WARDEN -> navController.navigate(Routes.WARDEN_HOME)
                                Role.STAFF -> navController.navigate(Routes.STAFF_HOME)
                                else -> navController.navigate(Routes.STUDENT_HOME)
                            }
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                notifications.filter { !it.isRead }.forEach { n ->
                                    app.repository.markNotificationAsRead(n.id)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark all read", color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { f ->
                    val isSelected = filter == f
                    Surface(
                        onClick = { filter = f },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = f,
                            color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No notifications yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("You'll see complaint updates and\nimportant hostel announcements here.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered) { n ->
                        NotificationCard(n) {
                            coroutineScope.launch {
                                app.repository.markNotificationAsRead(n.id)
                            }
                            if (n.complaintId != null) {
                                navController.navigate(Routes.complaintDetails(n.complaintId))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(n: com.hostelcare.app.data.model.Notification, onClick: () -> Unit) {
    val iconInfo = when (n.type) {
        com.hostelcare.app.data.model.NotificationType.AI_ANALYSIS_COMPLETED -> Pair(Icons.Default.AutoAwesome, PrimaryBlue)
        com.hostelcare.app.data.model.NotificationType.SUBMITTED -> Pair(Icons.Default.Assignment, PrimaryBlue)
        com.hostelcare.app.data.model.NotificationType.ASSIGNED -> Pair(Icons.Default.PersonOutline, Color(0xFFD97706))
        com.hostelcare.app.data.model.NotificationType.IN_PROGRESS, com.hostelcare.app.data.model.NotificationType.STATUS_UPDATED -> Pair(Icons.Default.Settings, Color(0xFF7C3AED))
        com.hostelcare.app.data.model.NotificationType.RESOLVED -> Pair(Icons.Default.CheckCircleOutline, Color(0xFF16A34A))
        com.hostelcare.app.data.model.NotificationType.ADVISORY -> Pair(Icons.Default.Info, TextMuted)
    }

    val timeDiff = System.currentTimeMillis() - n.createdAt
    val mins = timeDiff / 60000
    val hours = mins / 60
    val days = hours / 24
    
    val timeStr = when {
        mins < 60 -> "${maxOf(1, mins)} min ago"
        hours < 24 -> "$hours hours ago"
        days == 1L -> "1 day ago"
        else -> "$days days ago"
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (n.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconInfo.first, contentDescription = null, tint = iconInfo.second, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(n.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).padding(end = 8.dp))
                    Text(timeStr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(n.message, color = if (n.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, lineHeight = 20.sp)
            }
            
            if (!n.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(8.dp).background(PrimaryBlue, CircleShape).align(Alignment.CenterVertically))
            }
        }
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

