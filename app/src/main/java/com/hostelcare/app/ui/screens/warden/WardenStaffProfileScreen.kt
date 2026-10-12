package com.hostelcare.app.ui.screens.warden

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.data.remote.UpdateStaffRequest
import com.hostelcare.app.ui.navigation.Routes
import com.hostelcare.app.ui.screens.auth.CustomTextField
import com.hostelcare.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardenStaffProfileScreen(
    navController: NavController,
    app: HostelCareApp,
    staffId: String
) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: WardenViewModel = viewModel(factory = factory)

    val detailState by viewModel.staffDetailState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showEditDialog by remember { mutableStateOf(false) }
    var showToggleConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(staffId) {
        viewModel.loadStaffDetail(staffId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Staff Profile",
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
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Staff",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        when (val state = detailState) {
            is WardenStaffDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is WardenStaffDetailUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = DangerRed, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadStaffDetail(staffId) }) {
                            Text("Retry")
                        }
                    }
                }
            }

            is WardenStaffDetailUiState.Success -> {
                val staff = state.staff
                val statusColor = if (staff.isActive) SuccessGreen else DangerRed
                val statusText = if (staff.isActive) "Active Account" else "Deactivated Account"

                // Edit Staff Dialog
                if (showEditDialog) {
                    var editPhone by remember { mutableStateOf(staff.phone ?: "") }
                    var editDept by remember { mutableStateOf(staff.department) }
                    var editBlock by remember { mutableStateOf(staff.hostelBlock ?: "") }
                    var isSaving by remember { mutableStateOf(false) }

                    AlertDialog(
                        onDismissRequest = { if (!isSaving) showEditDialog = false },
                        title = { Text("Edit Staff Details", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Department", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                CustomTextField(value = editDept, onValueChange = { editDept = it }, placeholder = "Department")

                                Text("Phone Number", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                CustomTextField(value = editPhone, onValueChange = { editPhone = it }, placeholder = "Phone")

                                Text("Assigned Block", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                CustomTextField(value = editBlock, onValueChange = { editBlock = it }, placeholder = "Block")
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    isSaving = true
                                    viewModel.updateStaff(
                                        id = staff.id,
                                        request = UpdateStaffRequest(
                                            phone = editPhone.trim(),
                                            department = editDept.trim(),
                                            hostelBlock = editBlock.trim().ifBlank { null }
                                        ),
                                        onComplete = { success, err ->
                                            isSaving = false
                                            showEditDialog = false
                                            scope.launch {
                                                if (success) snackbarHostState.showSnackbar("Staff member updated")
                                                else snackbarHostState.showSnackbar(err ?: "Failed to update")
                                            }
                                        }
                                    )
                                },
                                enabled = !isSaving
                            ) {
                                Text("Save Changes")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditDialog = false }, enabled = !isSaving) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Toggle Active Status Confirm Dialog
                if (showToggleConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showToggleConfirmDialog = false },
                        title = {
                            Text(
                                text = if (staff.isActive) "Deactivate Staff Account?" else "Activate Staff Account?",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = if (staff.isActive) {
                                    "When deactivated, this staff member will not be able to log in or be assigned new tasks."
                                } else {
                                    "This staff member will be reactivated and will be able to log in and receive task assignments."
                                }
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showToggleConfirmDialog = false
                                    viewModel.toggleStaffStatus(
                                        id = staff.id,
                                        isActive = !staff.isActive,
                                        onComplete = { success, err ->
                                            scope.launch {
                                                if (success) {
                                                    snackbarHostState.showSnackbar("Staff account ${if (!staff.isActive) "activated" else "deactivated"}")
                                                } else {
                                                    snackbarHostState.showSnackbar(err ?: "Failed to update status")
                                                }
                                            }
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (staff.isActive) DangerRed else SuccessGreen
                                )
                            ) {
                                Text(if (staff.isActive) "Deactivate" else "Activate")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showToggleConfirmDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Staff Profile Header Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = staff.name.take(2).uppercase(),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = staff.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = staff.department,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = staff.staffId,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
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
                                            text = statusText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = statusColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Task Statistics Row (4 Cards)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMetricCard(
                            label = "Total",
                            value = staff.stats.totalAssigned.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            label = "In Progress",
                            value = staff.stats.inProgressCount.toString(),
                            color = PrimaryBlue,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            label = "Completed",
                            value = staff.stats.completedCount.toString(),
                            color = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            label = "Pending",
                            value = staff.stats.pendingCount.toString(),
                            color = PendingAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Contact & Assignment Info Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Contact & Assignment",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            InfoRow(icon = Icons.Default.Email, label = "Email", value = staff.email)
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow(icon = Icons.Default.Phone, label = "Phone", value = staff.phone?.ifBlank { "Not provided" } ?: "Not provided")
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow(icon = Icons.Default.Apartment, label = "Hostel", value = staff.hostelName ?: "Main Hostel")
                            if (!staff.hostelBlock.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                InfoRow(icon = Icons.Default.MeetingRoom, label = "Assigned Block", value = staff.hostelBlock)
                            }
                        }
                    }

                    // Account Status Action Button
                    OutlinedButton(
                        onClick = { showToggleConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (staff.isActive) DangerRed else SuccessGreen),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (staff.isActive) DangerRed else SuccessGreen
                        )
                    ) {
                        Icon(
                            imageVector = if (staff.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (staff.isActive) "Deactivate Staff Account" else "Activate Staff Account",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Recent Tasks Section
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Recent Assigned Tasks",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (staff.recentTasks.isEmpty()) {
                                Text(
                                    text = "No recent tasks assigned to this staff member.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    staff.recentTasks.forEach { task ->
                                        Surface(
                                            onClick = { navController.navigate(Routes.wardenComplaintDetails(task.id)) },
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = task.title,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${task.ticketId ?: "#CMP"} · Room ${task.roomNumber ?: "-"}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = task.status.replace("_", " ").replaceFirstChar { it.uppercase() },
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("$label: ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
