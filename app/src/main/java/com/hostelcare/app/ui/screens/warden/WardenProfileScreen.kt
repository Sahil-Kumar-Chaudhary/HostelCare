package com.hostelcare.app.ui.screens.warden

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.ui.components.WardenBottomNavigation
import com.hostelcare.app.ui.navigation.Routes
import com.hostelcare.app.ui.screens.auth.CustomTextField
import com.hostelcare.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardenProfileScreen(
    navController: NavController,
    app: HostelCareApp
) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: WardenViewModel = viewModel(factory = factory)

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle(null)
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            WardenBottomNavigation(
                navController = navController,
                currentRoute = Routes.WARDEN_PROFILE
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Warden Profile & Settings",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        // Edit Profile Dialog
        if (showEditProfileDialog) {
            val wardenData = (profileState as? WardenProfileUiState.Success)?.profile
            var editName by remember { mutableStateOf(wardenData?.name ?: currentUser?.name ?: "") }
            var editPhone by remember { mutableStateOf(wardenData?.phone ?: currentUser?.phone ?: "") }
            var isSaving by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { if (!isSaving) showEditProfileDialog = false },
                title = { Text("Edit Profile Details", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Full Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        CustomTextField(value = editName, onValueChange = { editName = it }, placeholder = "Full Name")

                        Text("Phone Number", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        CustomTextField(value = editPhone, onValueChange = { editPhone = it }, placeholder = "Phone Number")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isSaving = true
                            viewModel.updateProfile(
                                name = editName.trim(),
                                phone = editPhone.trim(),
                                onComplete = { success, err ->
                                    isSaving = false
                                    showEditProfileDialog = false
                                    scope.launch {
                                        if (success) snackbarHostState.showSnackbar("Profile updated successfully")
                                        else snackbarHostState.showSnackbar(err ?: "Failed to update profile")
                                    }
                                }
                            )
                        },
                        enabled = !isSaving
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileDialog = false }, enabled = !isSaving) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Change Password Dialog
        if (showChangePasswordDialog) {
            var currentPassword by remember { mutableStateOf("") }
            var newPassword by remember { mutableStateOf("") }
            var confirmPassword by remember { mutableStateOf("") }
            var passwordError by remember { mutableStateOf<String?>(null) }
            var isChanging by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { if (!isChanging) showChangePasswordDialog = false },
                title = { Text("Change Password", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (passwordError != null) {
                            Text(passwordError!!, color = DangerRed, fontSize = 12.sp)
                        }

                        Text("Current Password", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        CustomTextField(value = currentPassword, onValueChange = { currentPassword = it }, placeholder = "Current Password", isPassword = true)

                        Text("New Password", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        CustomTextField(value = newPassword, onValueChange = { newPassword = it }, placeholder = "Min 8 chars with mixed case & symbol", isPassword = true)

                        Text("Confirm New Password", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        CustomTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, placeholder = "Confirm New Password", isPassword = true)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (currentPassword.isBlank() || newPassword.isBlank()) {
                                passwordError = "All fields are required"
                                return@Button
                            }
                            if (newPassword != confirmPassword) {
                                passwordError = "New passwords do not match"
                                return@Button
                            }
                            if (newPassword.length < 8) {
                                passwordError = "Password must be at least 8 characters"
                                return@Button
                            }
                            isChanging = true
                            viewModel.changePassword(
                                currentPassword = currentPassword,
                                newPassword = newPassword,
                                onComplete = { success, err ->
                                    isChanging = false
                                    if (success) {
                                        showChangePasswordDialog = false
                                        scope.launch { snackbarHostState.showSnackbar("Password updated successfully") }
                                    } else {
                                        passwordError = err ?: "Failed to change password"
                                    }
                                }
                            )
                        },
                        enabled = !isChanging
                    ) {
                        Text("Update Password")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChangePasswordDialog = false }, enabled = !isChanging) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Logout Confirmation Dialog
        if (showLogoutConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirmDialog = false },
                title = { Text("Confirm Logout", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to log out from the Warden portal?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutConfirmDialog = false
                            viewModel.logout {
                                navController.navigate(Routes.PORTAL_SELECTION) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text("Log Out")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        val warden = (profileState as? WardenProfileUiState.Success)?.profile
        val nameStr = warden?.name ?: currentUser?.name ?: "Hostel Warden"
        val emailStr = warden?.email ?: currentUser?.email ?: ""
        val phoneStr = warden?.phone ?: currentUser?.phone ?: "Not provided"
        val hostelStr = warden?.hostelName ?: currentUser?.hostelName ?: "Main Hostel"
        val blockStr = warden?.hostelBlock ?: currentUser?.hostelBlock ?: ""
        val wardenIdStr = warden?.wardenId ?: currentUser?.wardenId ?: ""
        val genderStr = warden?.gender ?: currentUser?.gender ?: ""

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Hero Card
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
                            text = nameStr.take(2).uppercase(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = nameStr,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = emailStr,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Chief Warden · Campus-Wide (20 Hostels)${if (wardenIdStr.isNotBlank()) " · $wardenIdStr" else ""}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Personal Information Section
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
                        Text(
                            text = "Personal Information",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { showEditProfileDialog = true }) {
                            Text("Edit", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ProfileDetailRow(icon = Icons.Default.Person, label = "Full Name", value = nameStr)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (wardenIdStr.isNotBlank()) {
                        ProfileDetailRow(icon = Icons.Default.Badge, label = "Warden ID", value = wardenIdStr)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (genderStr.isNotBlank()) {
                        ProfileDetailRow(icon = Icons.Default.People, label = "Gender", value = genderStr)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    ProfileDetailRow(icon = Icons.Default.Email, label = "Email", value = emailStr)
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileDetailRow(icon = Icons.Default.Phone, label = "Phone", value = phoneStr)
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileDetailRow(
                        icon = Icons.Default.Apartment,
                        label = "Authorized Hostel",
                        value = if (warden?.isChiefWarden == true || emailStr.equals("warden@hostelcare.com", ignoreCase = true) || hostelStr.contains("All Hostels", ignoreCase = true)) "All Hostels (Campus-Wide)" else hostelStr
                    )
                }
            }

            // Warden Administration Section
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Administration",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        onClick = { navController.navigate(Routes.WARDEN_STAFF) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Engineering, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Manage Staff", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Assign and manage hostel maintenance staff across all hostels", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // App Preferences Section
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preferences",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Dark Mode", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (isDarkMode) "Dark theme enabled" else "Light theme enabled", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { ThemeManager.setDarkMode(context, it) }
                        )
                    }
                }
            }

            // Security & Account Section
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Security & Login",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        onClick = { showChangePasswordDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Change Password", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Logout Button
            Button(
                onClick = { showLogoutConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out from Warden Portal", color = DangerRed, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text("$label: ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
