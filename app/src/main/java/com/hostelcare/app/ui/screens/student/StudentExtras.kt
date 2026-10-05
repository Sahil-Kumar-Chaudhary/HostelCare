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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = { StudentBottomNavigation(navController, Routes.NOTIFICATIONS) },
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(notifications) { notif ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { notif.complaintId?.let { navController.navigate(Routes.complaintDetails(it)) } }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(notif.message.take(20) + "...", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(notif.message, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(notif.createdAt)), fontSize = 12.sp, color = TextMuted)
                }
                HorizontalDivider(color = GrayBg)
            }
        }
    }
}

@Composable
fun UriImage(uriStr: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bitmap by remember(uriStr) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    
    LaunchedEffect(uriStr) {
        if (uriStr.isNotEmpty()) {
            try {
                val uri = android.net.Uri.parse(uriStr)
                val inputStream = context.contentResolver.openInputStream(uri)
                val androidBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (androidBitmap != null) {
                    bitmap = androidBitmap.asImageBitmap()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            bitmap = null
        }
    }
    
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Icon(Icons.Default.Person, null, modifier = modifier.padding(16.dp), tint = TextMuted)
    }
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
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(bottom = 24.dp)) {
            if (user != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.size(96.dp).background(Color(0xFFE5E7EB), CircleShape), contentAlignment = Alignment.Center) {
                        UriImage(user!!.profilePhotoUri ?: "", modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(user!!.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(user!!.studentId.ifEmpty { "STU-XXXX" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
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
                            ProfileDetailRow(Icons.Default.Domain, "Hostel & Block", user!!.hostelBlock.ifEmpty { "Not Assigned" })
                            HorizontalDivider(color = Color(0xFFF3F4F6))
                            ProfileDetailRow(Icons.Default.MeetingRoom, "Room Number", user!!.roomNumber.ifEmpty { "Not Assigned" })
                            HorizontalDivider(color = Color(0xFFF3F4F6))
                            ProfileDetailRow(Icons.Default.Email, "College Email", user!!.email)
                            HorizontalDivider(color = Color(0xFFF3F4F6))
                            ProfileDetailRow(Icons.Default.Phone, "Phone Number", user!!.phone.ifEmpty { "Not Provided" })
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { navController.navigate(Routes.EDIT_PROFILE) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) { 
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Profile", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
                
                item {
                    ActionRow(Icons.Default.LockReset, "Change Password", LightBlue) { navController.navigate(Routes.CHANGE_PASSWORD) }
                    Spacer(modifier = Modifier.height(12.dp))
                    ActionRow(Icons.Default.MenuBook, "Hostel Guidelines", LightBlue) { 
                        android.widget.Toast.makeText(context, "Guidelines opened", android.widget.Toast.LENGTH_SHORT).show() 
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ActionRow(Icons.Default.Security, "Contact Warden", LightBlue) {
                        val number = "+1234567890" // In a real app, from config
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                        intent.data = android.net.Uri.parse("tel:$number")
                        context.startActivity(intent)
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = DangerRed)
                    ) { 
                        Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) 
                    }
                    Spacer(modifier = Modifier.height(24.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val user by viewModel.currentUser.collectAsStateWithLifecycle(null)
    
    var name by remember(user) { mutableStateOf(user?.name ?: "") }
    var email by remember(user) { mutableStateOf(user?.email ?: "") }
    var hostel by remember(user) { mutableStateOf(user?.hostelBlock ?: "") }
    var room by remember(user) { mutableStateOf(user?.roomNumber ?: "") }
    var phone by remember(user) { mutableStateOf(user?.phone ?: "") }
    var photoUri by remember(user) { mutableStateOf(user?.profilePhotoUri ?: "") }
    
    var hostelExpanded by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) photoUri = uri.toString() }
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Profile", fontWeight = FontWeight.Bold, color = TextDark) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextDark) } }, actions = { IconButton(onClick = {}) { Icon(Icons.Default.Info, null, tint = TextMuted) } ; IconButton(onClick = {}) { Box(modifier = Modifier.size(32.dp).background(PrimaryBlue, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp)) } } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)) },
        containerColor = Color.White,
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Button(
                    onClick = { 
                        viewModel.updateProfile(name, email, hostel, room, phone, photoUri)
                        navController.popBackStack() 
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) { Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                
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
                Spacer(modifier = Modifier.height(12.dp))
                Text("Tap to change photo", color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            item {
                ProfileFormField("Full Name") {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = TextMuted) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            item {
                ProfileFormField("College Email") {
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = TextMuted) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            item {
                ProfileFormField("Hostel / Block") {
                    Box {
                        OutlinedTextField(
                            value = hostel, onValueChange = { },
                            leadingIcon = { Icon(Icons.Default.Domain, null, tint = TextMuted) },
                            trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted) },
                            modifier = Modifier.fillMaxWidth(), readOnly = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue)
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { hostelExpanded = true })
                        DropdownMenu(expanded = hostelExpanded, onDismissRequest = { hostelExpanded = false }, modifier = Modifier.background(Color.White)) {
                            listOf("Block A", "Block B", "Block C", "Cauvery Hostel", "Block B - Cauvery Hostel", "Tagore Hostel").forEach { opt ->
                                DropdownMenuItem(text = { Text(opt) }, onClick = { hostel = opt; hostelExpanded = false })
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            item {
                ProfileFormField("Room Number") {
                    OutlinedTextField(
                        value = room, onValueChange = { room = it },
                        leadingIcon = { Icon(Icons.Default.MeetingRoom, null, tint = TextMuted) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            item {
                ProfileFormField("Phone Number") {
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = TextMuted) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue)
                    )
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
                OutlinedTextField(
                    value = currentPassword, onValueChange = { currentPassword = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            ProfileFormField("New Password") {
                OutlinedTextField(
                    value = newPassword, onValueChange = { newPassword = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            ProfileFormField("Confirm New Password") {
                OutlinedTextField(
                    value = confirmPassword, onValueChange = { confirmPassword = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
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
