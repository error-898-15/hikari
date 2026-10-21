package com.hikari.app.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hikari.app.data.CastMember
import com.hikari.app.data.MediaItem
import com.hikari.app.data.MediaType
import com.hikari.app.data.TitleExtras
import com.hikari.app.i18n.tr
import com.hikari.app.ui.PosterLoader
import kotlinx.coroutines.delay

/**
 * The television title page's header, in the layout of the reference client:
 *
 *   title logo (or the name)
 *   [ ▶ Play ]  ( + )  ( trailer )  ( download )
 *   Director: …
 *   the plot (three lines)
 *   genres  •  release date  •  TMDB 8.1
 *   [ R | RELEASED ]  •  1h 52m  •  United States  •  EN
 *
 * The artwork itself is the page's fixed backdrop, so this only draws the
 * text block, anchored bottom-left. The remote starts on Play ([playFocus]).
 */
@Composable
fun TvDetailHero(
    artTitle: String,
    heroLogo: String?,
    overview: String?,
    meta: MediaItem?,
    extras: TitleExtras?,
    playLabel: String,
    isSaved: Boolean,
    playFocus: FocusRequester,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onSave: () -> Unit,
    onTrailer: (() -> Unit)?,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        if (!TvMode.isTv) return@LaunchedEffect
        delay(200L)
        try {
            playFocus.requestFocus()
        } catch (t: Throwable) {
            // not attached yet — the remote can still walk to Play
        }
    }
    val details = extras?.details
    val director = details?.director?.trim()?.takeIf { it.isNotEmpty() }
    val genres = meta?.genres.orEmpty().take(3)
    val dateText = formatTvReleaseDate(details?.releaseDate)
        ?: (details?.year ?: meta?.year)?.toString()
    val score = details?.rating?.takeIf { it > 0.0 } ?: meta?.rating?.takeIf { it > 0.0 }
    val status = details?.status?.trim()?.takeIf { it.isNotEmpty() }?.uppercase()
        ?: if (meta?.type == MediaType.SERIES) "ONGOING" else "RELEASED"
    val cert = details?.certification?.trim()?.takeIf { it.isNotEmpty() }
    val runtime = details?.runtimeMinutes?.takeIf { it > 0 }?.let { mins ->
        if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
    }
    val country = details?.country?.trim()?.takeIf { it.isNotEmpty() }
    val language = details?.language?.trim()?.takeIf { it.isNotEmpty() }?.uppercase()

    Box(modifier) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(18.dp)
                .tvPress(previewPass = true, onClick = onBack),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = tr("Back"),
                tint = Color.White,
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.58f)
                .padding(start = 40.dp, end = 8.dp, bottom = 28.dp),
        ) {
            // 1. Title logo, or the name in big type when there is no logo.
            if (!heroLogo.isNullOrBlank()) {
                AsyncImage(
                    model = PosterLoader.model(heroLogo),
                    contentDescription = artTitle,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .heightIn(max = 100.dp),
                )
            } else {
                Text(
                    artTitle,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(14.dp))

            // 2. Play · save · trailer · download.
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TvPlayPill(label = playLabel, focusRequester = playFocus, onClick = onPlay)
                TvRoundAction(
                    icon = if (isSaved) Icons.Filled.Check else Icons.Filled.Add,
                    description = tr("Save"),
                    highlighted = isSaved,
                    onClick = onSave,
                )
                if (onTrailer != null) {
                    TvRoundAction(
                        icon = Icons.Filled.OndemandVideo,
                        description = tr("Trailer"),
                        onClick = onTrailer,
                    )
                }
                TvRoundAction(
                    icon = Icons.Filled.Download,
                    description = tr("Download"),
                    onClick = onDownload,
                )
            }
            Spacer(Modifier.height(14.dp))

            // 3. Director.
            if (director != null) {
                Text(
                    tr("Director") + ": " + director,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
            }

            // 4. Plot.
            if (!overview.isNullOrBlank()) {
                Text(
                    overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(12.dp))
            }

            // 5. Genres • date • score.
            val line1 = buildList {
                if (genres.isNotEmpty()) add(genres.joinToString(" \u2022 "))
                if (!dateText.isNullOrBlank()) add(dateText)
            }.joinToString("   \u2022   ")
            if (line1.isNotBlank() || score != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (line1.isNotBlank()) {
                        Text(
                            line1,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    if (score != null) {
                        if (line1.isNotBlank()) {
                            Text("\u2022", color = Color.White.copy(alpha = 0.65f), fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            color = Color(0xFF01B4E4).copy(alpha = 0.22f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color(0xFF01B4E4).copy(alpha = 0.65f)),
                        ) {
                            Text(
                                "TMDB",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF5CE1E6),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                        Text(
                            "%.1f".format(score),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // 6. [ R | RELEASED ] • runtime • country • language.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f)),
                ) {
                    Text(
                        if (cert != null) "$cert  |  $status" else status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                val line2 = listOfNotNull(runtime, country, language).joinToString("   \u2022   ")
                if (line2.isNotBlank()) {
                    Text(
                        "\u2022   $line2",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
    }
}

/** The wide white Play pill. White with a ring of the theme colour when the remote is on it. */
@Composable
private fun TvPlayPill(
    label: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .height(44.dp)
            .clip(shape)
            .background(Color.White)
            .border(
                width = if (focused) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = shape,
            )
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .tvPress(previewPass = true, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            maxLines = 1,
        )
    }
}

/** A round icon button: frosted while idle, solid white with a dark icon when the remote is on it. */
@Composable
private fun TvRoundAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val solid = focused || highlighted
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (solid) Color.White else Color.White.copy(alpha = 0.16f))
            .border(
                width = if (focused) 2.5.dp else 1.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                shape = CircleShape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .tvPress(previewPass = true, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (solid) Color.Black else Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * "Creator and Cast": a row of round photos with the name and the role under
 * each, the director first. An anime page passes [characters] = true and the
 * row says "Characters" instead. [onTrailer], when there is a trailer, adds a
 * "Trailer" entry beside the heading.
 */
@Composable
fun TvDetailCastRow(
    cast: List<CastMember>,
    characters: Boolean,
    director: String?,
    onCastClick: (CastMember) -> Unit,
    onTrailer: (() -> Unit)?,
) {
    val people = remember(cast, director, characters) {
        val list = ArrayList<CastMember>(cast.size + 1)
        val boss = director?.trim()?.takeIf { it.isNotEmpty() }
        if (!characters && boss != null && cast.none { it.name.equals(boss, ignoreCase = true) }) {
            list += CastMember(name = boss, character = "Director")
        }
        list.addAll(cast)
        list
    }
    if (people.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 8.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 40.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                tr(if (characters) "Characters" else "Creator and Cast"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            if (onTrailer != null) {
                Text("|", color = Color.White.copy(alpha = 0.35f), style = MaterialTheme.typography.titleMedium)
                var trailerFocused by remember { mutableStateOf(false) }
                Text(
                    tr("Trailer"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (trailerFocused) Color.Black else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (trailerFocused) Color.White else Color.Transparent)
                        .onFocusChanged { trailerFocused = it.isFocused }
                        .clickable(onClick = onTrailer)
                        .tvPress(previewPass = true, onClick = onTrailer)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 40.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            itemsIndexed(people) { _, member ->
                var focused by remember { mutableStateOf(false) }
                val photo = PosterLoader.model(member.profileUrl)
                Column(
                    Modifier
                        .width(104.dp)
                        .onFocusChanged { focused = it.isFocused }
                        .clickable { onCastClick(member) }
                        .tvPress(previewPass = true, onClick = { onCastClick(member) }),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Box(
                        Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(
                                width = if (focused) 2.5.dp else 1.dp,
                                color = if (focused) Color.White else Color.White.copy(alpha = 0.22f),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (photo != null) {
                            AsyncImage(
                                model = photo,
                                contentDescription = member.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(96.dp).clip(CircleShape),
                            )
                        } else {
                            Text(
                                member.name.trim().take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        member.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val role = member.character?.takeIf { it.isNotBlank() }
                    if (role != null) {
                        Text(
                            tr(role),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.68f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** "2026-09-23" -> "23 September 2026"; anything that is not a full date is returned as it came. */
private fun formatTvReleaseDate(raw: String?): String? {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null
    if (text.length < 10) return text
    return runCatching {
        val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .parse(text.substring(0, 10))
        java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale.getDefault())
            .format(parsed!!)
    }.getOrDefault(text)
}
