package com.tgserver.streamer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tgserver.streamer.R
import com.tgserver.streamer.data.model.MediaGroup
import com.tgserver.streamer.data.model.toMediaGroups
import com.tgserver.streamer.theme.*
import com.tgserver.streamer.ui.AppTab
import com.tgserver.streamer.ui.MainAppViewModel
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.tgserver.streamer.ui.components.AppUpdateBanner
import com.tgserver.streamer.ui.components.AppUpdateDialog
import com.tgserver.streamer.update.AppReleaseInfo
import com.tgserver.streamer.update.UpdateUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

// Pixel Dot-Matrix Font (Exact match with Nothing / Canva design)
private val NdotFont = FontFamily(
    Font(R.font.inter_regular, androidx.compose.ui.text.font.FontWeight.Normal),
    Font(R.font.inter_medium, androidx.compose.ui.text.font.FontWeight.Medium),
    Font(R.font.inter_semibold, androidx.compose.ui.text.font.FontWeight.SemiBold),
    Font(R.font.inter_bold, androidx.compose.ui.text.font.FontWeight.Bold)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: MainAppViewModel) {
    val telemetry by viewModel.telemetry.collectAsState()
    val config by viewModel.config.collectAsState()
    val media by viewModel.media.collectAsState()
    val mediaGroups by viewModel.mediaGroups.collectAsState()
    val catalogs by viewModel.catalogs.collectAsState()
    val session by viewModel.session.collectAsState()
    val haptic = LocalHapticFeedback.current

    var copiedRecently by remember { mutableStateOf(false) }
    var isAwakening by remember { mutableStateOf(false) }
    var awakeningDots by remember { mutableStateOf(".") }

    // Reset copied flag after 2.5s
    LaunchedEffect(copiedRecently) {
        if (copiedRecently) {
            delay(2500)
            copiedRecently = false
        }
    }

    // Refresh user profile & avatar when logged in
    LaunchedEffect(session.isLoggedIn) {
        if (session.isLoggedIn) {
            viewModel.refreshUserProfile()
        }
    }

    // Awakening dot animation loop
    LaunchedEffect(isAwakening) {
        if (isAwakening) {
            var dotCount = 1
            while (isAwakening) {
                delay(350)
                dotCount = (dotCount % 3) + 1
                awakeningDots = ".".repeat(dotCount)
            }
        }
    }

    // Clear awakening state once server confirms running
    LaunchedEffect(telemetry.isRunning) {
        if (telemetry.isRunning) {
            isAwakening = false
        }
    }

    // In-App Updater state
    val updateState by viewModel.updateState.collectAsState()
    val isUpdateDismissed by viewModel.isUpdateDismissed.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Library counts (Single-pass iteration for 60fps instant startup on 32-bit devices)
    var mCount = 0
    var epCount = 0
    var fCount = 0
    var totalBytes = 0L
    val uniqueSeries = mutableSetOf<String>()
    for (item in media) {
        val isMovie = item.type.equals("movie", ignoreCase = true)
        val isSeries = item.type.equals("series", ignoreCase = true)
        if (isMovie) mCount++
        if (isSeries) {
            epCount++
            uniqueSeries.add(item.title.trim().lowercase())
        }
        fCount += if (item.isSplit && item.parts.isNotEmpty()) item.parts.size else 1
        totalBytes += item.sizeBytes
    }
    val moviesCount = mCount
    val showsCount = uniqueSeries.size
    val episodesCount = epCount
    val contentFilesCount = fCount
    val totalServerSizeBytes = totalBytes

    val totalServerSizeFormatted = remember(totalServerSizeBytes) {
        if (totalServerSizeBytes <= 0L) "0.0 GB"
        else {
            val tb = totalServerSizeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0 * 1024.0)
            if (tb >= 1.0) {
                String.format(Locale.US, "%.1fTB", tb)
            } else {
                val gb = totalServerSizeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                String.format(Locale.US, "%.1fGB", gb)
            }
        }
    }

    // Recently Added media groups (Precomputed in background StateFlow on Dispatchers.Default)
    val recentlyAdded by viewModel.recentlyAddedGroups.collectAsState()

    // Stream URL
    val serverUrl = remember(telemetry) {
        if (telemetry.localIp.isNotBlank() && telemetry.port > 0) {
            "http://${telemetry.localIp}:${telemetry.port}"
        } else if (telemetry.manifestUrl.isNotBlank()) {
            telemetry.manifestUrl.removeSuffix("/manifest.json")
        } else {
            "http://127.0.0.1:7788"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOled)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(top = 10.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // TOP BRANDING HEADER (MysticFlix logo + welcome)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.mysticflix_logo),
                contentDescription = "MysticFlix Logo",
                modifier = Modifier.fillMaxWidth(0.86f),
                contentScale = ContentScale.FillWidth
            )
            val displayName = if (session.firstName.isNotBlank()) session.firstName else "Name"
            Row {
                Text(
                    text = "Welcome back, ",
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp
                )
                Text(
                    text = displayName,
                    color = Color(0xFFFF4552),
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp
                )
            }
            Text(
                text = "Your personal streaming hub",
                color = Color(0xFFD4D4D8),
                fontSize = 14.sp
            )
        }

        // IN-APP UPDATE PROMPT BANNER
        val activeUpdate = when (val s = updateState) {
            is UpdateUiState.Available -> s.info
            is UpdateUiState.Downloading -> AppReleaseInfo("NEW", "Update", "", "", "", s.fileName, s.totalBytes, "Compatible", true)
            is UpdateUiState.ReadyToInstall -> s.info
            else -> null
        }

        if (activeUpdate != null && !isUpdateDismissed) {
            AppUpdateBanner(
                info = activeUpdate,
                state = updateState,
                onActionClick = {
                    if (updateState is UpdateUiState.ReadyToInstall) {
                        viewModel.installDownloadedApk((updateState as UpdateUiState.ReadyToInstall).apkFile)
                    } else if (updateState is UpdateUiState.Available) {
                        viewModel.startDownloadUpdate((updateState as UpdateUiState.Available).info)
                    } else {
                        showUpdateDialog = true
                    }
                },
                onDismiss = { viewModel.dismissUpdateBanner() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }

        // HERO CARD: SERVER STATE (Sleep / Awakening / Awaked)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(
                    BorderStroke(1.dp, Color(0xFF7A141C)),
                    shape = RoundedCornerShape(28.dp)
                )
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (!telemetry.isRunning && !isAwakening) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isAwakening = true
                            viewModel.toggleServer()
                        }
                    },
                    onLongClick = {
                        if (telemetry.isRunning) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleServer()
                        }
                    }
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF12080A))
        ) {
            AnimatedContent(
                targetState = telemetry.isRunning,
                transitionSpec = {
                    (fadeIn(tween(300)) + expandVertically(tween(350))) togetherWith
                            (fadeOut(tween(250)) + shrinkVertically(tween(300)))
                },
                label = "server_state_animation"
            ) { isRunning ->
                if (!isRunning) {
                    // SLEEP / STANDBY MODE VIEW (Pure, sleek, centered)
                    val isStandby = telemetry.isStandby || config.alexaVoiceControlEnabled || com.tgserver.streamer.service.StreamerForegroundService.isStandbyMode
                    val idleStatusText = if (isStandby) "In Standby Mode" else "In Sleep mode"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(156.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAwakening) "Server Awakening$awakeningDots" else idleStatusText,
                            color = if (isAwakening || isStandby) Color(0xFFE2E8F0) else Color(0xFFA0A2AC),
                            fontFamily = NdotFont,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    // SERVER AWAKED VIEW (Stats + Integrated Lower Shelf)
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Upper Section: Title + 3 Stats
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 18.dp)
                        ) {
                            // Top Row: "Server Awaked" + subtle sleep button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Server Awaked",
                                    color = Color(0xFFD4D4D8),
                                    fontFamily = NdotFont,
                                    fontSize = 13.5.sp
                                )

                                Text(
                                    text = "sleep",
                                    color = Color(0xFF6B6E7B),
                                    fontFamily = NdotFont,
                                    fontSize = 10.5.sp,
                                    modifier = Modifier
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.toggleServer()
                                        }
                                        .padding(4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // 2 Large Prominent Metrics: Uptime & Speed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ServerLargeMetricItem(
                                    value = telemetry.uptimeFormatted,
                                    label = "uptime",
                                    modifier = Modifier.weight(1f)
                                )
                                ServerLargeMetricItem(
                                    value = telemetry.liveSpeedFormatted,
                                    label = "speed",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Secondary Traffic Details Shelf: Session & All-Time
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp)
                                    .background(Color(0xFF0A0405), shape = RoundedCornerShape(10.dp))
                                    .border(BorderStroke(0.8.dp, Color(0xFF3C1016)), shape = RoundedCornerShape(10.dp))
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "session",
                                        color = Color(0xFF71747F),
                                        fontFamily = NdotFont,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = telemetry.sessionDataFormatted,
                                        color = Color(0xFFD4D4D8),
                                        fontFamily = NdotFont,
                                        fontSize = 11.5.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .background(Color(0xFF3F414D), shape = CircleShape)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "all-time",
                                        color = Color(0xFF71747F),
                                        fontFamily = NdotFont,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = telemetry.allTimeDataFormatted,
                                        color = Color(0xFFD4D4D8),
                                        fontFamily = NdotFont,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }

                        // Lower Shelf: Stream URL
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A0405))
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF3C1016),
                                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.copyToClipboard(serverUrl, "URL copied")
                                    copiedRecently = true
                                }
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "URL: $serverUrl",
                                    color = Color(0xFFA6A8B2),
                                    fontFamily = NdotFont,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (copiedRecently) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "copied",
                                        color = Color(0xFF34D399),
                                        fontFamily = NdotFont,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // STATS CARD (Movies | Storage | Episodes <-> Shows, swipe the last column)
        var showEpisodesStat by remember { mutableStateOf(true) }
        val statDragState = rememberDraggableState { delta ->
            if (delta < -6f) showEpisodesStat = false else if (delta > 6f) showEpisodesStat = true
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1E080B), Color(0xFF090304))))
                .border(1.dp, Color(0xFF7A141C), RoundedCornerShape(28.dp))
                .padding(vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatColumn(
                    icon = Icons.Outlined.Movie,
                    value = "$moviesCount",
                    label = "Movies",
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectTab(AppTab.LIBRARY) }
                )
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A141C)))
                StatColumn(
                    icon = Icons.Outlined.Storage,
                    value = totalServerSizeFormatted,
                    label = "Storage",
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectTab(AppTab.LIBRARY) }
                )
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A141C)))
                StatColumn(
                    icon = Icons.Outlined.Tv,
                    value = if (showEpisodesStat) "$episodesCount" else "$showsCount",
                    label = if (showEpisodesStat) "Episodes" else "Shows",
                    dots = 2,
                    activeDot = if (showEpisodesStat) 0 else 1,
                    modifier = Modifier
                        .weight(1f)
                        .draggable(statDragState, Orientation.Horizontal)
                        .clickable { showEpisodesStat = !showEpisodesStat }
                )
            }
        }

        // RECENTLY ADDED SECTION (Edge-to-Edge Full Width Horizontal Row of Newest Media)
        if (recentlyAdded.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFE11D2E),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Recently Added",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }

                    Text(
                        text = "view all →",
                        color = Color(0xFFFB515B),
                        fontFamily = NdotFont,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.selectTab(AppTab.LIBRARY)
                            }
                            .padding(4.dp)
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    items(recentlyAdded, key = { it.key }) { group ->
                        RecentMediaCard(
                            group = group,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.openMediaDetails(group.key)
                            }
                        )
                    }
                }
            }
        }

        // PINNED CATALOGS SHELVES
        val pinnedCatalogs = remember(catalogs) {
            catalogs.filterNotNull().filter { it.pinToHome && !it.isHidden }
        }

        pinnedCatalogs.forEach { catalog ->
            val matchedGroups = remember(catalog, mediaGroups) {
                try {
                    viewModel.filterMediaForCatalog(catalog).take(15)
                } catch (e: Exception) {
                    emptyList()
                }
            }

            if (matchedGroups.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = catalog.name,
                                color = Color.White,
                                fontFamily = NdotFont,
                                fontSize = 14.sp,
                                letterSpacing = 0.8.sp
                            )
                            if (catalog.isAiPowered) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Powered",
                                    tint = Color(0xFFFB515B),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Text(
                            text = "view all →",
                            color = Color(0xFFFB515B),
                            fontFamily = NdotFont,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.selectTab(AppTab.CATALOGS)
                                }
                                .padding(4.dp)
                        )
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(matchedGroups, key = { it.key }) { group ->
                            RecentMediaCard(
                                group = group,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.openMediaDetails(group.key)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bottom clearance for floating liquid navbar
        Spacer(modifier = Modifier.height(72.dp))
    }

    // App Update Dialog with Changelog & Download progress
    val dialogInfo = when (val s = updateState) {
        is UpdateUiState.Available -> s.info
        is UpdateUiState.ReadyToInstall -> s.info
        else -> null
    }

    if (showUpdateDialog && dialogInfo != null) {
        AppUpdateDialog(
            info = dialogInfo,
            state = updateState,
            currentVersion = viewModel.getAppVersion(),
            onDownloadClick = { viewModel.startDownloadUpdate(dialogInfo) },
            onInstallClick = { file -> viewModel.installDownloadedApk(file) },
            onDismiss = { showUpdateDialog = false }
        )
    }
}

