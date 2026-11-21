package com.finora.app.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.finora.app.finance.Finance
import com.finora.app.ui.common.FText
import com.finora.app.ui.common.FaGlyph
import com.finora.app.ui.common.FaIcon
import com.finora.app.ui.common.GlassCard
import com.finora.app.ui.common.cssLinearGradient
import com.finora.app.ui.common.boxShadow
import com.finora.app.ui.common.parseHex
import com.finora.app.ui.theme.Finora
import com.finora.app.ui.theme.rgba
import com.finora.app.util.Fa
import kotlin.math.min

/** .home-breakdown-card — expense split: legend + donut, "without loans" switch, link to the full report. */
@Composable
fun BreakdownCard(
    fin: Finance,
    excludeLoans: Boolean,
    onToggleLoans: (Boolean) -> Unit,
    screenWidth: Float,
    onFullReport: () -> Unit
) {
    val c = Finora.colors
    val split = remember(fin) { fin.splitExpenseEntries(fin.expenseBreakdown()) }
    val entries = split.active
    val total = split.total

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(19.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                FText("خلاصه تقسیم هزینه‌ها", 14.4f, weight = FontWeight.Bold)
                FText("نمودار و درصد", 12f, color = c.muted)
            }
            LoanToggle(excludeLoans, onToggleLoans)

            val pieSize = min(150f, 0.37f * screenWidth)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                // legend (first child → right side in RTL)
                Column(Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (total == 0L && split.excluded.isEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            FaIcon(FaGlyph.CHART_PIE, 32f, color = c.accent1)
                            FText("هنوز هزینه‌ای ثبت نشده", 16.8f, Modifier.padding(top = 12.dp, bottom = 8.dp), FontWeight.Bold, c.text, TextAlign.Center)
                            FText("بعد از ثبت هزینه، خلاصه‌ی دسته‌ها اینجا نمایش داده می‌شه.", 14.72f, color = c.muted, align = TextAlign.Center, lineHeight = 14.72f * 1.8f)
                        }
                    } else {
                        for (e in entries) {
                            LegendRow(e.color, "${e.emoji} ${e.name}", Fa.percent(e.amount.toDouble() / total * 100, 1), false)
                        }
                        for (e in split.excluded) {
                            LegendRow(e.color, "${e.emoji} ${e.name}", "خارج از محاسبه", true)
                        }
                    }
                }
                Donut(entries.map { parseHex(it.color) to it.amount }, total, pieSize, screenWidth <= 480f, fin)
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(53.dp)
                    .boxShadow(if (c.isLight) rgba(90, 73, 242, .22f) else rgba(106, 92, 255, .22f), offsetY = 14.dp, blur = 32.dp, radius = 18.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .drawBehind {
                        drawRect(
                            cssLinearGradient(
                                135f, size.width, size.height,
                                0f to (if (c.isLight) Color(0xFF5A49F2) else Color(0xFF6A5CFF)),
                                1f to (if (c.isLight) Color(0xFF0A88AD) else Color(0xFF00D2FF))
                            )
                        )
                    }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onFullReport),
                contentAlignment = Alignment.Center
            ) {
                FText("مشاهده گزارش کامل", 16f, weight = FontWeight.ExtraBold, color = Color.White, align = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun LoanToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Finora.colors
    val knobX by animateDpAsState(if (checked) (-14).dp else 0.dp, tween(200), label = "knob")
    Row(
        Modifier
            .offset(y = (-4).dp)
            .padding(bottom = 8.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onChange(!checked) },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 34.dp, height = 20.dp)
                .clip(CircleShape)
                .drawBehind {
                    if (checked) drawRect(cssLinearGradient(135f, size.width, size.height, 0f to c.accent1, 1f to c.accent3))
                    else drawRect(if (c.isLight) rgba(15, 23, 42, .22f) else rgba(127, 127, 127, .32f))
                }
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 2.dp)
                    .offset(x = knobX)
                    .size(16.dp)
                    .boxShadow(if (c.isLight) rgba(15, 23, 42, .35f) else rgba(0, 0, 0, .30f), offsetY = 1.dp, blur = 3.dp, radius = 8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
        FText("بدون اقساط وام", 12.48f, color = if (checked) c.text else c.muted)
    }
}

