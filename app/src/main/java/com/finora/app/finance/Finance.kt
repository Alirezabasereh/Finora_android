package com.finora.app.finance

import com.finora.app.data.AppData
import com.finora.app.data.Category
import com.finora.app.data.Transaction
import com.finora.app.util.Jalali
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class MonthRange(val start: Long, val end: Long, val jYear: Int, val jMonth: Int)
data class DayParts(val jy: Int, val jm: Int, val jd: Int)

data class ExpenseEntry(
    val key: String, val amount: Long, val name: String, val color: String, val emoji: String, val excluded: Boolean
)

data class SmartForecast(
    val method: String, val monthsUsed: Int,
    val fixedPaid: Double, val fixedRemaining: Double, val fixedPlan: Double, val payDay: Int, val fixedRemainingDay: Int,
    val varRate: Double, val varLo: Double, val varHi: Double, val bigReserve: Double,
    val remaining: Double, val lo: Double, val hi: Double
)

data class PeakExpenseDay(val day: Int, val amount: Double, val dateMillis: Long)
data class TopCategory(val key: String?, val amount: Double, val category: Category?)

data class Snapshot(
    val currentMonth: MonthRange, val prevMonth: MonthRange,
    val currentExpenses: List<Transaction>, val prevExpenses: List<Transaction>,
    val currentIncome: List<Transaction>, val prevIncome: List<Transaction>,
    val currentExpenseTotal: Double, val prevExpenseTotal: Double,
    val currentIncomeTotal: Double, val prevIncomeTotal: Double,
    val diff: Double, val diffPct: Double,
    val daysPassed: Int, val monthDays: Int,
    val averageDailyExpense: Double, val historicalDailyExpense: Double, val historicalDailyRates: List<Double>,
    val forecastDailyRate: Double, val remainingDays: Int, val projectedRemainingExpense: Double, val projectedExpense: Double,
    val smartForecast: SmartForecast,
    val currentExpenseDays: Int, val averageExpenseOnActiveDays: Double,
    val rolling30ExpenseTotal: Double, val rolling30IncomeTotal: Double,
    val rolling30ExpenseAvg: Double, val rolling30IncomeAvg: Double,
    val averageDailyIncome: Double, val averageDailyExpenseInMonth: Double,
    val currentTop: TopCategory, val prevTop: TopCategory,
    val peakExpenseTransaction: Transaction?,
    val currentWeekExpenses: Double, val previousWeekExpenses: Double, val weekDelta: Double,
    val peakDay: PeakExpenseDay?, val expenseVsIncomePct: Double?
)

data class TodayBalance(
    val budget: Double, val md: Int, val td: Int, val perDay: DoubleArray, val cum: DoubleArray,
    val room: Double, val allowance: Double, val todaySpent: Double, val leftToday: Double,
    val useRatio: Double, val safeClass: String
)

data class TodayView(val value: String, val unit: String, val note: String, val icon: String)

data class HealthScore(
    val total: Int?, val hasData: Boolean,
    val savingsScore: Int?, val budgetScore: Int?, val trendScore: Int?, val trendIsBaseline: Boolean,
    val savingsRate: Double?
)

data class HealthLabel(val text: String, val color: String)

data class AccountBalances(val map: LinkedHashMap<String, Double>, val free: Double, val total: Double)

/**
 * All the derived numbers of the web app (summary, hero tiles, forecast, health score, breakdown…),
 * ported 1:1 from the JavaScript. `nowMillis` / `zone` are injectable so results can be compared to the web version.
 */
