package com.hostelcare.app.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.data.model.Complaint
import com.hostelcare.app.data.model.ComplaintCategory
import com.hostelcare.app.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewComplaintScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    
    val user by viewModel.currentUser.collectAsStateWithLifecycle(null)

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ComplaintCategory.OTHER) }
    var hostelBlock by remember { mutableStateOf("") }
    var roomNumber by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("New Complaint") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Problem Title") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = hostelBlock, onValueChange = { hostelBlock = it }, label = { Text("Hostel / Block") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = roomNumber, onValueChange = { roomNumber = it }, label = { Text("Room Number") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            
            // Simple category dropdown simulation
            Text("Category")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ComplaintCategory.values().take(3).forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.name) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { /* Simulated Image Picker */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Attach Photo (Optional)")
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val complaint = Complaint(
                        title = title,
                        description = description,
                        category = category,
                        hostelBlock = hostelBlock,
                        roomNumber = roomNumber,
                        studentId = user?.id ?: ""
                    )
                    viewModel.startNewComplaint(complaint)
                    navController.navigate(Routes.AI_REVIEW)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && description.isNotBlank()
            ) {
                Text("Continue to Review")
            }
        }
    }
}
