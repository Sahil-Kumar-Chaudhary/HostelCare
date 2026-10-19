package com.hostelcare.app.ui.screens.staff

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffUpdateStatusScreen(
    navController: NavController,
    app: HostelCareApp,
    taskId: String
) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StaffViewModel = viewModel(factory = factory)

    val taskDetailState by viewModel.taskDetailState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var notesText by remember { mutableStateOf("Started fixing the tap and checking pipeline.") }
    var selectedPhotoUrl by remember { mutableStateOf<String?>("https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=500") }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        viewModel.loadTaskDetail(taskId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                    Text(
                        text = "Update Status",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        when (val state = taskDetailState) {
            is StaffTaskDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
            }
            is StaffTaskDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message, color = Color(0xFFDC2626))
                }
            }
            is StaffTaskDetailUiState.Success -> {
                val task = state.task
                val currentStatus = task.status.lowercase()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Vertical Stepper
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                // Step 1: Assigned (Completed check)
                                StepperRow(
                                    title = "Assigned",
                                    subtitle = "12 Oct 2026, 08:17 AM\nAssigned by Warden",
                                    stepState = StepperState.COMPLETED,
                                    isLast = false
                                )

                                // Step 2: In Progress (Active)
                                StepperRow(
                                    title = "In Progress",
                                    subtitle = "Mark this task as in progress when you start working on it.",
                                    stepState = if (currentStatus == "assigned") StepperState.ACTIVE else StepperState.COMPLETED,
                                    isLast = false
                                )

                                // Step 3: Submitted (Pending)
                                StepperRow(
                                    title = "Submitted",
                                    subtitle = "Submit after completing the work. Warden will review it.",
                                    stepState = if (currentStatus == "under_review") StepperState.COMPLETED else StepperState.PENDING,
                                    isLast = false
                                )

                                // Step 4: Completed (Pending)
                                StepperRow(
                                    title = "Completed",
                                    subtitle = "Marked as resolved by Warden.",
                                    stepState = if (currentStatus == "resolved") StepperState.COMPLETED else StepperState.PENDING,
                                    isLast = true
                                )
                            }
                        }

                        // Add Notes Section
                        Column {
                            Text(
                                text = "Add Notes (Optional)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = notesText,
                                onValueChange = { notesText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                placeholder = {
                                    Text("Add any remarks or details about the work...", color = Color(0xFF94A3B8), fontSize = 14.sp)
                                }
                            )
                        }

                        // Upload Photo Section
                        Column {
                            Text(
                                text = "Upload Photo (Optional)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedPhotoUrl != null) {
                                    Box(
                                        modifier = Modifier.size(80.dp),
                                        contentAlignment = Alignment.TopEnd
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(76.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFE2E8F0)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = "Uploaded photo",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1E293B))
                                                .clickable { selectedPhotoUrl = null },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clickable {
                                            selectedPhotoUrl = "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=500"
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add photo",
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Action Button
                    Surface(
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            val buttonText = if (currentStatus == "assigned") "Update to In Progress" else "Submit for Review"

                            Button(
                                onClick = {
                                    isSubmitting = true
                                    if (currentStatus == "assigned") {
                                        viewModel.updateTaskProgress(taskId, "in_progress", notesText) { success, err ->
                                            isSubmitting = false
                                            if (success) {
                                                navController.navigate(Routes.staffTaskDetails(taskId)) {
                                                    popUpTo(Routes.STAFF_HOME)
                                                }
                                            } else {
                                                scope.launch { snackbarHostState.showSnackbar(err ?: "Update failed") }
                                            }
                                        }
                                    } else {
                                        viewModel.submitCompletion(
                                            id = taskId,
                                            completionNotes = notesText,
                                            completionPhotoUrl = selectedPhotoUrl
                                        ) { success, err ->
                                            isSubmitting = false
                                            if (success) {
                                                navController.navigate(Routes.staffWorkCompleted(taskId))
                                            } else {
                                                scope.launch { snackbarHostState.showSnackbar(err ?: "Submission failed") }
                                            }
                                        }
                                    }
                                },
                                enabled = !isSubmitting,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class StepperState {
    COMPLETED,
    ACTIVE,
    PENDING
}

@Composable
fun StepperRow(
    title: String,
    subtitle: String,
    stepState: StepperState,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            when (stepState) {
                StepperState.COMPLETED -> {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                StepperState.ACTIVE -> {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF))
                            .border(2.dp, Color(0xFF2563EB), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                        )
                    }
                }
                StepperState.PENDING -> {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .border(1.5.dp, Color(0xFFCBD5E1), CircleShape)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(44.dp)
                        .background(if (stepState == StepperState.COMPLETED) Color(0xFF2563EB) else Color(0xFFE2E8F0))
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 16.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = if (stepState != StepperState.PENDING) FontWeight.Bold else FontWeight.SemiBold,
                color = if (stepState != StepperState.PENDING) Color(0xFF0F172A) else Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                lineHeight = 16.sp
            )
        }
    }
}
