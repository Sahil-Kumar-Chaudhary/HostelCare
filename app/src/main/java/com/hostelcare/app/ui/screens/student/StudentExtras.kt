package com.hostelcare.app.ui.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val user by viewModel.currentUser.collectAsStateWithLifecycle(null)

    Scaffold(
        bottomBar = { StudentBottomNavigation(navController, Routes.PROFILE) },
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (user != null) {
                item {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(80.dp).background(GrayBg, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp), tint = TextMuted) }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(user!!.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Resident Student · ${user!!.studentId}", color = TextMuted)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.navigate(Routes.EDIT_PROFILE) }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                            Text("Edit Profile")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                item {
                    Text("Details", fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.padding(bottom = 8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight), shape = RoundedCornerShape(12.dp)) {
                        Column {
                            ProfileRow(Icons.Default.Business, "Hostel", user!!.hostelBlock)
                            HorizontalDivider(color = GrayBg)
                            ProfileRow(Icons.Default.MeetingRoom, "Room", user!!.roomNumber)
                            HorizontalDivider(color = GrayBg)
                            ProfileRow(Icons.Default.Email, "Email", user!!.email)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.logout(); navController.navigate(Routes.SPLASH) { popUpTo(0) } },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) { Text("Log Out") }
                }
            }
        }
    }
}

@Composable
fun ProfileRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontSize = 12.sp, color = TextMuted)
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = TextDark)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val user by viewModel.currentUser.collectAsStateWithLifecycle(null)
    
    var name by remember { mutableStateOf(user?.name ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var hostel by remember { mutableStateOf(user?.hostelBlock ?: "") }
    var room by remember { mutableStateOf(user?.roomNumber ?: "") }
    var phone by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Profile", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)) },
        containerColor = BackgroundLight
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(80.dp).background(GrayBg, CircleShape), contentAlignment = Alignment.Center) { 
                Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp), tint = TextMuted)
                Icon(Icons.Default.Edit, null, tint = PrimaryBlue, modifier = Modifier.align(Alignment.BottomEnd).background(SurfaceLight, CircleShape).padding(4.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            CustomTextField(name, { name = it }, "Full Name")
            Spacer(modifier = Modifier.height(12.dp))
            CustomTextField(email, { email = it }, "Email")
            Spacer(modifier = Modifier.height(12.dp))
            CustomTextField(hostel, { hostel = it }, "Hostel / Block")
            Spacer(modifier = Modifier.height(12.dp))
            CustomTextField(room, { room = it }, "Room Number")
            Spacer(modifier = Modifier.height(12.dp))
            CustomTextField(phone, { phone = it }, "Phone")
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { 
                    viewModel.updateProfile(name, email, hostel, room)
                    navController.popBackStack() 
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) { Text("Save Changes") }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { navController.popBackStack() }) { Text("Cancel", color = TextMuted) }
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
