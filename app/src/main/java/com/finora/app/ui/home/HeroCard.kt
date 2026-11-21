package com.finora.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finora.app.data.Account
import com.finora.app.finance.Banks
import com.finora.app.finance.Finance
import com.finora.app.finance.heroHealth
import com.finora.app.finance.heroToday
import com.finora.app.ui.common.BankLogos
import com.finora.app.ui.common.FText
import com.finora.app.ui.common.FaGlyph
import com.finora.app.ui.common.FaIcon
import com.finora.app.ui.common.boxShadow
import com.finora.app.ui.common.circleGlow
import com.finora.app.ui.common.cssLinearGradient
import com.finora.app.ui.common.ellipseGlow
import com.finora.app.ui.common.parseHex
import com.finora.app.ui.theme.Finora
import com.finora.app.ui.theme.Vazir
import com.finora.app.ui.theme.rgba
import com.finora.app.util.Fa
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private val FREE_COLOR = Color(0xFF94A3B8)
private val EaseOutCubic = Easing { f -> 1f - (1f - f).pow(3) }

private fun mixOver(c: Color, a: Float, base: Color): Color =
    Color(
        red = c.red * a + base.red * (1 - a), green = c.green * a + base.green * (1 - a),
        blue = c.blue * a + base.blue * (1 - a), alpha = 1f
    )

