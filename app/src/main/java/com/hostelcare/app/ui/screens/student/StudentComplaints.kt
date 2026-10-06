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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyComplaintsScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    
    var filter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Active", "Pending", "Resolved")
    
    val filtered = complaints.filter {
        when (filter) {
            "Active" -> it.status != ComplaintStatus.RESOLVED && it.status != ComplaintStatus.SUBMITTED
            "Pending" -> it.status == ComplaintStatus.SUBMITTED
            "Resolved" -> it.status == ComplaintStatus.RESOLVED
            else -> true
        }
    }

    Scaffold(
        bottomBar = { StudentBottomNavigation(navController, Routes.MY_COMPLAINTS) },
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("My Complaints", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEach { f ->
                    val isSelected = filter == f
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) LightBlue else Color.Transparent, RoundedCornerShape(20.dp))
                            .border(1.dp, if (isSelected) PrimaryBlue else Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
                            .clickable { filter = f }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(f, color = if (isSelected) PrimaryBlue else TextDark, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), contentPadding = PaddingValues(bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered) { c ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Routes.complaintDetails(c.id)) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(c.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${c.category.name} Â· ${c.status.name}", fontSize = 14.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(8.dp))
                            val dStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(c.createdAt))
                            Text(dStr, fontSize = 12.sp, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintDetailsScreen(navController: NavController, app: HostelCareApp, complaintId: String) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val detailsState by viewModel.complaintDetailsState.collectAsStateWithLifecycle()
    val staffList by viewModel.staffList.collectAsStateWithLifecycle(initialValue = emptyList())
    
    LaunchedEffect(complaintId) {
        viewModel.fetchComplaintDetails(complaintId)
    }

    if (detailsState is ComplaintDetailsState.Loading) {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Complaint Details", fontWeight = FontWeight.Bold, color = TextDark) },
                    navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextDark) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color.White
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
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Complaint Details", fontWeight = FontWeight.Bold, color = TextDark) },
                    navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextDark) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color.White
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text((detailsState as ComplaintDetailsState.Error).message, color = DangerRed)
            }
        }
        return
    }

    val complaint = (detailsState as ComplaintDetailsState.Success).complaint

    val dStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(complaint.createdAt))
    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(complaint.createdAt))

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Complaint Details", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextDark) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (complaint.status == ComplaintStatus.RESOLVED && complaint.feedbackRating == null) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { navController.navigate(Routes.feedback(complaint.id)) }, 
                        modifier = Modifier.weight(1f).height(52.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.White), 
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.StarRate, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Provide Feedback")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            // 1. Complaint Summary
            item {
                Text(complaint.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.background(LightBlue, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(PrimaryBlue, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(complaint.status.name.replace("_", " "), color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${complaint.hostelBlock} â€¢ Room ${complaint.roomNumber}", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                    Box(modifier = Modifier.background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(complaint.category.name, color = TextMuted, fontSize = 13.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(modifier = Modifier.background(if (complaint.priority.name == "HIGH") Color(0xFFFEE2E2) else Color(0xFFFEF9C3), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val pIcon = if (complaint.priority.name == "HIGH") Icons.Default.PriorityHigh else Icons.Default.LowPriority
                        Icon(pIcon, null, tint = if (complaint.priority.name == "HIGH") DangerRed else WarningOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${complaint.priority.name} Priority", color = if (complaint.priority.name == "HIGH") DangerRed else WarningOrange, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reported: $dStr, $timeStr", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
            
            // 2. Issue Description
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Issue Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(complaint.description, style = MaterialTheme.typography.bodyMedium, color = TextDark, lineHeight = 22.sp)
                
                if (complaint.photoUri != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Attached Photo", fontSize = 13.sp, color = TextMuted, modifier = Modifier.padding(bottom = 8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        UriImage(uriStr = complaint.photoUri)
                    }
                }
            }
            
            // 3. Progress
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timeline, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                val statuses = listOf(
                    ComplaintStatus.SUBMITTED to "Submitted",
                    ComplaintStatus.UNDER_REVIEW to "Under Review",
                    ComplaintStatus.ASSIGNED to "Assigned",
                    ComplaintStatus.IN_PROGRESS to "In Progress",
                    ComplaintStatus.RESOLVED to "Resolved"
                )
                val currentIdx = statuses.indexOfFirst { it.first == complaint.status }.takeIf { it >= 0 } ?: 0
                
                Column {
                    statuses.forEachIndexed { index, (_, label) ->
                        val isCompleted = index < currentIdx
                        val isCurrent = index == currentIdx
                        
                        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                                Box(
                                    modifier = Modifier.size(24.dp).background(if (isCompleted) SuccessGreen else if (isCurrent) PrimaryBlue else Color(0xFFF3F4F6), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleted) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    } else if (isCurrent) {
                                        Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
                                    }
                                }
                                if (index < statuses.lastIndex) {
                                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(if (isCompleted) SuccessGreen else Color(0xFFF3F4F6)).padding(vertical = 4.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.padding(bottom = if (index < statuses.lastIndex) 24.dp else 0.dp)) {
                                Text(label, style = MaterialTheme.typography.bodyLarge, color = if (isCompleted || isCurrent) TextDark else TextMuted, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                                if (isCompleted || isCurrent) {
                                    Text(timeStr, fontSize = 12.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
            
            // 4. Assigned Staff
            if (complaint.assignedStaffId != null) {
                item {
                    val assignedStaff = staffList.find { it.id == complaint.assignedStaffId }
                    if (assignedStaff != null) {
                        Text("Assigned Staff", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(48.dp).background(Color(0xFFF9FAFB), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, null, tint = TextMuted)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(assignedStaff.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextDark)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${assignedStaff.roleTitle} â€¢ Block ${complaint.hostelBlock}", fontSize = 13.sp, color = TextMuted)
                                }
                                val context = androidx.compose.ui.platform.LocalContext.current
                                Box(
                                    modifier = Modifier.size(40.dp).background(LightBlue, CircleShape).clickable {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply { data = android.net.Uri.parse("tel:+1234567890") }
                                        context.startActivity(intent)
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Phone, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
