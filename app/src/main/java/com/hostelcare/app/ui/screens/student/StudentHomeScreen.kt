package com.hostelcare.app.ui.screens.student

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.data.model.ComplaintCategory
import com.hostelcare.app.data.model.ComplaintStatus
import com.hostelcare.app.ui.components.StudentBottomNavigation
import com.hostelcare.app.ui.navigation.Routes
import com.hostelcare.app.ui.screens.auth.CustomTextField
import com.hostelcare.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val user by viewModel.currentUser.collectAsStateWithLifecycle(null)
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    
    val activeCount = complaints.count { it.status == ComplaintStatus.IN_PROGRESS || it.status == ComplaintStatus.ASSIGNED || it.status == ComplaintStatus.UNDER_REVIEW }
    val pendingCount = complaints.count { it.status == ComplaintStatus.SUBMITTED }
    val resolvedCount = complaints.count { it.status == ComplaintStatus.RESOLVED }

    Scaffold(
        bottomBar = { StudentBottomNavigation(navController, Routes.STUDENT_HOME) },
        containerColor = Color.White
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color.White)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(LightBlue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Home, null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("HostelCare", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("Student Portal", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.NotificationsNone, null, tint = TextDark, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(40.dp).background(GrayBg, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = TextMuted)
                }
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Good morning, ${user?.name?.split(" ")?.firstOrNull() ?: "Student"}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Need something fixed in your room?", style = MaterialTheme.typography.bodyLarge, color = TextMuted)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { navController.navigate(Routes.NEW_COMPLAINT) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Report a Problem", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Active", activeCount.toString(), Icons.Default.Description, LightBlue, PrimaryBlue, Modifier.weight(1f))
                        StatCard("Pending", pendingCount.toString(), Icons.Default.Schedule, OrangePillBg, WarningOrange, Modifier.weight(1f))
                        StatCard("Resolved", resolvedCount.toString(), Icons.Default.CheckCircle, GreenPillBg, SuccessGreen, Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("My Complaints", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                        Spacer(modifier = Modifier.weight(1f))
                        Row(modifier = Modifier.clickable { navController.navigate(Routes.MY_COMPLAINTS) }, verticalAlignment = Alignment.CenterVertically) {
                            Text("View all", color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.ChevronRight, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                items(complaints.take(3)) { c ->
                    val (icon, bg, fg) = when(c.status) {
                        ComplaintStatus.RESOLVED -> Triple(Icons.Default.Build, GreenPillBg, SuccessGreen)
                        ComplaintStatus.SUBMITTED -> Triple(Icons.Default.Wifi, OrangePillBg, WarningOrange)
                        else -> Triple(Icons.Default.WaterDrop, LightBlue, PrimaryBlue)
                    }
                    val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(c.createdAt))

                    Column(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Routes.complaintDetails(c.id)) }) {
                        Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextDark)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Room ${c.roomNumber} · ${c.category.name}", fontSize = 12.sp, color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Box(modifier = Modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text(c.status.name, color = fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(dateStr, fontSize = 12.sp, color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickActionCard("Report\nProblem", Icons.Default.Build, LightBlue, PrimaryBlue, Modifier.weight(1f)) { navController.navigate(Routes.NEW_COMPLAINT) }
                        QuickActionCard("Hostel\nGuidelines", Icons.Default.MenuBook, Color(0xFFF9FAFB), TextMuted, Modifier.weight(1f)) { }
                        QuickActionCard("Contact\nWarden", Icons.Default.Phone, Color(0xFFF9FAFB), TextMuted, Modifier.weight(1f)) { }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Announcements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                        Spacer(modifier = Modifier.weight(1f))
                        Row(modifier = Modifier.clickable { }, verticalAlignment = Alignment.CenterVertically) {
                            Text("View all", color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.ChevronRight, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Box(modifier = Modifier.size(40.dp).background(LightBlue, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Campaign, null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Water Supply Maintenance", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextDark)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("Oct 12", fontSize = 12.sp, color = TextMuted)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Block B water supply will be unavailable tomorrow from 2 PM – 4 PM.", fontSize = 14.sp, color = TextMuted, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, bg: Color, iconColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6))
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(label, fontSize = 12.sp, color = TextMuted)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun QuickActionCard(label: String, icon: ImageVector, bg: Color, fg: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, color = fg, textAlign = TextAlign.Center, lineHeight = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewComplaintScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val user by viewModel.currentUser.collectAsStateWithLifecycle(null)
    
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var hostelBlock by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("New Complaint", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)) },
        containerColor = BackgroundLight
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { CustomTextField(title, { title = it }, "Problem Title") }
                item { CustomTextField(category, { category = it }, "Category") }
                item { CustomTextField(hostelBlock, { hostelBlock = it }, "Hostel / Block") }
                item { CustomTextField(room, { room = it }, "Room Number") }
                item {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        placeholder = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedContainerColor = SurfaceLight,
                            focusedContainerColor = SurfaceLight
                        )
                    )
                }
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(SurfaceLight, RoundedCornerShape(8.dp)).clickable { /* add photo */ }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Add Photo", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val catEnum = try { ComplaintCategory.valueOf(category.uppercase()) } catch(e: Exception) { ComplaintCategory.OTHER }
                    val complaint = com.hostelcare.app.data.model.Complaint(
                        title = title,
                        description = desc,
                        category = catEnum,
                        hostelBlock = hostelBlock,
                        roomNumber = room,
                        studentId = user?.studentId ?: "UNKNOWN"
                    )
                    viewModel.startNewComplaint(complaint)
                    viewModel.analyzeComplaint()
                    navController.navigate(Routes.AI_REVIEW)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = title.isNotBlank() && desc.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) { Text("Continue") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiReviewScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val state = viewModel.newComplaintState.collectAsStateWithLifecycle().value

    Scaffold(
        topBar = { TopAppBar(title = { Text("AI Review", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)) },
        containerColor = BackgroundLight
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            when (state) {
                is NewComplaintState.Analyzing -> {
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Analyzing...", color = TextMuted)
                }
                is NewComplaintState.AnalysisComplete -> {
                    val analysis = state.result
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Category: ${analysis.category.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Priority: ${analysis.priority.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (analysis.priority.name == "HIGH") DangerRed else TextDark)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(analysis.summary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.submitComplaint()
                            navController.navigate(Routes.STUDENT_HOME) { popUpTo(0) }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) { Text("Confirm & Submit") }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LightBlue, contentColor = PrimaryBlue)
                    ) { Text("Edit Complaint") }
                }
                else -> {
                    Text("No complaint in progress.", color = TextMuted)
                }
            }
        }
    }
}
