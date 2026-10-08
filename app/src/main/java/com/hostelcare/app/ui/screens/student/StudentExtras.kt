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
import com.hostelcare.app.ui.components.StudentBottomNavigation
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
        bottomBar = { StudentBottomNavigation(navController, Routes.NOTIFICATIONS) },
        containerColor = Color(0xFFF9FAFB),
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold, color = TextDark) },
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { f ->
                    val isSelected = filter == f
                    Surface(
                        onClick = { filter = f },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFFEFF6FF) else Color.Transparent,
                        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else Color(0xFFE5E7EB))
                    ) {
                        Text(
                            text = f,
                            color = if (isSelected) PrimaryBlue else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)
            
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFD1D5DB))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No notifications yet", style = MaterialTheme.typography.titleMedium, color = TextDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("You'll see complaint updates and\nimportant hostel announcements here.", color = TextMuted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
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

    // Convert from timestamp if available, otherwise just use a relative string logic. Assuming we have timestamp in n.createdAt
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
        colors = CardDefaults.cardColors(containerColor = if (n.isRead) Color.White else Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (n.isRead) 0.dp else 0.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (n.isRead) Color(0xFFF3F4F6) else Color(0xFFE5E7EB))
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(40.dp).background(if (n.isRead) Color(0xFFF3F4F6) else Color.White, CircleShape).border(1.dp, Color(0xFFE5E7EB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconInfo.first, contentDescription = null, tint = iconInfo.second, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(n.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.weight(1f).padding(end = 8.dp))
                    Text(timeStr, color = TextMuted, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(n.message, color = if (n.isRead) TextMuted else TextDark, fontSize = 14.sp, lineHeight = 20.sp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val user by viewModel.currentUser.collectAsStateWithLifecycle(null)
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out?", fontWeight = FontWeight.Bold, color = TextDark) },
            text = { Text("Are you sure you want to log out?", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    navController.navigate(Routes.LOGIN) { popUpTo(0) }
                }) { Text("Log Out", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = TextDark) }
            },
            containerColor = Color.White
        )
    }

    Scaffold(
        bottomBar = { StudentBottomNavigation(navController, Routes.PROFILE) },
        containerColor = Color(0xFFF9FAFB),
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(PrimaryBlue, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Business, null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("HOSTELCARE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                            Text("Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Default.Tune, null, tint = TextDark) }
                    IconButton(onClick = { }) { 
                        Box(modifier = Modifier.size(32.dp).background(PrimaryBlue, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (user == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            val u = user!!
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.size(96.dp).background(Color(0xFFE5E7EB), CircleShape), contentAlignment = Alignment.Center) {
                        UriImage(u.profilePhotoUri ?: "", modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(u.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(u.studentId.ifEmpty { "STU-XXXX" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(32.dp))
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            ProfileDetailRow(Icons.Default.Domain, "Hostel & Block", u.hostelBlock.ifEmpty { "Not Assigned" })
                            HorizontalDivider(color = Color(0xFFF3F4F6))
                            ProfileDetailRow(Icons.Default.MeetingRoom, "Room Number", u.roomNumber.ifEmpty { "Not Assigned" })
                            HorizontalDivider(color = Color(0xFFF3F4F6))
                            ProfileDetailRow(Icons.Default.Email, "College Email", u.email)
                            HorizontalDivider(color = Color(0xFFF3F4F6))
                            ProfileDetailRow(Icons.Default.Phone, "Phone Number", u.phone.ifEmpty { "Not Provided" })
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { navController.navigate(Routes.EDIT_PROFILE) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Profile", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(16.dp)).clickable { navController.navigate(Routes.CHANGE_PASSWORD) }
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.LockReset, null, tint = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Change Password", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextDark, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextMuted, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, fontSize = 14.sp, color = TextMuted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark, textAlign = TextAlign.End)
    }
}

@Composable
fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, iconBg: Color, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(16.dp)).clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(iconBg, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
        }
    }
}

@Composable
fun ProfileFormField(label: String, required: Boolean = false, content: @Composable () -> Unit) {
    Column {
        Row {
            Text(label, fontSize = 13.sp, color = TextDark, fontWeight = FontWeight.Bold)
            if (required) {
                Text(" *", fontSize = 13.sp, color = DangerRed)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

﻿@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val user by viewModel.currentUser.collectAsStateWithLifecycle(null)
    val updateState by viewModel.updateProfileState.collectAsStateWithLifecycle()
    
    var name by remember(user) { mutableStateOf(user?.name ?: "") }
    var email by remember(user) { mutableStateOf(user?.email ?: "") }
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Edit Profile", fontWeight = FontWeight.Bold, color = TextDark) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextDark) } }, actions = { IconButton(onClick = {}) { Icon(Icons.Default.Info, null, tint = TextMuted) } ; IconButton(onClick = {}) { Box(modifier = Modifier.size(32.dp).background(PrimaryBlue, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp)) } } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)) },
        containerColor = Color.White,
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Button(
                    onClick = { 
                        if (!com.hostelcare.app.utils.NetworkUtils.isNetworkAvailable(context)) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("No internet connection") }
                        } else {
                            viewModel.updateProfile(name, email, hostel, room, phone)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmitting,
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
                ) { Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextDark) }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.size(100.dp).clickable { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(96.dp).background(Color(0xFFE5E7EB), CircleShape), contentAlignment = Alignment.Center) { 
                        UriImage(photoUri, modifier = Modifier.fillMaxSize())
                    }
                    Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-4).dp).size(32.dp).background(PrimaryBlue, CircleShape).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
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
                    OutlinedTextField(value = name, onValueChange = { name = it }, leadingIcon = { Icon(Icons.Default.Person, null, tint = TextMuted) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue))
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            
            item {
                ProfileFormField("College Email", required = true) {
                    OutlinedTextField(value = email, onValueChange = { email = it }, leadingIcon = { Icon(Icons.Default.Email, null, tint = TextMuted) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue))
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            
            item {
                ProfileFormField("Hostel / Block", required = true) {
                    Box {
                        OutlinedTextField(value = hostel, onValueChange = { }, leadingIcon = { Icon(Icons.Default.LocationCity, null, tint = TextMuted) }, trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted) }, modifier = Modifier.fillMaxWidth(), readOnly = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue))
                        Box(modifier = Modifier.matchParentSize().clickable { hostelExpanded = true })
                        DropdownMenu(expanded = hostelExpanded, onDismissRequest = { hostelExpanded = false }, modifier = Modifier.background(Color.White)) {
                            listOf("Tagore Block A", "Tagore Block B", "Raman Block A", "Raman Block B", "Curie Block", "Newton Block").forEach { block ->
                                DropdownMenuItem(text = { Text(block) }, onClick = { hostel = block; hostelExpanded = false })
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            
            item {
                ProfileFormField("Room Number", required = true) {
                    OutlinedTextField(value = room, onValueChange = { room = it }, leadingIcon = { Icon(Icons.Default.MeetingRoom, null, tint = TextMuted) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue))
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            
            item {
                ProfileFormField("Phone Number") {
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, leadingIcon = { Icon(Icons.Default.Phone, null, tint = TextMuted) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue))
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
        topBar = { TopAppBar(title = { Text("Change Password", fontWeight = FontWeight.Bold, color = TextDark) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextDark) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)) },
        containerColor = Color.White,
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
        topBar = { TopAppBar(title = { Text("Resolution Feedback", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)) },
        containerColor = BackgroundLight
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(complaint?.title ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Resolved", color = SuccessGreen, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("How was the resolution?", style = MaterialTheme.typography.titleMedium)
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
            TextButton(onClick = { navController.popBackStack() }) { Text("Skip for now", color = TextMuted) }
        }
    }
}