/** .hero-card — balance, budget ring, account chips and the four summary tiles. */
@Composable
fun HeroCard(
    fin: Finance,
    heroAccountId: String?,
    screenWidth: Float,
    onSelectAccount: (String?) -> Unit,
    onAddAccount: () -> Unit,
    onOpenToday: () -> Unit,
    onOpenHealth: () -> Unit
) {
    val c = Finora.colors
    val shape = RoundedCornerShape(28.dp)
    val data = fin.data
    val archived = fin.isViewingArchive
    val ab = remember(data, fin.nowMillis) { fin.accountBalances() }

    var sel: String? = if (archived) null else heroAccountId
    if (sel != null) {
        val bad = if (sel == "free") data.accounts.isEmpty() else !ab.map.containsKey(sel)
        if (bad) sel = null
    }
    val selAccount: Account? = if (sel != null && sel != "free") data.accounts.firstOrNull { it.id == sel } else null

    val contentW = screenWidth - 28f - 34f
    val borderColor = if (c.isLight) rgba(90, 73, 242, .22f) else rgba(255, 255, 255, .10f)

    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (c.isLight) Modifier
                    .boxShadow(rgba(15, 23, 42, .05f), offsetY = 1.dp, blur = 2.dp, radius = 28.dp)
                    .boxShadow(rgba(90, 73, 242, .30f), offsetY = 20.dp, blur = 44.dp, spread = (-16).dp, radius = 28.dp)
                else Modifier.boxShadow(rgba(0, 0, 0, .28f), offsetY = 18.dp, blur = 50.dp, radius = 28.dp)
            )
            .clip(shape)
            .background(if (c.isLight) Color.White else rgba(255, 255, 255, .07f))
            .drawBehind {
                val w = size.width
                val h = size.height
                if (c.isLight) {
                    ellipseGlow(w, 0f, w * 1.2f, h * 1.4f, rgba(90, 73, 242, .15f), .55f)
                    ellipseGlow(0f, h, w * .9f, h * 1.0f, rgba(10, 136, 173, .11f), .60f)
                    circleGlow(-50.dp.toPx() + 111.dp.toPx(), 309.25.dp.toPx() + 111.dp.toPx(), 111.dp.toPx(), rgba(214, 36, 127, .10f), .68f)
                } else {
                    ellipseGlow(w, 0f, w * 1.2f, h * 1.4f, rgba(106, 92, 255, .28f), .55f)
                    ellipseGlow(0f, h, w * .9f, h * 1.0f, rgba(0, 210, 255, .14f), .60f)
                    circleGlow(-50.dp.toPx() + 111.dp.toPx(), 309.25.dp.toPx() + 111.dp.toPx(), 111.dp.toPx(), rgba(255, 60, 172, .20f), .68f)
                }
            }
            .border(1.dp, borderColor, shape)
            .padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ───── balance + ring ─────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.Start) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val tint = if (selAccount != null) parseHex(selAccount.color).copy(alpha = .2f)
                    else if (sel == "free") FREE_COLOR.copy(alpha = .2f) else c.accent2.copy(alpha = .12f)
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(tint), contentAlignment = Alignment.Center) {
                        if (sel == null) {
                            FaIcon(FaGlyph.WALLET, 12.8f, color = c.accent2)
                        } else {
                            val bank = selAccount?.let { Banks.detect(it.name) }
                            val logo = bank?.let { BankLogos.byId[it.id] }
                            if (logo != null) {
                                Image(painterResource(logo), null, Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentScale = ContentScale.Fit)
                            } else {
                                FText(selAccount?.icon ?: "👛", 15.2f, lineHeight = 15.2f)
                            }
                        }
                    }
                    val labelText = when {
                        sel == null -> "موجودی کل"
                        selAccount != null -> if (Regex("^(کارت|حساب|بانک)").containsMatchIn(selAccount.name)) "موجودی ${selAccount.name}" else "موجودی کارت ${selAccount.name}"
                        else -> "موجودی بدون حساب"
                    }
                    FText(labelText, 15.2f, weight = FontWeight.SemiBold, color = c.muted, maxLines = 1)
                }
                val value = when {
                    sel == null -> fin.balance()
                    sel == "free" -> ab.free
                    else -> ab.map[sel] ?: 0.0
                }
                BalanceValue(Fa.plainMoney(value), 0.11f * contentW, available = contentW - 92f - 10f, negative = value < 0)
            }
            BudgetRing(fin.budgetUsagePct().toFloat())
        }

        // ───── account chips ─────
        if (!archived) {
            Box(Modifier.fillMaxWidth().height(42.dp)) {
                Row(
                    Modifier.fillMaxWidth().offset(y = (-2).dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (a in data.accounts) {
                        AcctChip(a.name, parseHex(a.color), sel == a.id, onClick = { onSelectAccount(a.id) }) {
                            AcctIcon(a.icon, parseHex(a.color), Banks.detect(a.name)?.id)
                        }
                    }
                    if (data.accounts.isNotEmpty() && (Fa.roundHalfUp(ab.free) != 0L || sel == "free")) {
                        AcctChip("بدون حساب", FREE_COLOR, sel == "free", onClick = { onSelectAccount("free") }) {
                            AcctIcon("👛", FREE_COLOR, null)
                        }
                    }
                    AcctChip(if (data.accounts.isNotEmpty()) "کارت جدید" else "افزودن کارت / حساب", c.accent1, false, add = true, onClick = onAddAccount) {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).background(if (c.isLight) mixOver(c.accent1, .18f, Color.White) else c.accent1.copy(alpha = .28f)),
                            contentAlignment = Alignment.Center
                        ) { FaIcon(FaGlyph.PLUS, 12f, color = c.muted) }
                    }
                }
            }
        }

        // ───── tiles ─────
        val statW = (contentW - 8f) / 2f - 26f
        val tod = remember(fin) { fin.heroToday() }
        val health = remember(fin) { fin.heroHealth() }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val incomeT = fin.incomeTotal().toDouble()
            val expenseT = fin.expenseTotal().toDouble()
            val measurer = rememberTextMeasurer()
            val dens = LocalDensity.current.density
            fun widthOf(text: String, f: Float): Float =
                measurer.measure(text, TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Black, fontSize = f.sp)).size.width / dens
            fun fit(text: String, avail: Float): Float {
                var f = 36f
                if (screenWidth >= 360f) f = min(f, 27f + (min(960f, max(360f, screenWidth)) - 360f) * (16f / 140f))
                for (i in 0 until 4) {
                    val w = widthOf(text, f)
                    if (!(avail > 0) || w <= avail + 0.5f || f <= 9f) break
                    f = max(9f, f * (avail / w) * 0.97f)
                }
                return f
            }
            val incText = Fa.plainMoney(incomeT)
            val expText = Fa.plainMoney(expenseT)
            val u = min(fit(incText, statW), fit(expText, statW))
            var ft = fit(if (tod.value != null) tod.value else "—", statW)
            if (ft > u * 1.15f) ft = u * 1.15f

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(c.good, Modifier.weight(1f), lightAlpha = .12f, lightBorder = .24f, darkAlpha = .10f) {
                    TileHead(FaGlyph.ARROW_TREND_UP, "درآمد", c.good)
                    TileAmount(incText, u, "تومان")
                }
                StatTile(c.bad, Modifier.weight(1f), lightAlpha = .10f, lightBorder = .22f, darkAlpha = .10f) {
                    TileHead(FaGlyph.ARROW_TREND_DOWN, "هزینه", c.bad)
                    TileAmount(expText, u, "تومان")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tc = when (tod.cls) { "c-bad" -> c.bad; "c-warn" -> c.warn; "c-idle" -> c.muted; else -> c.good }
                StatTile(tc, Modifier.weight(1f), lightAlpha = .12f, lightBorder = .28f, darkAlpha = .11f, onClick = onOpenToday) {
                    TileHead(FaGlyph.byName[tod.icon] ?: FaGlyph.BOLT, tod.label, tc, chevron = true)
                    Box(Modifier.fillMaxWidth().height(46.dp), contentAlignment = Alignment.CenterStart) {
                        Column(Modifier.fillMaxWidth()) {
                            FText(tod.value ?: "—", ft, Modifier.fillMaxWidth(), FontWeight.Black, c.text, lineHeight = ft * 1.1f, maxLines = 1, softWrap = false)
                            if (tod.unit.isNotEmpty()) FText(tod.unit, ft * 0.4116f, Modifier.fillMaxWidth(), FontWeight.Medium, c.muted, lineHeight = ft * 0.4116f * 1.4f, maxLines = 1)
                        }
                    }
                    Meter(tod.fill / 100f, tc, tod.spent, tod.pct)
                }
                val hc = health.colorHex?.let { parseHex(it) } ?: c.muted
                StatTile(hc, Modifier.weight(1f), lightAlpha = .12f, lightBorder = .28f, darkAlpha = .11f, onClick = onOpenHealth) {
                    TileHead(FaGlyph.HEART_PULSE, "سلامت مالی", hc, chevron = true)
                    Box(Modifier.fillMaxWidth().height(46.dp), contentAlignment = Alignment.CenterStart) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                            val bs = 0.2725f * (statW + 0f)
                            FText(health.score, bs, weight = FontWeight.Black, color = c.text, lineHeight = bs, maxLines = 1, softWrap = false)
                            FText("از ۱۰۰", 11f, weight = FontWeight.Medium, color = c.muted, lineHeight = 15.4f, modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                    Meter(health.fill / 100f, hc, health.note, null, more = true)
                }
            }
        }
    }
}

