package com.hostelcare.app.ui.screens.student

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.launch
import com.hostelcare.app.utils.NetworkUtils
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.data.model.ComplaintCategory
import com.hostelcare.app.data.model.ComplaintStatus
import com.hostelcare.app.ui.components.StudentBottomNavigation
import com.hostelcare.app.ui.components.HostelCareLogo
import com.hostelcare.app.ui.navigation.Routes
import com.hostelcare.app.ui.screens.auth.CustomTextField
import com.hostelcare.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val user by viewModel.currentUser.collectAsStateWithLifecycle(null)
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }
    
    val activeCount = complaints.count { it.status == ComplaintStatus.IN_PROGRESS || it.status == ComplaintStatus.ASSIGNED || it.status == ComplaintStatus.UNDER_REVIEW }
    val pendingCount = complaints.count { it.status == ComplaintStatus.SUBMITTED }
    val resolvedCount = complaints.count { it.status == ComplaintStatus.RESOLVED }

    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Scaffold(
        bottomBar = { StudentBottomNavigation(navController, Routes.STUDENT_HOME, unreadNotificationCount = unreadCount) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                HostelCareLogo(size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("HostelCare", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Student Portal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { navController.navigate(Routes.NOTIFICATIONS) }) {
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

            }
            
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("$greeting, ${user?.name?.split(" ")?.firstOrNull() ?: "Student"}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Need something fixed in your room?", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { navController.navigate(Routes.NEW_COMPLAINT) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Report a Problem", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Active", activeCount.toString(), Icons.Default.Description, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        StatCard("Pending", pendingCount.toString(), Icons.Default.Schedule, WarningOrange.copy(alpha = 0.15f), WarningOrange, Modifier.weight(1f))
                        StatCard("Resolved", resolvedCount.toString(), Icons.Default.CheckCircle, SuccessGreen.copy(alpha = 0.15f), SuccessGreen, Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("My Complaints", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.weight(1f))
                        Row(modifier = Modifier.clickable { navController.navigate(Routes.MY_COMPLAINTS) }, verticalAlignment = Alignment.CenterVertically) {
                            Text("View all", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                items(complaints.take(3)) { c ->
                    val (icon, bg, fg) = when(c.status) {
                        ComplaintStatus.RESOLVED -> Triple(Icons.Default.Build, SuccessGreen.copy(alpha = 0.15f), SuccessGreen)
                        ComplaintStatus.SUBMITTED -> Triple(Icons.Default.Wifi, WarningOrange.copy(alpha = 0.15f), WarningOrange)
                        ComplaintStatus.WITHDRAWN -> Triple(Icons.Default.Cancel, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> Triple(Icons.Default.WaterDrop, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                    }
                    val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(c.createdAt))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { navController.navigate(Routes.complaintDetails(c.id)) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Room ${c.roomNumber} • ${c.category.name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Box(modifier = Modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text(c.status.name, color = fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(dateStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickActionCard("Report\nProblem", Icons.Default.Build, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, Modifier.weight(1f)) { navController.navigate(Routes.NEW_COMPLAINT) }
                        QuickActionCard("Hostel\nGuidelines", Icons.Default.MenuBook, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) { 
                            android.widget.Toast.makeText(context, "Guidelines opened", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        QuickActionCard("Contact\nWarden", Icons.Default.Phone, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) { 
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                            intent.data = android.net.Uri.parse("tel:+1234567890")
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, bg: Color, iconColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(28.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(label, fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun QuickActionCard(label: String, icon: ImageVector, bg: Color, fg: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, color = fg, textAlign = TextAlign.Center, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun FormField(label: String, required: Boolean = false, content: @Composable () -> Unit) {
    Column {
        Row {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
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
fun NewComplaintScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val user by viewModel.currentUser.collectAsStateWithLifecycle(null)
    val draft by viewModel.draftComplaint.collectAsStateWithLifecycle()
    
    var title by remember(draft) { mutableStateOf(draft?.title ?: "") }
    var category by remember(draft) { mutableStateOf(if (draft?.category == ComplaintCategory.OTHER) "" else draft?.category?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "") }
    var hostelBlock by remember(draft, user) { mutableStateOf(draft?.hostelBlock?.takeIf { it.isNotBlank() } ?: user?.hostelBlock ?: "") }
    var room by remember(draft, user) { mutableStateOf(draft?.roomNumber?.takeIf { it.isNotBlank() } ?: user?.roomNumber ?: "") }
    var desc by remember(draft) { mutableStateOf(draft?.description ?: "") }
    
    var showPhotoSheet by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var imageSizeError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    var categoryExpanded by remember { mutableStateOf(false) }
    var hostelExpanded by remember { mutableStateOf(false) }

    val state = viewModel.newComplaintState.collectAsStateWithLifecycle().value
    val isSubmitting = state is NewComplaintState.Submitting

    LaunchedEffect(state) {
        if (state is NewComplaintState.Error) {
            snackbarHostState.showSnackbar((state as NewComplaintState.Error).message)
            viewModel.resetComplaintState()
        } else if (state is NewComplaintState.Success) {
            viewModel.selectedPhotoBytes = null
            viewModel.selectedPhotoMimeType = null
            selectedImageUri = null
            navController.popBackStack()
            navController.navigate(Routes.complaintDetails((state as NewComplaintState.Success).complaintId))
            viewModel.resetComplaintState()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val outStream = java.io.ByteArrayOutputStream()
            var valid = true
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outStream)
                    val bytes = outStream.toByteArray()
                    if (bytes.size > 5 * 1024 * 1024) {
                        imageSizeError = "Image is too large. Please choose another photo."
                        valid = false
                    } else {
                        imageSizeError = null
                        selectedImageUri = uri
                        viewModel.selectedPhotoBytes = bytes
                        viewModel.selectedPhotoMimeType = "image/jpeg"
                    }
                }
            } catch (e: Exception) {
                valid = false
            }
            if (!valid && imageSizeError == null) {
                imageSizeError = "Unable to process image."
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraUri != null) {
            val uri = cameraUri!!
            val outStream = java.io.ByteArrayOutputStream()
            var valid = true
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outStream)
                    val bytes = outStream.toByteArray()
                    if (bytes.size > 5 * 1024 * 1024) {
                        imageSizeError = "Image is too large. Please choose another photo."
                        valid = false
                    } else {
                        imageSizeError = null
                        selectedImageUri = uri
                        viewModel.selectedPhotoBytes = bytes
                        viewModel.selectedPhotoMimeType = "image/jpeg"
                    }
                }
            } catch (e: Exception) {
                valid = false
            }
            if (!valid && imageSizeError == null) {
                imageSizeError = "Unable to process image."
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            try {
                val tempFile = java.io.File.createTempFile("complaint_", ".jpg", context.cacheDir).apply {
                    createNewFile()
                    deleteOnExit()
                }
                val uri = androidx.core.content.FileProvider.getUriForFile(context, "com.hostelcare.app.fileprovider", tempFile)
                cameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                imageSizeError = "Unable to launch camera."
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Camera permission is required to take a photo.")
            }
        }
    }

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = PrimaryBlue,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("New Complaint", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (user?.hostelName.isNullOrBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Hostel Information Missing",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Please complete your hostel information in your profile before submitting a complaint.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { navController.navigate(Routes.EDIT_PROFILE) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Complete Profile")
                            }
                        }
                    }
                }
            }

            item {
                FormField("Problem Title", required = true) {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        placeholder = { Text("e.g. Water leakage in bathroom", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = inputColors
                    )
                }
            }
            
            item {
                FormField("Category", required = false) {
                    Box {
                        OutlinedTextField(
                            value = category, onValueChange = { },
                            placeholder = { Text("Select category", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.fillMaxWidth(), readOnly = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = inputColors
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { categoryExpanded = true })
                        DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                            listOf("Plumbing", "Electrical", "Internet/WiFi", "Cleaning", "Furniture", "Other").forEach { cat ->
                                DropdownMenuItem(text = { Text(cat, color = MaterialTheme.colorScheme.onSurface) }, onClick = { category = cat; categoryExpanded = false })
                            }
                        }
                    }
                }
            }
            
            item {
                FormField("Hostel", required = false) {
                    OutlinedTextField(
                        value = user?.hostelName?.takeIf { it.isNotBlank() } ?: "Not Assigned",
                        onValueChange = { },
                        placeholder = { Text("Hostel name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = inputColors
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FormField("Block", required = true) {
                            OutlinedTextField(
                                value = hostelBlock,
                                onValueChange = { hostelBlock = it },
                                placeholder = { Text("e.g. Block B", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                leadingIcon = { Icon(Icons.Default.Domain, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = inputColors
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        FormField("Room Number", required = true) {
                            OutlinedTextField(
                                value = room,
                                onValueChange = { room = it },
                                placeholder = { Text("e.g. 101", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                leadingIcon = { Icon(Icons.Default.MeetingRoom, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = inputColors
                            )
                        }
                    }
                }
            }
            
            item {
                FormField("Description", required = true) {
                    OutlinedTextField(
                        value = desc, onValueChange = { desc = it },
                        placeholder = { Text("Provide details about the issue...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = inputColors
                    )
                }
            }
            
            item {
                FormField("Add Photo (Optional)", required = false) {
                    if (selectedImageUri != null) {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            UriImage(selectedImageUri.toString(), modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(12.dp), fallbackIcon = Icons.Default.HideImage)
                            Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                                IconButton(onClick = { showPhotoSheet = true }, modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha=0.85f), CircleShape).size(36.dp)) {
                                    Icon(Icons.Default.Edit, "Change photo", tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { 
                                    selectedImageUri = null
                                    viewModel.selectedPhotoBytes = null
                                    viewModel.selectedPhotoMimeType = null
                                }, modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha=0.85f), CircleShape).size(36.dp)) {
                                    Icon(Icons.Default.Close, "Remove photo", tint = DangerRed, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    } else {
                        Column {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).border(1.dp, PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).clickable { showPhotoSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Tap to add photo", color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Supports JPG, PNG (Max 5 MB)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                            if (imageSizeError != null) {
                                Text(imageSizeError!!, color = DangerRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
            
            item {
                Button(
                    onClick = {
                        if (!NetworkUtils.isNetworkAvailable(context)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("No internet connection. Please check your connection and try again.")
                            }
                            return@Button
                        }
                        if (user?.hostelName.isNullOrBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Please complete your hostel information in your profile before submitting a complaint.")
                            }
                            return@Button
                        }
                        val catEnum = when (category) {
                            "Plumbing" -> com.hostelcare.app.data.model.ComplaintCategory.PLUMBING
                            "Electrical" -> com.hostelcare.app.data.model.ComplaintCategory.ELECTRICAL
                            "Internet/WiFi" -> com.hostelcare.app.data.model.ComplaintCategory.INTERNET
                            "Cleaning" -> com.hostelcare.app.data.model.ComplaintCategory.CLEANING
                            "Furniture" -> com.hostelcare.app.data.model.ComplaintCategory.FURNITURE
                            else -> com.hostelcare.app.data.model.ComplaintCategory.OTHER
                        }
                        val complaint = com.hostelcare.app.data.model.Complaint(
                            title = title,
                            description = desc,
                            category = catEnum,
                            hostelBlock = hostelBlock,
                            roomNumber = room,
                            studentId = user?.studentId ?: "UNKNOWN"
                        )
                        viewModel.startNewComplaint(complaint)
                        viewModel.submitComplaint()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmitting && !user?.hostelName.isNullOrBlank() && title.isNotBlank() && desc.isNotBlank() && hostelBlock.isNotBlank() && room.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Submit Complaint", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }
            }
        }
    }
    
    if (showPhotoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Add Photo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                Spacer(Modifier.height(16.dp))
                ListItem(
                    headlineContent = { Text("Choose from Gallery", color = MaterialTheme.colorScheme.onSurface) },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, null, tint = PrimaryBlue) },
                    modifier = Modifier.clickable {
                        showPhotoSheet = false
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Take Photo", color = MaterialTheme.colorScheme.onSurface) },
                    leadingContent = { Icon(Icons.Default.CameraAlt, null, tint = PrimaryBlue) },
                    modifier = Modifier.clickable {
                        showPhotoSheet = false
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}