package com.hostelcare.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
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
import com.hostelcare.app.ui.components.HostelCareLogo
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
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        singleLine = true
    )
}

@Composable
fun AppLogoAndTitle(
    logoSize: androidx.compose.ui.unit.Dp = 80.dp,
    showSubtitle: Boolean = true
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HostelCareLogo(size = logoSize)
        Spacer(modifier = Modifier.height(20.dp))
        Text("HostelCare", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        if (showSubtitle) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Making hostel life easier.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SplashScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle(null)
    val isSessionChecked by viewModel.isSessionChecked.collectAsStateWithLifecycle(false)
    
    LaunchedEffect(currentUser, isSessionChecked) {
        if (isSessionChecked) {
            delay(500)
            if (currentUser != null) {
                if (currentUser!!.role == Role.ADMIN) navController.navigate(Routes.ADMIN_HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
                else navController.navigate(Routes.STUDENT_HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
            } else {
                navController.navigate(Routes.LOGIN) { popUpTo(Routes.SPLASH) { inclusive = true } }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.align(Alignment.Center).offset(y = (-30).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AppLogoAndTitle(logoSize = 110.dp)
            Spacer(modifier = Modifier.height(36.dp))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
        
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("STUDENT HOUSING NETWORK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, letterSpacing = 1.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tagore Campus", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(24.dp), 
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        AppLogoAndTitle()
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Text("Welcome back", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Sign in with your student credentials to manage room maintenance and campus requests.", 
            style = MaterialTheme.typography.bodyMedium, 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        CustomTextField(email, { email = it }, "Email or Student ID", leadingIcon = { Icon(Icons.Default.Person, null) })
        Spacer(modifier = Modifier.height(16.dp))
        CustomTextField(password, { password = it }, "Password", isPassword = true, leadingIcon = { Icon(Icons.Default.Lock, null) })
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Forgot password?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.End).clickable { })
        
        if (uiState is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text((uiState as AuthState.Error).message, color = DangerRed, style = MaterialTheme.typography.bodySmall)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.login(email, password) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Text("or", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Don't have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Create account", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { navController.navigate(Routes.SIGN_UP) })
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Admin Login", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { navController.navigate(Routes.ADMIN_LOGIN) })
    }
}

@Composable
fun RegistrationFormField(
    label: String,
    required: Boolean = true,
    error: String? = null,
    helper: String? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            if (required) {
                Text(" *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DangerRed)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        content()
        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(error, fontSize = 12.sp, color = DangerRed)
        } else if (!helper.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(helper, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RegistrationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        readOnly = readOnly,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedBorderColor = if (isError) DangerRed else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            focusedBorderColor = if (isError) DangerRed else MaterialTheme.colorScheme.primary,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
fun SignUpScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var currentStep by remember { mutableStateOf(1) }

    // Step 1: Personal Details
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedCountryCode by remember { mutableStateOf("+91") }
    var showCountryMenu by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }

    // Step 2: College Details
    var collegeName by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var admissionYear by remember { mutableStateOf("") }
    var showAdmissionYearMenu by remember { mutableStateOf(false) }

    // Step 3: Hostel Details
    var hostelName by remember { mutableStateOf("") }
    var hostelBlock by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }

    // Step 4: Security
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }

    // Validation error maps
    var step1Errors by remember { mutableStateOf(mapOf<String, String>()) }
    var step2Errors by remember { mutableStateOf(mapOf<String, String>()) }
    var step3Errors by remember { mutableStateOf(mapOf<String, String>()) }
    var step4Errors by remember { mutableStateOf(mapOf<String, String>()) }

    // Handle system back navigation
    BackHandler(enabled = currentStep > 1) {
        currentStep--
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            viewModel.resetState()
            android.widget.Toast.makeText(app, "Account created successfully", android.widget.Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
    }

    // Step 4 password rules
    val hasMinLength = password.length >= 8
    val hasUpper = password.any { it.isUpperCase() }
    val hasLower = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }
    val allRulesMet = hasMinLength && hasUpper && hasLower && hasDigit && hasSpecial && password.length <= 128

    val strengthLabel: String
    val strengthColor: Color
    val strengthLevel: Int
    if (password.isEmpty()) {
        strengthLabel = ""
        strengthColor = Color.Transparent
        strengthLevel = 0
    } else if (allRulesMet) {
        if (password.length >= 10) {
            strengthLabel = "Strong"
            strengthColor = Color(0xFF10B981)
            strengthLevel = 4
        } else {
            strengthLabel = "Medium"
            strengthColor = Color(0xFFF59E0B)
            strengthLevel = 3
        }
    } else {
        val rulesCount = listOf(hasMinLength, hasUpper, hasLower, hasDigit, hasSpecial).count { it }
        if (rulesCount >= 3 && password.length >= 6) {
            strengthLabel = "Medium"
            strengthColor = Color(0xFFF59E0B)
            strengthLevel = 2
        } else {
            strengthLabel = "Weak"
            strengthColor = Color(0xFFEF4444)
            strengthLevel = 1
        }
    }



    fun validateStep1(): Boolean {
        val errs = mutableMapOf<String, String>()
        if (name.trim().isBlank()) errs["name"] = "Full name is required"
        else if (name.trim().length < 2) errs["name"] = "Name must be at least 2 characters"

        if (studentId.trim().isBlank()) errs["studentId"] = "Student ID is required"
        else if (!studentId.trim().matches(Regex("^[a-zA-Z0-9\\-_]+$"))) errs["studentId"] = "Student ID must be alphanumeric"

        val cleanEmail = email.trim().lowercase()
        val gmailRegex = Regex("^[a-zA-Z0-9_.+-]+@gmail\\.com$")
        if (cleanEmail.isBlank()) {
            errs["email"] = "Email is required"
        } else if (!cleanEmail.endsWith("@gmail.com")) {
            errs["email"] = "Only @gmail.com email addresses are allowed"
        } else if (!gmailRegex.matches(cleanEmail)) {
            errs["email"] = "Please enter a valid @gmail.com address"
        }

        if (phone.trim().isBlank()) errs["phone"] = "Phone number is required"
        else if (phone.trim().length < 7) errs["phone"] = "Enter a valid phone number"

        step1Errors = errs
        return errs.isEmpty()
    }

    fun validateStep2(): Boolean {
        val errs = mutableMapOf<String, String>()
        if (collegeName.trim().isBlank()) errs["collegeName"] = "College / University name is required"
        if (course.trim().isBlank()) errs["course"] = "Course / Department is required"
        if (admissionYear.trim().isBlank()) errs["admissionYear"] = "Please select admission year"
        step2Errors = errs
        return errs.isEmpty()
    }

    fun validateStep3(): Boolean {
        val errs = mutableMapOf<String, String>()
        if (hostelName.trim().isBlank()) errs["hostelName"] = "Hostel name is required"
        if (hostelBlock.trim().isBlank()) errs["hostelBlock"] = "Block is required"
        if (room.trim().isBlank()) errs["room"] = "Room number is required"
        step3Errors = errs
        return errs.isEmpty()
    }

    fun validateStep4(): Boolean {
        val errs = mutableMapOf<String, String>()
        if (!allRulesMet) errs["password"] = "Password must meet all 5 requirements"
        if (password != confirmPassword) errs["confirmPassword"] = "Passwords do not match"
        if (!acceptedTerms) errs["terms"] = "You must agree to the Terms of Service and Code of Conduct"
        step4Errors = errs
        return errs.isEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Top Navigation & Brand Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (currentStep > 1) currentStep-- else navController.popBackStack()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HostelCareLogo(size = 32.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 15.sp)) {
                            append("HOSTEL")
                        }
                        withStyle(SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 15.sp)) {
                            append("CARE")
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Heading & Step-specific subtitle
        Text(
            "Create your account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when (currentStep) {
                1 -> "Let's start with your personal details."
                2 -> "Now tell us about your college."
                3 -> "Tell us about your hostel details."
                else -> "Secure your account with a strong password."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 4-Step Stepper Progress Indicator
        val stepNames = listOf("Personal", "College", "Hostel", "Security")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stepNames.forEachIndexed { index, title ->
                val stepNum = index + 1
                val isCompleted = stepNum < currentStep
                val isActive = stepNum == currentStep

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(enabled = isCompleted) { currentStep = stepNum }
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = if (isActive || isCompleted) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "$stepNum",
                                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive || isCompleted) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (index < stepNames.size - 1) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .offset(y = (-8).dp)
                            .background(if (stepNum < currentStep) PrimaryBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section Heading Card with Icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryBlue, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (currentStep) {
                            1 -> Icons.Default.Person
                            2 -> Icons.Default.School
                            3 -> Icons.Default.Home
                            else -> Icons.Default.Lock
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = when (currentStep) {
                            1 -> "Personal Details"
                            2 -> "College Details"
                            3 -> "Hostel Details"
                            else -> "Secure Your Account"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (currentStep) {
                            1 -> "Enter your basic information"
                            2 -> "Enter your college or university information"
                            3 -> "Enter your hostel information"
                            else -> "Create a strong password to keep your account safe."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // STEP 1: PERSONAL DETAILS
        if (currentStep == 1) {
            RegistrationFormField(
                label = "Full Name",
                required = true,
                error = step1Errors["name"]
            ) {
                RegistrationTextField(
                    value = name,
                    onValueChange = { name = it; step1Errors = step1Errors - "name" },
                    placeholder = "Enter your full name",
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = TextMuted) },
                    isError = step1Errors.containsKey("name"),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            RegistrationFormField(
                label = "Student ID",
                required = true,
                error = step1Errors["studentId"]
            ) {
                RegistrationTextField(
                    value = studentId,
                    onValueChange = { studentId = it; step1Errors = step1Errors - "studentId" },
                    placeholder = "Enter your student ID",
                    leadingIcon = { Text("#", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp)) },
                    isError = step1Errors.containsKey("studentId")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            RegistrationFormField(
                label = "Email",
                required = true,
                error = step1Errors["email"]
            ) {
                RegistrationTextField(
                    value = email,
                    onValueChange = { email = it; step1Errors = step1Errors - "email" },
                    placeholder = "Enter your email (e.g. name@gmail.com)",
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = TextMuted) },
                    isError = step1Errors.containsKey("email"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            RegistrationFormField(
                label = "Phone Number",
                required = true,
                error = step1Errors["phone"]
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Row(
                            modifier = Modifier
                                .height(54.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .clickable { showCountryMenu = true }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(selectedCountryCode, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = showCountryMenu,
                            onDismissRequest = { showCountryMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            listOf("+91", "+1", "+44", "+61", "+971", "+65", "+81", "+49").forEach { code ->
                                DropdownMenuItem(
                                    text = { Text(code, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        selectedCountryCode = code
                                        showCountryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    RegistrationTextField(
                        value = phone,
                        onValueChange = { phone = it; step1Errors = step1Errors - "phone" },
                        placeholder = "Enter your mobile number",
                        isError = step1Errors.containsKey("phone"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    if (validateStep1()) {
                        currentStep = 2
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        // STEP 2: COLLEGE DETAILS
        if (currentStep == 2) {
            RegistrationFormField(
                label = "College / University Name",
                required = true,
                error = step2Errors["collegeName"]
            ) {
                RegistrationTextField(
                    value = collegeName,
                    onValueChange = { collegeName = it; step2Errors = step2Errors - "collegeName" },
                    placeholder = "Enter college or university name",
                    leadingIcon = { Icon(Icons.Default.School, null, tint = TextMuted) },
                    isError = step2Errors.containsKey("collegeName")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            RegistrationFormField(
                label = "Course / Department",
                required = true,
                error = step2Errors["course"],
                helper = "e.g. Computer Science & Engineering"
            ) {
                RegistrationTextField(
                    value = course,
                    onValueChange = { course = it; step2Errors = step2Errors - "course" },
                    placeholder = "Enter your course or department",
                    leadingIcon = { Icon(Icons.Default.MenuBook, null, tint = TextMuted) },
                    isError = step2Errors.containsKey("course")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            RegistrationFormField(
                label = "Admission Year",
                required = true,
                error = step2Errors["admissionYear"]
            ) {
                Box {
                    RegistrationTextField(
                        value = admissionYear,
                        onValueChange = {},
                        placeholder = "Select admission year",
                        leadingIcon = { Icon(Icons.Default.CalendarToday, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted) },
                        readOnly = true,
                        isError = step2Errors.containsKey("admissionYear")
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showAdmissionYearMenu = true }
                    )
                    DropdownMenu(
                        expanded = showAdmissionYearMenu,
                        onDismissRequest = { showAdmissionYearMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020", "2019", "2018").forEach { yr ->
                            DropdownMenuItem(
                                text = { Text(yr, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    admissionYear = yr
                                    showAdmissionYearMenu = false
                                    step2Errors = step2Errors - "admissionYear"
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { currentStep = 1 },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, PrimaryBlue),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (validateStep2()) {
                            currentStep = 3
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }

        // STEP 3: HOSTEL DETAILS
        if (currentStep == 3) {
            RegistrationFormField(
                label = "Hostel Name",
                required = true,
                error = step3Errors["hostelName"],
                helper = "e.g. University Hostel"
            ) {
                RegistrationTextField(
                    value = hostelName,
                    onValueChange = { hostelName = it; step3Errors = step3Errors - "hostelName" },
                    placeholder = "Enter hostel name",
                    leadingIcon = { Icon(Icons.Default.Home, null, tint = TextMuted) },
                    isError = step3Errors.containsKey("hostelName")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BLOCK: TEXT INPUT (NOT A DROPDOWN)
            RegistrationFormField(
                label = "Block",
                required = true,
                error = step3Errors["hostelBlock"],
                helper = "e.g. A or Tagore Block A"
            ) {
                RegistrationTextField(
                    value = hostelBlock,
                    onValueChange = { hostelBlock = it; step3Errors = step3Errors - "hostelBlock" },
                    placeholder = "Enter block name or code",
                    leadingIcon = { Icon(Icons.Default.Domain, null, tint = TextMuted) },
                    isError = step3Errors.containsKey("hostelBlock")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            RegistrationFormField(
                label = "Room Number",
                required = true,
                error = step3Errors["room"],
                helper = "e.g. 101 or A-23"
            ) {
                RegistrationTextField(
                    value = room,
                    onValueChange = { room = it; step3Errors = step3Errors - "room" },
                    placeholder = "Enter your room number",
                    leadingIcon = { Icon(Icons.Default.MeetingRoom, null, tint = TextMuted) },
                    isError = step3Errors.containsKey("room")
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { currentStep = 2 },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, PrimaryBlue),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (validateStep3()) {
                            currentStep = 4
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }

        // STEP 4: SECURE YOUR ACCOUNT
        if (currentStep == 4) {
            RegistrationFormField(
                label = "Password",
                required = true,
                error = step4Errors["password"]
            ) {
                RegistrationTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        step4Errors = step4Errors - "password"
                        if (confirmPassword.isNotEmpty() && it != confirmPassword) {
                            step4Errors = step4Errors + ("confirmPassword" to "Passwords do not match")
                        } else {
                            step4Errors = step4Errors - "confirmPassword"
                        }
                    },
                    placeholder = "Enter a strong password",
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextMuted) },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = null, tint = TextMuted)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = step4Errors.containsKey("password")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            RegistrationFormField(
                label = "Confirm Password",
                required = true,
                error = step4Errors["confirmPassword"]
            ) {
                RegistrationTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        if (password != it) {
                            step4Errors = step4Errors + ("confirmPassword" to "Passwords do not match")
                        } else {
                            step4Errors = step4Errors - "confirmPassword"
                        }
                    },
                    placeholder = "Re-enter your password",
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextMuted) },
                    trailingIcon = {
                        val icon = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(icon, contentDescription = null, tint = TextMuted)
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = step4Errors.containsKey("confirmPassword")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Password Requirements & Live Strength Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Password Requirements",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Strength Indicator
                        if (password.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Password Strength: ",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = strengthLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = strengthColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Strength Progress Bars (4 segments)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in 1..4) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .background(
                                        if (strengthLevel > 0 && i <= strengthLevel) strengthColor else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5 Requirements Checklist
                    val rules = listOf(
                        Pair(hasMinLength, "At least 8 characters"),
                        Pair(hasUpper, "At least one uppercase letter (A–Z)"),
                        Pair(hasLower, "At least one lowercase letter (a–z)"),
                        Pair(hasDigit, "At least one number (0–9)"),
                        Pair(hasSpecial, "At least one special character (!@#$ %^&*)")
                    )

                    rules.forEach { (met, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (met) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = if (met) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Terms & Conditions Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = acceptedTerms,
                    onCheckedChange = {
                        acceptedTerms = it
                        step4Errors = step4Errors - "terms"
                    },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue),
                    modifier = Modifier.offset(x = (-8).dp, y = (-8).dp)
                )
                Text(
                    text = buildAnnotatedString {
                        append("I agree to the HostelCare ")
                        withStyle(SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.SemiBold)) {
                            append("Terms of Service")
                        }
                        append(" and ")
                        withStyle(SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.SemiBold)) {
                            append("Hostel Resident Code of Conduct")
                        }
                        append(".")
                    },
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 18.sp,
                    modifier = Modifier.offset(x = (-8).dp)
                )
            }

            if (step4Errors.containsKey("terms")) {
                Text(
                    step4Errors["terms"]!!,
                    fontSize = 12.sp,
                    color = DangerRed,
                    modifier = Modifier.padding(start = 24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState is AuthState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    (uiState as AuthState.Error).message,
                    color = DangerRed,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Buttons (Back + Create Account)
            val isSubmitting = uiState is AuthState.Loading
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { currentStep = 3 },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, PrimaryBlue),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                    enabled = !isSubmitting
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val valid1 = validateStep1()
                        val valid2 = validateStep2()
                        val valid3 = validateStep3()
                        val valid4 = validateStep4()

                        if (!valid1) {
                            currentStep = 1
                        } else if (!valid2) {
                            currentStep = 2
                        } else if (!valid3) {
                            currentStep = 3
                        } else if (valid4) {
                            val fullPhone = if (phone.startsWith("+")) phone else "$selectedCountryCode ${phone.trim()}"
                            val newUser = User(
                                name = name.trim(),
                                email = email.trim().lowercase(),
                                studentId = studentId.trim(),
                                phone = fullPhone,
                                collegeName = collegeName.trim(),
                                course = course.trim(),
                                currentYear = admissionYear.trim(),
                                semester = "",
                                hostelName = hostelName.trim(),
                                hostelBlock = hostelBlock.trim(),
                                roomNumber = room.trim()
                            )
                            viewModel.register(newUser, password, confirmPassword, acceptedTerms)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = acceptedTerms && allRulesMet && password == confirmPassword && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create Account", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Already have an account? ", color = TextMuted, fontSize = 14.sp)
                Text(
                    "Log in",
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { navController.popBackStack() }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
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
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(24.dp), 
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = DangerRed, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Staff Login", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
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
            Text("Student Login Instead", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
