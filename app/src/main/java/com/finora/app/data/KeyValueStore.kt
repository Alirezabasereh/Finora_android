package com.finora.app.data

/** Minimal string key/value storage — the Android counterpart of the web app's localStorage. */
interface KeyValueStore {
    fun get(key: String): String?
    fun set(key: String, value: String)
    fun remove(key: String)
}

class MemoryStore : KeyValueStore {
    private val map = HashMap<String, String>()
    @Synchronized override fun get(key: String): String? = map[key]
    @Synchronized override fun set(key: String, value: String) { map[key] = value }
    @Synchronized override fun remove(key: String) { map.remove(key) }
}

/** Same storage keys the web app uses (so cloud data maps 1:1). */
object Keys {
    const val TRANSACTIONS = "finora_transactions"
    const val CATEGORIES = "finora_categories"
    const val GOALS = "finora_goals"
    const val ACCOUNTS = "finora_accounts"
    const val ACCOUNT_TRANSFERS = "finora_account_transfers"
    const val LOANS = "finora_loans"
    const val INITIAL_BALANCE = "finora_initial_balance"
    const val THEME = "finora_theme"
    const val PROFILE = "finora_profile"
    const val MONTHLY_ARCHIVE = "finora_monthly_archive"

    // device-local only (never synced)
    const val THEME_MODE = "finora_theme_mode"
    const val LAST_SEEN_MONTH = "finora_last_seen_month"
    const val EXCLUDE_LOANS = "finora_exclude_loans"
    const val LAST_ACCOUNT = "finora_last_account"
    const val SB_SESSION = "finora_sb_session"

    /** Exactly the keys the web app pushes to / pulls from Supabase `user_data.data`. */
    val SYNCED: List<String> = listOf(
        TRANSACTIONS, CATEGORIES, GOALS, ACCOUNTS, ACCOUNT_TRANSFERS,
        LOANS, INITIAL_BALANCE, THEME, PROFILE, MONTHLY_ARCHIVE
    )
}
