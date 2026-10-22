package com.hikari.app.tv

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hikari.app.data.Profiles
import com.hikari.app.i18n.tr
import com.hikari.app.ui.navigation.BottomTab
import com.hikari.app.ui.navigation.BottomTabs
import com.hikari.app.ui.navigation.Routes
import kotlinx.coroutines.launch

/**
 * TV Sidebar & Appearance preferences used by SettingsScreen.kt, TvRedesign.kt & TvCinematicHero.kt.
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

/**
 * Click modifier without the default yellow TvFocusProvider indication ring.
 */
@Composable
private fun Modifier.tvRailClick(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) {
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        onClick()
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
}

private fun BottomTab.isMainTvSidebarTab(): Boolean {
    val r = route.lowercase()
    val l = label.lowercase()
    if (r.contains("manga") || r.contains("telegram") || r.contains("extension") ||
        r.contains("iptv") || r.contains("stat") || r.contains("setting") ||
        l.contains("manga") || l.contains("telegram") || l.contains("extension") ||
        l.contains("iptv") || l.contains("stat") || l.contains("setting")
    ) {
        return false
    }
    return true
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
    // Main sidebar keeps only Home, Schedule, Browse, Library
    val mainTabs = remember(allTabs) {
        allTabs.filter { it.isMainTvSidebarTab() }.take(4)
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

    val isSettingsOpen = currentRoute == settingsRoute
    val autoHide = TvSidebarPrefs.autoHideSidebar
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
                color = if (railHasFocus) Color(0xFF161820).copy(alpha = 0.96f) else Color.Transparent,
                shape = drawerShape,
            )
            .border(
                width = if (railHasFocus) 1.dp else 0.dp,
                color = if (railHasFocus) Color.White.copy(alpha = 0.16f) else Color.Transparent,
                shape = drawerShape,
            )
            .padding(vertical = 14.dp, horizontal = if (railHasFocus) 10.dp else 4.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {
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

                // Clicking Profile / Settings & Acc directly opens SettingsScreen.kt!
                TvBottomActionRow(
                    icon = Icons.Filled.Person,
                    title = activeProfileName,
                    subtitle = "Settings & Acc",
                    railExpanded = railHasFocus,
                    selected = isSettingsOpen,
                    isProfile = true,
                    onClick = { onNavigate(settingsRoute) },
                )
            }
        }
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
            .tvRailClick(onClick = onClick)
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
            .tvRailClick(onClick = onClick)
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
