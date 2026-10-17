package com.hostelcare.app.ui.screens.warden

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.data.remote.CreateStaffRequest
import com.hostelcare.app.ui.screens.auth.CustomTextField
import com.hostelcare.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardenAddStaffScreen(
    navController: NavController,
    app: HostelCareApp
) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: WardenViewModel = viewModel(factory = factory)

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("Plumbing") }
    var hostelBlock by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var expandedDeptDropdown by remember { mutableStateOf(false) }

    val departments = listOf(
        "Plumbing",
        "Electrical",
        "Carpentry",
        "Cleaning & Sanitation",
        "Wi-Fi & Network",
        "General Maintenance"
    )

    val campusHostels = remember {
        (1..10).map { "BH-$it" } + (1..10).map { "GH-$it" }
    }
    var selectedHostel by remember { mutableStateOf("BH-1") }
    var expandedHostelDropdown by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun generateRandomPassword() {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$"
        val pwd = (1..10).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        password = pwd
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Staff Member",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                                errorMessage = "Please fill in all required fields (Name, Email, Password)"
                                return@Button
                            }
                            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                errorMessage = "Please enter a valid email address"
                                return@Button
                            }
                            if (password.length < 8) {
                                errorMessage = "Temporary password must be at least 8 characters"
                                return@Button
                            }

                            errorMessage = null
                            isSubmitting = true

                            viewModel.createStaff(
                                request = CreateStaffRequest(
                                    name = name.trim(),
                                    email = email.trim().toLowerCase(java.util.Locale.ROOT),
                                    phone = phone.trim(),
                                    department = department,
                                    hostelName = selectedHostel,
                                    hostelBlock = hostelBlock.trim().ifBlank { null },
                                    password = password
                                ),
                                onComplete = { success, error ->
                                    isSubmitting = false
                                    if (success) {
                                        navController.popBackStack()
                                    } else {
                                        errorMessage = error ?: "Failed to create staff member"
                                    }
                                }
                            )
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Create Staff Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Notice Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "New staff members can be assigned across any of the 20 campus hostels and can log in via the Maintenance Staff portal.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DangerRed.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = DangerRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Full Name
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Full Name *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                CustomTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    placeholder = "e.g. Ramesh Kumar",
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }

            // Email Address
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Email Address *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                CustomTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    placeholder = "e.g. ramesh.kumar@hostelcare.com",
                    leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }

            // Phone Number
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Phone Number", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                CustomTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        errorMessage = null
                    },
                    placeholder = "e.g. +91 98765 43210",
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }

            // Department Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Department / Trade *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Box {
                    Surface(
                        onClick = { expandedDeptDropdown = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(department, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    DropdownMenu(
                        expanded = expandedDeptDropdown,
                        onDismissRequest = { expandedDeptDropdown = false }
                    ) {
                        departments.forEach { dept ->
                            DropdownMenuItem(
                                text = { Text(dept) },
                                onClick = {
                                    department = dept
                                    expandedDeptDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Assigned Hostel
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Assigned Hostel *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Box {
                    Surface(
                        onClick = { expandedHostelDropdown = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(selectedHostel, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    DropdownMenu(
                        expanded = expandedHostelDropdown,
                        onDismissRequest = { expandedHostelDropdown = false }
                    ) {
                        campusHostels.forEach { h ->
                            DropdownMenuItem(
                                text = { Text(h) },
                                onClick = {
                                    selectedHostel = h
                                    expandedHostelDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Assigned Hostel Block
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Assigned Hostel Block (Optional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                CustomTextField(
                    value = hostelBlock,
                    onValueChange = { hostelBlock = it },
                    placeholder = "e.g. Block A or All Blocks",
                    leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }

            // Temporary Password
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Temporary Password *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = { generateRandomPassword() }) {
                        Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto-generate", fontSize = 12.sp)
                    }
                }

                CustomTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    placeholder = "Minimum 8 characters",
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
