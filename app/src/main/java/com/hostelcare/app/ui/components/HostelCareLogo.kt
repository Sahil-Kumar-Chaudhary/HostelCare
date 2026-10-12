package com.hostelcare.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hostelcare.app.R

/**
 * Reusable Compose component for the official HostelCare logo.
 * Displays the house-and-cross brand asset cleanly with configurable sizing.
 */
@Composable
fun HostelCareLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    contentDescription: String? = "HostelCare Logo"
) {
    Image(
        painter = painterResource(id = R.drawable.hostelcare_logo),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}
