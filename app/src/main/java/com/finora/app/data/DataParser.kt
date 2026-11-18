package com.finora.app.data

import com.finora.app.util.Fa
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.UUID

/** Port of the web app's loadState()/migrate*() normalisers. Read-only: never rewrites storage. */
object DataParser {

    val defaultCategories: List<Category> = listOf(
        Category("food", "خوراک", "🍔", "#ff8a65", "expense", 10000000, true),
        Category("transport", "حمل و نقل", "🚕", "#00d2ff", "expense", 5000000, true),
        Category("shopping", "خرید", "🛍", "#ff3cac", "expense", 7000000, true),
        Category("fun", "سرگرمی", "🎮", "#9b7bff", "expense", 4000000, true),
        Category("salary", "حقوق", "💼", "#18e6a6", "income", 0, true),
        Category("loanes", "اقساط وام", "💵", "#87ff9d", "expense", 0, true),
        Category("other", "سایر", "✨", "#7dd3fc", "expense", 0, true)
    )

    val defaultProfile = Profile(name = "کاربر", avatar = "", avatarIndex = -1, avatarMode = "upload")

    private val HEX6 = Regex("^#[0-9a-fA-F]{6}$")
    private val ISO_DAY = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun newId(): String = UUID.randomUUID().toString()

    // ───────────── JS-ish coercion helpers ─────────────

    /** safeJSON(): parsed value or null on any error. */
    fun safeJson(raw: String?): Any? {
        if (raw == null) return null
        return try {
            val v = JSONTokener(raw).nextValue()
            if (v === JSONObject.NULL) null else v
        } catch (e: Exception) {
            null
        }
    }

    private fun clean(v: Any?): Any? = if (v === JSONObject.NULL) null else v

    /** JS truthiness for the values that appear in stored JSON. */
    private fun truthy(v: Any?): Boolean = when (val c = clean(v)) {
        null -> false
        is Boolean -> c
        is Number -> c.toDouble() != 0.0 && !c.toDouble().isNaN()
        is String -> c.isNotEmpty()
        else -> true
    }

    /** String(v) as JS would print it. */
    private fun jsString(v: Any?): String = when (val c = clean(v)) {
        null -> ""
        is Double -> if (c == Math.floor(c) && !c.isInfinite()) c.toLong().toString() else c.toString()
        is Float -> if (c == Math.floor(c.toDouble()).toFloat()) c.toLong().toString() else c.toString()
        else -> c.toString()
    }

    /** Number(v); null when the result would be NaN. */
    private fun jsNumber(v: Any?, missing: Boolean = false): Double? {
        if (missing) return null
        return when (val c = clean(v)) {
            null -> 0.0
            is Number -> c.toDouble().takeIf { !it.isNaN() }
            is Boolean -> if (c) 1.0 else 0.0
            is String -> {
                val t = c.trim()
                if (t.isEmpty()) 0.0 else t.toDoubleOrNull()
            }
            else -> null
        }
    }

    private fun slugKey(name: String): String {
        val base = name.trim().lowercase()
        val safe = base.replace(Regex("[^a-z0-9\\u0600-\\u06FF]+"), "_").trim('_')
        return if (safe.isNotEmpty()) "cat_$safe" else "cat_${System.currentTimeMillis()}"
    }

    private fun opt(o: JSONObject, key: String): Any? = if (o.has(key)) o.opt(key) else null

    // ───────────── entities ─────────────

    fun normalizeCategory(item: Any?): Category? {
        val o = item as? JSONObject ?: return null
        val rawKey = opt(o, "key")
        val key = (if (truthy(rawKey)) jsString(rawKey) else slugKey(if (truthy(opt(o, "name"))) jsString(opt(o, "name")) else "custom")).trim()
        val rawName = opt(o, "name")
        val name = (if (truthy(rawName)) jsString(rawName) else key).trim().ifEmpty { key }
        val rawEmoji = opt(o, "emoji")
        val emoji = (if (truthy(rawEmoji)) jsString(rawEmoji) else "🏷️").trim().take(4)
        val colorRaw = jsString(opt(o, "color"))
        val color = if (HEX6.matches(colorRaw)) colorRaw else "#6a5cff"
        val type = if (jsString(opt(o, "type")) == "income") "income" else "expense"
        val budget = maxOf(0L, Fa.parseMoney(clean(opt(o, "budgetLimit"))))
        return Category(key, name, emoji, color, type, budget, truthy(opt(o, "builtin")))
    }

