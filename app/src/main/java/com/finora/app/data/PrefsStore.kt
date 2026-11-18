package com.finora.app.data

import android.content.Context
import android.content.SharedPreferences

/** SharedPreferences-backed implementation of the app's localStorage equivalent. */
class PrefsStore(context: Context) : KeyValueStore {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("finora_store", Context.MODE_PRIVATE)

    override fun get(key: String): String? = prefs.getString(key, null)
    override fun set(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun remove(key: String) { prefs.edit().remove(key).apply() }
}
