package com.finora.app.ui.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.finora.app.data.Account
import com.finora.app.data.Transaction
import com.finora.app.finance.Banks
import com.finora.app.finance.Finance
import com.finora.app.finance.TxFilters
import com.finora.app.finance.filteredTransactions
import com.finora.app.finance.formatTransactionDate
import com.finora.app.ui.common.BankLogos
import com.finora.app.ui.common.FText
import com.finora.app.ui.common.FaGlyph
import com.finora.app.ui.common.FaIcon
import com.finora.app.ui.common.GlassCard
import com.finora.app.ui.common.parseHex
import com.finora.app.ui.theme.Finora
import com.finora.app.ui.theme.rgba
import com.finora.app.util.Fa
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private fun mix(c: Color, a: Float, base: Color) = Color(
    c.red * a + base.red * (1 - a), c.green * a + base.green * (1 - a), c.blue * a + base.blue * (1 - a), 1f
)

/** Two-or-more inline items that wrap like CSS flex-wrap (column-gap [hGap], row-gap [vGap]). */
@Composable
private fun Wrap(modifier: Modifier = Modifier, hGap: Int, vGap: Int, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val h = hGap.dp.roundToPx()
        val v = vGap.dp.roundToPx()
        val placeables = measurables.map { it.measure(Constraints(maxWidth = constraints.maxWidth)) }
        var x = 0
        var y = 0
        var rowH = 0
        val pos = ArrayList<Pair<Int, Int>>()
        for (p in placeables) {
            if (x > 0 && x + p.width > constraints.maxWidth) { x = 0; y += rowH + v; rowH = 0 }
            pos.add(x to y)
            x += p.width + h
            rowH = maxOf(rowH, p.height)
        }
        layout(constraints.maxWidth, y + rowH) {
            placeables.forEachIndexed { i, p -> p.placeRelative(pos[i].first, pos[i].second) }
        }
    }
}

