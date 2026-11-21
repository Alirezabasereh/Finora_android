package com.finora.app.data

import com.finora.app.util.Fa
import com.finora.app.util.Jalali
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Write side of the data layer — month roll-over, archive upkeep and persistence, ported from the web app
 * (checkAndAutoArchive / archiveCurrentMonth / persistTransactions …). Every write to a synced key is reported
 * through [onChange] so the sync layer can schedule an upload, exactly like the web app's localStorage hook.
 */
class Ledger(
    private val store: KeyValueStore,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val onChange: (String) -> Unit = {}
) {
    // ───────────── low level ─────────────

    private fun put(key: String, value: String) {
        store.set(key, value)
        onChange(key)
    }

    fun txToJson(t: Transaction): JSONObject {
        val o = JSONObject()
        o.put("id", t.id)
        o.put("amount", t.amount)
        o.put("note", t.note)
        o.put("category", t.category)
        o.put("type", t.type)
        o.put("createdAt", t.createdAt)
        if (t.accountId != null) o.put("accountId", t.accountId)
        if (t.loanId != null) o.put("loanId", t.loanId)
        return o
    }

    private fun txListToJson(list: List<Transaction>): JSONArray {
        val a = JSONArray()
        for (t in list) a.put(txToJson(t))
        return a
    }

    private fun nowParts(): Jalali.Ymd = Jalali.parts(nowMillis(), zone)
    private fun partsOf(ms: Long): Jalali.Ymd = Jalali.parts(ms, zone)

    private fun startOfJalaliDay(jy: Int, jm: Int, jd: Int): Long {
        val g = Jalali.fromJalali(jy, jm, jd)
        return LocalDate.of(g.y, g.m, g.d).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private fun endOfJalaliDay(jy: Int, jm: Int, jd: Int): Long {
        val g = Jalali.fromJalali(jy, jm, jd)
        return LocalDate.of(g.y, g.m, g.d).atTime(LocalTime.of(23, 59, 59, 999_000_000)).atZone(zone).toInstant().toEpochMilli()
    }

    // ───────────── reads ─────────────

    fun liveTransactions(): List<Transaction> {
        val cats = DataParser.parseCategories(store.get(Keys.CATEGORIES))
        return DataParser.parseTransactions(store.get(Keys.TRANSACTIONS), cats)
    }

    fun initialBalance(): Long = Fa.parseMoney(store.get(Keys.INITIAL_BALANCE) ?: 0)

    // ───────────── persistence ─────────────

    fun persistTransactions(list: List<Transaction>, viewingArchive: Boolean = false) {
        put(Keys.TRANSACTIONS, txListToJson(list).toString())
        if (!viewingArchive) {
            val p = nowParts()
            archiveCurrentMonth(list, p.y, p.m, null)
        }
    }

    fun persistInitialBalance(value: Long) {
        put(Keys.INITIAL_BALANCE, value.toString())
    }

    /** Snapshots the live transactions of one Jalali month into the archive; returns that month's closing balance. */
    fun archiveCurrentMonth(live: List<Transaction>, jy: Int, jm: Int, openingBalance: Double?): Double {
        val key = Jalali.monthKey(jy, jm)
        val archive = (DataParser.safeJson(store.get(Keys.MONTHLY_ARCHIVE)) as? JSONObject) ?: JSONObject()
        val startTs = startOfJalaliDay(jy, jm, 1)
        val endTs = endOfJalaliDay(jy, jm, Jalali.daysInMonth(jy, jm))
        val monthTxs = live.filter { it.createdAt in startTs..endTs }
        val resolvedOpening = openingBalance ?: initialBalance().toDouble()
        val entry = JSONObject()
        entry.put("jy", jy)
        entry.put("jm", jm)
        entry.put("initialBalance", if (resolvedOpening == Math.floor(resolvedOpening) && !resolvedOpening.isInfinite()) resolvedOpening.toLong() else resolvedOpening)
        entry.put("transactions", txListToJson(monthTxs))
        archive.put(key, entry)
        put(Keys.MONTHLY_ARCHIVE, archive.toString())
        return resolvedOpening + monthTxs.sumOf { (if (it.type == "income") 1 else -1) * it.amount.toDouble() }
    }

    // ───────────── month roll-over ─────────────

    private fun fallbackExpenseKey(cats: List<Category>): String =
        (cats.firstOrNull { it.builtin && it.type == "expense" } ?: cats.firstOrNull { it.type == "expense" })?.key ?: "other"

    /**
     * Archives every stale month still sitting in the live list, carries the leftover balance into the new
     * month as a normal transaction and zeroes the one-time initial balance. Returns true when something changed.
     */
    fun checkAndAutoArchive(): Boolean {
        val today = nowParts()
        val todayKey = Jalali.monthKey(today.y, today.m)
        val cats = DataParser.parseCategories(store.get(Keys.CATEGORIES))
        var live = DataParser.parseTransactions(store.get(Keys.TRANSACTIONS), cats)

        val stale = java.util.TreeSet<String>()
        for (t in live) {
            val ts = t.createdAt
            if (ts == 0L) continue
            val p = partsOf(ts)
            val k = Jalali.monthKey(p.y, p.m)
            if (k != todayKey) stale.add(k)
        }

        var crossed = false
        if (stale.isNotEmpty()) {
            crossed = true
            var carry = initialBalance().toDouble()
            for (key in stale) {
                val parts = key.split("_")
                val ly = parts[0].toIntOrNull() ?: 0
                val lm = parts[1].toIntOrNull() ?: 0
                if (ly != 0 && lm != 0) carry = archiveCurrentMonth(live, ly, lm, carry)
            }
            val startTs = startOfJalaliDay(today.y, today.m, 1)
            live = live.filter { it.createdAt >= startTs }

            // addCarryOverTransaction
            val rounded = Fa.roundHalfUp(carry)
            if (rounded != 0L) {
                val categoryKey = if (cats.any { it.key == "other" }) "other" else fallbackExpenseKey(cats)
                val carryTx = Transaction(
                    id = DataParser.newId(),
                    amount = Math.abs(rounded),
                    note = "انتقال موجودی باقیمانده از ماه قبل",
                    category = categoryKey,
                    type = if (rounded >= 0) "income" else "expense",
                    createdAt = startTs
                )
                live = listOf(carryTx) + live
            }
            if (initialBalance() != 0L) persistInitialBalance(0)
            persistTransactions(live)
        }
        store.set(Keys.LAST_SEEN_MONTH, todayKey)
        return crossed
    }

    // ───────────── start-up ─────────────

    private fun sameTx(a: List<Transaction>, b: List<Transaction>): Boolean = a == b

    /**
     * What the web's loadState()+persistTransactions() guarantees after every start: the current month always has
     * an up-to-date snapshot in the archive. Only writes when the stored snapshot is missing or different.
     */
    fun refreshCurrentMonthArchive() {
        val p = nowParts()
        val key = Jalali.monthKey(p.y, p.m)
        val cats = DataParser.parseCategories(store.get(Keys.CATEGORIES))
        val live = DataParser.parseTransactions(store.get(Keys.TRANSACTIONS), cats)
        val startTs = startOfJalaliDay(p.y, p.m, 1)
        val endTs = endOfJalaliDay(p.y, p.m, Jalali.daysInMonth(p.y, p.m))
        val expected = live.filter { it.createdAt in startTs..endTs }
        val existing = DataParser.parseArchive(store.get(Keys.MONTHLY_ARCHIVE), cats)[key]
        val opening = initialBalance().toDouble()
        if (existing == null || existing.initialBalance != opening || !sameTx(existing.transactions, expected)) {
            archiveCurrentMonth(live, p.y, p.m, null)
        }
    }

    /** Everything the app must run once per start / resume (web: boot() -> loadState() + checkAndAutoArchive()). */
    fun bootTasks(): Boolean {
        val crossed = checkAndAutoArchive()
        refreshCurrentMonthArchive()
        return crossed
    }
}

