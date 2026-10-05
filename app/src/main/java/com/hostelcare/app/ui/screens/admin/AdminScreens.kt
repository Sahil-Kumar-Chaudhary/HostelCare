package com.hostelcare.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
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
import com.hostelcare.app.data.model.ComplaintStatus
import com.hostelcare.app.ui.navigation.Routes
import com.hostelcare.app.ui.screens.auth.CustomTextField
import com.hostelcare.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AdminViewModel = viewModel(factory = factory)
    
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    val pendingCount = complaints.count { it.status == ComplaintStatus.SUBMITTED || it.status == ComplaintStatus.UNDER_REVIEW }
    val inProgressCount = complaints.count { it.status == ComplaintStatus.ASSIGNED || it.status == ComplaintStatus.IN_PROGRESS }
    val resolvedCount = complaints.count { it.status == ComplaintStatus.RESOLVED }
    val needsAttention = complaints.filter { (it.priority.name == "HIGH" || it.priority.name == "EMERGENCY") && it.status != ComplaintStatus.RESOLVED }

    Scaffold(
        bottomBar = { com.hostelcare.app.ui.components.AdminBottomNavigation(navController, Routes.ADMIN_HOME) },
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Good morning", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Complaints", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).background(SurfaceLight, RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Column {
                        Text("Pending", fontSize = 12.sp, color = TextMuted)
                        Text("$pendingCount", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(modifier = Modifier.weight(1f).background(SurfaceLight, RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Column {
                        Text("In Progress", fontSize = 12.sp, color = TextMuted)
                        Text("$inProgressCount", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(modifier = Modifier.weight(1f).background(SurfaceLight, RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Column {
                        Text("Resolved", fontSize = 12.sp, color = TextMuted)
                        Text("$resolvedCount", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Needs Attention", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(needsAttention) { c ->
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Routes.adminComplaintDetails(c.id)) }) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(c.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Room ${c.roomNumber} · ${c.priority.name} · ${c.status.name}", fontSize = 14.sp, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminComplaintListScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AdminViewModel = viewModel(factory = factory)
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = { com.hostelcare.app.ui.components.AdminBottomNavigation(navController, Routes.ADMIN_COMPLAINTS) },
        topBar = { TopAppBar(title = { Text("All Complaints", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)) },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(complaints) { c ->
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Routes.adminComplaintDetails(c.id)) }) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(c.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Room ${c.roomNumber} · Status: ${c.status.name}", fontSize = 14.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminComplaintDetailsScreen(navController: NavController, app: HostelCareApp, complaintId: String) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AdminViewModel = viewModel(factory = factory)
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    val complaint = complaints.find { it.id == complaintId } ?: return

    Scaffold(
        topBar = { TopAppBar(title = { Text("Complaint Details", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)) },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(complaint.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Student: ${complaint.studentId}", style = MaterialTheme.typography.bodyMedium)
                        Text("Room: ${complaint.roomNumber}", style = MaterialTheme.typography.bodyMedium)
                        Text("Category: ${complaint.category.name}", style = MaterialTheme.typography.bodyMedium)
                        Text("Priority: ${complaint.priority.name}", style = MaterialTheme.typography.bodyMedium)
                        Text("Status: ${complaint.status.name}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(complaint.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(GrayBg, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Image, null, tint = TextMuted)
                        }
                    }
                }
            }
            if (complaint.aiSummary != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("AI Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(complaint.aiSummary!!, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                Text("Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate(Routes.assignStaff(complaintId)) }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue), shape = RoundedCornerShape(8.dp)) { Text("Assign Staff") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.updateComplaintStatus(complaintId, ComplaintStatus.IN_PROGRESS) }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = LightBlue, contentColor = PrimaryBlue), shape = RoundedCornerShape(8.dp)) { Text("Change Status") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate(Routes.resolveComplaint(complaintId)) }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White), shape = RoundedCornerShape(8.dp)) { Text("Mark as Resolved") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignStaffScreen(navController: NavController, app: HostelCareApp, complaintId: String) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AdminViewModel = viewModel(factory = factory)
    val staffList by viewModel.staffList.collectAsStateWithLifecycle()
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    val complaint = complaints.find { it.id == complaintId }
    var selectedId by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Assign Staff", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)) },
        containerColor = BackgroundLight
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (complaint != null) {
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(complaint.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Room ${complaint.roomNumber} · ${complaint.category.name}", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text("${complaint.priority.name} Priority", style = MaterialTheme.typography.bodyMedium, color = DangerRed)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            Text("Select Staff", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(staffList) { staff ->
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().clickable { selectedId = staff.id }) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedId == staff.id, onClick = { selectedId = staff.id }, colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(staff.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("${staff.roleTitle} · ${if(staff.isAvailable) "Available" else "Busy"}", fontSize = 14.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { 
                    selectedId?.let {
                        viewModel.assignStaff(complaintId, it)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = selectedId != null,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) { Text("Assign Staff") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolveComplaintScreen(navController: NavController, app: HostelCareApp, complaintId: String) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AdminViewModel = viewModel(factory = factory)
    var note by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Resolve Complaint", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)) },
        containerColor = BackgroundLight
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CustomTextField(note, { note = it }, "Resolution Note")
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.updateComplaintStatus(complaintId, ComplaintStatus.RESOLVED, note)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = note.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) { Text("Confirm Resolution") }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AdminViewModel = viewModel(factory = factory)
    
    Scaffold(
        bottomBar = { com.hostelcare.app.ui.components.AdminBottomNavigation(navController, Routes.ADMIN_PROFILE) },
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Staff Profile", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    viewModel.logout()
                    navController.navigate(Routes.SPLASH) { popUpTo(0) }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
            ) { Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}