/** The "تراکنش‌ها" section: header with the filter button and one row per (filtered) transaction. */
@Composable
fun TransactionsCard(
    fin: Finance,
    filters: TxFilters,
    onOpenFilter: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    val c = Finora.colors
    val list = remember(fin, filters) { fin.filteredTransactions(filters) }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(19.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                FText("تراکنش‌ها", 14.4f, weight = FontWeight.Bold)
                val shape = RoundedCornerShape(14.dp)
                val count = filters.activeCount()
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(shape)
                        .background(if (c.isLight) Color.White else rgba(255, 255, 255, .07f))
                        .border(1.dp, if (c.isLight) rgba(15, 23, 42, .11f) else rgba(255, 255, 255, .10f), shape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenFilter),
                    contentAlignment = Alignment.Center
                ) {
                    FaIcon(FaGlyph.SLIDERS, 16f, color = c.text)
                    if (count > 0) {
                        Box(
                            Modifier.align(Alignment.TopStart).padding(start = 3.dp, top = 3.dp).size(16.dp).clip(CircleShape).background(c.accent1),
                            contentAlignment = Alignment.Center
                        ) { FText(Fa.digits(count), 9.6f, color = Color.White, weight = FontWeight.Bold, lineHeight = 12f, align = TextAlign.Center) }
                    }
                }
            }
            if (list.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    FaIcon(FaGlyph.WALLET, 32f, color = c.accent1)
                    FText("تراکنشی مطابق فیلترها پیدا نشد", 16.8f, Modifier.padding(top = 12.dp, bottom = 8.dp), FontWeight.Bold, c.text, TextAlign.Center)
                    FText("فیلترها رو تغییر بده یا پاک‌سازی کن تا همه تراکنش‌ها دیده بشن.", 14.72f, color = c.muted, align = TextAlign.Center, lineHeight = 14.72f * 1.8f)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (t in list) key(t.id) { TransactionRow(fin, t, onEdit, onDelete) }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(fin: Finance, t: Transaction, onEdit: (Transaction) -> Unit, onDelete: (Transaction) -> Unit) {
    val c = Finora.colors
    val cat = fin.categoryByKey(t.category)
    val color = parseHex(cat?.color ?: if (t.type == "income") "#18e6a6" else "#ff5d7d")
    val title = (if (cat != null) cat.emoji + " " else "") + (cat?.name ?: fin.categoryLabel(t.category))
    val sign = if (t.type == "income") "+" else "-"
    val account: Account? = t.accountId?.let { id -> fin.data.accounts.firstOrNull { it.id == id } }
    val shape = RoundedCornerShape(22.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (c.isLight) c.surface else rgba(255, 255, 255, .05f))
            .border(1.dp, if (c.isLight) rgba(15, 23, 42, .10f) else rgba(255, 255, 255, .04f), shape)
            .padding(start = 13.dp, top = 14.dp, end = 13.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.133f)),
                contentAlignment = Alignment.Center
            ) {
                FaIcon(if (t.type == "income") FaGlyph.ARROW_TREND_UP else FaGlyph.ARROW_TREND_DOWN, 14.4f, color = color)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FText(title, 14.72f, Modifier.padding(bottom = 4.dp), FontWeight.Bold, c.text, lineHeight = 22.08f, maxLines = 1)
                BasicText(
                    text = if (t.note.isEmpty()) "بدون توضیح" else t.note,
                    style = TextStyle(color = c.muted, fontSize = 14.08.sp, fontFamily = com.finora.app.ui.theme.Vazir, lineHeight = 22.sp),
                    maxLines = 1, overflow = TextOverflow.Clip, softWrap = false
                )
                Wrap(Modifier.fillMaxWidth(), hGap = 8, vGap = 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        FaIcon(FaGlyph.CLOCK, 9.6f, color = c.muted, regular = true)
                        FText(formatTransactionDate(t.createdAt, fin.zone), 9.6f, color = c.muted, lineHeight = 14.4f, maxLines = 1, softWrap = false)
                    }
                    if (account != null) AccountBadge(account)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            FText(
                sign + Fa.money(t.amount), 14.08f, Modifier.padding(bottom = 6.dp), FontWeight.ExtraBold,
                if (t.type == "income") c.good else c.bad, TextAlign.Left, lineHeight = 20.416f, maxLines = 1, softWrap = false
            )
            if (!fin.isViewingArchive) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val editShape = RoundedCornerShape(10.dp)
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(editShape)
                            .background(c.accent2.copy(alpha = if (c.isLight) .10f else .12f))
                            .border(1.dp, c.accent2.copy(alpha = if (c.isLight) .22f else .16f), editShape)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onEdit(t) },
                        contentAlignment = Alignment.Center
                    ) { FaIcon(FaGlyph.PEN_TO_SQUARE, 16f, color = c.accent2) }
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.bad.copy(alpha = if (c.isLight) .10f else .12f))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDelete(t) },
                        contentAlignment = Alignment.Center
                    ) { FaIcon(FaGlyph.TRASH, 16f, color = c.bad) }
                }
            }
        }
    }
}

@Composable
private fun AccountBadge(a: Account) {
    val c = Finora.colors
    val col = parseHex(a.color)
    val text = if (c.isLight) mix(col, .72f, Color(0xFF0F172A)) else col
    val bg = if (c.isLight) mix(col, .14f, Color.White) else col.copy(alpha = .14f)
    val bank = Banks.detect(a.name)
    val logo = bank?.let { BankLogos.byId[it.id] }
    Row(
        Modifier.clip(RoundedCornerShape(99.dp)).background(bg).padding(horizontal = 7.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (logo != null) {
            Image(
                painterResource(logo), null,
                Modifier.padding(start = 2.dp).size(16.dp).clip(RoundedCornerShape(5.dp)).background(Color.White)
                    .border(1.dp, rgba(15, 23, 42, .14f), RoundedCornerShape(5.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            FText(a.icon, 9.92f, lineHeight = 14.88f)
        }
        FText(a.name, 9.92f, weight = FontWeight.Bold, color = text, lineHeight = 14.88f, maxLines = 1, softWrap = false)
    }
}
