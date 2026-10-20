package com.hikari.app.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.hikari.app.data.MediaItem
import com.hikari.app.data.MediaType
import com.hikari.app.i18n.tr
import com.hikari.app.ui.Artwork
import com.hikari.app.ui.components.PosterImage

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
        TvHeroFocusState.focusedItem = null
    }
    if (items.isEmpty() && focusedItem == null) return
    var index by remember(items) { mutableIntStateOf(0) }
    val item = focusedItem ?: items[index.coerceIn(items.indices)]
    val art = remember(item) { Artwork.heroModel(item) }
    val scheme = MaterialTheme.colorScheme
    val bg = scheme.background

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val h = ((((maxHeight * 0.42f).value * scale.coerceIn(0.5f, 1.5f)).coerceIn(190f, 480f)) *
            com.hikari.app.ui.HeaderPrefs.heightFactor).coerceIn(160f, 640f).dp

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

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to bg,
                            0.32f to bg.copy(alpha = 0.94f),
                            0.58f to bg.copy(alpha = 0.45f),
                            0.85f to Color.Transparent,
                        )
                    )
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to bg.copy(alpha = 0.35f),
                            0.20f to Color.Transparent,
                            0.65f to Color.Transparent,
                            1f to bg,
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.56f)
                    .padding(start = 24.dp, end = 12.dp, bottom = 14.dp),
            ) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 38.sp,
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    tvHeroMeta(item),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val typeBadge = when (item.type) {
                        MediaType.SERIES -> "SERIES"
                        MediaType.MOVIE -> "MOVIE"
                        else -> item.rawType.uppercase().ifBlank { "MEDIA" }
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.38f)),
                    ) {
                        Text(
                            typeBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }

                    item.rating?.takeIf { it > 0 }?.let { r ->
                        TvHeroScoreChip("%.1f".format(r))
                    }
                }

                if (item.overview?.isNotBlank() == true) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        item.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.80f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 19.sp,
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { onOpen(item) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Play"), fontWeight = FontWeight.Bold)
                    }
                    if (onToggleBookmark != null) {
                        val saved = item.uniqueId in bookmarkedIds
                        OutlinedButton(
                            onClick = { onToggleBookmark(item) },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White,
                            ),
                            border = BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.45f),
                            ),
                            contentPadding = PaddingValues(
                                horizontal = 14.dp,
                                vertical = 8.dp,
                            ),
                        ) {
                            Icon(
                                if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = tr("Bookmark"),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            if (focusedItem == null && items.size > 1) {
                TvHeroDots(
                    count = items.size,
                    active = index,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 18.dp),
                )
            }
        }
    }
}

private fun tvHeroMeta(item: MediaItem): String {
    val parts = ArrayList<String>(5)
    val kind = when (item.type) {
        MediaType.SERIES -> "Series"
        MediaType.MOVIE -> "Movie"
        else -> item.rawType.replaceFirstChar { it.uppercase() }.ifBlank { "Media" }
    }
    parts += kind
    parts += item.genres.take(3)
    item.year?.let { parts += it.toString() }
    return parts.joinToString(" • ")
}

@Composable
private fun TvHeroScoreChip(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
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
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun TvHeroDots(count: Int, active: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until count.coerceAtMost(8)) {
            Box(
                Modifier
                    .size(width = if (i == active) 18.dp else 6.dp, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (i == active) Color.White
                        else Color.White.copy(alpha = 0.4f)
                    )
            )
        }
    }
}
