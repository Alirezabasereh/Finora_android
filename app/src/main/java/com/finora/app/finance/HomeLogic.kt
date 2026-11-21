package com.finora.app.finance

import com.finora.app.data.Account
import com.finora.app.data.Transaction
import com.finora.app.util.Fa
import com.finora.app.util.Jalali
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

/** transaction-list filters (web: state.filters) */
data class TxFilters(
    val type: String = "all",        // all | income | expense
    val category: String = "all",
    val account: String = "all",     // all | none | <accountId>
    val query: String = "",
    val from: String = "",           // Gregorian "yyyy-MM-dd" or ""
    val to: String = "",
    val minAmount: String = "",
    val maxAmount: String = "",
    val sortBy: String = "dateDesc"  // dateDesc | dateAsc | amountDesc | amountAsc
) {
    fun activeCount(): Int {
        var n = 0
        if (query.trim().isNotEmpty()) n++
        if (type != "all") n++
        if (category != "all") n++
        if (account != "all") n++
        if (from.isNotEmpty()) n++
        if (to.isNotEmpty()) n++
        if (minAmount.isNotEmpty()) n++
        if (maxAmount.isNotEmpty()) n++
        if (sortBy != "dateDesc") n++
        return n
    }
}

/** JS parseFloat(): leading numeric prefix, NaN -> null */
fun jsParseFloat(s: String): Double? {
    val m = Regex("^\\s*[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?").find(s) ?: return null
    return m.value.trim().toDoubleOrNull()
}

fun Finance.filteredTransactions(f: TxFilters): List<Transaction> {
    val query = f.query.trim().lowercase()
    val fromTs = if (f.from.isNotEmpty()) runCatching { LocalDate.parse(f.from).atStartOfDay(zone).toInstant().toEpochMilli() }.getOrNull() else null
    val toTs = if (f.to.isNotEmpty()) runCatching { LocalDate.parse(f.to).atTime(LocalTime.of(23, 59, 59, 999_000_000)).atZone(zone).toInstant().toEpochMilli() }.getOrNull() else null
    val minAmt = if (f.minAmount.isNotEmpty()) jsParseFloat(f.minAmount) else null
    val maxAmt = if (f.maxAmount.isNotEmpty()) jsParseFloat(f.maxAmount) else null

    val filtered = activeTransactions().filter { t ->
        if (f.type != "all" && t.type != f.type) return@filter false
        if (f.category != "all" && t.category != f.category) return@filter false
        if (f.account == "none") { if (t.accountId != null) return@filter false }
        else if (f.account != "all" && t.accountId != f.account) return@filter false
        val created = t.createdAt
        if (fromTs != null && created < fromTs) return@filter false
        if (toTs != null && created > toTs) return@filter false
        val amt = t.amount.toDouble()
        if (minAmt != null && amt < minAmt) return@filter false
        if (maxAmt != null && amt > maxAmt) return@filter false
        if (query.isEmpty()) return@filter true
        val cat = categoryByKey(t.category)
        val hay = listOf(t.note, categoryLabel(t.category), cat?.emoji ?: "", if (t.type == "income") "درآمد" else "هزینه", t.amount.toString())
            .joinToString(" ").lowercase()
        hay.contains(query)
    }
    return when (f.sortBy) {
        "amountDesc" -> filtered.sortedByDescending { it.amount }
        "amountAsc" -> filtered.sortedBy { it.amount }
        "dateAsc" -> filtered.sortedBy { it.createdAt }
        else -> filtered.sortedByDescending { it.createdAt }
    }
}

/** formatTransactionDate(): «۲۸ آبان ۱۴۰۵، ساعت ۱۲:۳۰» */
fun formatTransactionDate(ms: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val dt = Instant.ofEpochMilli(ms).atZone(zone)
    val j = Jalali.toJalali(dt.year, dt.monthValue, dt.dayOfMonth)
    val hh = dt.hour.toString().padStart(2, '0')
    val mm = dt.minute.toString().padStart(2, '0')
    return "${Fa.digits(j.d)} ${Jalali.monthNames[j.m - 1]} ${Fa.digits(j.y)}، ساعت ${Fa.digits(hh)}:${Fa.digits(mm)}"
}

