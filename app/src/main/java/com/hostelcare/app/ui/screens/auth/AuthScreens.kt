package com.hostelcare.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.data.model.Role
import com.hostelcare.app.data.model.User
import com.hostelcare.app.ui.navigation.Routes
import com.hostelcare.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextMuted) },
        leadingIcon = leadingIcon,
        visualTransformation = if (isPassword && !passwordVisible) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        trailingIcon = trailingIcon ?: if (isPassword) { 
            { 
                val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(icon, contentDescription = if (passwordVisible) "Hide password" else "Show password", tint = TextMuted)
                }
            } 
        } else null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFE5E7EB),
            focusedBorderColor = PrimaryBlue,
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            focusedLeadingIconColor = PrimaryBlue,
            unfocusedLeadingIconColor = TextMuted,
            focusedTrailingIconColor = PrimaryBlue,
            unfocusedTrailingIconColor = TextMuted
        ),
        singleLine = true
    )
}

@Composable
fun AppLogoAndTitle() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(80.dp).background(LightBlue, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Home, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("HostelCare", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Making hostel life easier.", style = MaterialTheme.typography.bodyLarge, color = TextMuted)
    }
}

@Composable
fun SplashScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle(null)
    
    LaunchedEffect(currentUser) {
        delay(1000)
        if (currentUser != null) {
            if (currentUser!!.role == Role.ADMIN) navController.navigate(Routes.ADMIN_HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
            else navController.navigate(Routes.STUDENT_HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
        } else {
            navController.navigate(Routes.LOGIN) { popUpTo(Routes.SPLASH) { inclusive = true } }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.align(Alignment.Center).offset(y = (-40).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AppLogoAndTitle()
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
        
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Business, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("STUDENT HOUSING NETWORK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark, letterSpacing = 1.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tagore Campus", fontSize = 12.sp, color = TextMuted)
        }
    }
}

@Composable
fun LoginScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            viewModel.resetState()
            navController.navigate(Routes.STUDENT_HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(24.dp), 
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        AppLogoAndTitle()
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Text("Welcome back", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Sign in with your student credentials to manage room maintenance and campus requests.", 
            style = MaterialTheme.typography.bodyMedium, 
            color = TextMuted, 
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        CustomTextField(email, { email = it }, "Email or Student ID", leadingIcon = { Icon(Icons.Default.Person, null) })
        Spacer(modifier = Modifier.height(16.dp))
        CustomTextField(password, { password = it }, "Password", isPassword = true, leadingIcon = { Icon(Icons.Default.Lock, null) })
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Forgot password?", style = MaterialTheme.typography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.End).clickable { })
        
        if (uiState is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text((uiState as AuthState.Error).message, color = DangerRed, style = MaterialTheme.typography.bodySmall)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.login(email, password) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            if (uiState is AuthState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("Log in", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFF3F4F6))
            Text("or", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFF3F4F6))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp)).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Don't have an account?", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Create account", color = PrimaryBlue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { navController.navigate(Routes.SIGN_UP) })
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Admin Login", color = TextMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { navController.navigate(Routes.ADMIN_LOGIN) })
    }
}

@Composable
fun SignUpScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var hostelBlock by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }
    var expandedHostel by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            viewModel.resetState()
            navController.navigate(Routes.STUDENT_HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(24.dp).clickable { navController.popBackStack() })
        }
        
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Create Account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Register your student details to get started.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            CustomTextField(name, { name = it }, "Full Name", leadingIcon = { Icon(Icons.Default.Person, null) })
            Spacer(modifier = Modifier.height(16.dp))
            CustomTextField(studentId, { studentId = it }, "Student ID", leadingIcon = { Icon(Icons.Default.Numbers, null) })
            Spacer(modifier = Modifier.height(16.dp))
            CustomTextField(email, { email = it }, "College Email", leadingIcon = { Icon(Icons.Default.Email, null) })
            Spacer(modifier = Modifier.height(16.dp))
            CustomTextField(password, { password = it }, "Password", isPassword = true, leadingIcon = { Icon(Icons.Default.Lock, null) })
            Spacer(modifier = Modifier.height(16.dp))
            CustomTextField(confirmPassword, { confirmPassword = it }, "Confirm Password", isPassword = true, leadingIcon = { Icon(Icons.Default.Lock, null) })
            Spacer(modifier = Modifier.height(16.dp))
            
            // Simulating a dropdown for Hostel/Block
            Box {
                CustomTextField(
                    value = hostelBlock, 
                    onValueChange = { }, 
                    placeholder = "Hostel / Block", 
                    leadingIcon = { Icon(Icons.Default.Domain, null) },
                    trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) }
                )
                // Overlay click for dropdown
                Box(modifier = Modifier.matchParentSize().clickable { expandedHostel = true })
                
                DropdownMenu(expanded = expandedHostel, onDismissRequest = { expandedHostel = false }) {
                    listOf("Tagore Block A", "Tagore Block B", "Raman Block").forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = { hostelBlock = opt; expandedHostel = false }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            CustomTextField(room, { room = it }, "Room Number", leadingIcon = { Icon(Icons.Default.MeetingRoom, null) })
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = agreed, 
                    onCheckedChange = { agreed = it },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue),
                    modifier = Modifier.offset(x = (-12).dp, y = (-12).dp)
                )
                Text("I agree to the Hostel Resident Code of Conduct and allow maintenance staff entry for logged work orders.", style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.offset(x = (-12).dp))
            }
            
            if (uiState is AuthState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text((uiState as AuthState.Error).message, color = DangerRed, style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.register(User(name = name, email = email, studentId = studentId, hostelBlock = hostelBlock, roomNumber = room), password) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = agreed && name.isNotBlank() && email.isNotBlank() && password == confirmPassword && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (uiState is AuthState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("Already have an account? ", color = TextMuted)
                Text("Log in", color = PrimaryBlue, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { navController.popBackStack() })
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun AdminLoginScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            viewModel.resetState()
            navController.navigate(Routes.ADMIN_HOME) { popUpTo(Routes.ADMIN_LOGIN) { inclusive = true } }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundLight).verticalScroll(rememberScrollState()).padding(24.dp), 
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = DangerRed, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Staff Login", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        
        CustomTextField(email, { email = it }, "Email")
        Spacer(modifier = Modifier.height(16.dp))
        CustomTextField(password, { password = it }, "Password", isPassword = true)
        Spacer(modifier = Modifier.height(32.dp))
        
        if (uiState is AuthState.Error) {
            Text((uiState as AuthState.Error).message, color = DangerRed, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = { viewModel.login(email, password) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text("Log In")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = { navController.popBackStack() }) {
            Text("Student Login Instead", color = TextMuted)
        }
    }
}
