package com.hostelcare.app.ui.screens.student

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
fun StudentHomeScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    
    val user by viewModel.currentUser.collectAsStateWithLifecycle(null)
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()

    val activeCount = complaints.count { it.status == ComplaintStatus.IN_PROGRESS || it.status == ComplaintStatus.ASSIGNED }
    val pendingCount = complaints.count { it.status == ComplaintStatus.SUBMITTED || it.status == ComplaintStatus.UNDER_REVIEW }
    val resolvedCount = complaints.count { it.status == ComplaintStatus.RESOLVED }
    val recentComplaints = complaints.take(3)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HostelCare") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.PROFILE) }) {
                        Text("Profile") // Replace with Icon in real app
                    }
                }
            )
        },
        bottomBar = { StudentBottomNavigation(navController, Routes.STUDENT_HOME) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Welcome back, ${user?.name ?: ""}", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate(Routes.NEW_COMPLAINT) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Report a Problem")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatCard("Active", activeCount.toString())
                StatCard("Pending", pendingCount.toString())
                StatCard("Resolved", resolvedCount.toString())
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Recent Complaints", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn {
                if (recentComplaints.isEmpty()) {
                    item { Text("No recent complaints.") }
                }
                items(recentComplaints) { complaint ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            navController.navigate(Routes.complaintDetails(complaint.id))
                        }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(complaint.title, style = MaterialTheme.typography.titleSmall)
                            Text("Status: ${complaint.status.name}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String) {
    Card(modifier = Modifier.width(100.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(title, style = MaterialTheme.typography.bodySmall)
        }
    }
}
