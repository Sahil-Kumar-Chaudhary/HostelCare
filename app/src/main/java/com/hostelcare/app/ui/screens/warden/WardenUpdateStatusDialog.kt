package com.hostelcare.app.ui.screens.warden

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelcare.app.ui.screens.auth.CustomTextField
import com.hostelcare.app.ui.theme.*

@Composable
fun WardenUpdateStatusDialog(
    currentStatus: String,
    onDismiss: () -> Unit,
    onConfirm: (newStatus: String, note: String, resolutionNote: String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus.lowercase()) }
    var updateNote by remember { mutableStateOf("") }
    var resolutionNote by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val statusOptions = listOf(
        "submitted" to ("Submitted" to "Initial complaint logged"),
        "under_review" to ("Under Review" to "Investigating or reviewing work"),
        "assigned" to ("Assigned" to "Staff assigned for repair"),
        "in_progress" to ("In Progress" to "Repair actively underway"),
        "resolved" to ("Resolved" to "Issue verified and fixed"),
        "rejected" to ("Rejected" to "Not valid or duplicate")
    )

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = "Update Complaint Status",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select New Status:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Status Choices
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    statusOptions.forEach { (statusCode, pair) ->
                        val (label, desc) = pair
                        val isSelected = selectedStatus == statusCode
                        val statusColor = when (statusCode) {
                            "resolved" -> SuccessGreen
                            "in_progress", "assigned" -> PrimaryBlue
                            "under_review" -> Color(0xFFD97706)
                            "rejected" -> DangerRed
                            else -> PendingAmber
                        }

                        Surface(
                            onClick = { selectedStatus = statusCode },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) statusColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) statusColor else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) statusColor else Color.Transparent)
                                        .border(1.5.dp, statusColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (selectedStatus == "resolved") {
                    Text(
                        text = "Resolution Summary Note:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CustomTextField(
                        value = resolutionNote,
                        onValueChange = { resolutionNote = it },
                        placeholder = "Explain how the issue was inspected and resolved..."
                    )
                }

                Text(
                    text = "Status Timeline Note (Optional):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CustomTextField(
                    value = updateNote,
                    onValueChange = { updateNote = it },
                    placeholder = "Add remarks for student and staff..."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    onConfirm(
                        selectedStatus,
                        updateNote.trim(),
                        if (selectedStatus == "resolved") resolutionNote.trim() else null
                    )
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Apply Status")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancel")
            }
        }
    )
}
