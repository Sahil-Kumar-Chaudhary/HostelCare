package com.hostelcare.app.ui.screens.student

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.hostelcare.app.ui.components.StudentBottomNavigation
import com.hostelcare.app.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyComplaintsScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredComplaints = when (selectedFilter) {
        "Active" -> complaints.filter { it.status != ComplaintStatus.RESOLVED }
        "Resolved" -> complaints.filter { it.status == ComplaintStatus.RESOLVED }
        else -> complaints
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Complaints") }) },
        bottomBar = { StudentBottomNavigation(navController, Routes.MY_COMPLAINTS) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.NEW_COMPLAINT) }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("All", "Active", "Resolved").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (filteredComplaints.isEmpty()) {
                Text("No complaints found.")
            } else {
                LazyColumn {
                    items(filteredComplaints) { complaint ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                navController.navigate(Routes.complaintDetails(complaint.id))
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(complaint.title, style = MaterialTheme.typography.titleMedium)
                                Text("${complaint.category.name} • ${complaint.status.name}", style = MaterialTheme.typography.bodySmall)
                                Text("Priority: ${complaint.priority.name}", color = if (complaint.priority.name == "HIGH") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintDetailsScreen(navController: NavController, app: HostelCareApp, complaintId: String) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    val complaint = complaints.find { it.id == complaintId }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Complaint Details") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (complaint == null) {
                Text("Complaint not found.")
            } else {
                Text(complaint.title, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Status: ${complaint.status.name}")
                        Text("Category: ${complaint.category.name}")
                        Text("Priority: ${complaint.priority.name}")
                        Text("Location: ${complaint.hostelBlock}, Room ${complaint.roomNumber}")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Description", style = MaterialTheme.typography.titleMedium)
                Text(complaint.description)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (complaint.assignedStaffId != null) {
                    Text("Assigned Staff: ${complaint.assignedStaffId}", style = MaterialTheme.typography.bodyMedium)
                }
                
                if (complaint.resolutionNote != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Resolution Note", style = MaterialTheme.typography.titleSmall)
                            Text(complaint.resolutionNote)
                        }
                    }
                }
                
                if (complaint.status == ComplaintStatus.RESOLVED && complaint.feedbackRating == null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.navigate(Routes.feedback(complaint.id)) }) {
                        Text("Leave Feedback")
                    }
                }
            }
        }
    }
}
