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
import com.hostelcare.app.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiReviewScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    
    val state by viewModel.newComplaintState.collectAsStateWithLifecycle()
    val draft by viewModel.draftComplaint.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (state is NewComplaintState.Idle) {
            viewModel.analyzeComplaint()
        }
    }

    LaunchedEffect(state) {
        if (state is NewComplaintState.Success) {
            viewModel.resetComplaintState()
            navController.navigate(Routes.STUDENT_HOME) { popUpTo(Routes.NEW_COMPLAINT) { inclusive = true } }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("AI Review") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            when (state) {
                is NewComplaintState.Analyzing -> {
                    CircularProgressIndicator()
                    Text("AI is analyzing your complaint...")
                }
                is NewComplaintState.AnalysisComplete -> {
                    val analysis = (state as NewComplaintState.AnalysisComplete).result
                    Text("Complaint Summary", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Title: ${draft?.title}")
                            Text("Category: ${analysis.category.name}")
                            Text("Priority: ${analysis.priority.name}", color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(analysis.summary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.submitComplaint() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm & Submit")
                    }
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Edit Complaint")
                    }
                }
                is NewComplaintState.Submitting -> {
                    CircularProgressIndicator()
                    Text("Submitting...")
                }
                is NewComplaintState.Error -> {
                    Text((state as NewComplaintState.Error).message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.analyzeComplaint() }) {
                        Text("Retry")
                    }
                }
                else -> {}
            }
        }
    }
}
