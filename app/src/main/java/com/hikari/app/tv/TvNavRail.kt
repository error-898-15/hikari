package com.hikari.app.tv

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hikari.app.HikariApp
import com.hikari.app.data.Profiles
import com.hikari.app.i18n.tr
import com.hikari.app.ui.navigation.BottomTab
import com.hikari.app.ui.navigation.BottomTabs
import com.hikari.app.ui.navigation.Routes
import kotlinx.coroutines.launch

/**
 * TV Sidebar & Appearance preferences (persisted on disk and reactive in UI).
 */
object TvSidebarPrefs {
    private const val PREFS_NAME = "hikari_tv_ui_prefs"
    private var initialized = false

    var autoHideSidebar by mutableStateOf(false)
        private set
    var showReloadButton by mutableStateOf(false)
        private set
    var hideTitleLogo by mutableStateOf(false)
        private set
    var expandCardOnFocus by mutableStateOf(false)
        private set

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        autoHideSidebar = sp.getBoolean("auto_hide_sidebar", false)
        showReloadButton = sp.getBoolean("show_reload_button", false)
        hideTitleLogo = sp.getBoolean("hide_title_logo", false)
        expandCardOnFocus = sp.getBoolean("expand_card_on_focus", false)
    }

    fun setAutoHide(context: Context, value: Boolean) {
        autoHideSidebar = value
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("auto_hide_sidebar", value).apply()
    }

    fun setShowReload(context: Context, value: Boolean) {
        showReloadButton = value
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("show_reload_button", value).apply()
    }

    fun setHideLogo(context: Context, value: Boolean) {
        hideTitleLogo = value
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("hide_title_logo", value).apply()
    }

    fun setExpandCard(context: Context, value: Boolean) {
        expandCardOnFocus = value
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("expand_card_on_focus", value).apply()
    }
}

private fun BottomTab.isHubTab(): Boolean {
    val r = route.lowercase()
    val l = label.lowercase()
    return r.contains("manga") || r.contains("telegram") || r.contains("extension") ||
        l.contains("manga") || l.contains("telegram") || l.contains("extension")
}

private fun BottomTab.isSettingsTab(): Boolean {
    val r = route.lowercase()
    val l = label.lowercase()
    return r == Routes.SETTINGS.lowercase() || r.contains("setting") || l.contains("setting")
}