// ───────────────────────── pieces ─────────────────────────

@Composable
private fun BalanceValue(text: String, baseSize: Float, available: Float, negative: Boolean) {
    val c = Finora.colors
    val measurer = rememberTextMeasurer()
    val dens = LocalDensity.current.density
    fun widthOf(f: Float): Float =
        measurer.measure(text, TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Black, fontSize = f.sp, letterSpacing = (-0.3f).sp)).size.width / dens
    var f = baseSize
    for (i in 0 until 4) {
        val w = widthOf(f)
        if (!(available > 0) || w <= available || f <= 10f) break
        f = max(10f, f * (available / w) * 0.96f)
    }
    val w = widthOf(f)
    val h = f * 1.15f
    Column {
        val style = if (negative) TextStyle(
            color = c.bad, fontFamily = Vazir, fontWeight = FontWeight.Black, fontSize = f.sp, lineHeight = (f * 1.2f).sp,
            letterSpacing = (-0.3f).sp, textDirection = TextDirection.Rtl
        ) else TextStyle(
            brush = cssLinearGradient(
                135f, w, h, 0.3f to (if (c.isLight) Color(0xFF0F172A) else Color.White), 1f to (if (c.isLight) Color(0xFF4A3FD6) else Color(0xFFB9C4FF))
            ),
            fontFamily = Vazir, fontWeight = FontWeight.Black, fontSize = f.sp, lineHeight = (f * 1.2f).sp,
            letterSpacing = (-0.3f).sp, textDirection = TextDirection.Rtl
        )
        androidx.compose.foundation.text.BasicText(text = text, style = style, maxLines = 1, softWrap = false)
        FText("تومان", f * 0.34f, Modifier.padding(top = (f * 0.034f).dp), FontWeight.Medium, c.muted, lineHeight = f * 0.34f * 1.2f)
    }
}

@Composable
private fun AcctIcon(icon: String, color: Color, bankId: String?) {
    val c = Finora.colors
    val logo = bankId?.let { BankLogos.byId[it] }
    if (logo != null) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(Color.White)
                .border(1.dp, rgba(15, 23, 42, .10f), CircleShape).padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(logo), null, Modifier.size(24.dp).clip(CircleShape), contentScale = ContentScale.Fit)
        }
    } else {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(if (c.isLight) mixOver(color, .18f, Color.White) else color.copy(alpha = .28f)),
            contentAlignment = Alignment.Center
        ) { FText(icon, 15.2f, lineHeight = 15.2f, align = TextAlign.Center) }
    }
}

