package com.hostelcare.app.ui.screens.warden

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.data.remote.WardenRecentComplaint
import com.hostelcare.app.ui.components.WardenBottomNavigation
import com.hostelcare.app.ui.navigation.Routes
import com.hostelcare.app.ui.screens.auth.CustomTextField
import com.hostelcare.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardenComplaintsListScreen(
    navController: NavController,
    app: HostelCareApp,
    initialStatus: String = "all",
    initialPriority: String = "all"
) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: WardenViewModel = viewModel(factory = factory)

    val complaintsState by viewModel.complaintsState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(initialStatus) }
    var selectedCategory by remember { mutableStateOf("all") }
    var selectedPriority by remember { mutableStateOf(initialPriority) }

    fun refreshList() {
        viewModel.loadComplaints(
            search = if (searchQuery.isNotBlank()) searchQuery.trim() else null,
            status = if (selectedStatus != "all") selectedStatus else null,
            category = if (selectedCategory != "all") selectedCategory else null,
            priority = if (selectedPriority != "all") selectedPriority else null
        )
    }

    LaunchedEffect(selectedStatus, selectedCategory, selectedPriority) {
        refreshList()
    }

    val statusTabs = listOf(
        "all" to "All",
        "pending" to "Pending",
        "in_progress" to "In Progress",
        "resolved" to "Resolved"
    )

    val categories = listOf(
        "all" to "All Categories",
        "plumbing" to "Plumbing",
        "electrical" to "Electrical",
        "wifi" to "Wi-Fi",
        "furniture" to "Furniture",
        "cleaning" to "Cleaning",
        "other" to "Other"
    )

    val priorities = listOf(
        "all" to "All Priorities",
        "high" to "High",
        "emergency" to "Emergency",
        "medium" to "Medium",
        "low" to "Low"
    )

    Scaffold(
        bottomBar = {
            WardenBottomNavigation(
                navController = navController,
                currentRoute = Routes.WARDEN_COMPLAINTS
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Complaints",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Routes.WARDEN_HOME)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshList() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Input
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                CustomTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        refreshList()
                    },
                    placeholder = "Search by ticket, title, room, student...",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            // Status Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                statusTabs.forEach { (tabKey, tabLabel) ->
                    val isSelected = selectedStatus == tabKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { selectedStatus = tabKey }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabLabel,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Category & Priority Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category Pills
                categories.forEach { (catKey, catLabel) ->
                    val isCatSelected = selectedCategory == catKey
                    FilterChip(
                        selected = isCatSelected,
                        onClick = { selectedCategory = catKey },
                        label = { Text(catLabel, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Complaints List or Empty/Loading State
            when (val state = complaintsState) {
                is WardenComplaintsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is WardenComplaintsUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.message,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                is WardenComplaintsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                color = DangerRed,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { refreshList() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is WardenComplaintsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.complaints) { complaint ->
                            WardenComplaintListItemCard(
                                complaint = complaint,
                                onClick = {
                                    navController.navigate(Routes.wardenComplaintDetails(complaint.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WardenComplaintListItemCard(
    complaint: WardenRecentComplaint,
    onClick: () -> Unit
) {
    val statusColor = when (complaint.status.lowercase()) {
        "resolved" -> SuccessGreen
        "assigned", "in_progress" -> PrimaryBlue
        "under_review" -> Color(0xFFD97706)
        "withdrawn" -> Color(0xFF64748B)
        else -> PendingAmber
    }

    val statusLabel = when (complaint.status.lowercase()) {
        "submitted" -> "Pending"
        "under_review" -> "Under Review"
        "assigned" -> "Assigned"
        "in_progress" -> "In Progress"
        "resolved" -> "Resolved"
        "withdrawn" -> "Withdrawn"
        "rejected" -> "Rejected"
        else -> complaint.status.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    val priorityColor = when (complaint.priority.lowercase()) {
        "emergency" -> Color(0xFFDC2626)
        "high" -> DangerRed
        "medium" -> Color(0xFFD97706)
        else -> Color(0xFF64748B)
    }

    val formattedDate = remember(complaint.createdAt) {
        try {
            val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            iso.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = iso.parse(complaint.createdAt ?: "")
            if (date != null) {
                SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date)
            } else "-"
        } catch (e: Exception) {
            "-"
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Ticket ID + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = complaint.ticketId ?: "#CMP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Priority Tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = priorityColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = complaint.priority.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Complaint Title
            Text(
                text = complaint.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Student Room & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val studentName = complaint.student?.name ?: "Student"
                val roomStr = if (!complaint.roomNumber.isNullOrBlank()) "Room ${complaint.roomNumber}" else "Hostel"
                val blockStr = if (!complaint.hostelBlock.isNullOrBlank()) " (${complaint.hostelBlock})" else ""
                Text(
                    text = "$studentName · $roomStr$blockStr",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Staff Assignment & Category Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = complaint.assignedStaff?.name ?: "Unassigned",
                        fontSize = 12.sp,
                        fontWeight = if (complaint.assignedStaff != null) FontWeight.Medium else FontWeight.Normal,
                        color = if (complaint.assignedStaff != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = complaint.category.replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
