package com.hostelcare.app.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.data.model.ComplaintStatus
import com.hostelcare.app.ui.navigation.Routes
import com.hostelcare.app.ui.screens.student.StatCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AdminViewModel = viewModel(factory = factory)
    
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    val pendingCount = complaints.count { it.status == ComplaintStatus.SUBMITTED || it.status == ComplaintStatus.UNDER_REVIEW }
    val inProgressCount = complaints.count { it.status == ComplaintStatus.ASSIGNED || it.status == ComplaintStatus.IN_PROGRESS }
    val resolvedCount = complaints.count { it.status == ComplaintStatus.RESOLVED }
    val needsAttention = complaints.filter { it.priority.name == "HIGH" || it.priority.name == "EMERGENCY" && it.status != ComplaintStatus.RESOLVED }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Staff Triage Home") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, "Complaints") },
                    label = { Text("Complaints") },
                    selected = true,
                    onClick = { navController.navigate(Routes.ADMIN_COMPLAINTS) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, "Profile") },
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        viewModel.logout()
                        navController.navigate(Routes.SPLASH) { popUpTo(0) }
                    }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatCard("Pending", pendingCount.toString())
                StatCard("In Action", inProgressCount.toString())
                StatCard("Resolved", resolvedCount.toString())
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Needs Attention", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(needsAttention) { c ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { navController.navigate(Routes.adminComplaintDetails(c.id)) }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(c.title, style = MaterialTheme.typography.titleMedium)
                            Text("Priority: ${c.priority.name}", color = MaterialTheme.colorScheme.error)
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
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<ComplaintStatus?>(null) }
    
    val filtered = complaints.filter {
        (searchQuery.isBlank() || it.title.contains(searchQuery, true)) &&
        (selectedStatus == null || it.status == selectedStatus)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("All Complaints") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, label = { Text("Search") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(selected = selectedStatus == null, onClick = { selectedStatus = null }, label = { Text("All") })
                FilterChip(selected = selectedStatus == ComplaintStatus.SUBMITTED, onClick = { selectedStatus = ComplaintStatus.SUBMITTED }, label = { Text("New") })
                FilterChip(selected = selectedStatus == ComplaintStatus.IN_PROGRESS, onClick = { selectedStatus = ComplaintStatus.IN_PROGRESS }, label = { Text("Active") })
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(filtered) { c ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { navController.navigate(Routes.adminComplaintDetails(c.id)) }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(c.title, style = MaterialTheme.typography.titleMedium)
                            Text(c.status.name)
                        }
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
    val complaint = complaints.find { it.id == complaintId }

    Scaffold(topBar = { TopAppBar(title = { Text("Complaint Details") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (complaint != null) {
                Text(complaint.title, style = MaterialTheme.typography.headlineMedium)
                Text("Student ID: ${complaint.studentId}")
                Text("Room: ${complaint.hostelBlock}-${complaint.roomNumber}")
                Text("Status: ${complaint.status.name}")
                Spacer(modifier = Modifier.height(16.dp))
                Text(complaint.description)
                if (complaint.aiSummary != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI Summary: ${complaint.aiSummary}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { navController.navigate(Routes.assignStaff(complaint.id)) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Assign Staff")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.updateComplaintStatus(complaint.id, ComplaintStatus.IN_PROGRESS) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = complaint.status != ComplaintStatus.IN_PROGRESS
                ) {
                    Text("Mark In Progress")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate(Routes.resolveComplaint(complaint.id)) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Resolve Complaint")
                }
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
    var selectedStaffId by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Assign Staff") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Select Maintenance Personnel")
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(staffList) { staff ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedStaffId = staff.id }.padding(vertical = 8.dp)) {
                        RadioButton(selected = selectedStaffId == staff.id, onClick = { selectedStaffId = staff.id })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(staff.name, style = MaterialTheme.typography.bodyLarge)
                            Text(staff.roleTitle, style = MaterialTheme.typography.bodySmall)
                            Text(if (staff.isAvailable) "Available" else "Busy", color = if (staff.isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Button(
                onClick = {
                    selectedStaffId?.let {
                        viewModel.assignStaff(complaintId, it)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedStaffId != null
            ) {
                Text("Assign & Notify Staff")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolveComplaintScreen(navController: NavController, app: HostelCareApp, complaintId: String) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AdminViewModel = viewModel(factory = factory)
    var note by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Resolve Complaint") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Resolution Note") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.updateComplaintStatus(complaintId, ComplaintStatus.RESOLVED, note)
                    navController.navigate(Routes.ADMIN_HOME) { popUpTo(0) }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = note.isNotBlank()
            ) {
                Text("Mark as Resolved")
            }
        }
    }
}