class Finance(
    val data: AppData,
    val zone: ZoneId = ZoneId.systemDefault(),
    val nowMillis: Long = System.currentTimeMillis(),
    val excludeLoans: Boolean = false,
    val viewingMonthKey: String? = null
) {
    companion object {
        const val SF_BIG_PURCHASE_SHARE = 0.08
    }

    // ───────────── dates ─────────────

    private fun localDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    private fun startOfDay(d: LocalDate): Long = d.atStartOfDay(zone).toInstant().toEpochMilli()
    private fun endOfDay(d: LocalDate): Long = d.atTime(LocalTime.of(23, 59, 59, 999_000_000)).atZone(zone).toInstant().toEpochMilli()

    fun jalaliParts(ms: Long): DayParts {
        val p = Jalali.parts(ms, zone)
        return DayParts(p.y, p.m, p.d)
    }

    val nowParts: DayParts get() = jalaliParts(nowMillis)

    fun getMonthRange(offset: Int = 0, base: Long = nowMillis): MonthRange {
        val b = jalaliParts(base)
        var jy = b.jy
        var jm = b.jm + offset
        while (jm <= 0) { jm += 12; jy -= 1 }
        while (jm > 12) { jm -= 12; jy += 1 }
        val nextY = if (jm == 12) jy + 1 else jy
        val nextM = if (jm == 12) 1 else jm + 1
        val s = Jalali.fromJalali(jy, jm, 1)
        val e = Jalali.fromJalali(nextY, nextM, 1)
        return MonthRange(startOfDay(LocalDate.of(s.y, s.m, s.d)), startOfDay(LocalDate.of(e.y, e.m, e.d)), jy, jm)
    }

    fun daysInCurrentJalaliMonth(): Int = Jalali.daysInMonth(nowParts.jy, nowParts.jm)
    fun daysElapsedInMonth(): Int = max(1, nowParts.jd)

    // ───────────── transaction sources ─────────────

    fun activeTransactions(): List<Transaction> =
        if (viewingMonthKey == null) data.transactions else data.archive[viewingMonthKey]?.transactions ?: emptyList()

    fun activeInitialBalance(): Double =
        if (viewingMonthKey == null) data.initialBalance.toDouble() else data.archive[viewingMonthKey]?.initialBalance ?: 0.0

    val isViewingArchive: Boolean get() = viewingMonthKey != null

    /** live list + every archived month, de-duplicated by id (live wins). */
    val allCombined: List<Transaction> by lazy {
        val seen = HashSet<String>()
        val out = ArrayList<Transaction>()
        for (t in data.transactions) if (seen.add(t.id)) out.add(t)
        for (entry in data.archive.values) for (t in entry.transactions) if (seen.add(t.id)) out.add(t)
        out
    }

    private fun inRange(start: Long, end: Long, type: String = "all"): List<Transaction> =
        allCombined.filter { t ->
            val c = t.createdAt
            if (c < start || c >= end) false else (type == "all" || t.type == type)
        }

    private fun sum(list: List<Transaction>): Double = list.sumOf { it.amount.toDouble() }

    // ───────────── category helpers ─────────────

    fun categoryByKey(key: String): Category? = data.categories.firstOrNull { it.key == key }
    fun categoryLabel(key: String): String = categoryByKey(key)?.name ?: key
    fun categoryColor(key: String): String = categoryByKey(key)?.color ?: "#7dd3fc"
    fun categoryEmoji(key: String): String = categoryByKey(key)?.emoji ?: "✨"

    // ───────────── totals ─────────────

    fun incomeTotal(): Long = activeTransactions().filter { it.type == "income" }.sumOf { it.amount }
    fun expenseTotal(): Long = activeTransactions().filter { it.type == "expense" }.sumOf { it.amount }
    fun netFlow(): Long = incomeTotal() - expenseTotal()
    fun balance(): Double = activeInitialBalance() + netFlow()

    fun budgetBase(): Double = max(0.0, data.initialBalance.toDouble() + incomeTotal())
    fun budgetUsagePct(): Double {
        val base = budgetBase()
        return if (base > 0) min(100.0, max(0.0, expenseTotal() / base * 100.0)) else 0.0
    }
    fun budgetLevel(pct: Double): String = if (pct >= 90) "bad" else if (pct >= 70) "warn" else "ok"

    // ───────────── accounts ─────────────

    fun liveBalance(): Double {
        var net = 0.0
        for (t in data.transactions) net += (if (t.type == "income") 1 else -1) * t.amount.toDouble()
        return data.initialBalance.toDouble() + net
    }

    fun accountBalances(): AccountBalances {
        val map = LinkedHashMap<String, Double>()
        for (a in data.accounts) map[a.id] = a.opening
        for (t in allCombined) {
            val id = t.accountId ?: continue
            if (!map.containsKey(id)) continue
            map[id] = map[id]!! + (if (t.type == "income") 1 else -1) * t.amount.toDouble()
        }
        for (tr in data.accountTransfers) {
            if (tr.from.isNotEmpty() && map.containsKey(tr.from)) map[tr.from] = map[tr.from]!! - tr.amount
            if (tr.to.isNotEmpty() && map.containsKey(tr.to)) map[tr.to] = map[tr.to]!! + tr.amount
        }
        var s = 0.0
        for (v in map.values) s += v
        val total = liveBalance()
        return AccountBalances(map, total - s, total)
    }

    // ───────────── expense breakdown ─────────────

    fun expenseBreakdown(): List<ExpenseEntry> {
        val totals = LinkedHashMap<String, Long>()
        for (c in data.categories) if (c.type == "expense") totals[c.key] = 0L
        for (t in activeTransactions()) {
            if (t.type != "expense") continue
            val key = if (t.loanId != null) "loanes" else t.category
            totals[key] = (totals[key] ?: 0L) + t.amount
        }
        return totals.entries
            .map { (key, amount) ->
                ExpenseEntry(key, amount, categoryLabel(key), categoryColor(key), categoryEmoji(key), excludeLoans && key == "loanes")
            }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }
    }

    data class SplitEntries(val active: List<ExpenseEntry>, val excluded: List<ExpenseEntry>, val total: Long)

    fun splitExpenseEntries(entries: List<ExpenseEntry>): SplitEntries {
        val active = entries.filter { !it.excluded }
        return SplitEntries(active, entries.filter { it.excluded }, active.sumOf { it.amount })
    }

    // ───────────── helpers used by the forecast ─────────────

    private fun median(list: List<Double>): Double {
        val a = list.filter { it.isFinite() }.sorted()
        if (a.isEmpty()) return 0.0
        val m = a.size / 2
        return if (a.size % 2 == 1) a[m] else (a[m - 1] + a[m]) / 2
    }

    fun isFixedObligation(t: Transaction): Boolean {
        if (t.type != "expense") return false
        if (t.loanId != null || t.category == "loanes") return true
        val catName = try { categoryLabel(t.category) } catch (e: Exception) { "" }
        return Regex("قسط|اقساط").containsMatchIn("${t.note} $catName ${t.category}")
    }

    private fun monthKeyDash(ms: Long): String {
        val p = jalaliParts(ms)
        return "${p.jy}-${p.jm}"
    }

    /** Exact "installments still unpaid this month" from the loans module (null when there are no loans). */
    fun loanInstallmentsRemainingThisMonth(): Double? {
        val loans = data.loans
        if (loans.isEmpty()) return null
        val key = monthKeyDash(nowMillis)
        val paidBy = HashMap<String, Double>()
        for (t in data.transactions) {
            if (monthKeyDash(t.createdAt) != key) continue
            var id: String? = t.loanId
            if (id == null) {
                val m = Regex("^قسط وام: (.+)$").find(t.note)
                if (m != null) {
                    val l = loans.firstOrNull { it.title == m.groupValues[1] }
                    if (l != null) id = l.id
                }
            }
            if (id != null) paidBy[id] = (paidBy[id] ?: 0.0) + t.amount
        }
        var remaining = 0.0
        for (l in loans) {
            val inst = l.installmentAmount
            val active = l.paidInstallments < l.totalInstallments
            val paid = min(paidBy[l.id] ?: 0.0, inst)
            if (active) remaining += max(0.0, inst - paid)
        }
        return remaining
    }

    private data class DayAmount(val day: Int, val amount: Double)

    private fun weightedMedianDay(items: List<DayAmount>): Int {
        val total = items.sumOf { it.amount }
        if (total == 0.0) return 0
        val sorted = items.sortedBy { it.day }
        var acc = 0.0
        for (i in sorted) {
            acc += i.amount
            if (acc >= total / 2) return i.day
        }
        return sorted.last().day
    }

    private data class Hist(val days: Int, val fixedTotal: Double, val fixedDays: List<DayAmount>, val regularRate: Double, val bigTotal: Double)

    fun buildSmartForecast(now: Long, expenses: List<Transaction>, incomeTotal: Double, spent: Double, daysPassed: Int, monthDays: Int): SmartForecast {
        val td = daysPassed
        val md = monthDays
        val remainingDays = max(0, md - td)
        val all = allCombined
        fun dayOf(t: Transaction) = jalaliParts(t.createdAt).jd
        fun amt(t: Transaction) = t.amount.toDouble()

        val hist = ArrayList<Hist>()
        for (off in -1 downTo -3) {
            val r = getMonthRange(off, now)
            val exps = all.filter { it.type == "expense" && it.createdAt >= r.start && it.createdAt < r.end }
            if (exps.isEmpty()) continue
            val income = sum(all.filter { it.type == "income" && it.createdAt >= r.start && it.createdAt < r.end })
            val days = Jalali.daysInMonth(r.jYear, r.jMonth)
            val fixed = exps.filter { isFixedObligation(it) }
            val variable = exps.filter { !isFixedObligation(it) }
            val limit = (if (income != 0.0) income else sum(exps)) * SF_BIG_PURCHASE_SHARE
            val big = variable.filter { amt(it) >= limit }
            val regular = variable.filter { amt(it) < limit }
            hist.add(Hist(days, sum(fixed), fixed.map { DayAmount(dayOf(it), amt(it)) }, sum(regular) / days, sum(big)))
        }

        val curFixed = expenses.filter { isFixedObligation(it) }
        val curVariable = expenses.filter { !isFixedObligation(it) }
        val curLimit = (if (incomeTotal != 0.0) incomeTotal else spent) * SF_BIG_PURCHASE_SHARE
        val curBig = curVariable.filter { amt(it) >= curLimit }
        val curRegular = curVariable.filter { amt(it) < curLimit }
        val fixedPaid = sum(curFixed)

        val histFixedDay = weightedMedianDay(hist.flatMap { it.fixedDays })
        val payDay = if (fixedPaid > 0) max(1, weightedMedianDay(curFixed.map { DayAmount(dayOf(it), amt(it)) }))
        else max(1, if (histFixedDay != 0) histFixedDay else 1)
        val loanRemaining = loanInstallmentsRemainingThisMonth()
        var fixedRemaining: Double
        if (loanRemaining != null) {
            fixedRemaining = loanRemaining
        } else {
            val typical = median(hist.map { it.fixedTotal }.filter { it > 0 })
            fixedRemaining = if (typical > fixedPaid && !(fixedPaid > 0 && td >= histFixedDay + 7)) typical - fixedPaid else 0.0
        }
        fixedRemaining = if (remainingDays == 0) 0.0 else fixedRemaining
        val fixedPlan = fixedPaid + fixedRemaining
        val fixedRemainingDay = min(md, max(td + 1, if (histFixedDay != 0) histFixedDay else td + 1))

        val varRate: Double
        var varLo: Double
        var varHi: Double
        val method: String
        val rates = hist.map { it.regularRate }.filter { it >= 0 }
        if (rates.isNotEmpty()) {
            method = "history"
            varRate = median(rates)
            varLo = rates.minOrNull()!!
            varHi = rates.maxOrNull()!!
            if (rates.size == 1) { varLo = varRate * 0.7; varHi = varRate * 1.4 }
        } else {
            method = "own-pace"
            varRate = sum(curRegular) / max(1, td)
            varLo = varRate * 0.7
            varHi = varRate * 1.4
        }

        val bigMonths = hist.filter { it.bigTotal > 0 }
        val bigReserve = if (curBig.isEmpty() && hist.isNotEmpty() && bigMonths.size >= hist.size / 2.0)
            median(bigMonths.map { it.bigTotal }) else 0.0

        val remaining = fixedRemaining + varRate * remainingDays
        val lo = fixedRemaining + varLo * remainingDays
        val hi = fixedRemaining + varHi * remainingDays + (if (remainingDays > 0) bigReserve else 0.0)

        return SmartForecast(method, hist.size, fixedPaid, fixedRemaining, fixedPlan, payDay, fixedRemainingDay,
            varRate, varLo, varHi, bigReserve, remaining, lo, hi)
    }

    // ───────────── snapshot ─────────────

    private fun topCategory(list: List<Transaction>): TopCategory {
        val totals = LinkedHashMap<String, Double>()
        for (t in list) {
            if (t.type != "expense") continue
            totals[t.category] = (totals[t.category] ?: 0.0) + t.amount
        }
        var topKey: String? = null
        var topAmount = 0.0
        for ((k, v) in totals) if (v > topAmount) { topKey = k; topAmount = v }
        return TopCategory(topKey, topAmount, topKey?.let { categoryByKey(it) })
    }

    private fun countUniqueExpenseDays(list: List<Transaction>): Int {
        val days = HashSet<String>()
        for (t in list) {
            if (t.type != "expense") continue
            val p = jalaliParts(t.createdAt)
            days.add("${p.jy}-${p.jm}-${p.jd}")
        }
        return days.size
    }

    private fun peakExpenseTransaction(list: List<Transaction>): Transaction? {
        val ex = list.filter { it.type == "expense" }
        if (ex.isEmpty()) return null
        return ex.sortedWith { a, b ->
            val d = b.amount.compareTo(a.amount)
            if (d != 0) d else b.createdAt.compareTo(a.createdAt)
        }.first()
    }

    private fun peakExpenseDay(expenses: List<Transaction>): PeakExpenseDay? {
        if (expenses.isEmpty()) return null
        val totals = LinkedHashMap<String, Pair<Double, Long>>()
        for (t in expenses) {
            val d = localDate(t.createdAt)
            val key = "${d.year}-${d.monthValue - 1}-${d.dayOfMonth}"
            val prev = totals[key]
            totals[key] = if (prev != null) Pair(prev.first + t.amount, prev.second) else Pair(t.amount.toDouble(), t.createdAt)
        }
        var peak: Pair<Double, Long>? = null
        for (v in totals.values) if (peak == null || v.first > peak.first) peak = v
        if (peak == null) return null
        return PeakExpenseDay(localDate(peak.second).dayOfMonth, peak.first, peak.second)
    }

    fun rollingWindow(days: Int, base: Long): Pair<Long, Long> {
        val endDate = localDate(base)
        val startDate = endDate.minusDays(max(0, days - 1).toLong())
        return Pair(startOfDay(startDate), endOfDay(endDate))
    }

    fun weekWindow(offsetDays: Int, length: Int): Pair<Long, Long> {
        val endDate = localDate(nowMillis).minusDays(offsetDays.toLong())
        val startDate = endDate.minusDays((length - 1).toLong())
        return Pair(startOfDay(startDate), endOfDay(endDate))
    }

    private fun inRangeInclusive(start: Long, end: Long, type: String): List<Transaction> =
        allCombined.filter { t -> t.createdAt >= start && t.createdAt < end && t.type == type }

    val snapshot: Snapshot by lazy { buildSnapshot(nowMillis) }

    fun buildSnapshot(now: Long): Snapshot {
        val cur = getMonthRange(0, now)
        val prev = getMonthRange(-1, now)
        val curExp = inRange(cur.start, cur.end, "expense")
        val prevExp = inRange(prev.start, prev.end, "expense")
        val curInc = inRange(cur.start, cur.end, "income")
        val prevInc = inRange(prev.start, prev.end, "income")
        val curExpT = sum(curExp); val prevExpT = sum(prevExp)
        val curIncT = sum(curInc); val prevIncT = sum(prevInc)
        val diff = curExpT - prevExpT
        val diffPct = if (prevExpT > 0) diff / prevExpT * 100 else if (curExpT > 0) 100.0 else 0.0

        val nowP = jalaliParts(now)
        val daysPassed = max(1, nowP.jd)
        val monthDays = Jalali.daysInMonth(nowP.jy, nowP.jm)
        val avgDaily = curExpT / max(1, daysPassed)

        val rates = ArrayList<Double>()
        for (off in -1 downTo -3) {
            val h = getMonthRange(off, now)
            val tot = sum(inRange(h.start, h.end, "expense"))
            val days = Jalali.daysInMonth(h.jYear, h.jMonth)
            if (tot > 0 && days > 0) rates.add(tot / days)
        }
        val histDaily = if (rates.isNotEmpty()) rates.sum() / rates.size else 0.0
        val remainingDays = max(0, monthDays - daysPassed)

        val r30 = rollingWindow(30, now)
        val r30Exp = sum(allCombined.filter { it.type == "expense" && it.createdAt >= r30.first && it.createdAt < r30.second })
        val r30Inc = sum(allCombined.filter { it.type == "income" && it.createdAt >= r30.first && it.createdAt < r30.second })

        val sf = buildSmartForecast(now, curExp, curIncT, curExpT, daysPassed, monthDays)
        val projRemaining = sf.remaining
        val forecastDailyRate = if (remainingDays > 0) projRemaining / remainingDays else sf.varRate
        val projected = curExpT + projRemaining

        val expDays = countUniqueExpenseDays(curExp)
        val avgOnActive = curExpT / max(1, expDays)
        val avgDailyInc = curIncT / max(1, daysPassed)

        val week = weekWindow(0, 7)
        val prevWeekStart = localDate(week.first).minusDays(7).let { startOfDay(it) }
        val curWeekExp = sum(allCombined.filter { it.type == "expense" && it.createdAt >= week.first && it.createdAt < week.second })
        val prevWeekExp = sum(allCombined.filter { it.type == "expense" && it.createdAt >= prevWeekStart && it.createdAt < week.first })
        val weekDelta = if (prevWeekExp > 0) (curWeekExp - prevWeekExp) / prevWeekExp * 100 else if (curWeekExp > 0) 100.0 else 0.0

        return Snapshot(
            cur, prev, curExp, prevExp, curInc, prevInc, curExpT, prevExpT, curIncT, prevIncT, diff, diffPct,
            daysPassed, monthDays, avgDaily, histDaily, rates, forecastDailyRate, remainingDays, projRemaining, projected, sf,
            expDays, avgOnActive, r30Exp, r30Inc, r30Exp / 30, r30Inc / 30, avgDailyInc, avgDaily,
            topCategory(curExp), topCategory(prevExp), peakExpenseTransaction(curExp),
            curWeekExp, prevWeekExp, weekDelta, peakExpenseDay(curExp),
            if (curIncT > 0) curExpT / curIncT * 100 else null
        )
    }

    // ───────────── today balance / health ─────────────

    fun computeTodayBalance(snap: Snapshot = snapshot): TodayBalance {
        val budget = max(0.0, data.initialBalance.toDouble() + snap.currentIncomeTotal)
        val md = snap.monthDays
        val td = min(snap.daysPassed, md)
        val perDay = DoubleArray(md + 2)
        for (t in snap.currentExpenses) {
            val jd = jalaliParts(t.createdAt).jd
            if (jd in 1..md) perDay[jd] += t.amount.toDouble()
        }
        val cum = DoubleArray(md + 1)
        for (d in 1..md) cum[d] = cum[d - 1] + perDay[d]
        val spentBeforeToday = cum.getOrElse(td - 1) { 0.0 }
        val daysIncToday = md - td + 1
        val room = budget - spentBeforeToday - snap.smartForecast.fixedRemaining
        val allowance = if (room > 0) room / daysIncToday else 0.0
        val todaySpent = perDay.getOrElse(td) { 0.0 }
        val leftToday = allowance - todaySpent
        val useRatio = if (allowance > 0) todaySpent / allowance else if (todaySpent > 0) 2.0 else 0.0
        val safeClass = if (useRatio >= 1) "c-bad" else if (useRatio >= .7) "c-warn" else "c-good"
        return TodayBalance(budget, md, td, perDay, cum, room, allowance, todaySpent, leftToday, useRatio, safeClass)
    }

    fun todayBalanceView(tb: TodayBalance): TodayView {
        if (tb.allowance <= 0) return TodayView("۰", "تومان", "بودجه‌ی باقی‌مونده صفره؛ امروز خرجی نداشته باش", "hand")
        if (tb.leftToday >= 0) return TodayView(com.finora.app.util.Fa.plainMoney(tb.leftToday), "تومان", "امروز می‌تونی خرج کنی و هنوز روی مسیر بمونی", "bolt")
        return TodayView(com.finora.app.util.Fa.plainMoney(-tb.leftToday), "تومان بیشتر از سقف", "سقف امروز رد شده؛ فردا سقف کمی پایین‌تر میاد", "triangle-exclamation")
    }

    data class TxCount(val inc: Int, val exp: Int, val total: Int)

    fun todayTxCount(): TxCount {
        val d = localDate(nowMillis)
        val s = startOfDay(d)
        val e = endOfDay(d)
        var inc = 0
        var exp = 0
        for (t in data.transactions) {
            val ts = t.createdAt
            if (ts in s..e) { if (t.type == "income") inc++ else exp++ }
        }
        return TxCount(inc, exp, inc + exp)
    }

    data class ZeroSpend(val zero: Int, val tracked: Int)

    fun zeroSpendDays(): ZeroSpend {
        val now = nowParts
        val n = Jalali.daysInMonth(now.jy, now.jm)
        val last = min(now.jd, n)
        val exp = DoubleArray(n + 2)
        val cnt = IntArray(n + 2)
        for (t in data.transactions) {
            val ts = t.createdAt
            if (!(ts > 0)) continue
            val p = jalaliParts(ts)
            if (p.jy != now.jy || p.jm != now.jm) continue
            if (t.type != "income") exp[p.jd] += t.amount.toDouble()
            cnt[p.jd]++
        }
        var first = 0
        for (d in 1..n) if (cnt[d] != 0) { first = d; break }
        var zero = 0
        if (first != 0) for (d in first..last) if (exp[d] == 0.0) zero++
        return ZeroSpend(zero, if (first != 0) last - first + 1 else 0)
    }

    fun calcHealthScore(snap: Snapshot = snapshot): HealthScore {
        val income = snap.currentIncomeTotal
        val expense = snap.currentExpenseTotal
        val hasTransactions = income > 0 || expense > 0

        var savingsScore: Int? = null
        if (income > 0) {
            val savingsRate = max(0.0, (income - expense) / income)
            savingsScore = Fa.roundHalfUp(min(savingsRate * 2, 1.0) * 35).toInt()
        }
        val base = max(0.0, activeInitialBalance() + income)
        var budgetScore: Int? = null
        if (base > 0 && hasTransactions) {
            val used = expense / base
            budgetScore = if (used >= 1) 0 else if (used >= .9) 8 else if (used >= .8) 18 else if (used >= .6) 28 else 35
        }
        var trendScore: Int? = null
        var baseline = false
        if (snap.prevExpenseTotal > 0) {
            val d = snap.diffPct
            trendScore = if (d <= -20) 30 else if (d <= -5) 24 else if (d <= 5) 18 else if (d <= 20) 10 else 4
        } else if (expense > 0) {
            trendScore = 15
            baseline = true
        }
        val hasData = savingsScore != null || budgetScore != null || trendScore != null
        var total: Int? = null
        if (hasData) {
            val earned = (savingsScore ?: 0) + (budgetScore ?: 0) + (trendScore ?: 0)
            val maxAvail = (if (savingsScore != null) 35 else 0) + (if (budgetScore != null) 35 else 0) + (if (trendScore != null) 30 else 0)
            total = if (maxAvail > 0) Fa.roundHalfUp(earned.toDouble() / maxAvail * 100).toInt() else null
        }
        return HealthScore(total, hasData, savingsScore, budgetScore, trendScore, baseline,
            if (income > 0) (income - expense) / income else null)
    }

    fun healthLabel(score: Int): HealthLabel = when {
        score >= 85 -> HealthLabel("عالی", "#18e6a6")
        score >= 70 -> HealthLabel("خوب", "#18e6a6")
        score >= 50 -> HealthLabel("متوسط", "#ffb84d")
        score >= 30 -> HealthLabel("ضعیف", "#ff9040")
        else -> HealthLabel("نیاز به توجه", "#ff5d7d")
    }
}

private typealias Fa = com.finora.app.util.Fa
