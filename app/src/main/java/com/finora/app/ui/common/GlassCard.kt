package com.finora.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finora.app.ui.theme.Finora
import com.finora.app.ui.theme.rgba

/** `.glass` — translucent card (dark) / white card (light) with the web app's border + shadow tokens. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = Finora.colors
    val shape = RoundedCornerShape(radius)
    val shadowMod = if (c.isLight) {
        Modifier
            .boxShadow(rgba(15, 23, 42, .05f), offsetY = 1.dp, blur = 2.dp, radius = radius)
            .boxShadow(rgba(15, 23, 42, .16f), offsetY = 12.dp, blur = 30.dp, spread = (-10).dp, radius = radius)
    } else {
        Modifier.boxShadow(rgba(0, 0, 0, .28f), offsetY = 18.dp, blur = 50.dp, radius = radius)
    }
    Column(
        modifier = modifier
            .then(shadowMod)
            .background(c.card, shape)
            .border(1.dp, if (c.isLight) rgba(15, 23, 42, .10f) else c.border, shape),
        content = content
    )
}
