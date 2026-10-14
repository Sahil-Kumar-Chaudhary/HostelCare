package com.hostelcare.app.ui.screens.warden

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.hostelcare.app.data.remote.WardenStatsData
import com.hostelcare.app.ui.components.HostelCareLogo
import com.hostelcare.app.ui.components.WardenBottomNavigation
import com.hostelcare.app.ui.navigation.Routes
import com.hostelcare.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardenDashboardScreen(
    navController: NavController,
    app: HostelCareApp
) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: WardenViewModel = viewModel(factory = factory)

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle(null)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val unreadNotificationCount by viewModel.unreadCount.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showProfileDialog by remember { mutableStateOf(false) }
    var selectedComplaintForDetails by remember { mutableStateOf<WardenRecentComplaint?>(null) }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadDashboard(isExplicitRefresh = false)
    }

    Scaffold(
        bottomBar = {
            WardenBottomNavigation(
                navController = navController,
                currentRoute = Routes.WARDEN_HOME
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HostelCareLogo(size = 38.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "HostelCare",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Warden Portal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Refresh button
                IconButton(
                    onClick = { viewModel.loadDashboard(isExplicitRefresh = true) },
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Notification Bell
                IconButton(
                    onClick = {
                        navController.navigate(Routes.NOTIFICATIONS)
                    }
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Profile Avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                        .clickable { navController.navigate(Routes.WARDEN_PROFILE) },
                    contentAlignment = Alignment.Center
                ) {
                    val initials = currentUser?.name?.take(2)?.uppercase() ?: "W"
                    Text(
                        text = initials,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Main Content Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Greeting and Subtitle
                item {
                    val wardenFirstName = currentUser?.name?.split(" ")?.firstOrNull() ?: "Warden"
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$greeting, $wardenFirstName",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Manage hostel complaints and keep the hostel safe and comfortable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (val state = uiState) {
                    is WardenDashboardUiState.Loading -> {
                        item {
                            WardenDashboardLoadingPlaceholder()
                        }
                    }

                    is WardenDashboardUiState.Error -> {
                        item {
                            WardenDashboardErrorView(
                                message = state.message,
                                onRetry = { viewModel.loadDashboard() }
                            )
                        }
                        item {
                            WardenSummaryCardsRow(stats = WardenStatsData())
                        }
                        item {
                            WardenPriorityIssuesBanner(
                                count = 0,
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Connect to network to check priority issues")
                                    }
                                }
                            )
                        }
                        item {
                            WardenQuickActionsSection(
                                onActionClicked = { actionName ->
                                    scope.launch {
                                        when (actionName) {
                                            "All Complaints" -> snackbarHostState.showSnackbar("Complaints list will be fully navigable in Step 3B")
                                            "Assign Staff" -> snackbarHostState.showSnackbar("Assign Staff workflow is scheduled for Step 3B")
                                            "Manage Staff" -> snackbarHostState.showSnackbar("Staff Management is scheduled for Step 3C")
                                            "Reports" -> snackbarHostState.showSnackbar("Reports & Analytics will be available in future updates")
                                        }
                                    }
                                }
                            )
                        }
                    }

                    is WardenDashboardUiState.Empty -> {
                        item {
                            WardenSummaryCardsRow(stats = WardenStatsData())
                        }
                        item {
                            WardenPriorityIssuesBanner(
                                count = 0,
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("No high priority complaints need attention")
                                    }
                                }
                            )
                        }
                        item {
                            WardenDashboardEmptyView(
                                message = state.message,
                                onRefresh = { viewModel.loadDashboard(isExplicitRefresh = true) }
                            )
                        }
                        item {
                            WardenQuickActionsSection(
                                onActionClicked = { actionName ->
                                    when (actionName) {
                                        "All Complaints" -> navController.navigate(Routes.WARDEN_COMPLAINTS)
                                        "Assign Staff" -> navController.navigate(Routes.WARDEN_COMPLAINTS)
                                        "Manage Staff" -> navController.navigate(Routes.WARDEN_STAFF)
                                        "Reports" -> navController.navigate(Routes.WARDEN_REPORTS)
                                    }
                                }
                            )
                        }
                    }

                    is WardenDashboardUiState.Success -> {
                        val stats = state.data.stats
                        val recentComplaints = state.data.recentComplaints

                        // 4 Summary Stat Cards
                        item {
                            WardenSummaryCardsRow(stats = stats)
                        }

                        // Priority Issues Banner
                        item {
                            WardenPriorityIssuesBanner(
                                count = stats.priorityIssues,
                                onClick = {
                                    navController.navigate(Routes.WARDEN_COMPLAINTS)
                                }
                            )
                        }

                        // Quick Actions Grid (2x2)
                        item {
                            WardenQuickActionsSection(
                                onActionClicked = { actionName ->
                                    when (actionName) {
                                        "All Complaints" -> navController.navigate(Routes.WARDEN_COMPLAINTS)
                                        "Assign Staff" -> navController.navigate(Routes.WARDEN_COMPLAINTS)
                                        "Manage Staff" -> navController.navigate(Routes.WARDEN_STAFF)
                                        "Reports" -> navController.navigate(Routes.WARDEN_REPORTS)
                                    }
                                }
                            )
                        }

                        // Recent Complaints Section Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Complaints",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "View all",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        navController.navigate(Routes.WARDEN_COMPLAINTS)
                                    }
                                )
                            }
                        }

                        if (recentComplaints.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inbox,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No complaints filed yet in your hostel.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(recentComplaints) { complaint ->
                                WardenComplaintCard(
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

    // Warden Profile / Logout Dialog
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Text(
                    text = "Warden Profile",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = currentUser?.name ?: "Warden",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Email: ${currentUser?.email ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val hostel = currentUser?.hostelName ?: "Hostel"
                    val block = currentUser?.hostelBlock?.let { " (Block $it)" } ?: ""
                    Text(
                        text = "Assigned Hostel: $hostel$block",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Role: Warden",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showProfileDialog = false
                        viewModel.logout {
                            navController.navigate(Routes.PORTAL_SELECTION) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout", color = MaterialTheme.colorScheme.onError)
                }
            }
        )
    }

    // Complaint Details Bottom Sheet / Dialog
    selectedComplaintForDetails?.let { complaint ->
        AlertDialog(
            onDismissRequest = { selectedComplaintForDetails = null },
            title = {
                Text(
                    text = complaint.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WardenPriorityChip(priority = complaint.priority)
                        WardenStatusChip(status = complaint.status)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "Room ${complaint.roomNumber ?: "-"} • Block ${complaint.hostelBlock ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Category: ${complaint.category.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Submitted by: ${complaint.student?.name ?: "Student"}${complaint.student?.studentId?.let { " ($it)" } ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!complaint.description.isNullOrBlank()) {
                        Text(
                            text = "Description:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = complaint.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (!complaint.aiSummary.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "AI Summary:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = complaint.aiSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedComplaintForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * 4 Summary Stat Cards row matching Screen 1 in UI collage.
 */
@Composable
fun WardenSummaryCardsRow(stats: WardenStatsData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WardenStatCard(
            label = "Total",
            count = stats.total.toString(),
            icon = Icons.AutoMirrored.Filled.List,
            iconBg = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        WardenStatCard(
            label = "Pending",
            count = stats.pending.toString(),
            icon = Icons.Default.Schedule,
            iconBg = WarningOrange.copy(alpha = 0.15f),
            iconTint = WarningOrange,
            modifier = Modifier.weight(1f)
        )
        WardenStatCard(
            label = "In Progress",
            count = stats.inProgress.toString(),
            icon = Icons.Default.Engineering,
            iconBg = Color(0xFF0288D1).copy(alpha = 0.15f),
            iconTint = Color(0xFF0288D1),
            modifier = Modifier.weight(1f)
        )
        WardenStatCard(
            label = "Resolved",
            count = stats.resolved.toString(),
            icon = Icons.Default.CheckCircle,
            iconBg = SuccessGreen.copy(alpha = 0.15f),
            iconTint = SuccessGreen,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun WardenStatCard(
    label: String,
    count: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Priority Issues Alert Banner matching Screen 1 in UI collage.
 */
@Composable
fun WardenPriorityIssuesBanner(
    count: Int,
    onClick: () -> Unit
) {
    val containerBg = if (MaterialTheme.colorScheme.surface == SurfaceLight) {
        Color(0xFFFEF2F2)
    } else {
        Color(0xFF3B1D22)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerBg,
        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(DangerRed.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Priority Issues",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DangerRed
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = if (count > 0) {
                    "$count high priority complaint${if (count == 1) "" else "s"} need attention"
                } else {
                    "No high priority complaints need attention"
                }
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View priority issues",
                tint = DangerRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 2x2 Quick Actions grid matching Screen 1 in UI collage.
 */
@Composable
fun WardenQuickActionsSection(
    onActionClicked: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WardenQuickActionButton(
                label = "All Complaints",
                icon = Icons.AutoMirrored.Filled.Assignment,
                modifier = Modifier.weight(1f),
                onClick = { onActionClicked("All Complaints") }
            )
            WardenQuickActionButton(
                label = "Assign Staff",
                icon = Icons.Default.PersonAdd,
                modifier = Modifier.weight(1f),
                onClick = { onActionClicked("Assign Staff") }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WardenQuickActionButton(
                label = "Manage Staff",
                icon = Icons.Default.People,
                modifier = Modifier.weight(1f),
                onClick = { onActionClicked("Manage Staff") }
            )
            WardenQuickActionButton(
                label = "Reports",
                icon = Icons.Default.BarChart,
                modifier = Modifier.weight(1f),
                onClick = { onActionClicked("Reports") }
            )
        }
    }
}

@Composable
fun WardenQuickActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Complaint item card matching Screen 1 recent complaints list.
 */
@Composable
fun WardenComplaintCard(
    complaint: WardenRecentComplaint,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon box
            val (catIcon, catBg, catTint) = when (complaint.category.lowercase()) {
                "plumbing" -> Triple(Icons.Default.WaterDrop, LightBlue, PrimaryBlue)
                "electrical" -> Triple(Icons.Default.Bolt, OrangePillBg, WarningOrange)
                "carpentry", "maintenance" -> Triple(Icons.Default.Build, GrayBg, MaterialTheme.colorScheme.onSurfaceVariant)
                "cleaning" -> Triple(Icons.Default.CleaningServices, GreenPillBg, SuccessGreen)
                else -> Triple(Icons.Default.HomeRepairService, LightBlue, PrimaryBlue)
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(catBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = catIcon,
                    contentDescription = complaint.category,
                    tint = catTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = complaint.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))

                val locationInfo = buildString {
                    append("Room ${complaint.roomNumber ?: "-"}")
                    if (!complaint.hostelBlock.isNullOrBlank()) {
                        append(" (Block ${complaint.hostelBlock})")
                    }
                    append(" • ${complaint.category.replaceFirstChar { it.uppercase() }}")
                }
                Text(
                    text = locationInfo,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))
                val studentName = complaint.student?.name ?: "Student"
                Text(
                    text = "Submitted by: $studentName",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!complaint.createdAt.isNullOrBlank()) {
                    val formattedDate = formatComplaintDate(complaint.createdAt)
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status and Priority chips
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WardenPriorityChip(priority = complaint.priority)
                WardenStatusChip(status = complaint.status)
            }
        }
    }
}

@Composable
fun WardenPriorityChip(priority: String) {
    val (chipBg, chipFg) = when (priority.lowercase()) {
        "emergency", "high" -> Pair(RedPillBg, DangerRed)
        "medium" -> Pair(OrangePillBg, WarningOrange)
        else -> Pair(GreenPillBg, SuccessGreen)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = chipBg
    ) {
        Text(
            text = priority.replaceFirstChar { it.uppercase() },
            color = chipFg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun WardenStatusChip(status: String) {
    val (statusLabel, chipBg, chipFg) = when (status.lowercase()) {
        "submitted", "pending" -> Triple("Pending", OrangePillBg, WarningOrange)
        "under_review" -> Triple("Review", OrangePillBg, WarningOrange)
        "assigned", "in_progress" -> Triple("In Progress", LightBlue, PrimaryBlue)
        "resolved" -> Triple("Resolved", GreenPillBg, SuccessGreen)
        "rejected" -> Triple("Rejected", RedPillBg, DangerRed)
        else -> Triple(status.replaceFirstChar { it.uppercase() }, GrayBg, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = chipBg
    ) {
        Text(
            text = statusLabel,
            color = chipFg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun WardenDashboardLoadingPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading warden dashboard...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WardenDashboardErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Failed to load dashboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
fun WardenDashboardEmptyView(
    message: String,
    onRefresh: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircleOutline,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "All Clear!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Refresh")
            }
        }
    }
}

private fun formatComplaintDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val date = parser.parse(dateStr)
        if (date != null) {
            SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(date)
        } else {
            dateStr.take(10)
        }
    } catch (_: Exception) {
        try {
            val millis = dateStr.toLongOrNull()
            if (millis != null) {
                SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(millis))
            } else {
                dateStr.take(10)
            }
        } catch (_: Exception) {
            dateStr.take(10)
        }
    }
}
