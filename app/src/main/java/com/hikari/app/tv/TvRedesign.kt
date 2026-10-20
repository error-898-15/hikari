package com.hikari.app.tv

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import com.hikari.app.i18n.tr
import com.hikari.app.ui.Artwork
import com.hikari.app.ui.components.SettingsThemeTile

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
  Surface(
    tonalElevation = 2.dp,
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
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
  val v = label.trim().uppercase(java.util.Locale.US)
  val tint = when {
    v == "R" || v == "NC-17" || v == "TV-MA" || v == "MA" || v.startsWith("R18") -> Color(0xFFE5484D)
    v.startsWith("PG") || v == "M" -> Color(0xFFFFB020)
    v.startsWith("G") || v.startsWith("TV-Y") || v == "U" -> Color(0xFF34C759)
    else -> {
      val n = Regex("\\d+").find(v)?.value?.toIntOrNull()
      when {
        n == null -> Color(0xFF8A94A6)
        n >= 18 -> Color(0xFFE5484D)
        n >= 12 -> Color(0xFFFFB020)
        else -> Color(0xFF34C759)
      }
    }
  }
  val shape = RoundedCornerShape(9.dp)
  Box(
    modifier.clip(shape).background(tint.copy(alpha = 0.16f)).border(1.dp, tint.copy(alpha = 0.45f), shape).padding(horizontal = 8.dp, vertical = 2.dp)
  ) {
    Text(label, color = tint, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
  val hero = remember(item) { Artwork.heroModel(item) }
  val art = hero.first
  val shape = RoundedCornerShape(12.dp)

  Column(
    modifier
      .width(cardWidth)
      .onFocusChanged { state ->
        focused = state.isFocused
        if (state.isFocused) onFocused?.invoke()
      }
      .clickable(onClick = onClick)
      .tvPress(previewPass = true, onClick = onClick),
  ) {
    Box(
      Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .border(
          width = if (focused) 2.5.dp else 1.dp,
          color = if (focused) Color.White else Color.White.copy(alpha = 0.12f),
          shape = shape,
        ),
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
        modifier = Modifier.fillMaxSize().clip(shape),
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

@Composable
fun TvVerticalCard(
  item: MediaItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  cardWidth: Dp = 132.dp,
  expandOnFocus: Boolean = true,
  onFocused: (() -> Unit)? = null,
) {
  var focused by remember { mutableStateOf(false) }
  val posterArt = remember(item) { Artwork.model(item) }
  val heroArt = remember(item) { Artwork.heroModel(item).first }
  val cardHeight = cardWidth * 1.5f
  val targetWidth = if (focused && expandOnFocus) cardHeight * (16f / 9f) else cardWidth
  val animatedWidth by animateDpAsState(
    targetValue = targetWidth,
    animationSpec = tween(durationMillis = 220),
    label = "tv-card-expand",
  )
  val shape = RoundedCornerShape(12.dp)

  Box(
    modifier = modifier
      .width(animatedWidth)
      .height(cardHeight)
      .clip(shape)
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .border(
        width = if (focused) 2.5.dp else 1.dp,
        color = if (focused) Color.White else Color.White.copy(alpha = 0.12f),
        shape = shape,
      )
      .onFocusChanged { state ->
        focused = state.isFocused
        if (state.isFocused) onFocused?.invoke()
      }
      .clickable(onClick = onClick)
      .tvPress(previewPass = true, onClick = onClick),
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
      modifier = Modifier.fillMaxSize().clip(shape),
      contentScale = ContentScale.Crop,
      alignment = if (focused && expandOnFocus) Artwork.CINEMA_ALIGNMENT else Alignment.Center,
    )
    if (focused) {
      Box(
        Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              0f to Color.Transparent,
              0.45f to Color.Transparent,
              1f to Color.Black.copy(alpha = 0.88f),
            )
          )
      )
      Text(
        item.title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
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

  Column(Modifier.padding(top = 18.dp)) {
    Row(
      Modifier.fillMaxWidth().padding(horizontal = 24.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(Modifier.weight(1f)) {
        Text(
          tr(title),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (providerName.isNotBlank()) {
          Text(
            "from $providerName",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      if (onShowAll != null) {
        TextButton(
          onClick = onShowAll,
          modifier = Modifier.tvPress(previewPass = true, onClick = onShowAll),
        ) {
          Text(tr("Show All"), fontWeight = FontWeight.SemiBold)
        }
      }
    }
    Spacer(Modifier.height(10.dp))
    LazyRow(
      state = rowState,
      contentPadding = PaddingValues(horizontal = 24.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
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