private fun BottomTab.tvDisplayLabel(): String {
    val l = label.lowercase()
    return when {
        l == "search" -> "Browse"
        l == "my stuff" -> "Library"
        l == "anime" -> "Schedule"
        else -> label
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun TvNavRail(
    currentRoute: String?,
    hidden: Set<String>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { TvSidebarPrefs.init(context) }

    val allTabs = remember { BottomTabs }
    // Main sidebar keeps only Home, Browse (Search), Library (My Stuff), Schedule (Anime)
    val mainTabs = remember(allTabs) {
        allTabs.filterNot { it.isHubTab() || it.isSettingsTab() }
    }
    // Manga, Telegram, Extensions move into the Settings Hub
    val hubTabs = remember(allTabs) {
        allTabs.filter { it.isHubTab() }
    }
    val settingsRoute = remember(allTabs) {
        allTabs.firstOrNull { it.isSettingsTab() }?.route ?: Routes.SETTINGS
    }

    val profiles by Profiles.all.collectAsState()
    val activeProfileId by Profiles.activeId.collectAsState()
    val activeProfileName = remember(profiles, activeProfileId) {
        profiles.firstOrNull { it.id == activeProfileId }?.name ?: "Profile 1"
    }

    val requesters = remember(mainTabs) { mainTabs.map { FocusRequester() } }
    var initialFocusPending by remember(mainTabs) { mutableStateOf(true) }
    var railHasFocus by remember { mutableStateOf(false) }
    var showSettingsHub by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(mainTabs, currentRoute) {
        if (!initialFocusPending) return@LaunchedEffect
        val index = mainTabs.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 0
        withFrameNanos { }
        runCatching { listState.scrollToItem(index) }
        runCatching { requesters.getOrNull(index)?.requestFocus() }
        initialFocusPending = false
    }

    val autoHide = TvSidebarPrefs.autoHideSidebar
    // When Auto-Hide is ON and unfocused, keep a tiny 6.dp invisible focus target
    // on the left edge so pressing D-pad Left always catches focus and slides the bar out.
    val targetWidth = when {
        railHasFocus -> 228.dp
        autoHide -> 6.dp
        else -> 60.dp
    }
    val railWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "tv-rail-width",
    )
    val railAlpha by animateFloatAsState(
        targetValue = if (autoHide && !railHasFocus) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "tv-rail-alpha",
    )

    val drawerShape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .alpha(railAlpha)
            .onFocusChanged { state -> railHasFocus = state.hasFocus }
            .padding(
                start = if (autoHide && !railHasFocus) 0.dp else 8.dp,
                top = 12.dp,
                bottom = 12.dp,
                end = if (railHasFocus) 6.dp else 2.dp,
            )
            .background(
                color = if (railHasFocus) Color(0xFF1A1C23).copy(alpha = 0.96f) else Color.Transparent,
                shape = drawerShape,
            )
            .border(
                width = if (railHasFocus) 1.dp else 0.dp,
                color = if (railHasFocus) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                shape = drawerShape,
            )
            .padding(vertical = 14.dp, horizontal = if (railHasFocus) 10.dp else 4.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {
            // Top Navigation Icons (Home, Browse, Library, Schedule)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                itemsIndexed(mainTabs, key = { _, tab -> tab.route }) { index, tab ->
                    TvSlimNavItem(
                        icon = tab.icon,
                        label = tr(tab.tvDisplayLabel()),
                        selected = tab.route == currentRoute,
                        railExpanded = railHasFocus,
                        requester = requesters[index],
                        onClick = { onNavigate(tab.route) },
                        onFocus = {
                            scope.launch {
                                runCatching { listState.animateScrollToItem(index.coerceAtLeast(0)) }
                            }
                        },
                    )
                }
            }

            // Bottom Section: Optional Reload + Divider + Profile / Settings & Acc
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                if (TvSidebarPrefs.showReloadButton) {
                    TvBottomActionRow(
                        icon = Icons.Filled.Refresh,
                        title = tr("Reload"),
                        subtitle = null,
                        railExpanded = railHasFocus,
                        selected = false,
                        isProfile = false,
                        onClick = {
                            val route = currentRoute ?: mainTabs.firstOrNull()?.route ?: Routes.HOME
                            onNavigate(route)
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )

                val inSettingsOrHub = currentRoute == settingsRoute ||
                    hubTabs.any { it.route == currentRoute }

                TvBottomActionRow(
                    icon = Icons.Filled.Person,
                    title = activeProfileName,
                    subtitle = "Settings & Acc",
                    railExpanded = railHasFocus,
                    selected = inSettingsOrHub,
                    isProfile = true,
                    onClick = { showSettingsHub = true },
                )
            }
        }
    }

    if (showSettingsHub) {
        TvSlidingSettingsHubDialog(
            hubTabs = hubTabs,
            settingsRoute = settingsRoute,
            activeProfileName = activeProfileName,
            onNavigate = { route ->
                showSettingsHub = false
                onNavigate(route)
            },
            onDismiss = { showSettingsHub = false },
        )
    }
}

