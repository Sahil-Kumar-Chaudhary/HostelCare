package com.hostelcare.app.ui.screens.staff

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.ui.screens.auth.CustomTextField
import com.hostelcare.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffTaskDetailsScreen(
    navController: NavController,
    app: HostelCareApp,
    taskId: String
) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StaffViewModel = viewModel(factory = factory)

    val taskDetailState by viewModel.taskDetailState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCompleteDialog by remember { mutableStateOf(false) }
    var workNoteInput by remember { mutableStateOf("") }
    var isSavingNote by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        viewModel.loadTaskDetail(taskId)
    }

    if (showCompleteDialog) {
        var completionNote by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showCompleteDialog = false },
            title = { Text("Submit for Completion Review", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Describe the repair work carried out. The warden will review and mark the complaint resolved.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CustomTextField(
                        value = completionNote,
                        onValueChange = { completionNote = it },
                        placeholder = "e.g. Replaced washer in bathroom tap; tested water flow."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.submitCompletion(
                            id = taskId,
                            completionNotes = completionNote.trim().ifBlank { null },
                            completionPhotoUrl = null,
                            onComplete = { success, err ->
                                isSubmitting = false
                                showCompleteDialog = false
                                scope.launch {
                                    if (success) {
                                        snackbarHostState.showSnackbar("Work submitted for warden review")
                                    } else {
                                        snackbarHostState.showSnackbar(err ?: "Failed to submit completion")
                                    }
                                }
                            }
                        )
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Submit Review")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }, enabled = !isSubmitting) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (taskDetailState is StaffTaskDetailUiState.Success) {
                            (taskDetailState as StaffTaskDetailUiState.Success).task.ticketId ?: "Task Details"
                        } else {
                            "Task Details"
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadTaskDetail(taskId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        when (val state = taskDetailState) {
            is StaffTaskDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is StaffTaskDetailUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = DangerRed, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadTaskDetail(taskId) }) {
                            Text("Retry")
                        }
                    }
                }
            }

            is StaffTaskDetailUiState.Success -> {
                val task = state.task
                val isAssigned = task.status.lowercase() == "assigned"
                val isInProgress = task.status.lowercase() == "in_progress"
                val isUnderReview = task.status.lowercase() == "under_review"
                val isResolved = task.status.lowercase() == "resolved"

                val statusColor = when (task.status.lowercase()) {
                    "resolved" -> SuccessGreen
                    "in_progress" -> PrimaryBlue
                    "under_review" -> Color(0xFFD97706)
                    else -> PendingAmber
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Task Status Header Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = task.category.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = statusColor.copy(alpha = 0.12f),
                                    border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(statusColor)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = task.status.replace("_", " ").replaceFirstChar { it.uppercase() },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = statusColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = task.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = task.description ?: "No description provided.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // Location & Student Contact Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Location & Student", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            val rm = if (!task.roomNumber.isNullOrBlank()) "Room ${task.roomNumber}" else "Hostel"
                            val bl = if (!task.hostelBlock.isNullOrBlank()) " (${task.hostelBlock})" else ""
                            Text("Location: $rm$bl", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Student: ${task.student?.name ?: "-"}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            if (!task.student?.phone.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Contact Phone: ${task.student!!.phone}", fontSize = 13.sp, color = PrimaryBlue)
                            }
                        }
                    }

                    // Warden Instructions Card (if any)
                    if (!task.assignmentInstructions.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PrimaryBlue.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Warden Instructions", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(task.assignmentInstructions, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    // Action Controls for Staff Member
                    if (isAssigned) {
                        Button(
                            onClick = {
                                viewModel.updateTaskProgress(
                                    id = taskId,
                                    status = "in_progress",
                                    workNotes = "Staff started work",
                                    onComplete = { success, err ->
                                        scope.launch {
                                            if (success) snackbarHostState.showSnackbar("Work started! Status is now In Progress")
                                            else snackbarHostState.showSnackbar(err ?: "Failed to update status")
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Work on this Task", fontWeight = FontWeight.SemiBold)
                        }
                    } else if (isInProgress) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Work Progress Notes", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                                if (!task.workNotes.isNullOrBlank()) {
                                    Text("Current notes: ${task.workNotes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                CustomTextField(
                                    value = workNoteInput,
                                    onValueChange = { workNoteInput = it },
                                    placeholder = "Add latest progress note..."
                                )

                                Button(
                                    onClick = {
                                        if (workNoteInput.isBlank()) return@Button
                                        isSavingNote = true
                                        viewModel.updateTaskProgress(
                                            id = taskId,
                                            status = null,
                                            workNotes = workNoteInput.trim(),
                                            onComplete = { success, err ->
                                                isSavingNote = false
                                                if (success) {
                                                    workNoteInput = ""
                                                    scope.launch { snackbarHostState.showSnackbar("Progress note saved") }
                                                }
                                            }
                                        )
                                    },
                                    enabled = !isSavingNote && workNoteInput.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Note")
                                }
                            }
                        }

                        Button(
                            onClick = { showCompleteDialog = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit Work for Completion Review", fontWeight = FontWeight.SemiBold)
                        }
                    } else if (isUnderReview) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFD97706))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Work completed! Submitted for Warden review and sign-off.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    } else if (isResolved) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SuccessGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Task marked fully resolved and verified by hostel warden.",
                                    fontSize = 13.sp,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
