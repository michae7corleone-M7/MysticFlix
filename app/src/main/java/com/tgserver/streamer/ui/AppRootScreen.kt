package com.tgserver.streamer.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tgserver.streamer.R
import com.tgserver.streamer.theme.*
import com.tgserver.streamer.ui.screens.ApiCredentialsScreen
import com.tgserver.streamer.ui.screens.CatalogsScreen
import com.tgserver.streamer.ui.screens.ChannelsScreen
import com.tgserver.streamer.ui.screens.HomeScreen
import com.tgserver.streamer.ui.screens.LibraryScreen
import com.tgserver.streamer.ui.screens.SettingsScreen
import com.tgserver.streamer.ui.screens.TelegramLoginScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

private val NdotFont = FontFamily(
    Font(R.font.inter_regular, androidx.compose.ui.text.font.FontWeight.Normal),
    Font(R.font.inter_medium, androidx.compose.ui.text.font.FontWeight.Medium),
    Font(R.font.inter_semibold, androidx.compose.ui.text.font.FontWeight.SemiBold),
    Font(R.font.inter_bold, androidx.compose.ui.text.font.FontWeight.Bold)
)

private data class NavItemData(
    val tab: AppTab,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
)

private val NAV_TABS = listOf(
    NavItemData(AppTab.HOME, Icons.Filled.Home, Icons.Outlined.Home, "Home"),
    NavItemData(AppTab.LIBRARY, Icons.Filled.SmartDisplay, Icons.Outlined.SmartDisplay, "Library"),
    NavItemData(AppTab.CHANNELS, Icons.Filled.Sensors, Icons.Outlined.Sensors, "Channels"),
    NavItemData(AppTab.CATALOGS, Icons.Filled.Layers, Icons.Outlined.Layers, "Catalogs"),
    NavItemData(AppTab.SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings, "Settings")
)

@Composable
fun AppRootScreen(viewModel: MainAppViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val apiCreds by viewModel.apiCredentials.collectAsState()
    val session by viewModel.session.collectAsState()
    val syncProgress by viewModel.cloudSyncProgress.collectAsState()
    val haptic = LocalHapticFeedback.current

    val hasCredentials = apiCreds.first > 0 && apiCreds.second.isNotBlank()

    if (!hasCredentials) {
        ApiCredentialsScreen(viewModel = viewModel)
    } else if (!session.isLoggedIn) {
        TelegramLoginScreen(viewModel = viewModel)
    } else {
        var dragAccumulator by remember { mutableFloatStateOf(0f) }

        // Root Edge-to-Edge Container (No Scaffold bottomBar = No black box strip!)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundOled)
        ) {
            // Full-screen content layer (scrolls seamlessly behind the floating dock)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Horizontal swipe between tabs
                    .pointerInput(selectedTab) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccumulator = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulator += dragAmount
                            },
                            onDragEnd = {
                                val currentIdx = NAV_TABS.indexOfFirst { it.tab == selectedTab }
                                if (dragAccumulator < -90f && currentIdx < NAV_TABS.size - 1) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.selectTab(NAV_TABS[currentIdx + 1].tab)
                                } else if (dragAccumulator > 90f && currentIdx > 0) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.selectTab(NAV_TABS[currentIdx - 1].tab)
                                }
                                dragAccumulator = 0f
                            }
                        )
                    }
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        // Emil Kowalski Principle: Navigation tabs are high-frequency actions (50+ times/day).
                        // Full horizontal sliding causes visual exhaustion.
                        // Subtle scale (0.982f -> 1.0f) + snappy 160ms crossfade feels instant, calm and responsive.
                        (fadeIn(animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)) +
                         scaleIn(initialScale = 0.982f, animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)) +
                                scaleOut(targetScale = 0.982f, animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing))
                            )
                    },
                    label = "tab_content_switch"
                ) { tab ->
                    when (tab) {
                        AppTab.HOME -> HomeScreen(viewModel = viewModel)
                        AppTab.LIBRARY -> LibraryScreen(viewModel = viewModel)
                        AppTab.CHANNELS -> ChannelsScreen(viewModel = viewModel)
                        AppTab.CATALOGS -> CatalogsScreen(viewModel = viewModel)
                        AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }

            // Smooth ambient bottom scrim to gracefully dissolve scrolling cards before reaching the dock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x3B000000),
                                Color(0x94000000),
                                Color(0xE8000000)
                            )
                        )
                    )
            )

            // Minimalist Floating Glassmorphic Dock (True Floating Island, Zero Side Box)
            MinimalFloatingNavDock(
                currentTab = selectedTab,
                avatarUrl = session.avatarUrl,
                userInitials = session.initials,
                onTabSelected = { viewModel.selectTab(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )

            // Enterprise Floating True Cloud Sync Notification Pill (Non-blocking, top-aligned)
            AnimatedVisibility(
                visible = syncProgress != null,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(180, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(150)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 10.dp, start = 16.dp, end = 16.dp)
            ) {
                syncProgress?.let { progress ->
                    FloatingCloudSyncPill(
                        progress = progress,
                        onDismiss = { viewModel.dismissCloudSyncProgress() }
                    )
                }
            }
        }
    }
}

/**
 * Sleek, modern floating pill indicating background True Cloud Sync progress.
 * Non-blocking, glassmorphic dark surface with status-matched glowing borders.
 */
@Composable
private fun FloatingCloudSyncPill(
    progress: CloudSyncProgress,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val borderColor = when (progress.isSuccess) {
        true -> Color(0xFF2ECC71).copy(alpha = 0.55f)
        false -> Color(0xFFE74C3C).copy(alpha = 0.55f)
        null -> Color(0xFFFB515B).copy(alpha = 0.55f)
    }

    Box(
        modifier = Modifier
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color.Black)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xF0171212))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(24.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDismiss()
            }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (progress.isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    color = Color(0xFFFB515B),
                    strokeWidth = 2.dp
                )
            } else if (progress.isSuccess == true) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF2ECC71),
                    modifier = Modifier.size(15.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE74C3C),
                    modifier = Modifier.size(15.dp)
                )
            }

            Text(
                text = progress.message,
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = NdotFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = Color(0xFF717482),
                modifier = Modifier
                    .size(13.dp)
                    .clickable { onDismiss() }
            )
        }
    }
}

/**
 * MysticFlix bottom bar: five labelled tabs, thin red top line, red glow on the active tab.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MinimalFloatingNavDock(
    currentTab: AppTab,
    avatarUrl: String?,
    userInitials: String,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xF2060203))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF3C1016))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NAV_TABS.forEach { item ->
                NavBarItem(
                    item = item,
                    selected = currentTab == item.tab,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTabSelected(item.tab)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavBarItem(
    item: NavItemData,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeRed = Color(0xFFFF4552)
    val tint by animateColorAsState(
        targetValue = if (selected) activeRed else Color(0xFFA1A1AA),
        animationSpec = tween(160),
        label = "nav_tint"
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(30.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(activeRed)
            )
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        Brush.radialGradient(listOf(Color(0x55E11D2E), Color.Transparent)),
                        CircleShape
                    )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = item.label,
                color = tint,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
