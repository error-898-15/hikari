package com.hikari.app.tv

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hikari.app.i18n.tr
import com.hikari.app.ui.navigation.BottomTab
import com.hikari.app.ui.navigation.BottomTabs
import kotlinx.coroutines.launch

@Suppress("UNUSED_PARAMETER")
@Composable
fun TvNavRail(
    currentRoute: String?,
    hidden: Set<String>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = remember { BottomTabs }
    val requesters = remember(tabs) { tabs.map { FocusRequester() } }
    var initialFocusPending by remember(tabs) { mutableStateOf(true) }
    var railHasFocus by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val bringIntoView: (Int) -> Unit = { index ->
        scope.launch {
            runCatching { listState.animateScrollToItem(index.coerceAtLeast(0)) }
        }
    }

    LaunchedEffect(tabs, currentRoute) {
        if (!initialFocusPending) return@LaunchedEffect
        val index = tabs.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 0
        withFrameNanos { }
        try {
            listState.scrollToItem(index)
        } catch (_: Throwable) {
        }
        try {
            requesters.getOrNull(index)?.requestFocus()
        } catch (_: Throwable) {
        }
        initialFocusPending = false
    }

    val primary = MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier = modifier
            .width(TvUi.RAIL_WIDTH)
            .fillMaxHeight()
            .onFocusChanged { state -> railHasFocus = state.hasFocus }
            .padding(start = 8.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
            .background(
                if (railHasFocus) Color(0xFF0E1016).copy(alpha = 0.88f)
                else Color.Transparent,
                RoundedCornerShape(24.dp),
            )
            .padding(vertical = 18.dp),
    ) {
        val wordmarkAndGap = 30.dp
        val room = (maxHeight - wordmarkAndGap).coerceAtLeast(0.dp)
        val slot = if (tabs.isEmpty()) 74.dp else (room / tabs.size).coerceAtMost(74.dp)
        val compact = slot < 62.dp
        val rowHeight = (slot - 6.dp).coerceAtLeast(38.dp)
        val rowsTotal = (rowHeight + 6.dp) * tabs.size
        val fits = rowsTotal <= room
        val pad = if (fits) ((room - rowsTotal) / 2).coerceAtLeast(0.dp) else 0.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                "HIKARI",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                maxLines = 1,
                color = if (railHasFocus) Color.White else primary.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 18.dp),
            )
            Spacer(Modifier.height(14.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.Start,
                contentPadding = PaddingValues(vertical = pad),
            ) {
                itemsIndexed(tabs, key = { _, tab -> tab.route }) { index, tab ->
                    TvNavRailItem(
                        tab = tab,
                        selected = tab.route == currentRoute,
                        requester = requesters[index],
                        slot = slot,
                        compact = compact,
                        onClick = { onNavigate(tab.route) },
                        onFocus = { bringIntoView(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TvNavRailItem(
    tab: BottomTab,
    selected: Boolean,
    requester: FocusRequester,
    slot: Dp,
    compact: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val rowHeight = (slot - 6.dp).coerceAtLeast(38.dp)
    var focused by remember { mutableStateOf(false) }
    val expanded = focused
    val rowWidth by animateDpAsState(
        targetValue = if (expanded) TvUi.RAIL_ITEM_WIDTH + 78.dp else 52.dp,
        label = "rail-expand",
    )

    Box(
        modifier = Modifier
            .padding(vertical = 3.dp, horizontal = 10.dp)
            .width(rowWidth)
            .height(rowHeight)
            .clip(shape)
            // Nuvio স্টাইল: ফোকাস করলে সাদা পিল (White Pill) এবং আনফোকাসড অবস্থায় ক্লিন আইকন
            .background(
                when {
                    expanded -> Color.White
                    selected -> Color.White.copy(alpha = 0.14f)
                    else -> Color.Transparent
                }
            )
            .border(
                1.dp,
                when {
                    expanded -> Color.White
                    selected -> Color.White.copy(alpha = 0.24f)
                    else -> Color.Transparent
                },
                shape,
            )
            .onFocusChanged { state ->
                focused = state.isFocused
                if (state.isFocused) onFocus()
            }
            .tvPress(onClick = onClick)
            .clickable(onClick = onClick)
            .focusRequester(requester)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Box(
                modifier = Modifier.size(if (compact) 38.dp else 42.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    tab.icon,
                    contentDescription = tr(tab.label),
                    tint = when {
                        expanded -> Color.Black
                        selected -> Color.White
                        else -> scheme.onSurfaceVariant.copy(alpha = 0.78f)
                    },
                    modifier = Modifier.size(if (compact) 20.dp else 23.dp),
                )
            }
            if (expanded) {
                Text(
                    tr(tab.label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black,
                    modifier = Modifier.padding(end = 14.dp),
                )
            }
        }
    }
}
