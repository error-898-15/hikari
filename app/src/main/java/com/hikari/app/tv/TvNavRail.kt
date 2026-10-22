package com.hikari.app.tv

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.Coil
import com.hikari.app.HikariApp
import com.hikari.app.data.Profiles
import com.hikari.app.data.SearchScope
import com.hikari.app.data.StreamCache
import com.hikari.app.i18n.tr
import com.hikari.app.net.NetTuning
import com.hikari.app.ui.AuraColors
import com.hikari.app.ui.LoadingStyles
import com.hikari.app.ui.components.HeroStyles
import com.hikari.app.ui.navigation.BottomTab
import com.hikari.app.ui.navigation.BottomTabs
import com.hikari.app.ui.navigation.Routes
import com.hikari.app.ui.screens.DetailHeroStyles
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * TV Sidebar & Appearance preferences.
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
 * Safely invokes any suspend or regular setter method on AppStore without compile-time coupling.
 */
private suspend fun invokeStoreSetter(store: Any, methodName: String, arg: Any) {
    runCatching {
        val m2 = store.javaClass.methods.firstOrNull { it.name == methodName && it.parameterCount == 2 }
        if (m2 != null) {
            suspendCancellableCoroutine<Any?> { cont ->
                try {
                    val res = m2.invoke(store, arg, cont)
                    if (res !== COROUTINE_SUSPENDED) {
                        cont.resumeWith(Result.success(res))
                    }
                } catch (e: Throwable) {
                    cont.resumeWith(Result.failure(e))
                }
            }
            return
        }
        val m1 = store.javaClass.methods.firstOrNull { it.name == methodName && it.parameterCount == 1 }
        m1?.invoke(store, arg)
    }
}