@Composable
private fun TvSlimNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    railExpanded: Boolean,
    requester: FocusRequester,
    onClick: () -> Unit,
    onFocus: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = if (railExpanded) RoundedCornerShape(50) else RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(
                when {
                    focused -> Color(0xFFE8ECF4)
                    selected && !railExpanded -> Color.White.copy(alpha = 0.16f)
                    selected && railExpanded -> Color.White.copy(alpha = 0.12f)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (focused) 1.5.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = shape,
            )
            .onFocusChanged { state ->
                focused = state.isFocused
                if (state.isFocused) onFocus()
            }
            .focusRequester(requester)
            .tvPress(onClick = onClick)
            .clickable(onClick = onClick)
            .padding(horizontal = if (railExpanded) 14.dp else 0.dp),
        contentAlignment = if (railExpanded) Alignment.CenterStart else Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    focused -> Color(0xFF101216)
                    selected -> Color.White
                    else -> Color.White.copy(alpha = 0.72f)
                },
                modifier = Modifier.size(22.dp),
            )
            if (railExpanded) {
                Spacer(Modifier.width(14.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (focused || selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (focused) Color(0xFF101216) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TvBottomActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    railExpanded: Boolean,
    selected: Boolean,
    isProfile: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = if (railExpanded) RoundedCornerShape(50) else CircleShape

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (railExpanded && subtitle != null) 52.dp else 44.dp)
            .clip(shape)
            .background(
                when {
                    focused -> Color(0xFFE8ECF4)
                    selected -> Color.White.copy(alpha = 0.14f)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (focused) 1.5.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .tvPress(onClick = onClick)
            .clickable(onClick = onClick)
            .padding(horizontal = if (railExpanded) 10.dp else 0.dp),
        contentAlignment = if (railExpanded) Alignment.CenterStart else Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isProfile) Color(0xFFD87093) else Color.White.copy(alpha = 0.15f)
                    )
                    .border(
                        width = if (focused || selected) 1.5.dp else 1.dp,
                        color = if (focused) Color(0xFF101216) else Color.White.copy(alpha = 0.6f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (railExpanded) {
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (focused) Color(0xFF101216) else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (focused) Color(0xFF101216).copy(alpha = 0.75f)
                            else Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private data class TvSettingsCategory(
    val id: String,
    val title: String,
    val icon: ImageVector,
)

/**
 * Video (00:04 - 00:35) style 2-pane Sliding Settings & Hub Dialog:
 * Left side: "Settings" category pills (Hub & Addons, Appearance, Playback, Profiles & System)
 * Right side: Smooth vertical sliding panel with thin white focus borderlines.
 */
@Composable
private fun TvSlidingSettingsHubDialog(
    hubTabs: List<BottomTab>,
    settingsRoute: String,
    activeProfileName: String,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val categories = remember {
        listOf(
            TvSettingsCategory("hub", "Addons & Hub", Icons.Filled.Extension),
            TvSettingsCategory("appearance", "Appearance", Icons.Filled.Palette),
            TvSettingsCategory("playback", "Playback & Servers", Icons.Filled.PlayArrow),
            TvSettingsCategory("profiles", "Profiles & System", Icons.Filled.Settings),
        )
    }
    var selectedCatIndex by remember { mutableIntStateOf(0) }
    val firstCatRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { firstCatRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0C10).copy(alpha = 0.96f))
                .padding(horizontal = 36.dp, vertical = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                // LEFT BAR: "Settings" Categories (Video 00:04-00:35 style)
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight(),
                ) {
                    Text(
                        text = tr("Settings"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp, bottom = 18.dp),
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        itemsIndexed(categories, key = { _, c -> c.id }) { idx, cat ->
                            var focused by remember { mutableStateOf(false) }
                            val isSelected = selectedCatIndex == idx
                            val pillShape = RoundedCornerShape(50)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(pillShape)
                                    .background(
                                        when {
                                            focused -> Color(0xFFE8ECF4)
                                            isSelected -> Color.White.copy(alpha = 0.18f)
                                            else -> Color(0xFF1A1C23)
                                        }
                                    )
                                    .border(
                                        width = if (focused) 1.5.dp else 1.dp,
                                        color = if (focused) Color.White else Color.White.copy(alpha = 0.10f),
                                        shape = pillShape,
                                    )
                                    .onFocusChanged { state ->
                                        focused = state.isFocused
                                        if (state.isFocused) selectedCatIndex = idx
                                    }
                                    .then(if (idx == 0) Modifier.focusRequester(firstCatRequester) else Modifier)
                                    .tvPress(onClick = { selectedCatIndex = idx })
                                    .clickable { selectedCatIndex = idx }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.title,
                                    tint = if (focused) Color(0xFF101216) else Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = cat.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (focused) Color(0xFF101216) else Color.White,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = if (focused) Color(0xFF101216) else Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }

                // RIGHT PANEL: Smooth Sliding Options Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF161820).copy(alpha = 0.92f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                        .padding(26.dp),
                ) {
                    AnimatedContent(
                        targetState = selectedCatIndex,
                        transitionSpec = {
                            val direction = if (targetState >= initialState) 1 else -1
                            (slideInVertically(tween(260, easing = FastOutSlowInEasing)) { h -> direction * (h / 4) } +
                                fadeIn(tween(220))) togetherWith
                                (slideOutVertically(tween(220)) { h -> -direction * (h / 4) } +
                                    fadeOut(tween(180)))
                        },
                        label = "tv-settings-slide",
                    ) { catIdx ->
                        when (catIdx) {
                            0 -> {
                                // ADDONS & HUB: Extensions, Manga, Telegram + Full Settings
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    item {
                                        Text(
                                            text = "Addons, Manga & Telegram",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                        )
                                        Text(
                                            text = "Access Extensions, Manga, Telegram and system tools directly from here",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.68f),
                                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                                        )
                                    }

                                    itemsIndexed(hubTabs, key = { _, t -> t.route }) { _, tab ->
                                        val desc = when {
                                            tab.label.contains("Extension", true) ->
                                                "Manage installed providers, repositories and plugins"
                                            tab.label.contains("Manga", true) ->
                                                "Browse and read manga chapters from your extensions"
                                            tab.label.contains("Telegram", true) ->
                                                "Open Telegram channels and direct media streams"
                                            else -> "Open ${tab.label}"
                                        }
                                        TvSlidingSettingActionCard(
                                            title = tr(tab.label),
                                            subtitle = desc,
                                            trailingText = "Open",
                                            onClick = { onNavigate(tab.route) },
                                        )
                                    }

                                    item {
                                        TvSlidingSettingActionCard(
                                            title = tr("All System Settings"),
                                            subtitle = "Open the complete Hikari settings page (Providers, Backup, Logs & more)",
                                            trailingText = "Open",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }
                            }

                            1 -> {
                                // APPEARANCE: Auto-Hide Sidebar, Reload Button, Hide Logo, Card Expand
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    item {
                                        Text(
                                            text = "Appearance",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                        )
                                        Text(
                                            text = "Theme, layout mode, and visual presentation",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.68f),
                                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                                        )
                                    }

                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Auto-Hide Sidebar",
                                            subtitle = "Keep sidebar completely off-screen until focused via D-pad Left",
                                            checked = TvSidebarPrefs.autoHideSidebar,
                                            onCheckedChange = { TvSidebarPrefs.setAutoHide(context, it) },
                                        )
                                    }

                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Show Reload Button in Sidebar",
                                            subtitle = "Display a quick refresh action button at the bottom of the navigation drawer",
                                            checked = TvSidebarPrefs.showReloadButton,
                                            onCheckedChange = { TvSidebarPrefs.setShowReload(context, it) },
                                        )
                                    }

                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Hide Title Logo",
                                            subtitle = "Show plain text titles instead of graphical logos in hero and details UI",
                                            checked = TvSidebarPrefs.hideTitleLogo,
                                            onCheckedChange = { TvSidebarPrefs.setHideLogo(context, it) },
                                        )
                                    }

                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Expand Poster Card on Focus",
                                            subtitle = "Widen poster cards to 16:9 on focus (Off keeps 2:3 portrait with thin white border)",
                                            checked = TvSidebarPrefs.expandCardOnFocus,
                                            onCheckedChange = { TvSidebarPrefs.setExpandCard(context, it) },
                                        )
                                    }
                                }
                            }

                            2 -> {
                                // PLAYBACK & SERVERS
                                val app = context.applicationContext as HikariApp
                                val askServer by remember { app.store.askServerOnPlayFlow() }
                                    .collectAsState(initial = false)
                                val scope = rememberCoroutineScope()

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    item {
                                        Text(
                                            text = "Playback & Servers",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                        )
                                        Text(
                                            text = "Video resolution, server selection, and player preferences",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.68f),
                                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                                        )
                                    }

                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Choose Server Before Playing",
                                            subtitle = "Show all available servers to pick from instead of auto-playing the first server",
                                            checked = askServer,
                                            onCheckedChange = {
                                                scope.launch { runCatching { app.store.setAskServerOnPlay(it) } }
                                            },
                                        )
                                    }

                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Advanced Playback & Subtitle Settings",
                                            subtitle = "Configure player engine, subtitle style, buffer size and audio language",
                                            trailingText = "Configure",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }
                            }

                            else -> {
                                // PROFILES & SYSTEM
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    item {
                                        Text(
                                            text = "Profiles & System",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                        )
                                        Text(
                                            text = "Manage profiles, trackers (AniList / Simkl), and app settings",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.68f),
                                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                                        )
                                    }

                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Active Profile: $activeProfileName",
                                            subtitle = "Switch profiles, lock profiles, or customize your home catalog",
                                            trailingText = "Manage",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }

                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Open Full Settings",
                                            subtitle = "Access all Hikari settings, themes, collections and diagnostics",
                                            trailingText = "Open",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSlidingSettingToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.015f else 1f,
        animationSpec = tween(180),
        label = "setting-toggle-scale",
    )
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(if (focused) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f))
            .border(
                width = if (focused) 1.5.dp else 1.dp,
                color = if (focused) Color.White else Color.White.copy(alpha = 0.08f),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .tvPress(onClick = { onCheckedChange(!checked) })
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.70f),
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.22f),
            ),
        )
    }
}

@Composable
private fun TvSlidingSettingActionCard(
    title: String,
    subtitle: String,
    trailingText: String,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.015f else 1f,
        animationSpec = tween(180),
        label = "setting-action-scale",
    )
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(if (focused) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f))
            .border(
                width = if (focused) 1.5.dp else 1.dp,
                color = if (focused) Color.White else Color.White.copy(alpha = 0.08f),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .tvPress(onClick = onClick)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.70f),
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = trailingText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