    fun parseCategories(raw: String?): List<Category> {
        val saved = safeJson(raw)
        val list: List<Category> =
            if (saved is JSONArray && saved.length() > 0) {
                (0 until saved.length()).mapNotNull { normalizeCategory(saved.opt(it)) }
            } else defaultCategories
        val byKey = LinkedHashMap<String, Category>()
        for (c in defaultCategories) byKey[c.key] = c
        for (c in list) {
            val existing = byKey[c.key]
            byKey[c.key] = c.copy(builtin = existing?.builtin ?: c.builtin)
        }
        return byKey.values.toList()
    }

    private fun fallbackCategoryKey(type: String, cats: List<Category>): String =
        (cats.firstOrNull { it.builtin && it.type == type }
            ?: cats.firstOrNull { it.type == type }
            ?: cats.firstOrNull { it.type == "expense" })?.key ?: "other"

    fun parseTransactions(raw: String?, cats: List<Category>): List<Transaction> {
        val arr = safeJson(raw) as? JSONArray ?: return emptyList()
        val out = ArrayList<Transaction>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.opt(i) as? JSONObject ?: continue
            val type = if (jsString(opt(o, "type")) == "income") "income" else "expense"
            val amount = Fa.parseMoney(clean(opt(o, "amount")))
            if (amount <= 0) continue
            val catRaw = jsString(opt(o, "category"))
            val category = if (cats.any { it.key == catRaw }) catRaw else fallbackCategoryKey(type, cats)
            val created = jsNumber(opt(o, "createdAt"), missing = !o.has("createdAt"))?.toLong() ?: System.currentTimeMillis()
            val id = if (truthy(opt(o, "id"))) jsString(opt(o, "id")) else newId()
            out.add(
                Transaction(
                    id = id,
                    amount = amount,
                    note = jsString(opt(o, "note")).trim(),
                    category = category,
                    type = type,
                    createdAt = created,
                    accountId = if (truthy(opt(o, "accountId"))) jsString(opt(o, "accountId")) else null,
                    loanId = if (truthy(opt(o, "loanId"))) jsString(opt(o, "loanId")) else null
                )
            )
        }
        return out
    }

    fun parseGoals(raw: String?): List<Goal> {
        val arr = safeJson(raw) as? JSONArray ?: return emptyList()
        val out = ArrayList<Goal>()
        for (i in 0 until arr.length()) {
            val o = arr.opt(i) as? JSONObject ?: continue
            val target = Fa.parseMoney(clean(opt(o, "target")))
            if (!truthy(opt(o, "title")) || target == 0L) continue
            val deadlineRaw = jsString(opt(o, "deadline"))
            val colorRaw = jsString(opt(o, "color"))
            out.add(
                Goal(
                    id = if (truthy(opt(o, "id"))) jsString(opt(o, "id")) else newId(),
                    title = jsString(opt(o, "title")).trim(),
                    target = target,
                    saved = Fa.parseMoney(clean(opt(o, "saved"))),
                    deadline = if (ISO_DAY.matches(deadlineRaw)) deadlineRaw else "",
                    createdAt = (jsNumber(opt(o, "createdAt"), !o.has("createdAt")) ?: 0.0).toLong(),
                    color = if (HEX6.matches(colorRaw)) colorRaw else "#6a5cff"
                )
            )
        }
        return out
    }

    private val ACCOUNT_ICONS = listOf("🏦", "💳", "💰", "👛", "🪙", "🏧", "💵", "🏠")
    private val ACCOUNT_COLORS = listOf("#6a5cff", "#00d2ff", "#18e6a6", "#ffb84d", "#ff5d7d", "#ff7ac6", "#a78bfa", "#94a3b8")

    fun parseAccounts(raw: String?): List<Account> {
        val arr = safeJson(raw) as? JSONArray ?: return emptyList()
        val seen = HashSet<String>()
        val list = ArrayList<Account>()
        for (i in 0 until arr.length()) {
            val o = arr.opt(i) as? JSONObject ?: continue
            val name = jsString(opt(o, "name")).trim()
            if (name.isEmpty()) continue
            var id = jsString(opt(o, "id"))
            if (id.isEmpty() || seen.contains(id)) id = newId()
            seen.add(id)
            val iconRaw = jsString(opt(o, "icon")).trim()
            val colorRaw = jsString(opt(o, "color"))
            val opening = maxOf(0.0, jsNumber(opt(o, "opening"), !o.has("opening")) ?: 0.0)
            list.add(
                Account(
                    id = id,
                    name = name,
                    icon = iconRaw.ifEmpty { ACCOUNT_ICONS[0] },
                    color = if (HEX6.matches(colorRaw)) colorRaw else ACCOUNT_COLORS[0],
                    last4 = jsString(opt(o, "last4")).filter { it in '0'..'9' }.take(4),
                    opening = opening,
                    loanDefault = truthy(opt(o, "loanDefault")),
                    createdAt = (jsNumber(opt(o, "createdAt"), !o.has("createdAt")) ?: 0.0).toLong()
                )
            )
        }
        // only one account may be the loan default (the first one wins)
        val firstDefault = list.indexOfFirst { it.loanDefault }
        return list.mapIndexed { i, a -> if (a.loanDefault && firstDefault != i) a.copy(loanDefault = false) else a }
    }

    fun parseTransfers(raw: String?): List<AccountTransfer> {
        val arr = safeJson(raw) as? JSONArray ?: return emptyList()
        val out = ArrayList<AccountTransfer>()
        for (i in 0 until arr.length()) {
            val o = arr.opt(i) as? JSONObject ?: continue
            val amount = jsNumber(opt(o, "amount"), !o.has("amount")) ?: 0.0
            val from = jsString(opt(o, "from"))
            val to = jsString(opt(o, "to"))
            if (amount <= 0 || from == to) continue
            out.add(
                AccountTransfer(
                    id = if (truthy(opt(o, "id"))) jsString(opt(o, "id")) else newId(),
                    from = from, to = to, amount = amount,
                    createdAt = (jsNumber(opt(o, "createdAt"), !o.has("createdAt")) ?: 0.0).toLong()
                )
            )
        }
        return out
    }

    fun parseProfile(raw: String?): Profile {
        val o = safeJson(raw) as? JSONObject ?: return defaultProfile
        val nameRaw = jsString(opt(o, "name")).trim()
        val mode = if (jsString(opt(o, "avatarMode")) == "upload") "upload" else "preset"
        val idxVal = clean(opt(o, "avatarIndex"))
        val idx = if (idxVal is Number && idxVal.toDouble() == Math.floor(idxVal.toDouble())) idxVal.toInt() else 0
        val avatar = jsString(opt(o, "avatar")).trim()
        return Profile(name = nameRaw.ifEmpty { "کاربر" }, avatar = avatar, avatarIndex = idx, avatarMode = mode)
    }

    fun parse(store: KeyValueStore): AppData {
        val cats = parseCategories(store.get(Keys.CATEGORIES))
        val loansRaw = store.get(Keys.LOANS)
        return AppData(
            initialBalance = Fa.parseMoney(store.get(Keys.INITIAL_BALANCE) ?: 0),
            transactions = parseTransactions(store.get(Keys.TRANSACTIONS), cats),
            categories = cats,
            goals = parseGoals(store.get(Keys.GOALS)),
            accounts = parseAccounts(store.get(Keys.ACCOUNTS)),
            accountTransfers = parseTransfers(store.get(Keys.ACCOUNT_TRANSFERS)),
            profile = parseProfile(store.get(Keys.PROFILE)),
            loansJson = if (safeJson(loansRaw) is JSONArray) loansRaw!! else "[]"
        )
    }
}