@Composable
private fun LegendRow(colorHex: String, name: String, meta: String, excluded: Boolean) {
    val c = Finora.colors
    Row(
        Modifier.fillMaxWidth().then(if (excluded) Modifier.alpha(.45f) else Modifier).padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f, fill = false), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val dot = parseHex(colorHex)
            Box(
                Modifier.size(10.dp).drawBehind {
                    drawCircle(if (c.isLight) rgba(15, 23, 42, .05f) else rgba(255, 255, 255, .03f), radius = 9.dp.toPx())
                }.clip(CircleShape).background(if (excluded) Color(0xFF9CA3AF) else dot)
            )
            Text12(name, excluded)
        }
        if (excluded) {
            FText(meta, 10.66f, Modifier.padding(end = 4.dp), color = c.muted, align = TextAlign.End, lineHeight = 10.66f * 1.35f)
        } else {
            FText(meta, 14.08f, Modifier.padding(end = 4.dp), color = c.muted, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun Text12(text: String, strike: Boolean) {
    val c = Finora.colors
    androidx.compose.foundation.text.BasicText(
        text = text,
        style = androidx.compose.ui.text.TextStyle(
            color = c.text, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
            fontWeight = FontWeight.Bold, fontFamily = com.finora.app.ui.theme.Vazir,
            lineHeight = androidx.compose.ui.unit.TextUnit(17.4f, androidx.compose.ui.unit.TextUnitType.Sp),
            textDecoration = if (strike) TextDecoration.LineThrough else null
        ),
        maxLines = 1
    )
}

/** .pie-chart-sm — conic donut starting at 6 o'clock, with the total in the middle. */
@Composable
private fun Donut(slices: List<Pair<Color, Long>>, total: Long, sizeDp: Float, narrow: Boolean, fin: Finance) {
    val c = Finora.colors
    Box(Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(sizeDp.dp)) {
            val d = size.width
            if (total <= 0L) {
                drawCircle(rgba(255, 255, 255, .10f), radius = d / 2f)
            } else {
                var cursor = 0f
                for ((col, amount) in slices) {
                    val sweep = amount.toFloat() / total * 360f
                    drawArc(col, startAngle = 90f + cursor, sweepAngle = sweep + 0.4f, useCenter = true, topLeft = Offset.Zero, size = Size(d, d))
                    cursor += sweep
                }
            }
            drawCircle(if (c.isLight) rgba(15, 23, 42, .10f) else rgba(255, 255, 255, .06f), radius = d / 2f - 0.5f, style = Stroke(1.dp.toPx()))
            val inner = d / 2f - 20.dp.toPx()
            drawCircle(
                Brush.verticalGradient(
                    0f to (if (c.isLight) Color.White else Color(0xFF111827)), 1f to (if (c.isLight) Color(0xFFF3F5FB) else Color(0xFF0B1022)),
                    startY = d / 2f - inner, endY = d / 2f + inner
                ),
                radius = inner, center = Offset(d / 2f, d / 2f)
            )
            drawCircle(if (c.isLight) rgba(15, 23, 42, .06f) else rgba(255, 255, 255, .06f), radius = inner - 0.5f, center = Offset(d / 2f, d / 2f), style = Stroke(1.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FText(
                if (narrow) Fa.shortMoney(total.toDouble()) else Fa.money(total), 10.92f, weight = FontWeight.Black,
                align = TextAlign.Center, lineHeight = 13.65f
            )
            FText("مجموع هزینه", 10.88f, Modifier.padding(top = 3.dp), color = c.muted, align = TextAlign.Center, lineHeight = 17f)
        }
    }
}
