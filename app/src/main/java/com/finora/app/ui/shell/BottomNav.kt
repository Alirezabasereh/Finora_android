package com.finora.app.ui.shell

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.finora.app.ui.common.FText
import com.finora.app.ui.common.FaGlyph
import com.finora.app.ui.common.FaIcon
import com.finora.app.ui.common.boxShadow
import com.finora.app.ui.theme.Finora
import com.finora.app.ui.theme.rgba

enum class NavPage(val label: String, val glyph: String) {
    HOME("خانه", FaGlyph.HOUSE),
    ANALYTICS("تحلیل", FaGlyph.CHART_PIE),
    ADD("ثبت", FaGlyph.PLUS),          // action button, never "selected"
    BUDGET("بودجه", FaGlyph.WALLET),
    AI("AI", FaGlyph.BRAIN)
}

/**
 * .bottom-nav — floating bar: 8dp side margin, 14dp from the bottom, radius 28, padding 12/10/14,
 * five 52dp-wide items distributed with `space-around`, icon 17.6 + gap 6 + label 12.48.
 */
@Composable
fun BottomNav(current: NavPage, onSelect: (NavPage) -> Unit, modifier: Modifier = Modifier) {
    val c = Finora.colors
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (c.isLight) Modifier.boxShadow(rgba(15, 23, 42, .28f), offsetY = 12.dp, blur = 32.dp, spread = (-8).dp, radius = 28.dp)
                else Modifier
            )
            .background(c.navBg, shape)
            .border(1.dp, c.navBorder, shape)
            .padding(start = 11.dp, top = 13.dp, end = 11.dp, bottom = 15.dp),   // CSS padding + 1px border
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (page in NavPage.values()) {
            NavItem(page, page == current) { onSelect(page) }
        }
    }
}

@Composable
private fun NavItem(page: NavPage, active: Boolean, onClick: () -> Unit) {
    val c = Finora.colors
    val baseOpacity = if (c.isLight) 0.58f else 0.62f
    val opacity by animateFloatAsState(if (active) 1f else baseOpacity, tween(220), label = "navOpacity")
    val lift by animateFloatAsState(if (active) -2f else 0f, tween(220), label = "navLift")
    val color = if (active) c.accent2 else c.text
    Column(
        modifier = Modifier
            .width(52.dp)
            .graphicsLayer {
                alpha = opacity
                translationY = lift * density
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FaIcon(page.glyph, 17.6f, Modifier.height(17.6.dp), color)
        FText(page.label, 12.48f, color = color, lineHeight = 12.48f, maxLines = 1, softWrap = false)
    }
}