fun formatJalaliDate(ms: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val dt = Instant.ofEpochMilli(ms).atZone(zone)
    val j = Jalali.toJalali(dt.year, dt.monthValue, dt.dayOfMonth)
    return "${Fa.digits(j.d)} ${Jalali.monthNames[j.m - 1]} ${Fa.digits(j.y)}"
}

// ───────────── home insight chips ─────────────

data class InsightChip(val type: String, val icon: String, val title: String, val sub: String)

fun Finance.homeInsights(): List<InsightChip> {
    val chips = ArrayList<InsightChip>()
    val tc = todayTxCount()
    chips.add(
        InsightChip(
            if (tc.total > 0) "good" else "warn", "receipt", "تراکنش‌های امروز",
            if (tc.total > 0)
                Fa.digits(tc.total) + " تراکنش" + " · " + (if (tc.exp != 0) Fa.digits(tc.exp) + " هزینه" else "") +
                    (if (tc.exp != 0 && tc.inc != 0) "، " else "") + (if (tc.inc != 0) Fa.digits(tc.inc) + " درآمد" else "")
            else "هنوز تراکنشی ثبت نشده"
        )
    )
    val zs = zeroSpendDays()
    chips.add(
        InsightChip(
            "good", "leaf", "روزهای بدون خرج",
            if (zs.tracked > 0) Fa.digits(zs.zero) + " از " + Fa.digits(zs.tracked) + " روز · " +
                Fa.digits(Fa.roundHalfUp(zs.zero.toDouble() / zs.tracked * 100)) + "٪ از اول ماه"
            else "با ثبت اولین تراکنش شروع می‌شود"
        )
    )
    val snap = snapshot
    if (snap.projectedExpense > 0) {
        chips.add(InsightChip("good", "chart-line", "پیش‌بینی", "پایان ماه: ~" + Fa.compact(snap.projectedExpense) + " تومان"))
    }
    return chips
}

// ───────────── hero tiles ─────────────

data class HeroTodayUi(
    val cls: String,          // c-good | c-warn | c-bad | c-idle
    val label: String, val icon: String,
    val value: String?, val unit: String,   // value null -> "—"
    val tag: String, val note: String, val fill: Int, val spent: String, val pct: String
)

fun Finance.heroToday(): HeroTodayUi {
    if (viewingMonthKey != null) {
        val total = activeTransactions().filter { it.type == "expense" }.sumOf { it.amount }.toDouble()
        val m = viewingMonthKey.split("_")
        val n = Jalali.daysInMonth(m[0].toInt(), m[1].toInt()).let { if (it == 0) 30 else it }
        return HeroTodayUi("c-idle", "میانگین خرج روزانه", "calendar-day", Fa.plainMoney(total / n), "تومان", "",
            "ماه فعلی آرشیوی هست؛ «تراز امروز» فقط برای ماه جاری محاسبه می‌شه", 0, "کل هزینه: " + Fa.compact(total), "")
    }
    val tb = computeTodayBalance()
    if (tb.budget <= 0) {
        return HeroTodayUi("c-idle", "تراز امروز", "bolt", null, "", "", "با ثبت درآمد، سقف خرج امروز محاسبه می‌شه", 0, "", "")
    }
    val v = todayBalanceView(tb)
    val fill = min(100L, Fa.roundHalfUp(tb.useRatio * 100)).toInt()
    return HeroTodayUi(tb.safeClass, "تراز امروز", v.icon, v.value, v.unit, "سقف " + Fa.compact(tb.allowance), v.note, fill,
        "خرج امروز: " + Fa.compact(tb.todaySpent), Fa.digits(fill) + "٪ از سقف")
}

data class HeroHealthUi(val cls: String, val colorHex: String?, val score: String, val tag: String, val note: String, val fill: Int)

fun Finance.heroHealth(): HeroHealthUi {
    val h = calcHealthScore(snapshot)
    if (!h.hasData || h.total == null) return HeroHealthUi("c-idle", null, "--", "", "داده‌ی کافی نیست", 0)
    val lbl = healthLabel(h.total)
    return HeroHealthUi(
        "", lbl.color, Fa.digits(h.total), lbl.text,
        if (h.total >= 70) "وضعیتت خوبه" else if (h.total >= 50) "نیاز به کمی توجه" else "هزینه‌ها رو مرور کن", h.total
    )
}

fun Account.bankId(): String? = Banks.detect(name)?.id
