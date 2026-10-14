package com.hostelcare.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hostelcare.app.ui.navigation.Routes

@Composable
fun StudentBottomNavigation(
    navController: NavController,
    currentRoute: String,
    unreadNotificationCount: Int = 0
) {
    val items = listOf(
        Triple(Routes.STUDENT_HOME, "Home", Icons.Default.Home),
        Triple(Routes.MY_COMPLAINTS, "Complaints", Icons.AutoMirrored.Filled.List),
        Triple(Routes.NOTIFICATIONS, "Updates", Icons.Default.Notifications),
        Triple(Routes.PROFILE, "Profile", Icons.Default.Person)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 8.dp,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { (route, label, icon) ->
                    val selected = currentRoute == route
                    val badge = if (route == Routes.NOTIFICATIONS) unreadNotificationCount else 0
                    BottomNavItem(
                        icon = icon,
                        label = label,
                        selected = selected,
                        badgeCount = badge,
                        onClick = {
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(Routes.STUDENT_HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminBottomNavigation(navController: NavController, currentRoute: String) {
    val items = listOf(
        Triple(Routes.ADMIN_HOME, "Home", Icons.Default.Home),
        Triple(Routes.ADMIN_COMPLAINTS, "Complaints", Icons.AutoMirrored.Filled.List),
        Triple("admin_profile", "Profile", Icons.Default.Person)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 8.dp,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { (route, label, icon) ->
                    val selected = currentRoute == route
                    BottomNavItem(
                        icon = icon,
                        label = label,
                        selected = selected,
                        onClick = {
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    if (route == Routes.ADMIN_HOME) {
                                        popUpTo(Routes.ADMIN_HOME) { inclusive = true }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WardenBottomNavigation(
    navController: NavController,
    currentRoute: String,
    onTabSelected: ((String) -> Unit)? = null
) {
    val items = listOf(
        Triple(Routes.WARDEN_HOME, "Home", Icons.Default.Home),
        Triple(Routes.WARDEN_COMPLAINTS, "Complaints", Icons.AutoMirrored.Filled.List),
        Triple(Routes.WARDEN_STAFF, "Staff", Icons.Default.People),
        Triple(Routes.WARDEN_REPORTS, "Analytics", Icons.Default.Assessment),
        Triple(Routes.WARDEN_PROFILE, "Profile", Icons.Default.Person)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 8.dp,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { (route, label, icon) ->
                    val selected = currentRoute == route
                    BottomNavItem(
                        icon = icon,
                        label = label,
                        selected = selected,
                        onClick = {
                            if (currentRoute != route) {
                                if (onTabSelected != null) {
                                    onTabSelected(route)
                                } else {
                                    navController.navigate(route) {
                                        popUpTo(Routes.WARDEN_HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StaffBottomNavigation(
    navController: NavController,
    currentRoute: String,
    unreadNotificationCount: Int = 0,
    onProfileClick: (() -> Unit)? = null
) {
    val items = listOf(
        Triple(Routes.STAFF_HOME, "Home", Icons.Default.Home),
        Triple(Routes.STAFF_TASKS, "Tasks", Icons.AutoMirrored.Filled.List),
        Triple(Routes.STAFF_NOTIFICATIONS, "Notifications", Icons.Default.Notifications),
        Triple(Routes.STAFF_PROFILE, "Profile", Icons.Default.Person)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 8.dp,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { (route, label, icon) ->
                    val selected = currentRoute == route
                    val badge = if (route == Routes.STAFF_NOTIFICATIONS) unreadNotificationCount else 0
                    BottomNavItem(
                        icon = icon,
                        label = label,
                        selected = selected,
                        badgeCount = badge,
                        onClick = {
                            if (route == Routes.STAFF_PROFILE && onProfileClick != null) {
                                onProfileClick()
                            } else if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(Routes.STAFF_HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    val capsuleBg = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(capsuleBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ) {
                            Text(
                                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
