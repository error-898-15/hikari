package com.hikari.app.tv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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
    val status: String?,
)

private object TvHeroEnrichCache {
    val map = ConcurrentHashMap<String, EnrichedHeroData>()
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
            val finalOverview = ex?.overview?.takeIf { it.isNotBlank() } ?: mergedItem.overview
            val data = EnrichedHeroData(
                item = mergedItem.copy(overview = finalOverview),
                logoUrl = logo,
                status = ex?.details?.status,
            )
            TvHeroEnrichCache.map[rawItem.uniqueId] = data
            enriched = data
        }
    }

    val item = enriched?.item ?: rawItem
    val logoUrl = if (TvSidebarPrefs.hideTitleLogo) null else enriched?.logoUrl
    val bg = MaterialTheme.colorScheme.background

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val h = ((((maxHeight * 0.48f).value * scale.coerceIn(0.5f, 1.5f)).coerceIn(220f, 520f)) *
            com.hikari.app.ui.HeaderPrefs.heightFactor).coerceIn(200f, 640f).dp

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
            // Sliding Backdrop + Content (1000263555.jpg style)
            AnimatedContent(
                targetState = item,
                transitionSpec = {
                    (slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { w -> w / 10 } +
                        fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(tween(220)) { w -> -w / 10 } +
                            fadeOut(tween(180)))
                },
                label = "tv-hero-slide",
            ) { current ->
                val heroArt = remember(current) { Artwork.heroModel(current) }
                val posterArt = remember(current) { Artwork.model(current) }

                Box(Modifier.fillMaxSize()) {
                    // Full-bleed Backdrop Artwork
                    PosterImage(
                        model = heroArt.first,
                        contentDescription = current.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = Artwork.CINEMA_ALIGNMENT,
                    )

                    // Smooth Horizontal Gradient
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0f to bg.copy(alpha = 0.92f),
                                    0.42f to bg.copy(alpha = 0.68f),
                                    0.78f to bg.copy(alpha = 0.20f),
                                    1f to Color.Transparent,
                                )
                            )
                    )

                    // Smooth Bottom Gradient
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to bg.copy(alpha = 0.25f),
                                    0.45f to Color.Transparent,
                                    1f to bg,
                                )
                            )
                    )

                    // Left 2:3 Poster + Right Details Row (1000263555.jpg style)
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(0.78f)
                            .padding(start = 24.dp, end = 16.dp, bottom = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 2:3 Portrait Poster with Thin White Borderline
                        Box(
                            modifier = Modifier
                                .width(116.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = 1.2.dp,
                                    color = Color.White.copy(alpha = 0.70f),
                                    shape = RoundedCornerShape(12.dp),
                                ),
                        ) {
                            AsyncImage(
                                model = posterArt ?: heroArt.first,
                                contentDescription = current.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        Spacer(Modifier.width(22.dp))

                        // Title, Genre Pills, Overview & "Click to view details"
                        Column(modifier = Modifier.weight(1f)) {
                            // Top white badge ("FEATURED" / "NEW EPISODE")
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(4.dp),
                            ) {
                                val badgeLabel = when {
                                    TvHeroFocusState.isContinueWatching -> "CONTINUE WATCHING"
                                    current.type == MediaType.SERIES -> "SERIES"
                                    else -> "FEATURED"
                                }
                                Text(
                                    text = badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                            }

                            if (current.originalTitle.isNotBlank() &&
                                !current.originalTitle.equals(current.title, ignoreCase = true)
                            ) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = current.originalTitle.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            if (!logoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = PosterLoader.model(logoUrl),
                                    contentDescription = current.title,
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.CenterStart,
                                    modifier = Modifier
                                        .fillMaxWidth(0.65f)
                                        .heightIn(max = 76.dp),
                                )
                            } else {
                                Text(
                                    text = current.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 32.sp,
                                )
                            }

                            // Genre Pills Row (Adventure, Comedy, Fantasy, Slice of Life)
                            val pills = remember(current) {
                                buildList {
                                    addAll(current.genres.take(4))
                                    if (isEmpty()) {
                                        current.year?.let { add(it.toString()) }
                                    }
                                }
                            }
                            if (pills.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    pills.forEach { genre ->
                                        Surface(
                                            color = Color.White.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(50),
                                        ) {
                                            Text(
                                                text = genre,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            if (!current.overview.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = current.overview,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 17.sp,
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Click to view details",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }

            // Bottom-Center Carousel Dots (1000263555.jpg & Video 00:00 style)
            if (items.size > 1) {
                val activeDot = items.indexOfFirst { it.uniqueId == item.uniqueId }
                    .takeIf { it >= 0 } ?: index
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (i in 0 until items.size.coerceAtMost(8)) {
                        Box(
                            Modifier
                                .size(width = if (i == activeDot) 18.dp else 6.dp, height = 5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (i == activeDot) Color.White
                                    else Color.White.copy(alpha = 0.38f)
                                )
                        )
                    }
                }
            }
        }
    }
}
