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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hikari.app.data.CastMember
import com.hikari.app.data.MediaItem
import com.hikari.app.data.MediaType
import com.hikari.app.data.TitleDetails
import com.hikari.app.data.TitleExtras
import com.hikari.app.i18n.tr
import com.hikari.app.ui.Artwork
import com.hikari.app.ui.PosterLoader
import com.hikari.app.ui.components.SettingsThemeTile

object TvHeroFocusState {
  var focusedItem by mutableStateOf<MediaItem?>(null)
  var focusedSubtitle by mutableStateOf<String?>(null)
  var isContinueWatching by mutableStateOf(false)
}

private fun Any?.readAnyProp(vararg names: String): String? {
  if (this == null) return null
  for (n in names) {
    val v = runCatching {
      val getter = "get" + n.replaceFirstChar { it.uppercase() }
      val m = this.javaClass.methods.firstOrNull {
        it.name.equals(getter, ignoreCase = true) && it.parameterCount == 0
      }
      m?.invoke(this)
    }.getOrNull()
    when (v) {
      is String -> if (v.isNotBlank()) return v.trim()
      is Iterable<*> -> {
        val joined = v.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }.joinToString(", ")
        if (joined.isNotBlank()) return joined
      }
    }
  }
  return null
}

private fun CastMember.resolvePhotoUrl(): String? {
  val prop = this.readAnyProp("photoUrl", "profileUrl", "photo", "image", "avatar", "profilePath", "posterUrl", "thumb")
  if (!prop.isNullOrBlank()) {
    return if (prop.startsWith("/")) "https://image.tmdb.org/t/p/w300$prop" else prop
  }
  val fallback = runCatching {
    this.javaClass.declaredFields.firstNotNullOfOrNull { f ->
      f.isAccessible = true
      val v = f.get(this) as? String
      v?.takeIf { it.startsWith("http") || it.startsWith("/") }
    }
  }.getOrNull()
  return if (fallback != null && fallback.startsWith("/")) "https://image.tmdb.org/t/p/w300$fallback" else fallback
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

/**
 * 1000263426.jpg স্টাইলের বাটন রো:
 * [ ▶ (চওড়া সাদা পিল) ]  [ + (গোল সেভ বাটন) ]  [ 🎬 (গোল ট্রেলার বাটন) ]  [ ⬇ (গোল ডাউনলোড বাটন) ]
 */
@Composable
fun TvNuvioPlayRow(
  playFocus: FocusRequester,
  isSaved: Boolean,
  hasTrailer: Boolean,
  onPlay: () -> Unit,
  onSave: () -> Unit,
  onTrailer: () -> Unit,
  onDownload: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var playFocused by remember { mutableStateOf(false) }
  var saveFocused by remember { mutableStateOf(false) }
  var trailerFocused by remember { mutableStateOf(false) }
  var dlFocused by remember { mutableStateOf(false) }

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // ১. চওড়া সাদা Play পিল বাটন (শুধুমাত্র Play আইকন, ঠিক 1000263426.jpg এর মতো)
    Button(
      onClick = onPlay,
      colors = ButtonDefaults.buttonColors(
        containerColor = Color.White,
        contentColor = Color.Black,
      ),
      shape = RoundedCornerShape(50),
      contentPadding = PaddingValues(0.dp),
      modifier = Modifier
        .width(96.dp)
        .height(44.dp)
        .border(
          width = if (playFocused) 2.5.dp else 0.dp,
          color = if (playFocused) Color.White else Color.Transparent,
          shape = RoundedCornerShape(50),
        )
        .onFocusChanged { playFocused = it.isFocused }
        .focusRequester(playFocus)
        .tvPress(previewPass = true, onClick = onPlay),
    ) {
      Icon(Icons.Filled.PlayArrow, contentDescription = tr("Play"), modifier = Modifier.size(24.dp))
    }

    // ২. গোল '+' (Save / Library) বাটন
    OutlinedButton(
      onClick = onSave,
      shape = CircleShape,
      border = BorderStroke(
        width = if (saveFocused) 2.dp else 1.dp,
        color = if (saveFocused) Color.White else Color.White.copy(alpha = 0.28f),
      ),
      colors = ButtonDefaults.outlinedButtonColors(
        containerColor = if (saveFocused) Color.White else Color.White.copy(alpha = 0.14f),
        contentColor = if (saveFocused) Color.Black else Color.White,
      ),
      contentPadding = PaddingValues(0.dp),
      modifier = Modifier
        .size(44.dp)
        .onFocusChanged { saveFocused = it.isFocused }
        .tvPress(previewPass = true, onClick = onSave),
    ) {
      Icon(
        if (isSaved) Icons.Filled.Check else Icons.Filled.Add,
        contentDescription = tr("Save"),
        modifier = Modifier.size(20.dp),
      )
    }

    // ৩. গোল Trailer বাটন
    if (hasTrailer) {
      OutlinedButton(
        onClick = onTrailer,
        shape = CircleShape,
        border = BorderStroke(
          width = if (trailerFocused) 2.dp else 1.dp,
          color = if (trailerFocused) Color.White else Color.White.copy(alpha = 0.28f),
        ),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = if (trailerFocused) Color.White else Color.White.copy(alpha = 0.14f),
          contentColor = if (trailerFocused) Color.Black else Color.White,
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
          .size(44.dp)
          .onFocusChanged { trailerFocused = it.isFocused }
          .tvPress(previewPass = true, onClick = onTrailer),
      ) {
        Icon(Icons.Filled.OndemandVideo, contentDescription = tr("Trailer"), modifier = Modifier.size(19.dp))
      }
    }

    // ৪. গোল Download বাটন
    OutlinedButton(
      onClick = onDownload,
      shape = CircleShape,
      border = BorderStroke(
        width = if (dlFocused) 2.dp else 1.dp,
        color = if (dlFocused) Color.White else Color.White.copy(alpha = 0.28f),
      ),
      colors = ButtonDefaults.outlinedButtonColors(
        containerColor = if (dlFocused) Color.White else Color.White.copy(alpha = 0.14f),
        contentColor = if (dlFocused) Color.Black else Color.White,
      ),
      contentPadding = PaddingValues(0.dp),
      modifier = Modifier
        .size(44.dp)
        .onFocusChanged { dlFocused = it.isFocused }
        .tvPress(previewPass = true, onClick = onDownload),
    ) {
      Icon(Icons.Filled.Download, contentDescription = tr("Download"), modifier = Modifier.size(19.dp))
    }
  }
}