/**
 * Keeps only Home, Browse (Search), Library (My Stuff), and Schedule (Anime) in the main sidebar.
 * Moves Manga, Telegram, Extensions, IPTV, Stats, etc. into the Settings Hub.
 */
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
    val mainTabs = remember(allTabs) {
        allTabs.filter { it.isMainTvSidebarTab() }.take(4)
    }
    val hubTabs = remember(allTabs, mainTabs) {
        allTabs.filterNot { it in mainTabs || it.isSettingsTab() }
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
    val interactionSource = remember { MutableInteractionSource() }
    val shape = if (railExpanded) RoundedCornerShape(50) else RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(
                when {
                    focused -> Color.White
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
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
    val interactionSource = remember { MutableInteractionSource() }
    val shape = if (railExpanded) RoundedCornerShape(50) else CircleShape

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (railExpanded && subtitle != null) 52.dp else 44.dp)
            .clip(shape)
            .background(
                when {
                    focused -> Color.White
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
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
 * Complete 2-Pane Sliding Settings & Hub Dialog with ALL categories from the original SettingsScreen
 * (1000263580.jpg & 1000263581.jpg) + Addons/Manga/Telegram/IPTV/Stats (5_6210472566272828925.mp4 style).
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
    val app = context.applicationContext as HikariApp
    val scope = rememberCoroutineScope()

    // Live Store States
    val heroStyle by remember { app.store.heroStyleFlow() }.collectAsState(initial = HeroStyles.CAROUSEL)
    val heroScale by remember { app.store.heroScaleFlow() }.collectAsState(initial = 100)
    val heroOverview by remember { app.store.heroOverviewFlow() }.collectAsState(initial = true)
    val heroRating by remember { app.store.heroRatingFlow() }.collectAsState(initial = true)
    val heroMeta by remember { app.store.heroMetaFlow() }.collectAsState(initial = true)
    val hideContinue by remember { app.store.hideContinueFlow() }.collectAsState(initial = false)

    val showDetailRating by remember { app.store.showDetailRatingFlow() }.collectAsState(initial = true)
    val detailHeroStyle by remember { app.store.detailHeroStyleFlow() }.collectAsState(initial = DetailHeroStyles.WIDE)
    val detailLogoSize by remember { app.store.detailLogoSizeFlow() }.collectAsState(initial = 100)
    val tvDetailBig by remember { app.store.tvDetailBigFlow() }.collectAsState(initial = true)
    val tvBlurDetail by remember { app.store.tvBlurDetailFlow() }.collectAsState(initial = false)
    val tvEpisodeSize by remember { app.store.tvEpisodeSizeFlow() }.collectAsState(initial = 100)
    val tvEpViewMode by remember { app.store.tvEpisodeViewModeFlow() }.collectAsState(initial = 1)
    val episodeGlass by remember { app.store.episodeGlassFlow() }.collectAsState(initial = false)
    val episodeDesc by remember { app.store.episodeDescriptionFlow() }.collectAsState(initial = true)

    val showLoadingBanner by remember { app.store.showLoadingBannerFlow() }.collectAsState(initial = true)
    val loadingStyle by remember { app.store.loadingStyleFlow() }.collectAsState(initial = LoadingStyles.CINEMATIC)
    val loadingLogo by remember { app.store.loadingLogoFlow() }.collectAsState(initial = true)
    val loadingLogoSize by remember { app.store.loadingLogoSizeFlow() }.collectAsState(initial = 100)
    val loadingAuraColor by remember { app.store.loadingAuraColorFlow() }.collectAsState(initial = AuraColors.THEME)

    val askServerOnPlay by remember { app.store.askServerOnPlayFlow() }.collectAsState(initial = false)
    val playScopeAsk by remember { app.store.playScopeAskFlow() }.collectAsState(initial = false)
    val playWaitServers by remember { app.store.playWaitServersFlow() }.collectAsState(initial = false)
    val playMinServers by remember { app.store.playMinServersFlow() }.collectAsState(initial = 2)

    val nsfwEnabled by remember { app.store.nsfwEnabledFlow() }.collectAsState(initial = false)
    val repos by remember { app.store.reposFlow() }.collectAsState(initial = emptyList())
    val collections by remember { app.store.collectionsFlow() }.collectAsState(initial = emptyList())
    val trackers by remember { app.store.trackersFlow() }.collectAsState(initial = emptyList())
    val history by remember { app.store.historyFlow() }.collectAsState(initial = emptyList())
    val profiles by Profiles.all.collectAsState()
    val activeProfileId by Profiles.activeId.collectAsState()

    // All 19 categories from original SettingsScreen + Addons Hub
    val categories = remember {
        listOf(
            TvSettingsCategory("hub", "Addons, Manga & Hub", Icons.Filled.GridView),
            TvSettingsCategory("appearance", "Appearance & Theme", Icons.Filled.Star),
            TvSettingsCategory("layout", "App Layout", Icons.Filled.Dashboard),
            TvSettingsCategory("tv_remote", "TV & Remote", Icons.Filled.OndemandVideo),
            TvSettingsCategory("performance", "Performance", Icons.Filled.Tune),
            TvSettingsCategory("player", "Player", Icons.Filled.PlayArrow),
            TvSettingsCategory("playback", "Playback & Servers", Icons.Filled.List),
            TvSettingsCategory("network", "Network & Internet", Icons.Filled.Public),
            TvSettingsCategory("extensions", "Sources & Extensions", Icons.Filled.Settings),
            TvSettingsCategory("filters", "Content & Filters", Icons.Filled.Search),
            TvSettingsCategory("downloads", "Downloads", Icons.Filled.Download),
            TvSettingsCategory("trackers", "Trackers", Icons.Filled.Refresh),
            TvSettingsCategory("integrations", "Integrations", Icons.Filled.Translate),
            TvSettingsCategory("catalogs", "Personal Catalogs", Icons.Filled.Folder),
            TvSettingsCategory("privacy", "Privacy & Browsing", Icons.Filled.Lock),
            TvSettingsCategory("logs", "Logs & Diagnostics", Icons.Filled.Warning),
            TvSettingsCategory("backup", "Backup & Restore", Icons.Filled.Bookmark),
            TvSettingsCategory("profiles", "Profiles", Icons.Filled.Person),
            TvSettingsCategory("about", "About & App Data", Icons.Filled.Info),
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
                .padding(horizontal = 32.dp, vertical = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // LEFT PANE: Scrollable Categories List (Video 00:04-00:35 style)
                Column(
                    modifier = Modifier
                        .width(250.dp)
                        .fillMaxHeight(),
                ) {
                    Text(
                        text = tr("Settings"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 10.dp, bottom = 14.dp),
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(categories, key = { _, c -> c.id }) { idx, cat ->
                            var focused by remember { mutableStateOf(false) }
                            val interactionSource = remember { MutableInteractionSource() }
                            val isSelected = selectedCatIndex == idx
                            val pillShape = RoundedCornerShape(50)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(pillShape)
                                    .background(
                                        when {
                                            focused -> Color.White
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
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = { selectedCatIndex = idx },
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.title,
                                    tint = if (focused) Color(0xFF101216) else Color.White,
                                    modifier = Modifier.size(19.dp),
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

                // RIGHT PANE: Smooth Sliding Content Panel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF161820).copy(alpha = 0.94f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                        .padding(26.dp),
                ) {
                    AnimatedContent(
                        targetState = selectedCatIndex,
                        transitionSpec = {
                            val dir = if (targetState >= initialState) 1 else -1
                            (slideInVertically(tween(250, easing = FastOutSlowInEasing)) { h -> dir * (h / 4) } +
                                fadeIn(tween(200))) togetherWith
                                (slideOutVertically(tween(200)) { h -> -dir * (h / 4) } +
                                    fadeOut(tween(160)))
                        },
                        label = "tv-settings-pane-slide",
                    ) { catIdx ->
                        val catId = categories.getOrNull(catIdx)?.id ?: "hub"
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 20.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            when (catId) {
                                "hub" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Addons, Manga, Telegram & Hub",
                                            subtitle = "Quickly open Manga, Telegram, Extensions, IPTV and Stats moved from the main bar",
                                        )
                                    }
                                    itemsIndexed(hubTabs, key = { _, t -> t.route }) { _, tab ->
                                        val desc = when {
                                            tab.label.contains("Extension", true) ->
                                                "Manage installed providers, repositories, plugins & sources"
                                            tab.label.contains("Manga", true) ->
                                                "Browse and read manga chapters from your installed manga extensions"
                                            tab.label.contains("Telegram", true) ->
                                                "Browse and stream media from your Telegram channels"
                                            tab.label.contains("IPTV", true) ->
                                                "Watch live TV channels from your M3U / IPTV playlists"
                                            tab.label.contains("Stat", true) ->
                                                "View watch time spent, streaks and your most watched titles"
                                            else -> "Open ${tab.label}"
                                        }
                                        TvSlidingSettingActionCard(
                                            title = tr(tab.label),
                                            subtitle = desc,
                                            trailingText = "Open",
                                            onClick = { onNavigate(tab.route) },
                                        )
                                    }
                                }

                                "appearance" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Appearance & Theme",
                                            subtitle = "Theme, sidebar behavior, language, accent, icon & font",
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
                                            subtitle = "Show plain text titles instead of graphical logos in details and hero UI",
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
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Language, Theme, Accent & Font",
                                            subtitle = "Open full theme palette, app language and typography selector",
                                            trailingText = "Customize",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }

                                "layout" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "App Layout",
                                            subtitle = "UI scale, featured banner, posters, ratings & episode layout",
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Hide Continue Watching Shelf",
                                            subtitle = "Hide the Continue Watching row on the Home screen",
                                            checked = hideContinue,
                                            onCheckedChange = {
                                                scope.launch { invokeStoreSetter(app.store, "setHideContinue", it) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Show Overview on Featured Banner",
                                            subtitle = "Display story synopsis inside the Home hero banner",
                                            checked = heroOverview,
                                            onCheckedChange = {
                                                scope.launch { invokeStoreSetter(app.store, "setHeroOverview", it) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Show Rating on Featured Banner",
                                            subtitle = "Display IMDb / TMDB score badge on the Home hero banner",
                                            checked = heroRating,
                                            onCheckedChange = {
                                                scope.launch { invokeStoreSetter(app.store, "setHeroRating", it) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Show Metadata on Featured Banner",
                                            subtitle = "Display year, type and genres on the Home hero banner",
                                            checked = heroMeta,
                                            onCheckedChange = {
                                                scope.launch { invokeStoreSetter(app.store, "setHeroMeta", it) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Featured Banner Height Scale",
                                            subtitle = "Tap to cycle banner size on the Home screen",
                                            trailingText = "${heroScale}%",
                                            onClick = {
                                                val scales = listOf(80, 90, 100, 115, 130)
                                                val next = scales[(scales.indexOf(heroScale) + 1) % scales.size]
                                                scope.launch { invokeStoreSetter(app.store, "setHeroScale", next) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Show Review Ratings on Detail Screen",
                                            subtitle = "Display IMDb, TMDB, Rotten Tomatoes & Metacritic scores",
                                            checked = showDetailRating,
                                            onCheckedChange = {
                                                scope.launch { invokeStoreSetter(app.store, "setShowDetailRating", it) }
                                            },
                                        )
                                    }
                                    item {
                                        val epModeLabel = when (tvEpViewMode) {
                                            0 -> "Compact List"
                                            2 -> "Big Cinema Cards"
                                            else -> "Wide Thumbnail Grid"
                                        }
                                        TvSlidingSettingActionCard(
                                            title = "Episode UI Layout",
                                            subtitle = "Choose how episodes are displayed on series detail pages",
                                            trailingText = epModeLabel,
                                            onClick = {
                                                val next = when (tvEpViewMode) {
                                                    1 -> 2
                                                    2 -> 0
                                                    else -> 1
                                                }
                                                scope.launch { runCatching { app.store.setTvEpisodeViewMode(next) } }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Frosted-Glass Episode Cards",
                                            subtitle = "Draw translucent glass background behind episode cards",
                                            checked = episodeGlass,
                                            onCheckedChange = {
                                                scope.launch { invokeStoreSetter(app.store, "setEpisodeGlass", it) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Show Episode Descriptions",
                                            subtitle = "Show episode synopsis text underneath each episode title",
                                            checked = episodeDesc,
                                            onCheckedChange = {
                                                scope.launch { invokeStoreSetter(app.store, "setEpisodeDescription", it) }
                                            },
                                        )
                                    }
                                }

                                "tv_remote" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "TV & Remote",
                                            subtitle = "Television layout, screen edges, remote cursor & performance",
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Big TV Detail Layout",
                                            subtitle = "Use full-screen living-room layout on movie and series detail pages",
                                            checked = tvDetailBig,
                                            onCheckedChange = {
                                                scope.launch { invokeStoreSetter(app.store, "setTvDetailBig", it) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Full-Bleed & Blurred Detail Backdrop",
                                            subtitle = "Keep a smooth blurred backdrop behind scrolled detail sections",
                                            checked = tvBlurDetail,
                                            onCheckedChange = {
                                                scope.launch { invokeStoreSetter(app.store, "setTvBlurDetail", it) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "TV Episode Card Scale",
                                            subtitle = "Adjust the size of episode cards on the detail screen",
                                            trailingText = "${tvEpisodeSize}%",
                                            onClick = {
                                                val sizes = listOf(85, 100, 115, 130, 150)
                                                val next = sizes[(sizes.indexOf(tvEpisodeSize) + 1) % sizes.size]
                                                scope.launch { invokeStoreSetter(app.store, "setTvEpisodeSize", next) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Detail Title Logo Size",
                                            subtitle = "Scale the graphical title logo on detail pages",
                                            trailingText = "${detailLogoSize}%",
                                            onClick = {
                                                val sizes = listOf(75, 100, 125, 150)
                                                val next = sizes[(sizes.indexOf(detailLogoSize) + 1) % sizes.size]
                                                scope.launch { invokeStoreSetter(app.store, "setDetailLogoSize", next) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "D-Pad Virtual Mouse Cursor",
                                            subtitle = "Toggle on-screen mouse cursor controlled by your TV remote",
                                            checked = MouseMode.enabled,
                                            onCheckedChange = { MouseMode.toggle() },
                                        )
                                    }
                                }

                                "performance" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Performance",
                                            subtitle = "Fix lag, stutter, memory usage & image caches",
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Clear Image & Poster Cache",
                                            subtitle = "Free up RAM and disk space used by cached poster bitmaps",
                                            trailingText = "Clear Now",
                                            onClick = {
                                                runCatching {
                                                    Coil.imageLoader(context).memoryCache?.clear()
                                                    Toast.makeText(context, "Image memory cache cleared", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Advanced Performance Settings",
                                            subtitle = "Configure low-RAM mode, animations and hardware acceleration",
                                            trailingText = "Open",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }

                                "player" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Player",
                                            subtitle = "Controls, video enhancement, subtitles & loading screen",
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Show Loading Cover Screen",
                                            subtitle = "Display artwork cover while searching and buffering servers",
                                            checked = showLoadingBanner,
                                            onCheckedChange = {
                                                scope.launch { invokeStoreSetter(app.store, "setShowLoadingBanner", it) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Loading Screen Style",
                                            subtitle = "Choose the visual layout of the server loading screen",
                                            trailingText = loadingStyle.uppercase(),
                                            onClick = {
                                                val styles = listOf(
                                                    LoadingStyles.CINEMATIC,
                                                    LoadingStyles.POSTER,
                                                    LoadingStyles.SPOTLIGHT,
                                                    LoadingStyles.MINIMAL,
                                                )
                                                val next = styles[(styles.indexOf(loadingStyle) + 1) % styles.size]
                                                scope.launch { invokeStoreSetter(app.store, "setLoadingStyle", next) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Show Title Logo on Loading Screen",
                                            subtitle = "Draw the official movie/series logo wordmark on the loading screen",
                                            checked = loadingLogo,
                                            onCheckedChange = {
                                                scope.launch { invokeStoreSetter(app.store, "setLoadingLogo", it) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Loading Logo Size",
                                            subtitle = "Adjust the size of the title wordmark on the loading cover",
                                            trailingText = "${loadingLogoSize}%",
                                            onClick = {
                                                val sizes = listOf(75, 100, 125, 150)
                                                val next = sizes[(sizes.indexOf(loadingLogoSize) + 1) % sizes.size]
                                                scope.launch { invokeStoreSetter(app.store, "setLoadingLogoSize", next) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Subtitle Styling, Audio & Video Controls",
                                            subtitle = "Configure subtitle font size, background, audio delay and gestures",
                                            trailingText = "Configure",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }

                                "playback" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Playback & Servers",
                                            subtitle = "Where servers come from & when playback starts",
                                        )
                                    }
                                    var allExt by remember { mutableStateOf(SearchScope.allExtensions) }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Search All Installed Extensions",
                                            subtitle = "Search every enabled provider for streams instead of only the origin provider",
                                            checked = allExt,
                                            onCheckedChange = {
                                                allExt = it
                                                SearchScope.allExtensions = it
                                                scope.launch { invokeStoreSetter(app.store, "setSearchAllExtensions", it) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Choose Server Before Playing",
                                            subtitle = "Show the server selection list instead of auto-playing the first server found",
                                            checked = askServerOnPlay,
                                            onCheckedChange = {
                                                scope.launch { invokeStoreSetter(app.store, "setAskServerOnPlay", it) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Ask Search Scope on Play",
                                            subtitle = "Ask whether to search only this extension or all selected servers when pressing Play",
                                            checked = playScopeAsk,
                                            onCheckedChange = {
                                                scope.launch { invokeStoreSetter(app.store, "setPlayScopeAsk", it) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Wait for Multiple Servers First",
                                            subtitle = "Wait until several servers respond before starting playback",
                                            checked = playWaitServers,
                                            onCheckedChange = {
                                                scope.launch { invokeStoreSetter(app.store, "setPlayWaitServers", it) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Minimum Servers Before Playback Starts",
                                            subtitle = "Number of servers to gather when 'Wait for Multiple Servers' is enabled",
                                            trailingText = "$playMinServers servers",
                                            onClick = {
                                                val counts = listOf(1, 2, 3, 5, 8)
                                                val next = counts[(counts.indexOf(playMinServers) + 1) % counts.size]
                                                scope.launch { invokeStoreSetter(app.store, "setPlayMinServers", next) }
                                            },
                                        )
                                    }
                                }

                                "network" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Network and Internet",
                                            subtitle = "DNS mode, DoH & slow connection optimization",
                                        )
                                    }
                                    var slowConn by remember { mutableStateOf(NetTuning.slowConnection) }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Slow Connection Mode",
                                            subtitle = "Increase network timeouts so slow providers and mobile hotspots don't time out early",
                                            checked = slowConn,
                                            onCheckedChange = {
                                                slowConn = it
                                                NetTuning.slowConnection = it
                                                scope.launch { invokeStoreSetter(app.store, "setSlowConnection", it) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "DNS over HTTPS (DoH) & Proxy",
                                            subtitle = "Configure Cloudflare / Google / AdGuard DNS to bypass ISP blocks",
                                            trailingText = "Configure",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }

                                "extensions" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Sources & Extensions",
                                            subtitle = "Installed extensions (${repos.size} repositories), userscripts & verification",
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Manage Extensions & Repositories",
                                            subtitle = "Install, update, enable or disable CloudStream, Nuvio, Stremio & Aniyomi addons",
                                            trailingText = "Open",
                                            onClick = { onNavigate(Routes.EXTENSIONS) },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Userscripts & Site Verification",
                                            subtitle = "Manage custom userscripts and Cloudflare cookies in full settings",
                                            trailingText = "Open",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }

                                "filters" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Content & Filters",
                                            subtitle = "Adult content filter, metadata language & what the app shows you",
                                        )
                                    }
                                    item {
                                        TvSlidingSettingToggleCard(
                                            title = "Allow Adult (18+ / NSFW) Content",
                                            subtitle = "Show 18+ extensions and uncapped age certifications in catalogs and search",
                                            checked = nsfwEnabled,
                                            onCheckedChange = {
                                                scope.launch { invokeStoreSetter(app.store, "setNsfwEnabled", it) }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Content Language & Catalog Filters",
                                            subtitle = "Choose TMDB metadata language and hide unwanted genres or tabs",
                                            trailingText = "Configure",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }

                                "downloads" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Downloads",
                                            subtitle = "Offline copies, download directory & parallel saves",
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Manage Downloads & Storage Location",
                                            subtitle = "View active/completed downloads and change parallel thread count",
                                            trailingText = "Open",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }

                                "trackers" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Trackers",
                                            subtitle = "AniList, Simkl, MyAnimeList, Trakt and watch-progress syncing",
                                        )
                                    }
                                    if (trackers.isNotEmpty()) {
                                        itemsIndexed(trackers, key = { _, acc -> acc.kind.key }) { _, acc ->
                                            TvSlidingSettingActionCard(
                                                title = acc.kind.label,
                                                subtitle = "Connected as: ${acc.user.ifBlank { "Active Account" }}",
                                                trailingText = "Connected",
                                                onClick = { onNavigate(settingsRoute) },
                                            )
                                        }
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Connect or Manage Trackers",
                                            subtitle = "Sign in to AniList, Simkl, Trakt, MAL or Kitsu to auto-sync watched episodes",
                                            trailingText = "Manage",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }

                                "integrations" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Integrations",
                                            subtitle = "TMDB enrichment, MDBList ratings, anime metadata & external APIs",
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "TMDB, MDBList & Metadata Integrations",
                                            subtitle = "Configure custom API keys, rating sources and anime enrichment",
                                            trailingText = "Configure",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }

                                "catalogs" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Personal Catalog Creator",
                                            subtitle = "Your own collections (${collections.size} saved), folders & custom Home shelves",
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Open Personal Catalog Creator",
                                            subtitle = "Create folders (Netflix, Prime, Anime, etc.) and pin custom catalogs to Home",
                                            trailingText = "Open",
                                            onClick = { onNavigate(Routes.COLLECTIONS) },
                                        )
                                    }
                                }

                                "privacy" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Privacy & Browsing",
                                            subtitle = "Ad blocking, redirects & custom WebView user-agent",
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Ad Blocker, Redirect Shield & User-Agent",
                                            subtitle = "Configure WebView security rules and browser fingerprint",
                                            trailingText = "Configure",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }

                                "logs" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Logs & Diagnostics",
                                            subtitle = "App logs, provider outcomes & crash reports",
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "View & Export Diagnostic Logs",
                                            subtitle = "Inspect real-time extension logs, network errors and crash traces",
                                            trailingText = "Open Logs",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }

                                "backup" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Backup & Restore",
                                            subtitle = "One file with your whole setup (Extensions, Library, History & Settings)",
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Create Backup or Restore Setup",
                                            subtitle = "Export your entire Hikari configuration to a file or restore from backup",
                                            trailingText = "Open",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }

                                "profiles" -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "Profiles",
                                            subtitle = "Separate setups on one device — extensions, Library & watch history",
                                        )
                                    }
                                    itemsIndexed(profiles, key = { _, p -> p.id }) { _, prof ->
                                        val isCurrent = prof.id == activeProfileId
                                        TvSlidingSettingActionCard(
                                            title = prof.name,
                                            subtitle = if (isCurrent) "Active Profile" else "Tap to switch to this profile",
                                            trailingText = if (isCurrent) "Active ✓" else "Switch",
                                            onClick = {
                                                if (!isCurrent) {
                                                    scope.launch {
                                                        val ok = runCatching { Profiles.switchTo(app, prof.id) }.getOrDefault(false)
                                                        if (!ok) {
                                                            Toast.makeText(
                                                                context,
                                                                "Profile is locked — unlock it in full Settings",
                                                                Toast.LENGTH_SHORT,
                                                            ).show()
                                                        }
                                                    }
                                                }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Create or Lock Profiles",
                                            subtitle = "Add a new profile, set a PIN lock, or delete an existing profile",
                                            trailingText = "Manage",
                                            onClick = { onNavigate(settingsRoute) },
                                        )
                                    }
                                }

                                else -> {
                                    item {
                                        TvSettingsHeader(
                                            title = "About, Updates & Clear Data",
                                            subtitle = "Version info, updates, watch history (${history.size} items) & app reset",
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "Clear Watch History (${history.size} items)",
                                            subtitle = "Remove all Continue Watching and watch history entries from this profile",
                                            trailingText = "Clear",
                                            onClick = {
                                                scope.launch {
                                                    history.forEach { h ->
                                                        runCatching { app.store.removeHistory(h.uniqueKey) }
                                                    }
                                                    Toast.makeText(context, "Watch history cleared", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                        )
                                    }
                                    item {
                                        TvSlidingSettingActionCard(
                                            title = "About, Updates & Clear App Data",
                                            subtitle = "Check for app updates, view changelog or perform a full factory reset",
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
private fun TvSettingsHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.68f),
            modifier = Modifier.padding(top = 2.dp),
        )
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
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.012f else 1f,
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
                width = if (focused) 1.6.dp else 1.dp,
                color = if (focused) Color.White else Color.White.copy(alpha = 0.08f),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .tvPress(onClick = { onCheckedChange(!checked) })
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(horizontal = 20.dp, vertical = 15.dp),
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
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.012f else 1f,
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
                width = if (focused) 1.6.dp else 1.dp,
                color = if (focused) Color.White else Color.White.copy(alpha = 0.08f),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .tvPress(onClick = onClick)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 15.dp),
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
