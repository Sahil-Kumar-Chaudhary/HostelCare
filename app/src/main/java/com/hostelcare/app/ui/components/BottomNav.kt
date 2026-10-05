package com.hostelcare.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.hostelcare.app.ui.navigation.Routes

@Composable
fun StudentBottomNavigation(navController: NavController, currentRoute: String) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == Routes.STUDENT_HOME,
            onClick = {
                if (currentRoute != Routes.STUDENT_HOME) {
                    navController.navigate(Routes.STUDENT_HOME) {
                        popUpTo(Routes.STUDENT_HOME) { inclusive = true }
                    }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Complaints") },
            label = { Text("Complaints") },
            selected = currentRoute == Routes.MY_COMPLAINTS,
            onClick = {
                if (currentRoute != Routes.MY_COMPLAINTS) {
                    navController.navigate(Routes.MY_COMPLAINTS)
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Notifications, contentDescription = "Notifications") },
            label = { Text("Updates") },
            selected = currentRoute == Routes.NOTIFICATIONS,
            onClick = {
                if (currentRoute != Routes.NOTIFICATIONS) {
                    navController.navigate(Routes.NOTIFICATIONS)
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == Routes.PROFILE,
            onClick = {
                if (currentRoute != Routes.PROFILE) {
                    navController.navigate(Routes.PROFILE)
                }
            }
        )
    }
}