@Composable
private fun StatColumn(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    dots: Int = 0,
    activeDot: Int = 0
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFFF4552),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(text = label, color = Color(0xFFA1A1AA), fontSize = 12.sp)
        if (dots > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(dots) { i ->
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (i == activeDot) Color.White else Color(0xFF50505A))
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, BorderSilver, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun RecentMediaCard(
    group: MediaGroup,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val posterShape = RoundedCornerShape(3.dp)
    Column(
        modifier = Modifier
            .width(114.dp)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(114.dp)
                .height(171.dp)
                .shadow(
                    elevation = if (pressed) 18.dp else 0.dp,
                    shape = posterShape,
                    ambientColor = Color(0xFFFF4552),
                    spotColor = Color(0xFFFF4552)
                )
                .clip(posterShape)
                .border(
                    BorderStroke(if (pressed) 2.dp else 1.dp, if (pressed) Color(0xFFFF4552) else Color(0xFF7A141C)),
                    posterShape
                )
        ) {
            RemoteImage(
                url = group.posterUrl,
                contentDescription = group.title,
                isSeries = group.type == "series" || group.type == "anime",
                modifier = Modifier.fillMaxSize()
            )

            // Episode count badge if series/anime
            if (group.type == "series" || group.type == "anime") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFBE1222).copy(alpha = 0.92f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${group.distinctEpisodeCount} EPS",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp
                    )
                }
            }
        }

        // Title
        Text(
            text = group.title,
            color = Color.White,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Year · Size
        val yearText = group.year?.takeIf { it.isNotBlank() } ?: "2026"
        Text(
            text = "$yearText · ${group.formattedTotalSize}",
            color = Color(0xFFFB515B),
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ServerLargeMetricItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            color = Color.White,
            fontFamily = NdotFont,
            fontSize = 24.sp,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color(0xFF8E909A),
            fontFamily = NdotFont,
            fontSize = 10.5.sp,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileAvatar(
    photoPath: String?,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(photoPath) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(photoPath) {
        if (photoPath.isNullOrBlank()) {
            bitmap = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val clean = photoPath.removePrefix("file://")
                val file = File(clean)
                if (file.exists() && file.length() > 0) {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (bmp != null) {
                        withContext(Dispatchers.Main) {
                            bitmap = bmp.asImageBitmap()
                        }
                        return@withContext
                    }
                }
                if (photoPath.startsWith("http://") || photoPath.startsWith("https://")) {
                    val req = okhttp3.Request.Builder()
                        .url(photoPath)
                        .header("User-Agent", "NoxMedia/1.0 (Android)")
                        .build()
                    val client = okhttp3.OkHttpClient()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            resp.body?.byteStream()?.use { stream ->
                                val bmp = BitmapFactory.decodeStream(stream)
                                if (bmp != null) {
                                    withContext(Dispatchers.Main) {
                                        bitmap = bmp.asImageBitmap()
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    if (bitmap != null) {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .border(1.5.dp, Color(0x66FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "Profile Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        ConcentricRingEmblem(modifier = modifier)
    }
}

@Composable
private fun SiriWaveform(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "siri_wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "siri_phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val maxAmplitude = height * 0.42f

        // 3 harmonic waves with smooth gaussian/sine envelope for seamless end tapering
        val waveConfigs = listOf(
            Triple(1.0f, 0.0f, Color(0xEEFFFFFF) to 1.6f),
            Triple(1.3f, 1.2f, Color(0xB3FF8087) to 1.2f),
            Triple(0.8f, 2.5f, Color(0x80FF8087) to 1.0f)
        )

        for ((freqMult, phaseOffset, style) in waveConfigs) {
            val (color, strokeWidthDp) = style
            val strokeWidthPx = strokeWidthDp.dp.toPx()
            val path = Path()
            val steps = 36
            val dx = width / steps

            for (i in 0..steps) {
                val x = i * dx
                val normX = x / width
                val envelope = kotlin.math.sin(kotlin.math.PI * normX).toFloat()
                val angle = (normX * 2.2f * 2 * kotlin.math.PI * freqMult + phase + phaseOffset).toFloat()
                val y = centerY + kotlin.math.sin(angle) * maxAmplitude * envelope

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
private fun ConcentricRingEmblem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.5.dp, Color(0x40FFFFFF), CircleShape)
            .padding(4.dp)
            .border(1.5.dp, Color(0x99FFFFFF), CircleShape)
            .padding(3.dp)
            .clip(CircleShape)
            .background(Color(0xFF110E0E)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val radius = size.minDimension / 2f
            drawCircle(
                color = Color(0x35FFFFFF),
                radius = radius * 0.75f,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0x70FFFFFF),
                radius = radius * 0.4f,
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFFFFFFF),
                radius = 1.8.dp.toPx()
            )
        }
    }
}