@Composable
private fun AcctChip(
    label: String, color: Color, pressed: Boolean, add: Boolean = false, onClick: () -> Unit, icon: @Composable () -> Unit
) {
    val c = Finora.colors
    val shape = RoundedCornerShape(999.dp)
    val bg = when {
        add -> Color.Transparent
        pressed -> if (c.isLight) mixOver(color, .14f, Color.White) else color.copy(alpha = .18f)
        else -> if (c.isLight) c.surface else rgba(255, 255, 255, .05f)
    }
    val borderCol = when {
        pressed -> color
        add -> if (c.isLight) c.lineStrong else rgba(255, 255, 255, .10f)
        else -> if (c.isLight) rgba(15, 23, 42, .10f) else rgba(255, 255, 255, .10f)
    }
    Row(
        Modifier
            .height(40.dp)
            .clip(shape)
            .background(bg)
            .drawBehind {
                if (pressed) {
                    val sw = 1.dp.toPx()
                    drawRoundRect(
                        color = color, topLeft = Offset(1.5f * sw, 1.5f * sw),
                        size = androidx.compose.ui.geometry.Size(size.width - 3f * sw, size.height - 3f * sw),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                        style = Stroke(sw)
                    )
                }
                if (add) {
                    val sw = 1.dp.toPx()
                    drawRoundRect(
                        color = borderCol, topLeft = Offset(sw / 2, sw / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                        style = Stroke(sw, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
                    )
                }
            }
            .then(if (!add) Modifier.border(1.dp, borderCol, shape) else Modifier)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(start = 5.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // RTL: first child (icon) sits at the right edge
        icon()
        FText(label, 12.8f, weight = FontWeight.Bold, color = if (add) c.muted else c.text, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun StatTile(
    color: Color, modifier: Modifier, lightAlpha: Float, lightBorder: Float, darkAlpha: Float,
    onClick: (() -> Unit)? = null, content: @Composable () -> Unit
) {
    val c = Finora.colors
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .height(126.dp)
            .clip(shape)
            .drawBehind {
                val brush = if (c.isLight)
                    cssLinearGradient(160f, size.width, size.height, 0f to color.copy(alpha = lightAlpha), .75f to Color.White, 1f to Color.White)
                else
                    cssLinearGradient(160f, size.width, size.height, 0f to color.copy(alpha = darkAlpha), 1f to rgba(255, 255, 255, .03f))
                drawRect(brush)
            }
            .border(1.dp, if (c.isLight) color.copy(alpha = lightBorder) else rgba(255, 255, 255, .10f), shape)
            .then(if (onClick != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick) else Modifier)
            .padding(start = 13.dp, top = 11.dp, end = 13.dp, bottom = 11.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) { content() }
}

@Composable
private fun TileHead(glyph: String, label: String, color: Color, chevron: Boolean = false) {
    val c = Finora.colors
    Row(Modifier.fillMaxWidth().height(24.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
            FaIcon(glyph, 10.88f, color = color)
        }
        FText(label, 11.84f, Modifier.weight(1f), color = c.muted, lineHeight = 15.984f, maxLines = 1)
        if (chevron) FaIcon(FaGlyph.CHEVRON_LEFT, 11.2f, color = c.muted)
    }
}

@Composable
private fun TileAmount(text: String, size: Float, unit: String) {
    val c = Finora.colors
    Column(Modifier.fillMaxWidth().padding(vertical = 11.83.dp)) {
        FText(text, size, Modifier.fillMaxWidth(), FontWeight.Black, c.text, lineHeight = size * 1.1f, maxLines = 1, softWrap = false)
        FText(unit, size * 0.42f, Modifier.fillMaxWidth(), FontWeight.Medium, c.muted, lineHeight = size * 0.42f * 1.4f)
    }
}

@Composable
private fun Meter(fraction: Float, color: Color, foot: String, footEnd: String?, more: Boolean = false) {
    val c = Finora.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(99.dp)).background(rgba(127, 127, 127, .20f))) {
            val f = fraction.coerceIn(0f, 1f)
            if (f > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(f)
                        .height(5.dp)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(99.dp))
                        .drawBehind {
                            drawRect(cssLinearGradient(90f, size.width, size.height, 0f to color.copy(alpha = .55f), 1f to color))
                        }
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            FText(foot, 9.92f, color = c.muted, lineHeight = 15f, maxLines = 1)
            if (more) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    FText("جزئیات", 9.92f, weight = FontWeight.SemiBold, color = color, lineHeight = 15f)
                    FaIcon(FaGlyph.CHEVRON_LEFT, 9.6f, color = color)
                }
            } else if (footEnd != null) {
                FText(footEnd, 9.92f, color = c.muted, lineHeight = 15f, maxLines = 1)
            }
        }
    }
}

/** .balance-ring — conic arc cyan → violet → pink with caps, disc and the animated percentage. */
@Composable
private fun BudgetRing(targetPct: Float) {
    val c = Finora.colors
    val anim = remember { Animatable(0f) }
    LaunchedEffect(targetPct) { anim.animateTo(targetPct, tween(900, easing = EaseOutCubic)) }
    val pct = anim.value
    val track = if (c.isLight) rgba(15, 23, 42, .09f) else rgba(248, 250, 252, .09f)
    val c1 = if (c.isLight) Color(0xFF0A88AD) else Color(0xFF00D2FF)
    val c2 = if (c.isLight) Color(0xFF5A49F2) else Color(0xFF6A5CFF)
    val c3 = if (c.isLight) Color(0xFFD6247F) else Color(0xFFFF3CAC)

    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(92.dp)) {
            val s = size.width / 92f
            val center = Offset(size.width / 2f, size.height / 2f)
            val p = (pct / 100f).coerceIn(0f, 1f)
            rotate(-90f, pivot = center) {
                val brush = if (p > 0f)
                    Brush.sweepGradient(0f to c1, 0.55f * p to c2, p to c3, p to track, 1f to track, center = center)
                else Brush.sweepGradient(0f to track, 1f to track, center = center)
                drawCircle(brush = brush, radius = 46f * s, center = center)
            }
            // disc
            val discR = 34.95f * s
            val discCenterY = center.y - 34.95f * s + 0.28f * 69.9f * s
            val discBrush = if (c.isLight)
                Brush.radialGradient(0f to Color(0xFFE8E6FD), .58f to Color(0xFFF3F5FB), 1f to Color(0xFFE8ECF7), center = Offset(center.x, discCenterY), radius = 61.3f * s)
            else
                Brush.radialGradient(0f to Color(0xFF1D2245), .58f to Color(0xFF0B1022), 1f to Color(0xFF070B17), center = Offset(center.x, discCenterY), radius = 61.3f * s)
            drawCircle(brush = discBrush, radius = discR, center = center)
            val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, discR)) }
            clipPath(disc) {
                val top = center.y - discR
                drawRect(
                    Brush.verticalGradient(0f to (if (c.isLight) rgba(15, 23, 42, .07f) else rgba(255, 255, 255, .06f)), 0.12f to Color.Transparent, startY = top, endY = top + discR * 2),
                    topLeft = Offset(center.x - discR, top), size = androidx.compose.ui.geometry.Size(discR * 2, discR * 2)
                )
                if (!c.isLight) drawRect(
                    Brush.verticalGradient(0.82f to Color.Transparent, 1f to rgba(0, 0, 0, .28f), startY = top, endY = top + discR * 2),
                    topLeft = Offset(center.x - discR, top), size = androidx.compose.ui.geometry.Size(discR * 2, discR * 2)
                )
            }
            drawCircle(color = if (c.isLight) rgba(15, 23, 42, .10f) else rgba(255, 255, 255, .10f), radius = discR - 0.5f * s, center = center, style = Stroke(1f * s))
            // caps
            if (p > 0f) {
                drawCircle(c1, radius = 3.95f * s, center = Offset(center.x, center.y - 42.05f * s))
                val ang = Math.toRadians((p * 360f).toDouble())
                val ex = center.x + 42f * s * sin(ang).toFloat()
                val ey = center.y - 42f * s * cos(ang).toFloat()
                drawCircle(c3.copy(alpha = .35f), radius = 4.55f * s + 2.76f * s, center = Offset(ex, ey))
                drawCircle(c3, radius = 4.55f * s, center = Offset(ex, ey))
                drawCircle(if (c.isLight) Color.White else rgba(255, 255, 255, .5f), radius = 4.55f * s, center = Offset(ex, ey), style = Stroke(0.74f * s))
                drawCircle(Color.White.copy(alpha = .92f), radius = 2.05f * s, center = Offset(ex, ey))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.76.dp)) {
            val pctBrush = if (c.isLight) Brush.verticalGradient(0f to Color(0xFF0F172A), 1f to Color(0xFF312D84))
            else Brush.verticalGradient(0f to Color(0xFFF8FAFC), 1f to Color(0xFFB8B3FE))
            androidx.compose.foundation.text.BasicText(
                text = Fa.percent(pct.toDouble(), 0),
                style = TextStyle(
                    fontFamily = Vazir, fontWeight = FontWeight.Black, fontSize = 18.4.sp, lineHeight = 18.4.sp,
                    letterSpacing = (-0.368).sp, brush = pctBrush, textAlign = TextAlign.Center
                ),
                maxLines = 1, softWrap = false
            )
            FText("مصرف بودجه", 9.28f, color = c.muted, lineHeight = 15f, align = TextAlign.Center, maxLines = 1, softWrap = false)
        }
    }
}
