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
import androidx.compose.ui.Modifier
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
            "Active" -> it.status != com.hostelcare.app.data.model.ComplaintStatus.RESOLVED && it.status != com.hostelcare.app.data.model.ComplaintStatus.SUBMITTED
            "Pending" -> it.status == com.hostelcare.app.data.model.ComplaintStatus.SUBMITTED
            "Resolved" -> it.status == com.hostelcare.app.data.model.ComplaintStatus.RESOLVED
            else -> true
        }
    }

    Scaffold(
        bottomBar = { StudentBottomNavigation(navController, Routes.MY_COMPLAINTS) },
        containerColor = Color(0xFFF9FAFB),
        topBar = {
            TopAppBar(
                title = { Text("My Complaints", fontWeight = FontWeight.Bold, color = TextDark) },
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { f ->
                    val isSelected = filter == f
                    Surface(
                        onClick = { filter = f },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFFEFF6FF) else Color.Transparent,
                        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else Color(0xFFE5E7EB))
                    ) {
                        Text(
                            text = f,
                            color = if (isSelected) PrimaryBlue else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFD1D5DB))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No complaints found", style = MaterialTheme.typography.titleMedium, color = TextDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("You don't have any complaints matching this filter.", color = TextMuted, fontSize = 14.sp)
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
        com.hostelcare.app.data.model.ComplaintStatus.SUBMITTED -> Pair(PrimaryBlue, Color(0xFFEFF6FF))
        com.hostelcare.app.data.model.ComplaintStatus.UNDER_REVIEW -> Pair(Color(0xFF7C3AED), Color(0xFFF5F3FF))
        com.hostelcare.app.data.model.ComplaintStatus.ASSIGNED, com.hostelcare.app.data.model.ComplaintStatus.IN_PROGRESS -> Pair(Color(0xFFD97706), Color(0xFFFEF3C7))
        com.hostelcare.app.data.model.ComplaintStatus.RESOLVED -> Pair(Color(0xFF16A34A), Color(0xFFF0FDF4))
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(
                    text = c.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
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
                Text("$catName | $ticketStr", color = TextMuted, fontSize = 13.sp)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Domain, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Block ${c.hostelBlock} Â· Room ${c.roomNumber}", color = TextMuted, fontSize = 13.sp)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("$dStr Â· $timeStr", color = TextMuted, fontSize = 13.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = c.description,
                    color = TextDark,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(24.dp))
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
    
    var isAnalyzing by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(complaintId) {
        viewModel.fetchComplaintDetails(complaintId)
    }

    LaunchedEffect(detailsState) {
        if (detailsState is ComplaintDetailsState.Success) {
            val c = (detailsState as ComplaintDetailsState.Success).complaint
            if (c.aiSummary.isNullOrBlank() && !isAnalyzing && aiError == null) {
                isAnalyzing = true
                val result = app.repository.triggerAiAnalysis(c.id)
                isAnalyzing = false
                if (result.isSuccess) {
                    viewModel.fetchComplaintDetails(c.id)
                } else {
                    aiError = "AI analysis is temporarily unavailable."
                }
            }
        }
    }

    if (detailsState is ComplaintDetailsState.Loading) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Complaint Details", fontWeight = FontWeight.Bold, color = TextDark) },
                    navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextDark) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color(0xFFF9FAFB)
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
                    title = { Text("Complaint Details", fontWeight = FontWeight.Bold, color = TextDark) },
                    navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextDark) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color(0xFFF9FAFB)
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text((detailsState as ComplaintDetailsState.Error).message, color = DangerRed)
            }
        }
        return
    }

    val complaint = (detailsState as ComplaintDetailsState.Success).complaint
    val dStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(complaint.createdAt))
    val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(complaint.createdAt))

    Scaffold(
        containerColor = Color(0xFFF9FAFB),
        topBar = {
            TopAppBar(
                title = { Text("Complaint Details", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextDark) } },
                actions = { IconButton(onClick = { }) { Icon(Icons.Filled.MoreVert, "More", tint = TextDark) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(complaint.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDark)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.background(LightBlue, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(6.dp).background(PrimaryBlue, CircleShape))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(complaint.status.name.replace("_", " "), color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Ticket ID", color = TextMuted, fontSize = 11.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FileCopy, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(complaint.ticketId.ifBlank { "HC-${complaint.id.take(6).uppercase()}" }, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF3F4F6))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row {
                                    Icon(Icons.Default.Domain, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Location", color = TextMuted, fontSize = 11.sp)
                                        Text("Block ${complaint.hostelBlock} Â· Room ${complaint.roomNumber}", color = TextDark, fontSize = 13.sp)
                                    }
                                }
                            }
                            Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color(0xFFF3F4F6)))
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Row {
                                    Icon(Icons.Default.Build, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Category", color = TextMuted, fontSize = 11.sp)
                                        Text(if (complaint.aiSummary.isNullOrBlank() && aiError == null) "Not assigned yet" else complaint.category.name.lowercase().replaceFirstChar { it.uppercase() }, color = TextDark, fontSize = 13.sp)
                                    }
                                }
                            }
                            Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color(0xFFF3F4F6)))
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Row {
                                    Icon(Icons.Default.Flag, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Priority", color = TextMuted, fontSize = 11.sp)
                                        Text(if (complaint.aiSummary.isNullOrBlank() && aiError == null) "Not assigned yet" else complaint.priority.name.lowercase().replaceFirstChar { it.uppercase() }, color = TextDark, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Reported On", color = TextMuted, fontSize = 11.sp)
                                Text("${dStr} Â· ${timeStr}", color = TextDark, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Issue Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(complaint.description, color = TextDark, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }

            if (!complaint.photoUri.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Attached Photo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                                }
                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            // Placeholder for actual photo
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (aiError != null) Color(0xFFFEF2F2) else Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (aiError != null) Color(0xFFFECACA) else Color(0xFFE5E7EB))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Assessment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                            if (aiError != null) {
                                OutlinedButton(
                                    onClick = { 
                                        aiError = null
                                        isAnalyzing = true
                                        coroutineScope.launch {
                                            val result = app.repository.triggerAiAnalysis(complaint.id)
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
                        
                        if (aiError != null) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(aiError!!, color = DangerRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("We couldn't analyze this complaint right now. Your complaint has been submitted successfully, and you can continue tracking its status. AI analysis will be retried automatically.", color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
                                }
                            }
                        } else if (isAnalyzing || complaint.aiSummary.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Analyzing complaint details...", color = TextMuted, fontSize = 14.sp)
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Category", color = TextMuted, fontSize = 11.sp)
                                    Text(complaint.category.name.lowercase().replaceFirstChar { it.uppercase() }, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Priority", color = TextMuted, fontSize = 11.sp)
                                    Text(complaint.priority.name.lowercase().replaceFirstChar { it.uppercase() }, color = if (complaint.priority.name == "HIGH" || complaint.priority.name == "EMERGENCY") DangerRed else if (complaint.priority.name == "MEDIUM") WarningOrange else TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Summary", color = TextMuted, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(complaint.aiSummary!!, color = TextDark, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val statuses = listOf(
                            com.hostelcare.app.data.model.ComplaintStatus.SUBMITTED to "Submitted",
                            com.hostelcare.app.data.model.ComplaintStatus.UNDER_REVIEW to "Under Review",
                            com.hostelcare.app.data.model.ComplaintStatus.ASSIGNED to "Assigned",
                            com.hostelcare.app.data.model.ComplaintStatus.IN_PROGRESS to "In Progress",
                            com.hostelcare.app.data.model.ComplaintStatus.RESOLVED to "Resolved"
                        )
                        
                        val currentIdx = statuses.indexOfFirst { it.first == complaint.status }
                        statuses.forEachIndexed { idx, pair ->
                            val isPast = idx <= currentIdx
                            val isCurrent = idx == currentIdx
                            Row(verticalAlignment = Alignment.Top) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.size(18.dp).background(if (isCurrent) Color.Transparent else if (isPast) Color(0xFFE5E7EB) else Color(0xFFE5E7EB), CircleShape).border(if (isCurrent) BorderStroke(4.dp, PrimaryBlue) else BorderStroke(0.dp, Color.Transparent), CircleShape), contentAlignment = Alignment.Center) {
                                        if (isPast && !isCurrent) {
                                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF9CA3AF), CircleShape))
                                        }
                                    }
                                    if (idx < statuses.size - 1) {
                                        Box(modifier = Modifier.padding(vertical = 4.dp).width(2.dp).height(24.dp).background(if (idx < currentIdx) Color(0xFFE5E7EB) else Color(0xFFE5E7EB)))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(pair.second, color = if (isPast) TextDark else TextMuted, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                                    if (isCurrent) {
                                        Text("${dStr} Â· ${timeStr}", color = TextMuted, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}