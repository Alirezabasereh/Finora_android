package com.finora.app.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finora.app.net.SyncBarState
import com.finora.app.ui.common.FText
import com.finora.app.ui.theme.rgba

/** #syncBar — slides in from the top; syncing = violet, saved = green, error = red. */
@Composable
fun SyncBar(state: SyncBarState, modifier: Modifier = Modifier) {
    // keep the last non-empty content while sliding out
    var shown by remember { mutableStateOf(state) }
    LaunchedEffect(state) { if (state.type.isNotEmpty()) shown = state }

    AnimatedVisibility(
        visible = state.visible,
        modifier = modifier,
        enter = slideInVertically(tween(300)) { -it },
        exit = slideOutVertically(tween(300)) { -it }
    ) {
        val (bg, fg) = when (shown.type) {
            "saved" -> rgba(24, 230, 166, .85f) to Color(0xFF04261A)
            "error" -> rgba(255, 93, 125, .85f) to Color.White
            else -> rgba(106, 92, 255, .85f) to Color.White
        }
        Box(
            Modifier.fillMaxWidth().background(bg).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            FText(shown.message, 13.12f, Modifier.fillMaxWidth(), FontWeight.SemiBold, fg, TextAlign.Center)
        }
    }
}
