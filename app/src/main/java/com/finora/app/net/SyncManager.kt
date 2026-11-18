package com.finora.app.net

import com.finora.app.data.Keys
import com.finora.app.data.KeyValueStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class SyncBarState(val visible: Boolean = false, val type: String = "", val message: String = "")

/**
 * Cloud sync with the exact semantics of the web app's Supabase block:
 *  - the whole app state is one JSON object in `user_data.data` (key -> raw JSON string)
 *  - local change  -> debounced (1s) upsert
 *  - pull          -> never overwrites an unsaved local change; result is discarded if the user edited meanwhile
 *  - "realtime"    -> lightweight polling of `updated_at` while the app is in the foreground
 */
class SyncManager(
    private val scope: CoroutineScope,
    private val store: KeyValueStore,
    private val api: SupabaseApi,
    private val sessionProvider: suspend () -> Session?,
    private val onRemoteApplied: () -> Unit,
    private val isBusy: () -> Boolean = { false },
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val io: kotlin.coroutines.CoroutineContext = Dispatchers.IO
) {
    private val _bar = MutableStateFlow(SyncBarState())
    val bar: StateFlow<SyncBarState> = _bar

    /** true while a user is signed in (set by the controller). */
    @Volatile var enabled: Boolean = false

    private var isLoading = false
    private var isSaving = false
    private var saveQueued = false
    private var refreshing = false
    private var refreshAgain = false
    private var lastRefreshAt = 0L
    var hasPendingSave = false
        private set
    private var localChangeCounter = 0
    private var lastServerMillis: Long? = null

    private var saveJob: Job? = null
    private var barJob: Job? = null
    private var pollJob: Job? = null
    private var safetyJob: Job? = null

    // ───────────── status bar ─────────────

    fun showSync(type: String, message: String) {
        _bar.value = SyncBarState(true, type, message)
        barJob?.cancel()
        if (type == "saved") {
            barJob = scope.launch {
                delay(2000)
                _bar.value = _bar.value.copy(visible = false)
            }
        }
    }

    // ───────────── state <-> storage ─────────────

    private fun collectLocalState(): JSONObject {
        val out = JSONObject()
        for (key in Keys.SYNCED) {
            val raw = store.get(key)
            if (raw != null) out.put(key, raw)
        }
        return out
    }

    /** Remote values may be strings or already-parsed JSON; local storage always keeps raw strings. */
    private fun applyRemoteState(obj: JSONObject) {
        for (key in Keys.SYNCED) {
            if (obj.has(key) && !obj.isNull(key)) {
                val v = obj.get(key)
                store.set(key, if (v is String) v else v.toString())
            }
        }
    }

    // ───────────── load / save ─────────────

    /** First load after sign-in / app start (web: loadFromSupabase). */
    suspend fun loadFromServer() {
        if (!enabled || isLoading) return
        if (hasPendingSave) {
            saveJob?.cancel()
            saveToServer()
            return
        }
        isLoading = true
        safetyJob?.cancel()
        safetyJob = scope.launch { delay(15_000); isLoading = false }
        showSync("syncing", "⏳ در حال بارگذاری اطلاعات...")
        try {
            val s = sessionProvider()
            if (s != null) {
                val remote = withContext(io) { api.fetchUserData(s) }
                val d = remote.data
                if (d != null && d.length() > 0) applyRemoteState(d)
                lastServerMillis = remote.updatedAtMillis
                onRemoteApplied()
                showSync("saved", "✅ اطلاعات بارگذاری شد")
            }
        } catch (e: Exception) {
            showSync("error", "❌ خطا در بارگذاری اطلاعات")
        } finally {
            safetyJob?.cancel()
            isLoading = false
        }
    }

    /** Call after any write to a synced key (web: the localStorage.setItem hook). */
    fun onLocalChange(key: String) {
        if (isLoading || !enabled) return
        if (key !in Keys.SYNCED) return
        hasPendingSave = true
        localChangeCounter++
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(1000)
            saveToServer()
        }
    }

    suspend fun saveToServer() {
        if (!enabled || isLoading) return
        if (isSaving) { saveQueued = true; return }
        isSaving = true
        showSync("syncing", "☁️ در حال ذخیره...")
        try {
            val s = sessionProvider() ?: return
            val startCounter = localChangeCounter
            val snapshot = collectLocalState()
            val ts = nowMillis()
            withContext(io) { api.upsertUserData(s, snapshot, ts) }
            if (localChangeCounter == startCounter) hasPendingSave = false
            lastServerMillis = ts
            showSync("saved", "✅ ذخیره شد")
        } catch (e: Exception) {
            showSync("error", "❌ خطا در ذخیره‌سازی")
        } finally {
            isSaving = false
            if (saveQueued) {
                saveQueued = false
                saveToServer()
            }
        }
    }

    /** Safe pull (web: syncFromServer) — used for resume, polling and remote-change events. */
    suspend fun syncFromServer(force: Boolean = false) {
        if (!enabled || isLoading) return
        if (refreshing) { if (force) refreshAgain = true; return }
        if (isBusy()) return
        val now = nowMillis()
        if (!force && now - lastRefreshAt < 1500) return
        lastRefreshAt = now
        refreshing = true
        try {
            if (hasPendingSave || isSaving) {
                saveJob?.cancel()
                saveToServer()
            }
            val startCounter = localChangeCounter
            showSync("syncing", "⏳ در حال بارگذاری اطلاعات...")
            val s = sessionProvider() ?: return
            val remote = withContext(io) { api.fetchUserData(s) }
            // the user edited while we were fetching -> drop the server copy, the edit gets saved
            if (localChangeCounter != startCounter || hasPendingSave || isSaving || isBusy()) return
            isLoading = true
            safetyJob?.cancel()
            safetyJob = scope.launch { delay(15_000); isLoading = false }
            try {
                val d = remote.data
                if (d != null && d.length() > 0) applyRemoteState(d)
                lastServerMillis = remote.updatedAtMillis
                onRemoteApplied()
            } finally {
                safetyJob?.cancel()
                isLoading = false
            }
            showSync("saved", "✅ اطلاعات بارگذاری شد")
        } catch (e: Exception) {
            showSync("error", "❌ خطا در بارگذاری اطلاعات")
        } finally {
            refreshing = false
            if (refreshAgain) {
                refreshAgain = false
                syncFromServer(force = true)
            }
        }
    }

    // ───────────── foreground polling ("realtime") ─────────────

    fun startPolling(intervalMs: Long = 12_000) {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                try { pollOnce() } catch (_: Exception) { }
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    internal suspend fun pollOnce() {
        if (!enabled || isLoading || isSaving || refreshing) return
        if (hasPendingSave) { saveToServer(); return }
        val s = sessionProvider() ?: return
        val remote = withContext(io) { api.fetchUpdatedAt(s) } ?: return
        if (remote != lastServerMillis) syncFromServer(force = true)
    }

    /** Sign-out / shutdown. */
    fun stop() {
        enabled = false
        stopPolling()
        saveJob?.cancel()
        safetyJob?.cancel()
        hasPendingSave = false
        isLoading = false
        isSaving = false
        refreshing = false
        lastServerMillis = null
        _bar.value = SyncBarState()
    }
}
