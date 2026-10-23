package com.hikari.app.tv

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hikari.app.data.MediaItem
import com.hikari.app.data.MediaType
import com.hikari.app.i18n.tr
import com.hikari.app.ui.Artwork
import com.hikari.app.ui.components.SettingsThemeTile
import kotlin.math.roundToInt

object TvHeroFocusState {
  var focusedItem by mutableStateOf<MediaItem?>(null)
  var focusedSubtitle by mutableStateOf<String?>(null)
  var isContinueWatching by mutableStateOf(false)
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun TvSettingsSourceCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  iconRes: Int? = null,
) {
  var focused by remember { mutableStateOf(false) }
  val interactionSource = remember { MutableInteractionSource() }
  val shape = RoundedCornerShape(20.dp)
  Surface(
    tonalElevation = 2.dp,
    shape = shape,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = BorderStroke(
      width = if (focused) 1.8.dp else 1.dp,
      color = if (focused) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    ),
    modifier = modifier
      .clip(shape)
      .onFocusChanged { focused = it.isFocused }
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
      )
      .tvPress(previewPass = true, onClick = onClick),
  ) {
    Row(
      Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SettingsThemeTile(icon, title, 52.dp)
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
      }
      Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun TvCertChip(label: String, modifier: Modifier = Modifier) {
  val shape = RoundedCornerShape(5.dp)
  Box(
    modifier
      .clip(shape)
      .background(Color.White.copy(alpha = 0.14f))
      .border(1.dp, Color.White.copy(alpha = 0.45f), shape)
      .padding(horizontal = 8.dp, vertical = 2.dp)
  ) {
    Text(label, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
  }
}

@Composable
fun TvCinemaCard(
  item: MediaItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  cardWidth: Dp = 260.dp,
  onFocused: (() -> Unit)? = null,
) {
  var focused by remember { mutableStateOf(false) }
  val interactionSource = remember { MutableInteractionSource() }
  val hero = remember(item) { Artwork.heroModel(item) }
  val art = hero.first
  val outerShape = RoundedCornerShape(16.dp)
  val innerShape = RoundedCornerShape(13.dp)
  val scale by animateFloatAsState(
    targetValue = if (focused) 1.03f else 1f,
    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
    label = "cinema-card-scale",
  )

  Column(
    modifier
      .width(cardWidth)
      .scale(scale)
      .onFocusChanged { state ->
        focused = state.isFocused
        if (state.isFocused) {
          TvHeroFocusState.isContinueWatching = false
          TvHeroFocusState.focusedSubtitle = null
          TvHeroFocusState.focusedItem = item
          onFocused?.invoke()
        }
      }
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
      )
      .tvPress(previewPass = true, onClick = onClick),
  ) {
    Box(
      Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .border(
          width = if (focused) 1.8.dp else 1.dp,
          color = if (focused) Color.White else Color.White.copy(alpha = 0.12f),
          shape = outerShape,
        )
        .padding(if (focused) 3.dp else 0.dp)
        .clip(innerShape)
        .background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        Icons.Filled.Movie,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.size(28.dp),
      )
      AsyncImage(
        model = art,
        contentDescription = item.title,
        modifier = Modifier.fillMaxSize().clip(innerShape),
        contentScale = ContentScale.Crop,
        alignment = Artwork.CINEMA_ALIGNMENT,
      )
      Box(
        Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              0f to Color.Transparent,
              0.45f to Color.Transparent,
              1f to Color.Black.copy(alpha = 0.85f),
            )
          )
      )
      Text(
        item.title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(horizontal = 12.dp, vertical = 10.dp),
      )
    }
  }
}

/**
 * 1000263553.jpg style Vertical Poster Card:
 * indication = null disables the yellow TvFocusProvider ring so ONLY the thin white borderline
 * around the poster artwork is drawn!
 */
