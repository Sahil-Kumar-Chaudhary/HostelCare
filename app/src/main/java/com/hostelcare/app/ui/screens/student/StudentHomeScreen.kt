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
    
    val activeCount = complaints.count { it.status == ComplaintStatus.IN_PROGRESS || it.status == ComplaintStatus.ASSIGNED || it.status == ComplaintStatus.UNDER_REVIEW }
    val pendingCount = complaints.count { it.status == ComplaintStatus.SUBMITTED }
    val resolvedCount = complaints.count { it.status == ComplaintStatus.RESOLVED }

    Scaffold(
        bottomBar = { StudentBottomNavigation(navController, Routes.STUDENT_HOME) },
        containerColor = Color.White
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color.White)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(LightBlue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Home, null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("HostelCare", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("Student Portal", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.NotificationsNone, null, tint = TextDark, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(40.dp).background(GrayBg, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = TextMuted)
                }
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Good morning, ${user?.name?.split(" ")?.firstOrNull() ?: "Student"}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Need something fixed in your room?", style = MaterialTheme.typography.bodyLarge, color = TextMuted)
                    
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
                        StatCard("Active", activeCount.toString(), Icons.Default.Description, LightBlue, PrimaryBlue, Modifier.weight(1f))
                        StatCard("Pending", pendingCount.toString(), Icons.Default.Schedule, OrangePillBg, WarningOrange, Modifier.weight(1f))
                        StatCard("Resolved", resolvedCount.toString(), Icons.Default.CheckCircle, GreenPillBg, SuccessGreen, Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("My Complaints", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                        Spacer(modifier = Modifier.weight(1f))
                        Row(modifier = Modifier.clickable { navController.navigate(Routes.MY_COMPLAINTS) }, verticalAlignment = Alignment.CenterVertically) {
                            Text("View all", color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.ChevronRight, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                items(complaints.take(3)) { c ->
                    val (icon, bg, fg) = when(c.status) {
                        ComplaintStatus.RESOLVED -> Triple(Icons.Default.Build, GreenPillBg, SuccessGreen)
                        ComplaintStatus.SUBMITTED -> Triple(Icons.Default.Wifi, OrangePillBg, WarningOrange)
                        else -> Triple(Icons.Default.WaterDrop, LightBlue, PrimaryBlue)
                    }
                    val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(c.createdAt))

                    Column(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Routes.complaintDetails(c.id)) }) {
                        Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextDark)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Room ${c.roomNumber} Â· ${c.category.name}", fontSize = 12.sp, color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Box(modifier = Modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text(c.status.name, color = fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(dateStr, fontSize = 12.sp, color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickActionCard("Report\nProblem", Icons.Default.Build, LightBlue, PrimaryBlue, Modifier.weight(1f)) { navController.navigate(Routes.NEW_COMPLAINT) }
                        QuickActionCard("Hostel\nGuidelines", Icons.Default.MenuBook, Color(0xFFF9FAFB), TextMuted, Modifier.weight(1f)) { 
                            android.widget.Toast.makeText(context, "Guidelines opened", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        QuickActionCard("Contact\nWarden", Icons.Default.Phone, Color(0xFFF9FAFB), TextMuted, Modifier.weight(1f)) { 
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6))
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(label, fontSize = 12.sp, color = TextMuted)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun QuickActionCard(label: String, icon: ImageVector, bg: Color, fg: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, color = fg, textAlign = TextAlign.Center, lineHeight = 16.sp)
        }
    }
}

@Composable
fun FormField(label: String, required: Boolean = false, content: @Composable () -> Unit) {
    Column {
        Row {
            Text(label, fontSize = 13.sp, color = TextDark)
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

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("New Complaint", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                FormField("Problem Title", required = true) {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        placeholder = { Text("e.g. Water leakage in bathroom", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Description, null, tint = TextMuted) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue)
                    )
                }
            }
            
            item {
                FormField("Category", required = false) {
                    Box {
                        OutlinedTextField(
                            value = category, onValueChange = { },
                            placeholder = { Text("Select category", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Build, null, tint = TextMuted) },
                            trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted) },
                            modifier = Modifier.fillMaxWidth(), readOnly = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue)
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { categoryExpanded = true })
                        DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }, modifier = Modifier.background(Color.White)) {
                            listOf("Plumbing", "Electrical", "Internet/WiFi", "Cleaning", "Furniture", "Other").forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; categoryExpanded = false })
                            }
                        }
                    }
                }
            }
            
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FormField("Hostel / Block", required = true) {
                            Box {
                                OutlinedTextField(
                                    value = hostelBlock, onValueChange = { },
                                    placeholder = { Text("Block", color = TextMuted) },
                                    leadingIcon = { Icon(Icons.Default.LocationCity, null, tint = TextMuted) },
                                    trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted) },
                                    modifier = Modifier.fillMaxWidth(), readOnly = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue)
                                )
                                Box(modifier = Modifier.matchParentSize().clickable { hostelExpanded = true })
                                DropdownMenu(expanded = hostelExpanded, onDismissRequest = { hostelExpanded = false }, modifier = Modifier.background(Color.White)) {
                                    listOf("Tagore Block A", "Tagore Block B", "Raman Block A", "Raman Block B", "Curie Block", "Newton Block").forEach { block ->
                                        DropdownMenuItem(text = { Text(block) }, onClick = { hostelBlock = block; hostelExpanded = false })
                                    }
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        FormField("Room Number", required = true) {
                            OutlinedTextField(
                                value = room, onValueChange = { room = it },
                                placeholder = { Text("e.g. 101", color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.MeetingRoom, null, tint = TextMuted) },
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue)
                            )
                        }
                    }
                }
            }
            
            item {
                FormField("Description", required = true) {
                    OutlinedTextField(
                        value = desc, onValueChange = { desc = it },
                        placeholder = { Text("Provide details about the issue...", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue)
                    )
                }
            }
            
            item {
                FormField("Add Photo (Optional)", required = false) {
                    if (selectedImageUri != null) {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            UriImage(selectedImageUri.toString(), modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(12.dp), fallbackIcon = Icons.Default.HideImage)
                            Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                                IconButton(onClick = { showPhotoSheet = true }, modifier = Modifier.background(Color.White.copy(alpha=0.7f), CircleShape).size(36.dp)) {
                                    Icon(Icons.Default.Edit, "Change photo", tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { 
                                    selectedImageUri = null
                                    viewModel.selectedPhotoBytes = null
                                    viewModel.selectedPhotoMimeType = null
                                }, modifier = Modifier.background(Color.White.copy(alpha=0.7f), CircleShape).size(36.dp)) {
                                    Icon(Icons.Default.Close, "Remove photo", tint = DangerRed, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    } else {
                        Column {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(120.dp).background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).clickable { showPhotoSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Tap to add photo", color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Supports JPG, PNG (Max 5 MB)", color = TextMuted, fontSize = 11.sp)
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
                    enabled = !isSubmitting && title.isNotBlank() && desc.isNotBlank() && hostelBlock.isNotBlank() && room.isNotBlank(),
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
            containerColor = Color.White
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Add Photo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                Spacer(Modifier.height(16.dp))
                ListItem(
                    headlineContent = { Text("Choose from Gallery") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, null, tint = PrimaryBlue) },
                    modifier = Modifier.clickable {
                        showPhotoSheet = false
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Take Photo") },
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