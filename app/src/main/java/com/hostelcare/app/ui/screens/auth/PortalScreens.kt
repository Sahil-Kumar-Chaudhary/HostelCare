package com.hostelcare.app.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.hostelcare.app.AppViewModelFactory
import com.hostelcare.app.HostelCareApp
import com.hostelcare.app.data.model.Role
import com.hostelcare.app.ui.navigation.Routes
import com.hostelcare.app.ui.theme.*

/**
 * Portal Selection Screen
 * Serves as the initial entry point when no authenticated session is active.
 * Presents clear pathways to Student, Warden, and Maintenance Staff portals.
 */
@Composable
fun PortalSelectionScreen(navController: NavController, app: HostelCareApp) {
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutAppDialog(onDismiss = { showAboutDialog = false })
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val totalHeight = maxHeight
        val totalWidth = maxWidth
        val isNarrow = totalWidth < 360.dp
        val isShort = totalHeight < 680.dp
        val extraHeight = (totalHeight - 620.dp).coerceAtLeast(0.dp)

        // Dynamic vertical spacing that expands proportionally on tall phones while compressing safely on short phones
        val topToBannerSpacer = (10.dp + extraHeight * 0.03f).coerceIn(8.dp, 16.dp)
        val bannerToBrandingSpacer = (12.dp + extraHeight * 0.04f).coerceIn(10.dp, 20.dp)
        val brandingToWelcomeSpacer = (20.dp + extraHeight * 0.10f).coerceIn(16.dp, 36.dp)
        val welcomeToCardsSpacer = (16.dp + extraHeight * 0.07f).coerceIn(14.dp, 26.dp)
        val cardSpacing = (14.dp + extraHeight * 0.05f).coerceIn(12.dp, 18.dp)
        val cardsToFooterSpacer = (28.dp + extraHeight * 0.20f).coerceIn(22.dp, 60.dp)
        val bottomSpacer = (14.dp + extraHeight * 0.08f).coerceIn(12.dp, 32.dp)

        val bannerHeight = if (isShort) 96.dp else 118.dp
        val bannerWidth = if (isNarrow) 196.dp else 226.dp
        val cardMinHeight = if (isShort) 82.dp else 90.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isNarrow) 14.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .padding(top = if (isShort) 8.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top action bar with "About App"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { showAboutDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "?",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "About App",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(topToBannerSpacer))

                // Campus Building Illustration Banner
                CampusIllustrationBanner(
                    bannerHeight = bannerHeight,
                    bannerWidth = bannerWidth
                )

                Spacer(modifier = Modifier.height(bannerToBrandingSpacer))

                // Branding Title and Tagline
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Hostel",
                        fontSize = if (isNarrow) 24.sp else 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Care",
                        fontSize = if (isNarrow) 24.sp else 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Safe Hostel. Happy Students.",
                    fontSize = if (isNarrow) 13.sp else 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(brandingToWelcomeSpacer))

                // Welcome Headline & Subtitle
                Text(
                    text = "Welcome to HostelCare",
                    fontSize = if (isNarrow) 20.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Choose your portal to continue",
                    fontSize = if (isNarrow) 13.5.sp else 14.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(welcomeToCardsSpacer))

                // Portal Card 1: Student Portal
                PortalCard(
                    title = "Student Portal",
                    description = "Submit complaints and track progress",
                    icon = Icons.Default.School,
                    iconTint = Color(0xFF2563EB),
                    iconContainerColor = if (MaterialTheme.colorScheme.surface == Color.White) Color(0xFFEBF2FE) else Color(0xFF1E3A8A).copy(alpha = 0.35f),
                    onClick = {
                        navController.navigate(Routes.LOGIN)
                    },
                    isNarrow = isNarrow,
                    cardMinHeight = cardMinHeight
                )

                Spacer(modifier = Modifier.height(cardSpacing))

                // Portal Card 2: Warden Portal
                PortalCard(
                    title = "Warden Portal",
                    description = "Manage complaints, assign staff and monitor work",
                    icon = Icons.Default.Security,
                    iconTint = Color(0xFFD97706),
                    iconContainerColor = if (MaterialTheme.colorScheme.surface == Color.White) Color(0xFFFEF3C7) else Color(0xFF78350F).copy(alpha = 0.35f),
                    onClick = {
                        navController.navigate(Routes.WARDEN_LOGIN)
                    },
                    isNarrow = isNarrow,
                    cardMinHeight = cardMinHeight
                )

                Spacer(modifier = Modifier.height(cardSpacing))

                // Portal Card 3: Maintenance Staff Portal
                PortalCard(
                    title = "Maintenance Staff Portal",
                    description = "View assigned tasks and update repair progress",
                    icon = Icons.Default.Build,
                    iconTint = Color(0xFF16A34A),
                    iconContainerColor = if (MaterialTheme.colorScheme.surface == Color.White) Color(0xFFDCFCE7) else Color(0xFF064E3B).copy(alpha = 0.35f),
                    onClick = {
                        navController.navigate(Routes.STAFF_LOGIN)
                    },
                    isNarrow = isNarrow,
                    cardMinHeight = cardMinHeight
                )

                Spacer(modifier = Modifier.height(cardsToFooterSpacer))

                // Footer note
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "One app for a better hostel experience",
                        fontSize = if (isNarrow) 11.5.sp else 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(bottomSpacer))
            }
        }
    }
}

