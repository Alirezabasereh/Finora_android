package com.finora.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finora.app.finance.Finance
import com.finora.app.finance.InsightChip
import com.finora.app.finance.homeInsights
import com.finora.app.ui.common.FText
import com.finora.app.ui.common.FaGlyph
import com.finora.app.ui.common.FaIcon
import com.finora.app.ui.common.boxShadow
import com.finora.app.ui.common.cssLinearGradient
import com.finora.app.ui.theme.Finora
import com.finora.app.ui.theme.rgba

/** .home-insights-row — up to three equal chips (today's transactions, no-spend days, forecast). */
@Composable
fun InsightsRow(fin: Finance) {
    val chips = fin.homeInsights()
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (chip in chips) Chip(chip, Modifier.weight(1f).fillMaxHeight())
        if (chips.size < 3) for (i in chips.size until 3) Box(Modifier.weight(1f))
    }
}

@Composable
private fun Chip(chip: InsightChip, modifier: Modifier) {
    val c = Finora.colors
    val accent = when (chip.type) { "warn" -> c.warn; "bad" -> c.bad; else -> c.good }
    val shape = RoundedCornerShape(20.dp)
    val iconGlyph = FaGlyph.byName[chip.icon] ?: FaGlyph.CIRCLE_INFO
    Column(
        modifier
            .then(
                if (c.isLight) Modifier
                    .boxShadow(rgba(15, 23, 42, .06f), offsetY = 1.dp, blur = 2.dp, radius = 20.dp)
                    .boxShadow(rgba(15, 23, 42, .18f), offsetY = 8.dp, blur = 22.dp, spread = (-8).dp, radius = 20.dp)
                else Modifier
            )
            .clip(shape)
            .drawBehind {
                val brush = if (c.isLight) cssLinearGradient(160f, size.width, size.height, 0f to accent.copy(alpha = .10f), .70f to Color.White, 1f to Color.White)
                else cssLinearGradient(160f, size.width, size.height, 0f to accent.copy(alpha = if (chip.type == "good") .07f else .09f), 1f to rgba(255, 255, 255, .07f))
                drawRect(brush)
                // 3px top accent line fading in from the start edge
                drawRect(
                    cssLinearGradient(90f, size.width, 3.dp.toPx(), 0f to accent.copy(alpha = 0f), 1f to accent),
                    size = androidx.compose.ui.geometry.Size(size.width, 3.dp.toPx())
                )
            }
            .border(1.dp, accent.copy(alpha = if (c.isLight) .26f else if (chip.type == "good") .22f else .28f), shape)
            .padding(start = 13.dp, top = 15.dp, end = 13.dp, bottom = 15.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha = if (chip.type == "good") .16f else .18f)), contentAlignment = Alignment.Center) {
            FaIcon(iconGlyph, 16f, color = accent)
        }
        Column {
            FText(chip.title, 13.44f, Modifier.padding(bottom = 2.dp), FontWeight.Bold, c.text)
            FText(chip.sub, 11.84f, color = c.muted, lineHeight = 17.76f)
        }
    }
}
