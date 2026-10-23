package com.hikari.app.ui.screens

import com.hikari.app.tv.TvMode
import com.hikari.app.tv.TvUi
import com.hikari.app.i18n.tr
import com.hikari.app.i18n.trTag
import com.hikari.app.i18n.I18n
import com.hikari.app.ui.components.GlassSearchField

import android.app.Application
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.hikari.app.HikariApp
import com.hikari.app.data.ContentRepository
import com.hikari.app.data.MediaItem
import com.hikari.app.data.MediaType
import com.hikari.app.data.ProviderType
import com.hikari.app.providers.ContentProvider
import com.hikari.app.ui.Artwork
import com.hikari.app.ui.PosterLoader
import com.hikari.app.ui.RatingBadge
import com.hikari.app.ui.components.EmptyState
import com.hikari.app.ui.navigation.LocalTaskbarInset
import com.hikari.app.ui.navigation.Routes
import com.hikari.app.ui.rememberPosterScore
import com.hikari.app.ui.rememberPosterStyle
import com.hikari.app.ui.rememberVisibleItems
import com.hikari.app.tv.tvPress
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Self-contained preference for showing/hiding the Search Filters (Genre, Year & Kind)
 * below the search bar. Default is FALSE (hidden), and can be toggled in Settings > App Layout.
 */
object SearchFilterPrefs {
    private const val PREFS_NAME = "hikari_search_filter_prefs"
    private const val KEY_SHOW_FILTERS = "show_search_filters"

    var showSearchFilters by mutableStateOf(false)
        private set

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        showSearchFilters = p.getBoolean(KEY_SHOW_FILTERS, false)
    }

    fun setShowFilters(context: Context, enabled: Boolean) {
        showSearchFilters = enabled
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOW_FILTERS, enabled)
            .apply()
    }
}

/**
 * Card for Settings > App Layout to toggle Search Filters on/off.
 */
