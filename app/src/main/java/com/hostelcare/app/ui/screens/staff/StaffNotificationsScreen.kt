package com.hostelcare.app.ui.screens.staff

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.data.remote.InAppNotificationItem
import com.hostelcare.app.ui.components.StaffBottomNavigation
import com.hostelcare.app.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffNotificationsScreen(
    navController: NavController,
    app: HostelCareApp
) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: StaffViewModel = viewModel(factory = factory)

    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf("all") } // "all" or "unread"

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F172A)
                            )
                        }
                        Text(
                            text = "Notifications",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    IconButton(onClick = { viewModel.markAllNotificationsRead() }) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Mark all read",
                            tint = Color(0xFF475569)
                        )
                    }
                }
            }
        },
        bottomBar = {
            StaffBottomNavigation(
                navController = navController,
                currentRoute = Routes.STAFF_NOTIFICATIONS,
                unreadNotificationCount = unreadCount
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val (allCount, unreadTabCount) = when (val state = notificationsState) {
                is StaffNotificationsUiState.Success -> Pair(state.notifications.size, state.unreadCount)
                else -> Pair(0, 0)
            }

            // Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isAllSelected = selectedTab == "all"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAllSelected) Color(0xFF2563EB) else Color.White,
                    border = BorderStroke(1.dp, if (isAllSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = "all" }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All ($allCount)",
                            color = if (isAllSelected) Color.White else Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                val isUnreadSelected = selectedTab == "unread"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isUnreadSelected) Color(0xFF2563EB) else Color.White,
                    border = BorderStroke(1.dp, if (isUnreadSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = "unread" }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Unread ($unreadTabCount)",
                            color = if (isUnreadSelected) Color.White else Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontWeight = if (isUnreadSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Notification List
            when (val state = notificationsState) {
                is StaffNotificationsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF2563EB))
                    }
                }
                is StaffNotificationsUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No notifications found.",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    }
                }
                is StaffNotificationsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = Color(0xFFDC2626), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadNotifications() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is StaffNotificationsUiState.Success -> {
                    val filtered = if (selectedTab == "unread") {
                        state.notifications.filter { !it.isRead }
                    } else {
                        state.notifications
                    }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedTab == "unread") "No unread notifications." else "No notifications.",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { item ->
                                StaffNotificationCard(
                                    item = item,
                                    onClick = {
                                        if (!item.isRead) {
                                            viewModel.markNotificationRead(item.id)
                                        }
                                        if (!item.complaintId.isNullOrBlank()) {
                                            navController.navigate(Routes.staffTaskDetails(item.complaintId))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffNotificationCard(
    item: InAppNotificationItem,
    onClick: () -> Unit
) {
    val (icon, iconTint, iconBg) = getNotificationVisuals(item.type, item.title)
    val timeAgo = formatTimeAgo(item.createdAt)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.message,
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )

                if (timeAgo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = timeAgo,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

fun getNotificationVisuals(type: String, title: String): Triple<ImageVector, Color, Color> {
    val t = type.lowercase()
    val tit = title.lowercase()
    return when {
        t == "complaint_assigned" || tit.contains("assigned") ->
            Triple(Icons.Default.Build, Color(0xFF2563EB), Color(0xFFEFF6FF))
        t == "status_updated" || tit.contains("status") ->
            Triple(Icons.Default.CheckCircle, Color(0xFF16A34A), Color(0xFFDCFCE7))
        t == "work_submitted" || t == "completion_requested" || tit.contains("submitted") ->
            Triple(Icons.Default.Description, Color(0xFFD97706), Color(0xFFFEF3C7))
        else ->
            Triple(Icons.Default.Notifications, Color(0xFF64748B), Color(0xFFF1F5F9))
    }
}