/**
 * Reusable clickable Portal selection card
 */
@Composable
fun PortalCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    iconContainerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNarrow: Boolean = false,
    cardMinHeight: androidx.compose.ui.unit.Dp = 88.dp
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
        shadowElevation = 1.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = cardMinHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isNarrow) 14.dp else 18.dp,
                    vertical = if (isNarrow) 14.dp else 18.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (isNarrow) 48.dp else 56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(if (isNarrow) 24.dp else 28.dp)
                )
            }
            Spacer(modifier = Modifier.width(if (isNarrow) 12.dp else 16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = if (isNarrow) 15.5.sp else 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = if (isNarrow) 12.5.sp else 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = if (isNarrow) 16.sp else 18.5.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Campus illustration banner mimicking the hostel building with soft accents
 */
@Composable
private fun CampusIllustrationBanner(
    bannerHeight: androidx.compose.ui.unit.Dp = 118.dp,
    bannerWidth: androidx.compose.ui.unit.Dp = 226.dp
) {
    Box(
        modifier = Modifier
            .size(width = bannerWidth, height = bannerHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (MaterialTheme.colorScheme.surface == Color.White) Color(0xFFF0F6FE)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                // Left tree accent
                Icon(
                    imageVector = Icons.Default.Park,
                    contentDescription = null,
                    tint = Color(0xFF4ADE80),
                    modifier = Modifier.size(if (bannerHeight < 100.dp) 22.dp else 28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Central Campus Building
                Icon(
                    imageVector = Icons.Default.Apartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (bannerHeight < 100.dp) 48.dp else 62.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Right tree accent
                Icon(
                    imageVector = Icons.Default.Park,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(if (bannerHeight < 100.dp) 22.dp else 28.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = Color(0xFF93C5FD).copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = Color(0xFF93C5FD).copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Shared top navigation bar with Back and Switch Portal buttons
 */
@Composable
fun AuthTopBar(
    onBack: () -> Unit,
    onSwitchPortal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        TextButton(
            onClick = onSwitchPortal,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Switch Portal",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Hero avatar badge for login screens
 */
@Composable
fun RoleHeroBadge(
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    badgeIcon: ImageVector? = null,
    badgeTint: Color? = null,
    modifier: Modifier = Modifier,
    badgeSize: androidx.compose.ui.unit.Dp = 104.dp
) {
    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size((badgeSize.value * 0.56f).dp)
        )
        if (badgeIcon != null && badgeTint != null) {
            val subBadgeSize = (badgeSize.value * 0.28f).dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .size(subBadgeSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = badgeTint,
                    modifier = Modifier.size((subBadgeSize.value * 0.60f).dp)
                )
            }
        }
    }
}

/**
 * Access Notice Card for administrative and staff login screens
 */
@Composable
fun AccessNoticeCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * Dialog explaining about the application
 */
@Composable
fun AboutAppDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "About HostelCare",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "HostelCare is a comprehensive digital living platform designed to modernize hostel maintenance and community living.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "• Student Portal: Lodge room issues, attach photo evidence, track live status, and get Gemini AI repair insights.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• Warden Portal: Monitor complaints, triage urgency, and coordinate facility operations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• Maintenance Staff: Manage work orders, update repair progress, and record resolutions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "HostelCare v1.0 • Safe Hostel. Happy Students.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Dialog for Forgot Password
 */
@Composable
fun ForgotPasswordDialog(portalTitle: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Reset Password", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Text(
                text = "Password reset for $portalTitle accounts is managed by the campus hostel administration. Please contact your hostel warden or administrator with your registered credentials to reset your password.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Understood", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Warden Login Screen
 */
@Composable
fun WardenLoginScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var customErrorMessage by remember { mutableStateOf<String?>(null) }

    // Intercept system back to return to Portal Selection
    BackHandler {
        viewModel.resetState()
        navController.navigate(Routes.PORTAL_SELECTION) {
            popUpTo(Routes.PORTAL_SELECTION) { inclusive = true }
        }
    }

    if (showForgotDialog) {
        ForgotPasswordDialog(portalTitle = "Warden", onDismiss = { showForgotDialog = false })
    }

    // Role-aware security validation
    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            val user = app.repository.getCurrentUser()
            if (user != null && user.role == Role.WARDEN) {
                viewModel.resetState()
                navController.navigate(Routes.WARDEN_HOME) {
                    popUpTo(Routes.PORTAL_SELECTION) { inclusive = true }
                }
            } else {
                viewModel.resetState()
                app.repository.logout()
                customErrorMessage = "This account is not a warden account. Please use the appropriate portal."
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val isNarrow = totalWidth < 360.dp
        val isShort = totalHeight < 680.dp
        val badgeSize = if (isShort) 88.dp else 104.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isNarrow) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .padding(vertical = if (isShort) 8.dp else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top navigation bar
                AuthTopBar(
                    onBack = {
                        viewModel.resetState()
                        navController.navigate(Routes.PORTAL_SELECTION) {
                            popUpTo(Routes.PORTAL_SELECTION) { inclusive = true }
                        }
                    },
                    onSwitchPortal = {
                        viewModel.resetState()
                        navController.navigate(Routes.PORTAL_SELECTION) {
                            popUpTo(Routes.PORTAL_SELECTION) { inclusive = true }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(if (isShort) 10.dp else 16.dp))

                // Warden Hero Avatar
                RoleHeroBadge(
                    icon = Icons.Default.SupervisorAccount,
                    iconTint = Color(0xFFD97706),
                    containerColor = if (MaterialTheme.colorScheme.surface == Color.White) Color(0xFFFEF3C7) else Color(0xFF78350F).copy(alpha = 0.35f),
                    badgeIcon = Icons.Default.Security,
                    badgeTint = Color(0xFFD97706),
                    badgeSize = badgeSize
                )

                Spacer(modifier = Modifier.height(if (isShort) 12.dp else 20.dp))

                Text(
                    text = "Warden Login",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Manage hostel complaints, assign staff and monitor work",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = if (isNarrow) 8.dp else 16.dp)
                )

                Spacer(modifier = Modifier.height(if (isShort) 18.dp else 28.dp))

                // Email Field
                CustomTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        customErrorMessage = null
                    },
                    placeholder = "Email",
                    leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password Field
                CustomTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        customErrorMessage = null
                    },
                    placeholder = "Password",
                    isPassword = true,
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Remember me & Forgot Password Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Remember me",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Forgot Password?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showForgotDialog = true }
                    )
                }

                if (customErrorMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DangerRed.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = customErrorMessage!!,
                            color = DangerRed,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp),
                            lineHeight = 18.sp
                        )
                    }
                } else if (uiState is AuthState.Error) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = (uiState as AuthState.Error).message,
                        color = DangerRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Login Button
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            customErrorMessage = "Please enter your email and password"
                            return@Button
                        }
                        customErrorMessage = null
                        viewModel.login(email.trim(), password, portal = "warden")
                    },
                    enabled = uiState !is AuthState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (uiState is AuthState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Log In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Info notice card
                AccessNoticeCard(
                    title = "Warden account is provided by the administration.",
                    message = "If you don't have an account, please contact the hostel administration."
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Maintenance Staff Login Screen
 */
@Composable
fun StaffLoginScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var customErrorMessage by remember { mutableStateOf<String?>(null) }

    // Intercept system back to return to Portal Selection
    BackHandler {
        viewModel.resetState()
        navController.navigate(Routes.PORTAL_SELECTION) {
            popUpTo(Routes.PORTAL_SELECTION) { inclusive = true }
        }
    }

    if (showForgotDialog) {
        ForgotPasswordDialog(portalTitle = "Maintenance Staff", onDismiss = { showForgotDialog = false })
    }

    // Role-aware security validation
    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            val user = app.repository.getCurrentUser()
            if (user != null && user.role == Role.STAFF) {
                viewModel.resetState()
                navController.navigate(Routes.STAFF_HOME) {
                    popUpTo(Routes.PORTAL_SELECTION) { inclusive = true }
                }
            } else {
                // Mismatched role: a student or warden account was entered
                viewModel.resetState()
                app.repository.logout()
                customErrorMessage = "This account is not a maintenance staff account. Please use the appropriate portal."
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val isNarrow = totalWidth < 360.dp
        val isShort = totalHeight < 680.dp
        val badgeSize = if (isShort) 88.dp else 104.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isNarrow) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .padding(vertical = if (isShort) 8.dp else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top navigation bar
                AuthTopBar(
                    onBack = {
                        viewModel.resetState()
                        navController.navigate(Routes.PORTAL_SELECTION) {
                            popUpTo(Routes.PORTAL_SELECTION) { inclusive = true }
                        }
                    },
                    onSwitchPortal = {
                        viewModel.resetState()
                        navController.navigate(Routes.PORTAL_SELECTION) {
                            popUpTo(Routes.PORTAL_SELECTION) { inclusive = true }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(if (isShort) 10.dp else 16.dp))

                // Staff Hero Avatar
                RoleHeroBadge(
                    icon = Icons.Default.Build,
                    iconTint = Color(0xFF2563EB),
                    containerColor = Color(0xFFE0EDFF),
                    badgeIcon = Icons.Default.Build,
                    badgeTint = Color(0xFF2563EB),
                    badgeSize = badgeSize
                )

                Spacer(modifier = Modifier.height(if (isShort) 12.dp else 20.dp))

                Text(
                    text = "Staff Login",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Access your assigned tasks and update progress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = if (isNarrow) 8.dp else 16.dp)
                )

                Spacer(modifier = Modifier.height(if (isShort) 18.dp else 28.dp))

                // Email Field
                CustomTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        customErrorMessage = null
                    },
                    placeholder = "Email",
                    leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password Field
                CustomTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        customErrorMessage = null
                    },
                    placeholder = "Password",
                    isPassword = true,
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Remember me & Forgot Password Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Remember me",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Forgot Password?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showForgotDialog = true }
                    )
                }

                if (customErrorMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DangerRed.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = customErrorMessage!!,
                            color = DangerRed,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp),
                            lineHeight = 18.sp
                        )
                    }
                } else if (uiState is AuthState.Error) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = (uiState as AuthState.Error).message,
                        color = DangerRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Login Button
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            customErrorMessage = "Please enter your email and password"
                            return@Button
                        }
                        customErrorMessage = null
                        viewModel.login(email.trim(), password, portal = "staff")
                    },
                    enabled = uiState !is AuthState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (uiState is AuthState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Log In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Info notice card
                AccessNoticeCard(
                    title = "Staff account is provided by the warden.",
                    message = "If you don't have an account, please contact your warden."
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Warden Placeholder Screen
 * Safe destination for authenticated Warden sessions prior to full Warden Dashboard rollout in Step 3.
 * Preserves authorization, prevents incorrect dashboard routing, and supports verified logout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardenPlaceholderScreen(navController: NavController, app: HostelCareApp) {
    val factory = AppViewModelFactory(app.repository, app.aiAnalyzer)
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle(null)
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Warden Portal",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                app.repository.logout()
                                navController.navigate(Routes.PORTAL_SELECTION) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Log Out",
                            tint = DangerRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            RoleHeroBadge(
                icon = Icons.Default.Group,
                iconTint = Color(0xFFD97706),
                containerColor = Color(0xFFFEF3C7),
                badgeIcon = Icons.Default.Security,
                badgeTint = Color(0xFFD97706),
                badgeSize = 100.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = currentUser?.name ?: "Campus Warden",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = currentUser?.email ?: "",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!currentUser?.hostelName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "Assigned Hostel: ${currentUser?.hostelName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Informational status card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SuccessGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Warden Session Active",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Role: WARDEN (Verified)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Welcome to HostelCare Warden Portal. Your account provisioning and Warden credentials have been securely verified. The full Warden management dashboard (complaint triage, staff assignment, and escalation oversight) will be activated in Step 3.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        app.repository.logout()
                        navController.navigate(Routes.PORTAL_SELECTION) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