@Composable
fun SearchFiltersSettingCard() {
    val context = LocalContext.current
    LaunchedEffect(Unit) { SearchFilterPrefs.init(context) }
    Column(Modifier.padding(16.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { SearchFilterPrefs.setShowFilters(context, !SearchFilterPrefs.showSearchFilters) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    tr("Search filters (Genre, Year & Type)"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    tr("Off hides the Movie/Series/Anime, Year and Genre filter bars under the search box for a cleaner search screen."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = SearchFilterPrefs.showSearchFilters,
                onCheckedChange = { SearchFilterPrefs.setShowFilters(context, it) },
            )
        }
    }
}

@OptIn(FlowPreview::class)
class SearchViewModel(
    app: Application,
    private val savedState: SavedStateHandle,
) : AndroidViewModel(app) {
    private val manager = (app as HikariApp).providers
    private val repo = ContentRepository(manager)
    private val hikari: HikariApp = app as HikariApp
    private val collections = com.hikari.app.data.CollectionsRepository(manager)

    val userCollections: Flow<List<com.hikari.app.data.Collection>> =
        (app as HikariApp).store.collectionsFlow()

    private val _query = MutableStateFlow(savedState.get<String>("query") ?: "")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedProviders =
        MutableStateFlow(savedState.get<ArrayList<String>>("providers")?.toSet() ?: emptySet())
    val selectedProviders: StateFlow<Set<String>> = _selectedProviders.asStateFlow()

    val providers: StateFlow<List<ContentProvider>> = manager.providers

    val results: StateFlow<List<MediaItem>> = SearchSession.results
    val searching: StateFlow<Boolean> = SearchSession.searching

    private val _collectionHits =
        MutableStateFlow<List<com.hikari.app.data.CollectionsRepository.CollectionHit>>(emptyList())
    val collectionHits: StateFlow<List<com.hikari.app.data.CollectionsRepository.CollectionHit>> =
        _collectionHits.asStateFlow()

    val history: Flow<List<String>> = hikari.store.searchHistoryFlow()

    fun forgetSearch(query: String) {
        viewModelScope.launch {
            runCatching { hikari.store.removeSearchHistory(query) }
        }
    }

    fun forgetAllSearches() {
        viewModelScope.launch {
            runCatching { hikari.store.clearSearchHistory() }
        }
    }

    init {
        viewModelScope.launch {
            combine(_query.debounce(400).distinctUntilChanged(), _selectedProviders) { q, sel ->
                q to sel
            }
                .collectLatest { (q, selection) ->
                    if (q.isBlank()) {
                        _collectionHits.value = emptyList()
                        SearchSession.clear()
                        return@collectLatest
                    }
                    viewModelScope.launch {
                        runCatching { hikari.store.addSearchHistory(q) }
                    }
                    if (q.trim().length < 2) _collectionHits.value = emptyList()
                    val extensionIds = selection.filterNot { Routes.isCollectionScope(it) }
                        .toSet()
                    val catalogIds = selection.filter { Routes.isCollectionScope(it) }
                        .map { Routes.collectionIdOfScope(it) }
                        .toSet()
                    val sweepExtensions = selection.isEmpty() || extensionIds.isNotEmpty()
                    kotlinx.coroutines.coroutineScope {
                        launch {
                            val cap = if (catalogIds.isEmpty()) 24 else 60
                            runCatching {
                                collections.searchIn(q, catalogIds, cap) { partial ->
                                    _collectionHits.value = partial
                                }
                            }.onSuccess { _collectionHits.value = it }
                        }
                        launch {
                            if (sweepExtensions) SearchSession.search(repo, q, extensionIds)
                            else SearchSession.clear()
                        }
                    }
                }
        }
    }

    fun setQuery(q: String) {
        _query.value = q
        savedState["query"] = q
    }

    fun selectAll() {
        _selectedProviders.value = emptySet()
        savedState["providers"] = ArrayList<String>()
    }

    fun selectProvider(id: String) {
        if (id.isBlank()) return
        _selectedProviders.value = setOf(id)
        savedState["providers"] = ArrayList(listOf(id))
    }

    fun setProviders(keys: List<String>) {
        val next = keys.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (next.isEmpty()) {
            selectAll()
            return
        }
        _selectedProviders.value = next.toSet()
        savedState["providers"] = ArrayList(next)
    }
}

@Composable
fun SearchScreen(
    nav: NavHostController,
    initialQuery: String = "",
    initialProvider: String = "",
) {
    val vm: SearchViewModel = viewModel()
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val searching by vm.searching.collectAsState()
    val collectionHits by vm.collectionHits.collectAsState()
    val selected by vm.selectedProviders.collectAsState()
    val providers by vm.providers.collectAsState()
    val history by vm.history.collectAsState(initial = emptyList())
    val collections by vm.userCollections.collectAsState(initial = emptyList())

    var translatedFrom by remember { mutableStateOf<String?>(null) }
    var targetLang by rememberSaveable { mutableStateOf("zh-CN") }
    var langMenu by remember { mutableStateOf(false) }
    var translating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    LaunchedEffect(Unit) { SearchFilterPrefs.init(context) }

    val searchFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var searchAutoFocused by rememberSaveable { mutableStateOf(false) }
    val searchIsTv = TvMode.current()
    LaunchedEffect(Unit) {
        if (searchAutoFocused) return@LaunchedEffect
        searchAutoFocused = true
        if (searchIsTv) return@LaunchedEffect
        kotlinx.coroutines.delay(250)
        runCatching {
            searchFocus.requestFocus()
            keyboard?.show()
        }
    }
    val overscanFlow = remember(context) {
        (context.applicationContext as HikariApp).store.tvOverscanFlow()
    }
    val overscanDp by overscanFlow.collectAsState(initial = TvUi.DEFAULT_OVERSCAN_DP)
    val tvEdge = if (TvMode.current()) overscanDp.dp else 0.dp

    var kindFilterKey by rememberSaveable { mutableStateOf("all") }
    val kindFilter = SearchKindFilter.fromKey(kindFilterKey)

    var yearsKey by rememberSaveable { mutableStateOf("") }
    val yearsFilter: Set<Int> = remember(yearsKey) {
        yearsKey.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it > 1800 }.toSet()
    }
    fun setYears(next: Set<Int>) {
        yearsKey = next.sortedDescending().joinToString(",")
    }
    fun toggleYear(year: Int) {
        setYears(if (year in yearsFilter) yearsFilter - year else yearsFilter + year)
    }

    var genreKey by rememberSaveable { mutableStateOf("") }
    val genreFilter: Set<String> = remember(genreKey) {
        genreKey.split(',').mapNotNull { it.trim().takeIf { g -> g.isNotEmpty() } }.toSet()
    }
    fun setGenres(next: Set<String>) {
        genreKey = next.sorted().joinToString(",")
    }
    fun toggleGenre(genre: String) {
        val key = genre.trim().lowercase()
        if (key.isEmpty()) return
        setGenres(if (key in genreFilter) genreFilter - key else genreFilter + key)
    }

    val showFilters = SearchFilterPrefs.showSearchFilters
    val filterOn = showFilters && (kindFilter != SearchKindFilter.ALL ||
        yearsFilter.isNotEmpty() || genreFilter.isNotEmpty())
    val filtered = remember(results, filterOn, kindFilter, yearsKey, genreKey) {
        if (!filterOn) results
        else results.filter { it.passesSearchFilter(kindFilter, yearsFilter, genreFilter) }
    }
    val keptUnknownKind = remember(results, filterOn, kindFilter, yearsKey, genreKey) {
        if (!filterOn) 0
        else results.count { it.unknownKindKept(kindFilter, yearsFilter, genreFilter) }
    }
    val hiddenNoYear = remember(results, filterOn, kindFilter, yearsKey, genreKey) {
        if (!filterOn || yearsFilter.isEmpty()) 0 else results.count {
            it.year == null && it.passesSearchFilter(kindFilter, emptySet(), genreFilter)
        }
    }
    val hiddenNoGenre = remember(results, filterOn, kindFilter, yearsKey, genreKey) {
        if (!filterOn || genreFilter.isEmpty()) 0 else results.count {
            it.genres.isEmpty() && it.passesSearchFilter(kindFilter, yearsFilter, emptySet())
        }
    }

    val engineOf = remember(providers) {
        providers.associate { it.config.id to it.config.type.groupLabel }
    }
    var engineKey by rememberSaveable { mutableStateOf("") }
    val engineCounts = remember(filtered, engineOf) {
        val counts = HashMap<String, Int>()
        for (item in filtered) {
            val label = engineOf[item.providerId] ?: continue
            counts[label] = (counts[label] ?: 0) + 1
        }
        counts.entries.sortedByDescending { it.value }.associate { it.key to it.value }
    }
    val activeEngine = engineKey.takeIf { engineCounts.containsKey(it) }.orEmpty()
    val visible = remember(filtered, activeEngine, engineOf) {
        if (activeEngine.isBlank()) filtered
        else filtered.filter { engineOf[it.providerId] == activeEngine }
    }
    LaunchedEffect(engineCounts, engineKey) {
        if (engineKey.isNotBlank() && !engineCounts.containsKey(engineKey)) engineKey = ""
    }

    val soleName = remember(selected, providers, collections) {
        if (selected.size != 1) return@remember null
        val key = selected.first()
        if (Routes.isCollectionScope(key)) {
            collections.firstOrNull { it.id == Routes.collectionIdOfScope(key) }?.name
        } else {
            providers.firstOrNull { it.config.id == key }?.config?.name
        }
    }

    LaunchedEffect(Unit) {
        if (initialQuery.isNotBlank()) vm.setQuery(initialQuery)
        if (initialProvider.isNotBlank()) vm.selectProvider(initialProvider)
    }

    fun toggleTranslate() {
        if (translating) return
        val restore = translatedFrom
        if (restore != null) {
            vm.setQuery(restore)
            translatedFrom = null
            return
        }
        val src = query.trim()
        if (src.isEmpty()) return
        scope.launch {
            translating = true
            val out = com.hikari.app.data.Translator.translateTo(src, targetLang)
            translating = false
            if (out.isNotBlank() && out != src) {
                translatedFrom = src
                vm.setQuery(out)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (TvMode.current()) {
            com.hikari.app.tv.TvScreenHeader(
                title = tr("Search"),
                subtitle = if (selected.isEmpty()) tr("All providers")
                else if (soleName != null) soleName
                else tr("%s sources").replace("%s", selected.size.toString())
            )
        }
        GlassSearchField(
            value = query,
            onValueChange = { vm.setQuery(it); translatedFrom = null },
            placeholder = when {
                selected.isEmpty() -> "Search across all providers…"
                soleName != null -> "Search in $soleName…"
                else -> "Search in ${selected.size} selected sources…"
            },
            focusRequester = searchFocus,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (com.hikari.app.tv.TvMode.current()) 20.dp else 16.dp,
                    vertical = if (com.hikari.app.tv.TvMode.current()) 10.dp else 8.dp
                ),
            trailing = {
                Box {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .tvPress { toggleTranslate() }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { toggleTranslate() },
                                    onLongPress = { langMenu = true },
                                )
                            }
                            .padding(10.dp)
                    ) {
                        if (translating) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Filled.Translate,
                                contentDescription = if (translatedFrom != null)
                                    "Show original English" else "Translate to $targetLang",
                                tint = if (translatedFrom != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = langMenu,
                        onDismissRequest = { langMenu = false },
                    ) {
                        com.hikari.app.data.Translator.LANGUAGES.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(if (code == targetLang) "$label  ✓" else label)
                                },
                                onClick = {
                                    targetLang = code
                                    langMenu = false
                                    translatedFrom = null
                                },
                            )
                        }
                    }
                }
            }
        )
        // ---- Recent searches ----
        if (query.isBlank() && history.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    tr("Recent searches"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { vm.forgetAllSearches() }) {
                    Text(tr("Clear all"), style = MaterialTheme.typography.labelSmall)
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                items(history.distinct(), key = { it }) { past ->
                    HistoryChip(
                        text = past,
                        onPick = {
                            translatedFrom = null
                            vm.setQuery(past)
                        },
                        onDelete = { vm.forgetSearch(past) },
                    )
                }
            }
        }
        if (providers.isNotEmpty() || collections.isNotEmpty()) {
            var showProviders by remember { mutableStateOf(false) }
            var providerKind by remember { mutableStateOf<ProviderType?>(null) }
            val scopeLabel = when {
                selected.isEmpty() -> tr("All providers")
                soleName != null -> soleName
                else -> I18n.t("%s sources").replace("%s", selected.size.toString())
            }
            val scopeNote = when {
                selected.isEmpty() -> tr("Searching every source")
                soleName != null -> I18n.t("Searching in %s").replace("%s", soleName)
                else -> I18n.t("%s sources selected").replace("%s", selected.size.toString())
            }
            ProviderScopeButton(
                label = scopeLabel,
                supporting = scopeNote,
                onClick = { showProviders = true },
            )
            if (showProviders) {
                ProviderPickerSheet(
                    providers = providers,
                    collections = collections,
                    selection = selected.toList(),
                    filter = providerKind,
                    onFilter = { providerKind = it },
                    onManageCollections = {
                        showProviders = false
                        Routes.safeNavigate(nav, Routes.COLLECTIONS)
                    },
                    onPick = { key ->
                        showProviders = false
                        if (key == null) vm.selectAll() else vm.selectProvider(key)
                    },
                    onDone = { keys ->
                        showProviders = false
                        vm.setProviders(keys)
                    },
                    onDismiss = { showProviders = false },
                )
            }
        }

        // ---- Optional Result filters (Hidden by default; enabled via Settings > App Layout) ----
        if (showFilters) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                item {
                    FilterChip(
                        selected = kindFilter == SearchKindFilter.ALL,
                        onClick = { kindFilterKey = SearchKindFilter.ALL.key },
                        label = { Text(tr("Both")) },
                        shape = RoundedCornerShape(24.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = kindFilter == SearchKindFilter.MOVIES,
                        onClick = { kindFilterKey = SearchKindFilter.MOVIES.key },
                        label = { Text(tr("Movies")) },
                        shape = RoundedCornerShape(24.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = kindFilter == SearchKindFilter.SERIES,
                        onClick = { kindFilterKey = SearchKindFilter.SERIES.key },
                        label = { Text(tr("Series")) },
                        shape = RoundedCornerShape(24.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = kindFilter == SearchKindFilter.ANIME,
                        onClick = { kindFilterKey = SearchKindFilter.ANIME.key },
                        label = { Text(tr("Anime")) },
                        shape = RoundedCornerShape(24.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                }
                item {
                    val picked = when {
                        yearsFilter.isEmpty() -> tr("Any year")
                        yearsFilter.size <= 3 ->
                            yearsFilter.sortedDescending().joinToString(", ")
                        else -> tr("%s years").replace("%s", yearsFilter.size.toString())
                    }
                    FilterChip(
                        selected = yearsFilter.isNotEmpty(),
                        onClick = { setYears(emptySet()) },
                        label = { Text(if (yearsFilter.isEmpty()) picked else "$picked ✕") },
                        shape = RoundedCornerShape(24.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp)
                    .height(34.dp),
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        YearChip(
                            label = tr("Any"),
                            selected = yearsFilter.isEmpty(),
                            onClick = { setYears(emptySet()) },
                        )
                    }
                    items(SEARCH_YEARS) { year ->
                        YearChip(
                            label = year.toString(),
                            selected = year in yearsFilter,
                            onClick = { toggleYear(year) },
                        )
                    }
                }
            }
            var genreQuery by rememberSaveable { mutableStateOf("") }
            val genreChips = remember(genreQuery) {
                val q = genreQuery.trim().lowercase()
                if (q.isEmpty()) SEARCH_GENRES
                else SEARCH_GENRES.filter { g ->
                    g.lowercase().contains(q) || I18n.t(g).lowercase().contains(q)
                }
            }
            val genreStripState = rememberLazyListState()
            LaunchedEffect(genreQuery) {
                if (genreChips.isNotEmpty()) genreStripState.scrollToItem(0)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        tr("Genre"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (genreFilter.isEmpty()) tr("Any genre")
                        else I18n.t("%s picked").replace("%s", genreFilter.size.toString()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (genreFilter.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                GlassSearchField(
                    value = genreQuery,
                    onValueChange = { genreQuery = it },
                    placeholder = tr("Search genres"),
                    height = 36.dp,
                    modifier = Modifier.width(172.dp),
                )
            }
            if (genreChips.isEmpty()) {
                Text(
                    tr("No genre matches") + " \u201c${genreQuery.trim()}\u201d",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp)
                        .height(34.dp),
                ) {
                    LazyRow(
                        state = genreStripState,
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        item {
                            GenreChip(
                                label = tr("Any"),
                                selected = genreFilter.isEmpty(),
                                onClick = { setGenres(emptySet()) },
                            )
                        }
                        items(genreChips) { genre ->
                            GenreChip(
                                label = trTag(genre),
                                selected = genre.trim().lowercase() in genreFilter,
                                onClick = { toggleGenre(genre) },
                            )
                        }
                    }
                }
            }
            if ((filterOn || activeEngine.isNotBlank()) && results.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                    Text(
                        if (visible.isEmpty()) {
                            tr("Nothing in these results matches the filter.")
                        } else {
                            tr("%s shown").replace("%s", visible.size.toString()) +
                                " · " +
                                tr("%s found").replace("%s", results.size.toString())
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (visible.isNotEmpty() && keptUnknownKind > 0) {
                        Text(
                            tr("%s have no year or kind, so they are kept")
                                .replace("%s", keptUnknownKind.toString()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (hiddenNoYear > 0) {
                        Text(
                            tr("%s have no year, so they are hidden")
                                .replace("%s", hiddenNoYear.toString()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (hiddenNoGenre > 0) {
                        Text(
                            tr("%s have no genre, so they are hidden")
                                .replace("%s", hiddenNoGenre.toString()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (searching) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        // Expanded provider state for "See all >" full grid view
        var expandedProviderId by rememberSaveable(query) { mutableStateOf<String?>(null) }
        BackHandler(enabled = expandedProviderId != null) {
            expandedProviderId = null
        }

        // ---- Live Suggestions strip while typing ----
        val liveSuggestions = remember(query, visible) {
            val q = query.trim()
            if (q.length < 2 || visible.isEmpty()) emptyList()
            else visible.map { it.title.trim() }
                .filter { it.isNotBlank() && !it.equals(q, ignoreCase = true) }
                .distinct()
                .take(8)
        }
        if (liveSuggestions.isNotEmpty() && expandedProviderId == null) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            ) {
                items(liveSuggestions, key = { "sug:$it" }) { suggestion ->
                    Surface(
                        onClick = {
                            translatedFrom = null
                            vm.setQuery(suggestion)
                        },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                suggestion,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 180.dp),
                            )
                        }
                    }
                }
            }
        }

        if (query.isBlank() && results.isEmpty() && collectionHits.isEmpty()) {
            EmptyState(
                title = tr("Search"),
                subtitle = tr("Type something to search across every provider."),
                actionLabel = null,
                action = null
            )
        } else if (!searching && results.isEmpty() && collectionHits.isEmpty()) {
            EmptyState(
                title = tr("No results"),
                subtitle = I18n.t("Nothing matched \"$query\". Try a different title, or deselect providers in the row above."),
                actionLabel = null,
                action = null
            )
        } else {
            val namesById = remember(providers) {
                providers.associateBy({ it.config.id }, { it.config.name })
            }
            Column(Modifier.fillMaxSize()) {
                if (engineCounts.size >= 2 || activeEngine.isNotBlank()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        item {
                            EngineChip(
                                label = tr("All") + " · " + filtered.size,
                                selected = activeEngine.isBlank(),
                                onClick = { engineKey = "" },
                            )
                        }
                        items(engineCounts.entries.toList(), key = { it.key }) { entry ->
                            EngineChip(
                                label = entry.key + " · " + entry.value,
                                selected = activeEngine == entry.key,
                                onClick = {
                                    engineKey = if (activeEngine == entry.key) "" else entry.key
                                },
                            )
                        }
                    }
                }
                if (collectionHits.isNotEmpty() && expandedProviderId == null) {
                    CollectionHitsRow(
                        if (filterOn) {
                            collectionHits.filter {
                                it.item.passesSearchFilter(kindFilter, yearsFilter, genreFilter)
                            }
                        } else {
                            collectionHits
                        }
                    ) { hit ->
                        Routes.safeNavigate(
                            nav,
                            Routes.detail(
                                hit.item.providerId,
                                hit.item.type,
                                hit.item.id,
                                hit.item.title,
                                hit.item.posterUrl,
                                hit.item.rawType,
                            )
                        )
                    }
                }

                val gridItems = rememberVisibleItems(visible)
                val groupedByProvider = remember(gridItems) {
                    gridItems.groupBy { it.providerId }.entries.toList()
                }
                val style = rememberPosterStyle()
                val tvSearch = TvMode.current()

                // ---- Provider-wise Horizontal Rows with "See all >" ----
                if (expandedProviderId == null && selected.size != 1 && groupedByProvider.size > 1) {
                    LazyColumn(
                        state = com.hikari.app.ui.components.keptListState("searchRows:$query|$activeEngine"),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            top = 8.dp + tvEdge,
                            bottom = LocalTaskbarInset.current + 16.dp + tvEdge +
                                if (tvSearch) 28.dp else 0.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        items(groupedByProvider, key = { "prov-row:" + it.key }) { (provId, provItems) ->
                            val provName = namesById[provId] ?: provId
                            Column(Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp + tvEdge, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = provName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(
                                        onClick = { expandedProviderId = provId },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = tr("See all") + " (${provItems.size}) ›",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp + tvEdge),
                                    horizontalArrangement = Arrangement.spacedBy(if (tvSearch) 14.dp else 10.dp),
                                ) {
                                    items(provItems, key = { it.uniqueId }) { item ->
                                        val go = {
                                            Routes.safeNavigate(
                                                nav,
                                                if (item.rawType == "manga") Routes.mangaRoute(item.providerId, item.id, item.title, item.posterUrl)
                                                else Routes.detail(item.providerId, item.type, item.id, item.title, item.posterUrl, item.rawType)
                                            )
                                        }
                                        if (tvSearch) {
                                            Box(Modifier.width(if (style.isCinema(true)) 240.dp else 148.dp)) {
                                                if (style.isCinema(true)) {
                                                    com.hikari.app.tv.TvCinemaCard(item = item, onClick = go)
                                                } else {
                                                    com.hikari.app.tv.TvVerticalCard(item = item, onClick = go)
                                                }
                                            }
                                        } else {
                                            val badge = rememberPosterScore(item, style)
                                            val phoneCinema = style.isCinema(false)
                                            val hero = if (phoneCinema) Artwork.heroModel(item) else null
                                            Column(
                                                Modifier
                                                    .width(if (phoneCinema) 190.dp else 116.dp)
                                                    .clickable { go() }
                                            ) {
                                                Box(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(if (phoneCinema) 16f / 9f else 2f / 3f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                ) {
                                                    AsyncImage(
                                                        model = if (phoneCinema) hero?.first else Artwork.model(item),
                                                        contentDescription = item.title,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop,
                                                        alignment = if (phoneCinema) com.hikari.app.ui.Artwork.CINEMA_ALIGNMENT else Alignment.Center
                                                    )
                                                    if (badge != null) {
                                                        RatingBadge(
                                                            text = badge,
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(4.dp),
                                                        )
                                                    }
                                                }
                                                Text(
                                                    item.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val activeGridItems = remember(gridItems, expandedProviderId) {
                        if (expandedProviderId == null) gridItems
                        else gridItems.filter { it.providerId == expandedProviderId }
                    }
                    if (expandedProviderId != null) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp + tvEdge, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { expandedProviderId = null }) {
                                Text("‹ " + tr("All providers"))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = namesById[expandedProviderId] ?: expandedProviderId.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    val searchKeys = remember(activeGridItems) { activeGridItems.map { it.uniqueId } }
                    val searchCols = if (tvSearch) 3 else TvUi.resultsColumns()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(searchCols),
                        state = com.hikari.app.ui.components.keptGridState(
                            "search:$query|$activeEngine|${expandedProviderId.orEmpty()}",
                            itemKeys = searchKeys,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = 10.dp + tvEdge,
                            end = 10.dp + tvEdge,
                            top = 8.dp + tvEdge,
                            bottom = LocalTaskbarInset.current + 12.dp + tvEdge +
                                if (TvMode.current()) 28.dp else 0.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(if (tvSearch) 14.dp else 8.dp),
                        verticalArrangement = Arrangement.spacedBy(if (tvSearch) 20.dp else 10.dp)
                    ) {
                        items(activeGridItems, key = { it.uniqueId }) { item ->
                            if (tvSearch) {
                                val go = {
                                    Routes.safeNavigate(
                                        nav,
                                        if (item.rawType == "manga") Routes.mangaRoute(item.providerId, item.id, item.title, item.posterUrl)
                                        else Routes.detail(item.providerId, item.type, item.id, item.title, item.posterUrl, item.rawType)
                                    )
                                }
                                if (style.isCinema(true)) {
                                    com.hikari.app.tv.TvCinemaCard(item = item, onClick = go)
                                } else {
                                    com.hikari.app.tv.TvVerticalCard(item = item, onClick = go)
                                }
                            } else {
                                val badge = rememberPosterScore(item, style)
                                Column(
                                    Modifier
                                        .clickable {
                                            Routes.safeNavigate(
                                                nav,
                                                if (item.rawType == "manga") {
                                                    Routes.mangaRoute(
                                                        item.providerId, item.id, item.title, item.posterUrl
                                                    )
                                                } else {
                                                    Routes.detail(item.providerId, item.type, item.id, item.title, item.posterUrl, item.rawType)
                                                }
                                            )
                                        }
                                ) {
                                    val phoneCinema = style.isCinema(false)
                                    val hero = if (phoneCinema) Artwork.heroModel(item) else null
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(if (phoneCinema) 16f / 9f else 2f / 3f)
                                            .clip(RoundedCornerShape(10.dp))
                                    ) {
                                        if (phoneCinema && hero != null && !hero.second) {
                                            AsyncImage(
                                                model = hero.first,
                                                contentDescription = item.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                alignment = com.hikari.app.ui.Artwork.CINEMA_ALIGNMENT
                                            )
                                        } else {
                                            AsyncImage(
                                                model = if (phoneCinema) hero?.first else Artwork.model(item),
                                                contentDescription = item.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                alignment = if (phoneCinema) com.hikari.app.ui.Artwork.CINEMA_ALIGNMENT else Alignment.Center
                                            )
                                        }
                                        if (badge != null) {
                                            RatingBadge(
                                                text = badge,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = if (TvMode.current()) 1 else 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    if (!TvMode.current()) namesById[item.providerId]?.let { name ->
                                        Text(
                                            name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
private fun CollectionHitsRow(
    hits: List<com.hikari.app.data.CollectionsRepository.CollectionHit>,
    onOpen: (com.hikari.app.data.CollectionsRepository.CollectionHit) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            tr("From your collections"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(hits.distinctBy { it.item.uniqueId + "|" + it.label }, key = { it.item.uniqueId + "|" + it.label }) { hit ->
                Column(
                    Modifier
                        .width(104.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpen(hit) }
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        AsyncImage(
                            model = Artwork.model(hit.item),
                            contentDescription = hit.item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Text(
                        hit.item.title,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        hit.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private enum class SearchKindFilter(val key: String) {
    ALL("all"),
    MOVIES("movies"),
    SERIES("series"),
    ANIME("anime");

    companion object {
        fun fromKey(key: String?): SearchKindFilter =
            entries.firstOrNull { it.key == key } ?: ALL
    }
}

private val SEARCH_GENRES: List<String> = com.hikari.app.data.Genres.ALL

fun MediaItem.looksAnime(): Boolean {
    if (rawType.equals("anime", true)) return true
    if (genres.any { g ->
        val t = g.trim().lowercase()
        t == "anime" || t == "animation" || t == "animated" || t == "cartoon" ||
            t.contains("anime") || t.contains("cartoon")
    }) return true
    val p = providerId.trim().lowercase()
    if (p.startsWith("aniyomi|")) return true
    if (p.contains("anime")) return true
    return p.contains("/anime/") || p.contains("|anime|") || p.endsWith("|anime")
}

private val SEARCH_YEARS: List<Int> = run {
    val thisYear = runCatching {
        java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    }.getOrDefault(2025)
    ((thisYear + 1) downTo 1950).toList()
}

@Composable
private fun YearChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
        else null,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun GenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
        else null,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun EngineChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
        else null,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun HistoryChip(
    text: String,
    onPick: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        onClick = onPick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 170.dp),
            )
            Box(
                Modifier
                    .padding(start = 2.dp)
                    .size(26.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = I18n.t("Forget \"%s\"").replace("%s", text),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun ProviderScopeButton(
    label: String,
    supporting: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    supporting,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = tr("Choose providers"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun MediaItem.passesSearchFilter(
    kind: SearchKindFilter,
    years: Set<Int>,
    genres: Set<String>,
): Boolean {
    if (years.isNotEmpty() && (year == null || year !in years)) return false
    if (genres.isNotEmpty() && genres.none { q -> this.genres.any { g -> com.hikari.app.data.Genres.matches(g, q) } }) return false
    return when (kind) {
        SearchKindFilter.ALL -> true
        SearchKindFilter.MOVIES -> type != MediaType.SERIES && !looksAnime()
        SearchKindFilter.SERIES -> type != MediaType.MOVIE && !looksAnime()
        SearchKindFilter.ANIME -> looksAnime()
    }
}

private fun MediaItem.unknownKindKept(
    kind: SearchKindFilter,
    years: Set<Int>,
    genres: Set<String>,
): Boolean {
    if (kind == SearchKindFilter.ALL) return false
    return type == MediaType.UNKNOWN && passesSearchFilter(kind, years, genres)
}
