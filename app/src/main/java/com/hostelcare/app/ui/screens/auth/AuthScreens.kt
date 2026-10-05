package com.hostelcare.app.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.data.model.Role
import com.hostelcare.app.data.model.User
import com.hostelcare.app.ui.navigation.Routes
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle(null)
    
    LaunchedEffect(currentUser) {
        delay(1000)
        if (currentUser != null) {
            if (currentUser!!.role == Role.ADMIN) {
                navController.navigate(Routes.ADMIN_HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
            } else {
                navController.navigate(Routes.STUDENT_HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
            }
        } else {
            navController.navigate(Routes.LOGIN) { popUpTo(Routes.SPLASH) { inclusive = true } }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "HostelCare", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Making hostel life easier.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
fun LoginScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var email by remember { mutableStateOf("alex.j@campus.edu") }
    var password by remember { mutableStateOf("password") }

    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            viewModel.resetState()
            navController.navigate(Routes.STUDENT_HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome back", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email or Student ID") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (uiState is AuthState.Error) {
            Text((uiState as AuthState.Error).message, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.login(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState !is AuthState.Loading
        ) {
            if (uiState is AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Log in")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { navController.navigate(Routes.SIGN_UP) }) {
            Text("Don't have an account? Create account")
        }
        TextButton(onClick = { navController.navigate(Routes.ADMIN_LOGIN) }) {
            Text("Admin Login")
        }
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

    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            viewModel.resetState()
            navController.navigate(Routes.STUDENT_HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = studentId, onValueChange = { studentId = it }, label = { Text("Student ID") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = hostelBlock, onValueChange = { hostelBlock = it }, label = { Text("Hostel / Block") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Room Number") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if(name.isNotBlank() && email.isNotBlank()) {
                    viewModel.register(User(name = name, email = email, studentId = studentId, hostelBlock = hostelBlock, roomNumber = room), password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Account")
        }
    }
}

@Composable
fun AdminLoginScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var email by remember { mutableStateOf("admin@campus.edu") }
    var password by remember { mutableStateOf("admin") }

    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            viewModel.resetState()
            navController.navigate(Routes.ADMIN_HOME) { popUpTo(Routes.ADMIN_LOGIN) { inclusive = true } }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Staff & Admin Portal", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Staff ID or Official Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.login(email, password, true) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign In to Dashboard")
        }
    }
}
