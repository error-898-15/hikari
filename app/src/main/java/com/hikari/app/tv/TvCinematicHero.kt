package com.hikari.app.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hikari.app.HikariApp
import com.hikari.app.data.ContentRepository
import com.hikari.app.data.MediaItem
import com.hikari.app.data.MediaType
import com.hikari.app.data.TmdbMeta
import com.hikari.app.ui.Artwork
import com.hikari.app.ui.PosterLoader
import com.hikari.app.ui.components.PosterImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private data class EnrichedHeroData(
    val item: MediaItem,
    val logoUrl: String?,
    val certification: String?,
    val status: String?,
    val runtimeMinutes: Int?,
    val releaseDate: String?,
    val rating: Double?,
)

private object TvHeroEnrichCache {
    val map = ConcurrentHashMap<String, EnrichedHeroData>()
}

private fun Any?.readHeroProp(vararg names: String): String? {
    if (this == null) return null
    for (n in names) {
        val v = runCatching {
            val getter = "get" + n.replaceFirstChar { it.uppercase() }
            val m = this.javaClass.methods.firstOrNull {
                it.name.equals(getter, ignoreCase = true) && it.parameterCount == 0
            }
            m?.invoke(this)
        }.getOrNull()
        if (v is String && v.isNotBlank()) return v.trim()
    }
    return null
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun TvCinematicHero(
    items: List<MediaItem>,
    onOpen: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    bookmarkedIds: Set<String> = emptySet(),
    onToggleBookmark: ((MediaItem) -> Unit)? = null,
    focusedItem: MediaItem? = TvHeroFocusState.focusedItem,
) {
    LaunchedEffect(items) {
        if (TvHeroFocusState.focusedItem == null && items.isNotEmpty()) {
            TvHeroFocusState.focusedItem = items.first()
        }
    }
    if (items.isEmpty() && focusedItem == null) return
    var index by remember(items) { mutableIntStateOf(0) }
    val rawItem = focusedItem ?: items[index.coerceIn(items.indices)]

    var enriched by remember(rawItem.uniqueId) {
        mutableStateOf(TvHeroEnrichCache.map[rawItem.uniqueId])
    }

    LaunchedEffect(rawItem.uniqueId) {
        val cached = TvHeroEnrichCache.map[rawItem.uniqueId]
        if (cached != null) {
            enriched = cached
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            val repo = runCatching { ContentRepository(HikariApp.instance.providers) }.getOrNull()
            val fullMeta = runCatching { repo?.metaFor(rawItem) }.getOrNull() ?: rawItem
            val mergedItem = fullMeta.copy(
                title = rawItem.title.ifBlank { fullMeta.title },
                posterUrl = fullMeta.posterUrl ?: rawItem.posterUrl,
                backdropUrl = fullMeta.backdropUrl ?: rawItem.backdropUrl,
                overview = fullMeta.overview?.ifBlank { rawItem.overview } ?: rawItem.overview,
                genres = if (fullMeta.genres.isNotEmpty()) fullMeta.genres else rawItem.genres,
                year = fullMeta.year ?: rawItem.year,
                rating = fullMeta.rating ?: rawItem.rating,
            )
            val logo = runCatching { TmdbMeta.logo(mergedItem) }.getOrNull()
            val ex = runCatching { TmdbMeta.extras(mergedItem) }.getOrNull()
            val det = ex?.details
            val finalOverview = ex?.overview?.takeIf { it.isNotBlank() } ?: mergedItem.overview
            val finalRating = det?.rating?.takeIf { it > 0.0 } ?: mergedItem.rating
            val data = EnrichedHeroData(
                item = mergedItem.copy(overview = finalOverview, rating = finalRating),
                logoUrl = logo,
                certification = det?.certification?.trim()?.takeIf { it.isNotBlank() },
                status = det.readHeroProp("status", "releaseStatus"),
                runtimeMinutes = det?.runtimeMinutes?.takeIf { it > 0 },
                releaseDate = det.readHeroProp("releaseDate", "released", "firstAirDate"),
                rating = finalRating,
            )
            TvHeroEnrichCache.map[rawItem.uniqueId] = data
            enriched = data
        }
    }

    val item = enriched?.item ?: rawItem
    val logoUrl = enriched?.logoUrl
    val art = remember(item) { Artwork.heroModel(item) }
    val bg = MaterialTheme.colorScheme.background

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val h = ((((maxHeight * 0.46f).value * scale.coerceIn(0.5f, 1.5f)).coerceIn(210f, 500f)) *
            com.hikari.app.ui.HeaderPrefs.heightFactor).coerceIn(180f, 640f).dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(h)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown ||
                        event.nativeKeyEvent.repeatCount != 0) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            TvHeroFocusState.focusedItem = null
                            if (items.isNotEmpty()) index = (index - 1 + items.size) % items.size
                            true
                        }
                        Key.DirectionRight -> {
                            TvHeroFocusState.focusedItem = null
                            if (items.isNotEmpty()) index = (index + 1) % items.size
                            true
                        }
                        else -> false
                    }
                }
                .clickable { onOpen(item) },
        ) {
            // ডান পাশে ওয়াইড ব্যাকড্রপ ইমেজ (1000263428.jpg স্টাইল)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.76f)
                    .fillMaxHeight()
                    .align(Alignment.TopEnd)
            ) {
                PosterImage(
                    model = art.first,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Artwork.CINEMA_ALIGNMENT,
                )
            }

            // বাম দিক থেকে ডানে স্মুথ ডার্ক গ্র্যাডিয়েন্ট
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to bg,
                            0.34f to bg.copy(alpha = 0.95f),
                            0.60f to bg.copy(alpha = 0.45f),
                            0.88f to Color.Transparent,
                        )
                    )
            )

            // নিচের দিকে স্মুথ ভার্টিকাল ফেড গ্র্যাডিয়েন্ট
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to bg.copy(alpha = 0.30f),
                            0.25f to Color.Transparent,
                            0.68f to Color.Transparent,
                            1f to bg,
                        )
                    )
            )

            // বাম পাশের তথ্য (Title Logo -> Meta -> Badges -> Overview)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.56f)
                    .padding(start = 26.dp, end = 12.dp, bottom = 10.dp),
            ) {
                if (!logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = PosterLoader.model(logoUrl),
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .heightIn(max = 88.dp),
                    )
                } else {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 36.sp,
                    )
                }

                Spacer(Modifier.height(10.dp))

                // মেটাডেটা লাইন ১: Movie • Romance • 1h 52m • 23 September 2026
                Text(
                    buildHeroMetaLine(
                        item = item,
                        runtimeMinutes = enriched?.runtimeMinutes,
                        releaseDate = enriched?.releaseDate,
                        episodeSubtitle = TvHeroFocusState.focusedSubtitle,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.88f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(8.dp))

                // ব্যাজ সারি: [ R | RELEASED ] বা [ NEXT UP ] + হলুদ IMDb রেটিং
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val badgeText = when {
                        TvHeroFocusState.isContinueWatching -> "NEXT UP"
                        !enriched?.certification.isNullOrBlank() && !enriched?.status.isNullOrBlank() ->
                            "${enriched?.certification}  |  ${enriched?.status?.uppercase()}"
                        !enriched?.certification.isNullOrBlank() ->
                            "${enriched?.certification}  |  " + (if (item.type == MediaType.SERIES) "ONGOING" else "RELEASED")
                        !enriched?.status.isNullOrBlank() ->
                            enriched!!.status!!.uppercase()
                        item.type == MediaType.SERIES -> "ONGOING"
                        else -> "RELEASED"
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f)),
                    ) {
                        Text(
                            badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }

                    val score = enriched?.rating ?: item.rating
                    score?.takeIf { it > 0 }?.let { r ->
                        TvHeroScoreChip("%.1f".format(r))
                    }
                }

                if (!item.overview.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        item.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}

private fun buildHeroMetaLine(
    item: MediaItem,
    runtimeMinutes: Int?,
    releaseDate: String?,
    episodeSubtitle: String?,
): String {
    val parts = ArrayList<String>(6)
    if (!episodeSubtitle.isNullOrBlank()) {
        parts += episodeSubtitle
    } else {
        val kind = when (item.type) {
            MediaType.SERIES -> "Series"
            MediaType.MOVIE -> "Movie"
            else -> item.rawType.replaceFirstChar { it.uppercase() }.ifBlank { "Movie" }
        }
        parts += kind
    }
    parts += item.genres.take(2)
    runtimeMinutes?.takeIf { it > 0 }?.let { mins ->
        parts += if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
    }
    val dateOrYear = releaseDate?.takeIf { it.isNotBlank() } ?: item.year?.toString()
    if (!dateOrYear.isNullOrBlank()) {
        parts += if (item.type == MediaType.SERIES && dateOrYear.length == 4) "$dateOrYear–" else dateOrYear
    }
    return parts.joinToString(" • ")
}

@Composable
private fun TvHeroScoreChip(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            color = Color(0xFFF5C518),
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(
                "IMDb",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
