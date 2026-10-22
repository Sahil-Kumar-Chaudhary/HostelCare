package com.hostelcare.app.ui.screens.student

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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelcare.app.utils.NetworkUtils
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.data.model.ComplaintStatus
import com.hostelcare.app.ui.components.StudentBottomNavigation
import com.hostelcare.app.ui.navigation.Routes
import com.hostelcare.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MyComplaintsScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    
    var filter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Active", "Pending", "Resolved")
    
    val filtered = complaints.filter {
        when (filter) {
            "Active" -> it.status != com.hostelcare.app.data.model.ComplaintStatus.RESOLVED && it.status != com.hostelcare.app.data.model.ComplaintStatus.SUBMITTED && it.status != com.hostelcare.app.data.model.ComplaintStatus.WITHDRAWN
            "Pending" -> it.status == com.hostelcare.app.data.model.ComplaintStatus.SUBMITTED
            "Resolved" -> it.status == com.hostelcare.app.data.model.ComplaintStatus.RESOLVED
            else -> true
        }
    }

    Scaffold(
        bottomBar = { StudentBottomNavigation(navController, Routes.MY_COMPLAINTS) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("My Complaints", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                actions = {
                    Button(
                        onClick = { navController.navigate(Routes.NEW_COMPLAINT) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 16.dp).height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Complaint", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { f ->
                    val isSelected = filter == f
                    Surface(
                        onClick = { filter = f },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = f,
                            color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No complaints found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("You don't have any complaints matching this filter.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filtered) { c ->
                        ComplaintCard(c) {
                            navController.navigate(Routes.complaintDetails(c.id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComplaintCard(c: com.hostelcare.app.data.model.Complaint, onClick: () -> Unit) {
    val dStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(c.createdAt))
    val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(c.createdAt))
    
    val statusColorInfo = when (c.status) {
        com.hostelcare.app.data.model.ComplaintStatus.SUBMITTED -> Pair(PrimaryBlue, MaterialTheme.colorScheme.primaryContainer)
        com.hostelcare.app.data.model.ComplaintStatus.UNDER_REVIEW -> Pair(Color(0xFF9333EA), Color(0xFF9333EA).copy(alpha = 0.15f))
        com.hostelcare.app.data.model.ComplaintStatus.ASSIGNED, com.hostelcare.app.data.model.ComplaintStatus.IN_PROGRESS -> Pair(Color(0xFFD97706), Color(0xFFD97706).copy(alpha = 0.15f))
        com.hostelcare.app.data.model.ComplaintStatus.RESOLVED -> Pair(Color(0xFF16A34A), Color(0xFF16A34A).copy(alpha = 0.15f))
        com.hostelcare.app.data.model.ComplaintStatus.WITHDRAWN -> Pair(Color(0xFF64748B), Color(0xFF64748B).copy(alpha = 0.15f))
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(
                    text = c.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                )
                
                Box(
                    modifier = Modifier.background(statusColorInfo.second, RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(statusColorInfo.first, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(c.status.name.replace("_", " "), color = statusColorInfo.first, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                val catName = if (c.aiSummary.isNullOrBlank()) "Unassigned" else c.category.name.lowercase().replaceFirstChar { it.uppercase() }
                val ticketStr = c.ticketId.ifBlank { "HC-${c.id.take(6).uppercase()}" }
                Text("$catName | $ticketStr", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Domain, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Block ${c.hostelBlock} • Room ${c.roomNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("$dStr • $timeStr", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = c.description,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ComplaintDetailsScreen(navController: NavController, app: HostelCareApp, complaintId: String) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
    val detailsState by viewModel.complaintDetailsState.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var isWithdrawing by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(complaintId) {
        viewModel.fetchComplaintDetails(complaintId)
    }

    LaunchedEffect(complaintId, (detailsState as? ComplaintDetailsState.Success)?.complaint?.aiStatus) {
        val currentStatus = (detailsState as? ComplaintDetailsState.Success)?.complaint?.aiStatus
        if (currentStatus == "processing") {
            kotlinx.coroutines.delay(3000)
            viewModel.fetchComplaintDetails(complaintId)
        }
    }

    if (detailsState is ComplaintDetailsState.Loading) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Complaint Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    navigationIcon = { IconButton(onClick = { if (!navController.popBackStack()) navController.navigate(Routes.MY_COMPLAINTS) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        }
        return
    }

    if (detailsState is ComplaintDetailsState.Error) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Complaint Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    navigationIcon = { IconButton(onClick = { if (!navController.popBackStack()) navController.navigate(Routes.MY_COMPLAINTS) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text((detailsState as ComplaintDetailsState.Error).message, color = DangerRed)
            }
        }
        return
    }

if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { if (!isWithdrawing) showWithdrawDialog = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Withdraw Complaint?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to withdraw this complaint? This request will be canceled and staff will not be assigned. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isWithdrawing = true
                        viewModel.withdrawComplaint(complaintId) { success, msg ->
                            isWithdrawing = false
                            showWithdrawDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msg ?: if (success) "Complaint withdrawn successfully" else "Failed to withdraw complaint")
                            }
                        }
                    },
                    enabled = !isWithdrawing,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isWithdrawing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onError, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Withdraw Complaint")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showWithdrawDialog = false },
                    enabled = !isWithdrawing
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val complaint = (detailsState as ComplaintDetailsState.Success).complaint
    val dStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(complaint.createdAt))
    val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(complaint.createdAt))

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Complaint Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = { IconButton(onClick = { if (!navController.popBackStack()) navController.navigate(Routes.MY_COMPLAINTS) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) } },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, "More options", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copy Ticket ID") },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                onClick = {
                                    menuExpanded = false
                                    val tId = complaint.ticketId.ifBlank { "HC-${complaint.id.take(6).uppercase()}" }
                                    clipboardManager.setText(AnnotatedString(tId))
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Ticket ID $tId copied to clipboard")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Refresh Details") },
                                leadingIcon = {
                                    if (isRefreshing) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                                    }
                                },
                                enabled = !isRefreshing,
                                onClick = {
                                    menuExpanded = false
                                    isRefreshing = true
                                    coroutineScope.launch {
                                        val ok = viewModel.refreshComplaintDetails(complaintId)
                                        isRefreshing = false
                                        if (ok) {
                                            snackbarHostState.showSnackbar("Complaint details refreshed")
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to refresh complaint details")
                                        }
                                    }
                                }
                            )
                            HorizontalDivider()
                            val canWithdraw = complaint.status == ComplaintStatus.SUBMITTED
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when {
                                            complaint.status == ComplaintStatus.WITHDRAWN -> "Complaint Withdrawn"
                                            canWithdraw -> "Withdraw Complaint"
                                            else -> "Withdraw (Not Eligible)"
                                        },
                                        color = if (canWithdraw) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (canWithdraw) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                enabled = canWithdraw,
                                onClick = {
                                    menuExpanded = false
                                    showWithdrawDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    border = BorderStroke(1.dp, PrimaryBlue),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back to My Complaints", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(complaint.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(6.dp).background(PrimaryBlue, CircleShape))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(complaint.status.name.replace("_", " "), color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Ticket ID", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FileCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(complaint.ticketId.ifBlank { "HC-${complaint.id.take(6).uppercase()}" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row {
                                    Icon(Icons.Default.Domain, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Location", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        Text("Block ${complaint.hostelBlock} • Room ${complaint.roomNumber}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                    }
                                }
                            }
                            Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outlineVariant))
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Row {
                                    Icon(Icons.Default.Build, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Category", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        Text(complaint.category.name.lowercase().replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                    }
                                }
                            }
                            Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outlineVariant))
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Row {
                                    Icon(Icons.Default.Flag, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Priority", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        Text(complaint.priority.name.lowercase().replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Reported On", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Text("${dStr} • ${timeStr}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Issue Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(complaint.description, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }

            if (!complaint.photoUri.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Attached Photo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            if (!complaint.photoUri.isNullOrEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    UriImage(uriStr = complaint.photoUri!!, modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(12.dp), fallbackIcon = Icons.Default.HideImage)
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.HideImage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("No photo attached", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                val isCompleted = !complaint.aiSummary.isNullOrBlank() || complaint.aiStatus == "completed"
                val isProcessing = isAnalyzing || complaint.aiStatus == "processing"
                val isFailed = aiError != null || (!isCompleted && !isProcessing && complaint.aiStatus == "failed")
                val displayError = aiError ?: if (complaint.aiStatus == "failed") "AI analysis is temporarily unavailable." else null

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (isFailed) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Assessment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            if (isFailed) {
                                OutlinedButton(
                                    onClick = { 
                                        aiError = null
                                        isAnalyzing = true
                                        coroutineScope.launch {
                                            if (!NetworkUtils.isNetworkAvailable(context)) {
                                                aiError = "No internet connection. Please check your connection and try again."
                                                isAnalyzing = false
                                                return@launch
                                            }
                                            val result = app.repository.triggerAiAnalysis(complaint.id, forceRetry = true)
                                            isAnalyzing = false
                                            if (result.isSuccess) {
                                                viewModel.fetchComplaintDetails(complaint.id)
                                            } else {
                                                aiError = "AI analysis is temporarily unavailable."
                                            }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp),
                                    border = BorderStroke(1.dp, PrimaryBlue),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retry", fontSize = 12.sp)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isFailed) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(displayError ?: "AI analysis is temporarily unavailable.", color = DangerRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("We couldn't analyze this complaint right now. Your complaint has been submitted successfully, and you can continue tracking its status. Use the Retry button to try again.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
                                }
                            }
                        } else if (isProcessing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Analyzing complaint details...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                        } else if (isCompleted) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Category", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    Text(complaint.category.name.lowercase().replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Priority", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    Text(complaint.priority.name.lowercase().replaceFirstChar { it.uppercase() }, color = if (complaint.priority.name == "HIGH" || complaint.priority.name == "EMERGENCY") DangerRed else if (complaint.priority.name == "MEDIUM") WarningOrange else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Summary", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(complaint.aiSummary ?: "", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, lineHeight = 20.sp)
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("No AI assessment performed yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                OutlinedButton(
                                    onClick = {
                                        aiError = null
                                        isAnalyzing = true
                                        coroutineScope.launch {
                                            if (!NetworkUtils.isNetworkAvailable(context)) {
                                                aiError = "No internet connection. Please check your connection and try again."
                                                isAnalyzing = false
                                                return@launch
                                            }
                                            val result = app.repository.triggerAiAnalysis(complaint.id, forceRetry = false)
                                            isAnalyzing = false
                                            if (result.isSuccess) {
                                                viewModel.fetchComplaintDetails(complaint.id)
                                            } else {
                                                aiError = "AI analysis is temporarily unavailable."
                                            }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp),
                                    border = BorderStroke(1.dp, PrimaryBlue),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Analyze", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val statuses = if (complaint.status == ComplaintStatus.WITHDRAWN) {
                            listOf(
                                ComplaintStatus.SUBMITTED to "Submitted",
                                ComplaintStatus.WITHDRAWN to "Withdrawn"
                            )
                        } else {
                            listOf(
                                ComplaintStatus.SUBMITTED to "Submitted",
                                ComplaintStatus.UNDER_REVIEW to "Under Review",
                                ComplaintStatus.ASSIGNED to "Assigned",
                                ComplaintStatus.IN_PROGRESS to "In Progress",
                                ComplaintStatus.RESOLVED to "Resolved"
                            )
                        }
                        
                        val currentIdx = statuses.indexOfFirst { it.first == complaint.status }
                        val activeColor = if (complaint.status == ComplaintStatus.WITHDRAWN) Color(0xFF64748B) else PrimaryBlue
                        statuses.forEachIndexed { idx, pair ->
                            val isPast = idx <= currentIdx
                            val isCurrent = idx == currentIdx
                            Row(verticalAlignment = Alignment.Top) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.size(18.dp).background(if (isCurrent) Color.Transparent else if (isPast) activeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape).border(if (isCurrent) BorderStroke(4.dp, activeColor) else BorderStroke(0.dp, Color.Transparent), CircleShape), contentAlignment = Alignment.Center) {
                                        if (isPast && !isCurrent) {
                                            Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
                                        }
                                    }
                                    if (idx < statuses.size - 1) {
                                        Box(modifier = Modifier.padding(vertical = 4.dp).width(2.dp).height(24.dp).background(if (idx < currentIdx) activeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(pair.second, color = if (isPast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                                    if (isCurrent) {
                                        Text("${dStr} • ${timeStr}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (complaint.status == ComplaintStatus.RESOLVED) {
                item {
                    Button(
                        onClick = { navController.navigate(Routes.feedback(complaint.id)) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rate Resolution Feedback", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}