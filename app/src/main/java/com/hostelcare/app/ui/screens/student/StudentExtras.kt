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
import com.hostelcare.app.ui.components.StudentBottomNavigation
import com.hostelcare.app.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Notifications") }) },
        bottomBar = { StudentBottomNavigation(navController, Routes.NOTIFICATIONS) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (notifications.isEmpty()) {
                item { Text("No notifications.") }
            }
            items(notifications) { notif ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                        viewModel.markNotificationRead(notif.id)
                        notif.complaintId?.let { navController.navigate(Routes.complaintDetails(it)) }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(notif.title, style = MaterialTheme.typography.titleSmall)
                        Text(notif.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
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
        topBar = { TopAppBar(title = { Text("Profile") }) },
        bottomBar = { StudentBottomNavigation(navController, Routes.PROFILE) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (user != null) {
                Text(user!!.name, style = MaterialTheme.typography.headlineMedium)
                Text(user!!.studentId)
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Email: ${user!!.email}")
                        Text("Hostel: ${user!!.hostelBlock}")
                        Text("Room: ${user!!.roomNumber}")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { navController.navigate(Routes.EDIT_PROFILE) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Edit Profile")
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.logout()
                        navController.navigate(Routes.SPLASH) { popUpTo(0) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Logout")
                }
            }
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
    var hostelBlock by remember { mutableStateOf(user?.hostelBlock ?: "") }
    var room by remember { mutableStateOf(user?.roomNumber ?: "") }

    Scaffold(topBar = { TopAppBar(title = { Text("Edit Profile") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = hostelBlock, onValueChange = { hostelBlock = it }, label = { Text("Hostel / Block") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Room") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.updateProfile(name, email, hostelBlock, room)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(navController: NavController, app: HostelCareApp, complaintId: String) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    
    var rating by remember { mutableStateOf(5) }
    var note by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Complaint Feedback") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Rating (1-5)", style = MaterialTheme.typography.titleMedium)
            Slider(value = rating.toFloat(), onValueChange = { rating = it.toInt() }, valueRange = 1f..5f, steps = 3)
            Text(rating.toString(), style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Optional feedback") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.submitFeedback(complaintId, rating, note)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit Feedback")
            }
        }
    }
}