/**
 * 1000263426.jpg স্টাইলের বাম পাশের সম্পূর্ণ Hero ইনফরমেশন ব্লক:
 * ১. Title Logo
 * ২. Play / + / Trailer বাটন রো
 * ৩. Director: ...
 * ৪. Overview (3 lines)
 * ৫. Romance • Comedy  •  23 September 2026  •  [TMDB] 8.1
 * ৬. [ R | RELEASED ]  •  1h 52m  •  United States  •  EN
 */
@Composable
fun TvNuvioHeroColumn(
  artTitle: String,
  heroLogo: String?,
  displayOverview: String?,
  meta: MediaItem?,
  extras: TitleExtras?,
  playRow: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  val det: TitleDetails? = extras?.details
  val director = remember(det, extras) {
    det.readAnyProp("director", "directors", "creator", "creators")
  }
  val releaseDate = remember(det, meta) {
    det.readAnyProp("releaseDate", "released", "firstAirDate") ?: meta?.year?.toString()
  }
  val statusText = remember(det, meta) {
    det.readAnyProp("status", "releaseStatus")?.uppercase()
      ?: if (meta?.type == MediaType.SERIES) "ONGOING" else "RELEASED"
  }
  val certText = remember(det) {
    det?.certification?.trim()?.takeIf { it.isNotEmpty() }
  }
  val countryText = remember(det) {
    det.readAnyProp("country", "countries", "originCountry")
  }
  val langText = remember(det) {
    det.readAnyProp("language", "originalLanguage", "languages")?.uppercase()
  }
  val scoreValue = det?.rating?.takeIf { it > 0.0 } ?: meta?.rating?.takeIf { it > 0.0 }

  Column(modifier = modifier) {
    // ১. Title Logo অথবা বড় Title Text
    if (!heroLogo.isNullOrBlank()) {
      AsyncImage(
        model = PosterLoader.model(heroLogo),
        contentDescription = artTitle,
        contentScale = ContentScale.Fit,
        alignment = Alignment.CenterStart,
        modifier = Modifier
          .fillMaxWidth(0.65f)
          .heightIn(max = 112.dp),
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

    Spacer(Modifier.height(16.dp))

    // ২. Play + Save (+) + Trailer বাটন রো
    playRow()

    Spacer(Modifier.height(16.dp))

    // ৩. Director লাইন
    if (!director.isNullOrBlank()) {
      Text(
        "Director: $director",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = Color.White.copy(alpha = 0.90f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(8.dp))
    }

    // ৪. Overview (৩-৪ লাইন)
    if (!displayOverview.isNullOrBlank()) {
      var expanded by remember(displayOverview) { mutableStateOf(false) }
      Text(
        displayOverview,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.88f),
        maxLines = if (expanded) Int.MAX_VALUE else 3,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 20.sp,
        modifier = Modifier
          .fillMaxWidth()
          .clickable { expanded = !expanded },
      )
      Spacer(Modifier.height(14.dp))
    }

    // ৫. মেটাডেটা লাইন ১: Romance • Comedy  •  23 September 2026  •  [TMDB] 8.1
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      val line1 = buildList {
        if (meta?.genres?.isNotEmpty() == true) {
          add(meta.genres.take(3).joinToString(" • "))
        }
        if (!releaseDate.isNullOrBlank()) {
          add(releaseDate)
        }
      }.joinToString("   •   ")

      if (line1.isNotBlank()) {
        Text(
          line1,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = Color.White.copy(alpha = 0.90f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (scoreValue != null) {
        if (line1.isNotBlank()) {
          Text("•", color = Color.White.copy(alpha = 0.65f), fontWeight = FontWeight.Bold)
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
          "%.1f".format(scoreValue),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = Color.White,
        )
      }
    }

    Spacer(Modifier.height(10.dp))

    // ৬. মেটাডেটা লাইন ২: [ R | RELEASED ]  •  1h 52m  •  United States  •  EN
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      val badgeLabel = if (!certText.isNullOrBlank()) "$certText  |  $statusText" else statusText
      Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f)),
      ) {
        Text(
          badgeLabel,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
      }

      val line2 = buildList {
        det?.runtimeMinutes?.takeIf { it > 0 }?.let { mins ->
          add(if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m")
        }
        if (!countryText.isNullOrBlank()) add(countryText)
        if (!langText.isNullOrBlank()) add(langText)
      }.joinToString("   •   ")

      if (line2.isNotBlank()) {
        Text(
          "•   $line2",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = Color.White.copy(alpha = 0.85f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/**
 * 1000263427.jpg স্টাইলের Creator and Cast সেকশন (গোল ছবির সারি):
 * Creator and Cast  |  More like this  |  Trailer
 */
@Composable
fun TvNuvioCastRow(
  cast: List<CastMember>,
  directorName: String? = null,
  hasTrailer: Boolean = false,
  onCastClick: (CastMember) -> Unit,
  onTrailerClick: (() -> Unit)? = null,
) {
  if (cast.isEmpty()) return
  Column(
    Modifier
      .fillMaxWidth()
      .padding(top = 18.dp, bottom = 8.dp)
  ) {
    Row(
      Modifier.padding(horizontal = 42.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        "Creator and Cast",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
      )
      Text("|", color = Color.White.copy(alpha = 0.35f), style = MaterialTheme.typography.titleMedium)
      Text(
        "More like this",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = Color.White.copy(alpha = 0.65f),
      )
      if (hasTrailer && onTrailerClick != null) {
        Text("|", color = Color.White.copy(alpha = 0.35f), style = MaterialTheme.typography.titleMedium)
        Text(
          "Trailer",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = Color.White.copy(alpha = 0.65f),
          modifier = Modifier.clickable { onTrailerClick() },
        )
      }
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 42.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
      items(cast, key = { it.name + "|" + (it.character ?: "") }) { member ->
        var focused by remember { mutableStateOf(false) }
        val photoUrl = remember(member) { member.resolvePhotoUrl() }
        Column(
          modifier = Modifier
            .width(104.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onCastClick(member) }
            .tvPress(previewPass = true, onClick = { onCastClick(member) }),
          horizontalAlignment = Alignment.Start,
        ) {
          Box(
            modifier = Modifier
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
            Icon(
              Icons.Filled.Person,
              contentDescription = null,
              tint = Color.White.copy(alpha = 0.4f),
              modifier = Modifier.size(36.dp),
            )
            if (!photoUrl.isNullOrBlank()) {
              AsyncImage(
                model = PosterLoader.model(photoUrl),
                contentDescription = member.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
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
            ?: if (directorName != null && member.name.equals(directorName, ignoreCase = true)) "Director" else ""
          if (role.isNotBlank()) {
            Text(
              role,
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
  val shape = RoundedCornerShape(14.dp)

  Column(
    modifier
      .width(cardWidth)
      .onFocusChanged { state ->
        focused = state.isFocused
        if (state.isFocused) {
          TvHeroFocusState.isContinueWatching = false
          TvHeroFocusState.focusedSubtitle = null
          TvHeroFocusState.focusedItem = item
          onFocused?.invoke()
        }
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
          color = if (focused) Color.White else Color.White.copy(alpha = 0.14f),
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
        if (state.isFocused) {
          TvHeroFocusState.isContinueWatching = false
          TvHeroFocusState.focusedSubtitle = null
          TvHeroFocusState.focusedItem = item
          onFocused?.invoke()
        }
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
