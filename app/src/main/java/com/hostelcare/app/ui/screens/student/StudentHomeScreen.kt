package com.hostelcare.app.ui.screens.student

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
                    
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickActionCard("Report\nProblem", Icons.Default.Build, LightBlue, PrimaryBlue, Modifier.weight(1f)) { navController.navigate(Routes.NEW_COMPLAINT) }
                        QuickActionCard("Hostel\nGuidelines", Icons.Default.MenuBook, Color(0xFFF9FAFB), TextMuted, Modifier.weight(1f)) { 
                            android.widget.Toast.makeText(context, "Guidelines opened", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        QuickActionCard("Contact\nWarden", Icons.Default.Phone, Color(0xFFF9FAFB), TextMuted, Modifier.weight(1f)) { 
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                            intent.data = android.net.Uri.parse("tel:+1234567890")
                            context.startActivity(intent)
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

@Composable
fun FormField(label: String, required: Boolean = false, content: @Composable () -> Unit) {
    Column {
        Row {
            Text(label, fontSize = 13.sp, color = TextDark)
            if (required) {
                Text(" *", fontSize = 13.sp, color = DangerRed)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewComplaintScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val user by viewModel.currentUser.collectAsStateWithLifecycle(null)
    val draft by viewModel.draftComplaint.collectAsStateWithLifecycle()
    
    var title by remember(draft) { mutableStateOf(draft?.title ?: "") }
    var category by remember(draft) { mutableStateOf(if (draft?.category == ComplaintCategory.OTHER) "" else draft?.category?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "") }
    var hostelBlock by remember(draft, user) { mutableStateOf(draft?.hostelBlock?.takeIf { it.isNotBlank() } ?: user?.hostelBlock ?: "") }
    var room by remember(draft, user) { mutableStateOf(draft?.roomNumber?.takeIf { it.isNotBlank() } ?: user?.roomNumber ?: "") }
    var desc by remember(draft) { mutableStateOf(draft?.description ?: "") }
    var photoAdded by remember { mutableStateOf(false) }
    
    var categoryExpanded by remember { mutableStateOf(false) }
    var hostelExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("New Complaint", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextDark) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp).navigationBarsPadding()) {
                Button(
                    onClick = {
                        val catEnum = try { ComplaintCategory.valueOf(category.uppercase()) } catch(e: Exception) { ComplaintCategory.OTHER }
                        val complaint = com.hostelcare.app.data.model.Complaint(
                            id = draft?.id ?: java.util.UUID.randomUUID().toString(),
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
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = title.isNotBlank() && desc.isNotBlank() && hostelBlock.isNotBlank() && room.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Analyze Complaint", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().background(LightBlue, RoundedCornerShape(12.dp)).padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Report maintenance issues in your hostel. Our team will review and assign it to the relevant staff.", style = MaterialTheme.typography.bodyMedium, color = TextDark, lineHeight = 20.sp)
                }
            }
            
            item {
                FormField("Problem Title", required = true) {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        placeholder = { Text("e.g. Water leakage in bathroom", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Description, null, tint = TextMuted) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue)
                    )
                }
            }
            
            item {
                FormField("Category", required = false) {
                    Box {
                        OutlinedTextField(
                            value = category, onValueChange = { },
                            placeholder = { Text("Select category", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Build, null, tint = TextMuted) },
                            trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted) },
                            modifier = Modifier.fillMaxWidth(), readOnly = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue)
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { categoryExpanded = true })
                        DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }, modifier = Modifier.background(Color.White)) {
                            listOf("Plumbing", "Electrical", "Internet", "Cleaning", "Furniture", "Other").forEach { opt ->
                                DropdownMenuItem(text = { Text(opt) }, onClick = { category = opt; categoryExpanded = false })
                            }
                        }
                    }
                }
            }
            
            item {
                FormField("Hostel / Block", required = true) {
                    Box {
                        OutlinedTextField(
                            value = hostelBlock, onValueChange = { },
                            placeholder = { Text("Select hostel/block", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Domain, null, tint = TextMuted) },
                            trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted) },
                            modifier = Modifier.fillMaxWidth(), readOnly = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue)
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { hostelExpanded = true })
                        DropdownMenu(expanded = hostelExpanded, onDismissRequest = { hostelExpanded = false }, modifier = Modifier.background(Color.White)) {
                            listOf("Block A", "Block B", "Block C", "Tagore Hostel", "Block B - Tagore Hostel").forEach { opt ->
                                DropdownMenuItem(text = { Text(opt) }, onClick = { hostelBlock = opt; hostelExpanded = false })
                            }
                        }
                    }
                }
            }
            
            item {
                FormField("Room Number", required = true) {
                    OutlinedTextField(
                        value = room, onValueChange = { room = it },
                        placeholder = { Text("e.g. B-204", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.MeetingRoom, null, tint = TextMuted) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue)
                    )
                }
            }
            
            item {
                FormField("Description", required = true) {
                    OutlinedTextField(
                        value = desc, onValueChange = { if (it.length <= 500) desc = it },
                        placeholder = { Text("Describe the issue in detail...", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Subject, null, tint = TextMuted) },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = PrimaryBlue),
                        supportingText = { Text("${desc.length}/500", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) }
                    )
                }
            }
            
            item {
                FormField("Add Photo (Optional)", required = false) {
                    if (photoAdded) {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Image, null, tint = TextMuted, modifier = Modifier.size(40.dp))
                            IconButton(onClick = { photoAdded = false }, modifier = Modifier.align(Alignment.TopEnd)) {
                                Icon(Icons.Default.Close, "Remove photo", tint = DangerRed)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.dp, PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { photoAdded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddPhotoAlternate, null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tap to add photo", color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Supports JPG, PNG (Max 5 MB)", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiReviewScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StudentViewModel = viewModel(factory = factory)
    val state = viewModel.newComplaintState.collectAsStateWithLifecycle().value
    val draft by viewModel.draftComplaint.collectAsStateWithLifecycle()
    var submittingId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(state) {
        if (state is NewComplaintState.Success && submittingId != null) {
            navController.navigate(Routes.complaintDetails(submittingId!!)) {
                popUpTo(Routes.STUDENT_HOME)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Review Complaint", fontWeight = FontWeight.Bold, color = TextDark) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextDark) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)) },
        containerColor = Color.White,
        bottomBar = {
            if (state is NewComplaintState.AnalysisComplete) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                    Button(
                        onClick = {
                            submittingId = draft?.id
                            viewModel.submitComplaint()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) { Text("Confirm & Submit", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = TextDark)
                    ) { Text("Edit Complaint", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            when (state) {
                is NewComplaintState.Analyzing, is NewComplaintState.Submitting -> {
                    CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(if (state is NewComplaintState.Analyzing) "Analyzing complaint..." else "Submitting...", style = MaterialTheme.typography.titleMedium, color = TextDark)
                }
                is NewComplaintState.AnalysisComplete -> {
                    val analysis = state.result
                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("AI Suggestion", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("We've categorized and prioritized your issue.", color = TextMuted, textAlign = TextAlign.Center)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Color(0xFFE5E7EB))) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Category", fontSize = 13.sp, color = TextMuted)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(analysis.category.name, fontWeight = FontWeight.SemiBold, color = TextDark)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Priority", fontSize = 13.sp, color = TextMuted)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(analysis.priority.name, fontWeight = FontWeight.Bold, color = if (analysis.priority.name == "HIGH") DangerRed else WarningOrange)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = Color(0xFFE5E7EB))
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Text("Summary", fontSize = 13.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(analysis.summary, style = MaterialTheme.typography.bodyLarge, color = TextDark, lineHeight = 24.sp)
                        }
                    }
                }
                else -> {
                    Text(if (state is NewComplaintState.Error) state.message else "No complaint in progress.", color = TextMuted)
                }
            }
        }
    }
}