@Composable
fun TvVerticalCard(
  item: MediaItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  cardWidth: Dp = 148.dp,
  expandOnFocus: Boolean = TvSidebarPrefs.expandCardOnFocus,
  onFocused: (() -> Unit)? = null,
) {
  var focused by remember { mutableStateOf(false) }
  val interactionSource = remember { MutableInteractionSource() }
  val posterArt = remember(item) { Artwork.model(item) }
  val heroArt = remember(item) { Artwork.heroModel(item).first }
  val cardHeight = cardWidth * 1.48f
  val targetWidth = if (focused && expandOnFocus) cardHeight * (16f / 9f) else cardWidth
  val animatedWidth by animateDpAsState(
    targetValue = targetWidth,
    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
    label = "tv-card-expand",
  )
  val scale by animateFloatAsState(
    targetValue = if (focused && !expandOnFocus) 1.035f else 1f,
    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
    label = "tv-card-scale",
  )

  val outerShape = RoundedCornerShape(16.dp)
  val innerShape = RoundedCornerShape(13.dp)

  Column(
    modifier = modifier
      .width(animatedWidth)
      .scale(scale)
      .onFocusChanged { state ->
        focused = state.isFocused
        if (state.isFocused) {
          TvHeroFocusState.isContinueWatching = false
          TvHeroFocusState.focusedSubtitle = null
          TvHeroFocusState.focusedItem = item
          onFocused?.invoke()
        }
      }
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
      )
      .tvPress(previewPass = true, onClick = onClick),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(cardHeight)
        .border(
          width = if (focused) 1.8.dp else 1.dp,
          color = if (focused) Color.White else Color.White.copy(alpha = 0.10f),
          shape = outerShape,
        )
        .padding(if (focused) 3.dp else 0.dp)
        .clip(innerShape)
        .background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        Icons.Filled.Movie,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.size(28.dp),
      )
      AsyncImage(
        model = if (focused && expandOnFocus) (heroArt ?: posterArt) else posterArt,
        contentDescription = item.title,
        modifier = Modifier.fillMaxSize().clip(innerShape),
        contentScale = ContentScale.Crop,
        alignment = if (focused && expandOnFocus) Artwork.CINEMA_ALIGNMENT else Alignment.Center,
      )

      item.rating?.takeIf { it > 0 }?.let { r ->
        val pct = if (r <= 10f) (r * 10f).roundToInt() else r.roundToInt()
        Surface(
          color = Color(0xFF14161E).copy(alpha = 0.82f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(7.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Text(
              text = "★",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF66E0E0),
            )
            Text(
              text = "$pct%",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White,
            )
          }
        }
      }
    }

    Text(
      text = item.title,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = Color.White,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      lineHeight = 18.sp,
      modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
    )

    val sub = buildList {
      val typeLabel = when (item.type) {
        MediaType.SERIES -> "TV"
        MediaType.MOVIE -> "Movie"
        else -> item.rawType.uppercase().ifBlank { "TV" }
      }
      add(typeLabel)
      item.year?.let { add(it.toString()) }
    }.joinToString(" • ")

    Text(
      text = sub,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Medium,
      color = Color.White.copy(alpha = 0.65f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(top = 2.dp, start = 2.dp, end = 2.dp),
    )
  }
}

@Composable
fun TvCinemaRow(
  title: String,
  providerName: String,
  items: List<MediaItem>,
  onOpen: (MediaItem) -> Unit,
  onShowAll: (() -> Unit)? = null,
  vertical: Boolean = true,
  keeperKey: String = "",
  onItemFocus: ((MediaItem) -> Unit)? = null,
) {
  if (items.isEmpty()) return
  val seen = LinkedHashSet<String>()
  val unique = items.filter { it.uniqueId.let { k -> if (seen.add(k)) true else false } }
  val rowKeys = remember(unique) { unique.map { it.uniqueId } }
  val rowState = if (keeperKey.isBlank()) com.hikari.app.ui.components.keptListState("tvRow:$title:$providerName")
  else com.hikari.app.ui.components.keptListState(keeperKey, itemKeys = rowKeys)

  Column(Modifier.padding(top = 20.dp)) {
    Row(
      Modifier.fillMaxWidth().padding(horizontal = 24.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          tr(title),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Box(
          Modifier
            .width(26.dp)
            .height(2.dp)
            .background(Color(0xFF66E0E0).copy(alpha = 0.7f), RoundedCornerShape(1.dp))
        )
      }
      if (onShowAll != null) {
        TextButton(
          onClick = onShowAll,
          modifier = Modifier.tvPress(previewPass = true, onClick = onShowAll),
        ) {
          Text(tr("Show All"), fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f))
        }
      }
    }
    Spacer(Modifier.height(10.dp))
    LazyRow(
      state = rowState,
      contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      items(unique, key = { it.uniqueId }) { item ->
        if (vertical) {
          TvVerticalCard(
            item = item,
            onClick = { onOpen(item) },
            onFocused = { onItemFocus?.invoke(item) },
          )
        } else {
          TvCinemaCard(
            item = item,
            onClick = { onOpen(item) },
            onFocused = { onItemFocus?.invoke(item) },
          )
        }
      }
    }
  }
}
