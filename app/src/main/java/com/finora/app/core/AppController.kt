package com.finora.app.core

import com.finora.app.Config
import com.finora.app.data.AppData
import com.finora.app.data.DataParser
import com.finora.app.data.KeyValueStore
import com.finora.app.data.Keys
import com.finora.app.net.ApiException
import com.finora.app.net.Session
import com.finora.app.net.SupabaseApi
import com.finora.app.net.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class Screen { SPLASH, AUTH, MAIN }
enum class AuthView { LOGIN, REGISTER, FORGOT, RECOVER }

data class AuthUi(
    val view: AuthView = AuthView.LOGIN,
    val busy: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

data class UserInfo(val id: String, val email: String)

/** Android-free application brain: session, auth flows, cloud sync, parsed data and theme mode. */
class AppController(
    private val scope: CoroutineScope,
    private val store: KeyValueStore,
    private val api: SupabaseApi,
    private val io: kotlin.coroutines.CoroutineContext = Dispatchers.IO,
    private val splashMinMillis: Long = 500
) {
    private val _screen = MutableStateFlow(Screen.SPLASH)
    val screen: StateFlow<Screen> = _screen

    private val _authUi = MutableStateFlow(AuthUi())
    val authUi: StateFlow<AuthUi> = _authUi

    private val _user = MutableStateFlow<UserInfo?>(null)
    val user: StateFlow<UserInfo?> = _user

    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data

    private val _themeMode = MutableStateFlow(loadThemeMode())
    /** "system" | "light" | "dark" */
    val themeMode: StateFlow<String> = _themeMode

    private var session: Session? = null
    private var recoveryMode = false
    private val sessionMutex = Mutex()
    private var started = false
    private var foreground = false

    val sync: SyncManager = SyncManager(
        scope = scope, store = store, api = api,
        sessionProvider = { ensureSession() },
        onRemoteApplied = { reloadData() },
        io = io
    )

    // ───────────── theme ─────────────

    private fun loadThemeMode(): String {
        val m = store.get(Keys.THEME_MODE)
        if (m == "light" || m == "dark" || m == "system") return m
        return if (store.get(Keys.THEME) == "light") "light" else "system"
    }

    /** mode: "light" | "dark" | "system" — mirrors persistThemeMode()+persistTheme() of the web app. */
    fun setThemeMode(mode: String) {
        if (mode != "light" && mode != "dark" && mode != "system") return
        _themeMode.value = mode
        store.set(Keys.THEME_MODE, mode)
        store.set(Keys.THEME, if (mode == "light") "light" else "dark")
        sync.onLocalChange(Keys.THEME)
    }

    // ───────────── data ─────────────

    fun reloadData() {
        _data.value = DataParser.parse(store)
    }

    // ───────────── session plumbing ─────────────

    private fun setSession(s: Session?) {
        session = s
        if (s == null) {
            store.remove(Keys.SB_SESSION)
            _user.value = null
        } else {
            store.set(Keys.SB_SESSION, s.toJson())
            _user.value = UserInfo(s.userId, s.email)
        }
    }

    /** Returns a usable session, refreshing the access token when it is about to expire. */
    suspend fun ensureSession(): Session? = sessionMutex.withLock {
        var s = session ?: return@withLock null
        if (s.isExpiring()) {
            try {
                s = withContext(io) { api.refresh(s.refreshToken) }
                setSession(s)
            } catch (e: ApiException) {
                if (!e.offline && (e.status == 400 || e.status == 401 || e.status == 403)) {
                    // refresh token revoked/expired for good -> back to the login screen
                    forceSignOutLocally()
                    return@withLock null
                }
                // offline: keep the stale session, the calls will simply fail until we are back online
            }
        }
        s
    }

    private fun forceSignOutLocally() {
        sync.stop()
        for (k in Keys.SYNCED) store.remove(k)
        setSession(null)
        recoveryMode = false
        _data.value = AppData()
        _authUi.value = AuthUi()
        _screen.value = Screen.AUTH
    }

    // ───────────── start-up ─────────────

    fun start() {
        if (started) return
        started = true
        scope.launch {
            val minDelay = async { delay(splashMinMillis) }
            val stored = Session.fromJson(store.get(Keys.SB_SESSION))
            if (stored != null) setSession(stored)
            handleSession()
            minDelay.await()
            if (_screen.value == Screen.SPLASH) _screen.value = if (session != null) Screen.MAIN else Screen.AUTH
        }
    }

    private suspend fun handleSession() {
        if (recoveryMode) {
            if (session != null) {
                _authUi.value = AuthUi(view = AuthView.RECOVER)
                _screen.value = Screen.AUTH
                return
            }
            recoveryMode = false
        }
        val s = session
        if (s == null) {
            sync.stop()
            _screen.value = Screen.AUTH
            return
        }
        sync.enabled = true
        reloadData()                 // local copy first, so offline start works
        sync.loadFromServer()
        reloadData()
        if (session != null) {
            _screen.value = Screen.MAIN
            if (foreground) sync.startPolling()
        }
    }

    // ───────────── foreground / background ─────────────

    fun onForeground() {
        foreground = true
        if (!sync.enabled) return
        scope.launch { sync.syncFromServer(force = false) }
        sync.startPolling()
    }

    fun onBackground() {
        foreground = false
        sync.stopPolling()
    }

    // ───────────── auth UI state ─────────────

    fun showAuthView(view: AuthView) {
        _authUi.value = AuthUi(view = view)
    }

    private fun setError(msg: String) {
        _authUi.value = _authUi.value.copy(busy = false, error = msg, success = null)
    }

    private fun setBusy() {
        _authUi.value = _authUi.value.copy(busy = true, error = null, success = null)
    }

    // ───────────── auth actions ─────────────

    fun login(emailRaw: String, password: String) {
        val email = emailRaw.trim()
        if (email.isEmpty() || password.isEmpty()) return setError("ایمیل و رمز عبور را وارد کنید.")
        setBusy()
        scope.launch {
            try {
                val s = withContext(io) { api.signIn(email, password) }
                onSignedIn(s)
            } catch (e: ApiException) {
                setError(translateAuthError(e))
            } catch (e: Exception) {
                setError(translateAuthError(ApiException(0, e.message ?: "", offline = true)))
            }
        }
    }

    private suspend fun onSignedIn(s: Session) {
        recoveryMode = false
        setSession(s)
        handleSession()
        _authUi.value = AuthUi()
    }

    fun register(emailRaw: String, password: String, confirm: String) {
        val email = emailRaw.trim()
        if (email.isEmpty() || password.isEmpty()) return setError("ایمیل و رمز عبور را وارد کنید.")
        if (password.length < 6) return setError("رمز عبور باید حداقل ۶ کاراکتر باشد.")
        if (password != confirm) return setError("رمز عبور و تکرار آن یکسان نیستند.")
        setBusy()
        scope.launch {
            try {
                val s = withContext(io) { api.signUp(email, password) }
                if (s != null) {
                    onSignedIn(s)          // project auto-confirms e-mails -> signed in straight away
                } else {
                    _authUi.value = _authUi.value.copy(busy = false, error = null, success = "✅ ثبت‌نام موفق! لطفا وارد حساب خود شوید.")
                    delay(1500)
                    if (_authUi.value.view == AuthView.REGISTER) showAuthView(AuthView.LOGIN)
                }
            } catch (e: ApiException) {
                setError(translateAuthError(e))
            } catch (e: Exception) {
                setError(translateAuthError(ApiException(0, e.message ?: "", offline = true)))
            }
        }
    }

    fun sendRecovery(emailRaw: String) {
        val email = emailRaw.trim()
        if (email.isEmpty()) return setError("ایمیل را وارد کنید.")
        setBusy()
        scope.launch {
            try {
                withContext(io) { api.recover(email, Config.RECOVERY_REDIRECT) }
                _authUi.value = _authUi.value.copy(
                    busy = false, error = null,
                    success = "✅ اگر این ایمیل ثبت‌نام شده باشد، لینک تعیین رمز جدید برایش ارسال شد. پوشه‌ی اسپم را هم بررسی کنید."
                )
            } catch (e: ApiException) {
                setError(translateAuthError(e))
            } catch (e: Exception) {
                setError(translateAuthError(ApiException(0, e.message ?: "", offline = true)))
            }
        }
    }

    fun saveRecoveredPassword(pw: String, pw2: String) {
        if (pw.isEmpty()) return setError("رمز جدید را وارد کنید.")
        if (pw.length < 6) return setError("رمز عبور باید حداقل ۶ کاراکتر باشد.")
        if (pw != pw2) return setError("رمز عبور و تکرار آن یکسان نیستند.")
        val s = session ?: return setError("نشست شما منقضی شده؛ دوباره وارد شوید.")
        setBusy()
        scope.launch {
            try {
                val fresh = ensureSession() ?: s
                withContext(io) { api.updatePassword(fresh.accessToken, pw) }
                recoveryMode = false
                _authUi.value = AuthUi()
                handleSession()
                sync.showSync("saved", "✅ رمز عبور تغییر کرد")
            } catch (e: ApiException) {
                setError(translateAuthError(e))
            } catch (e: Exception) {
                setError(translateAuthError(ApiException(0, e.message ?: "", offline = true)))
            }
        }
    }

    /** finora://auth-callback#access_token=...&refresh_token=...&type=recovery (or an error fragment). */
    fun onAuthCallback(fragment: Map<String, String>) {
        if (fragment.isEmpty()) return
        if (fragment.containsKey("error") || fragment.containsKey("error_code")) {
            _authUi.value = AuthUi(view = AuthView.FORGOT, error = "لینک بازیابی نامعتبر یا منقضی شده است. دوباره درخواست دهید.")
            if (_screen.value != Screen.MAIN) _screen.value = Screen.AUTH
            return
        }
        if (fragment["type"] != "recovery" || !fragment.containsKey("access_token")) return
        scope.launch {
            try {
                val s = withContext(io) { api.sessionFromFragment(fragment) }
                recoveryMode = true
                setSession(s)
                _authUi.value = AuthUi(view = AuthView.RECOVER)
                _screen.value = Screen.AUTH
            } catch (e: Exception) {
                _authUi.value = AuthUi(view = AuthView.FORGOT, error = "لینک بازیابی نامعتبر یا منقضی شده است. دوباره درخواست دهید.")
                _screen.value = Screen.AUTH
            }
        }
    }

    fun logout() {
        val s = session
        forceSignOutLocally()
        if (s != null) scope.launch { withContext(io) { api.logout(s.accessToken) } }
    }

    companion object {
        /** Port of translateAuthError() (+ a friendly offline message the browser version did not have). */
        fun translateAuthError(e: ApiException): String {
            if (e.offline) return "اتصال به اینترنت برقرار نیست."
            val msg = e.message ?: ""
            return when {
                msg.contains("Invalid login") -> "ایمیل یا رمز عبور اشتباه است."
                msg.contains("Email not confirmed") -> "ایمیل شما تایید نشده."
                msg.contains("User already registered") -> "این ایمیل قبلا ثبت‌نام شده است."
                msg.contains("Password should be") -> "رمز عبور باید حداقل ۶ کاراکتر باشد."
                msg.contains("rate limit") -> "تعداد تلاش زیاد است. کمی صبر کنید."
                msg.contains("only request this after") -> "برای امنیت، چند ثانیه صبر کنید و دوباره تلاش کنید."
                msg.contains("different from the old password") -> "رمز جدید باید با رمز قبلی متفاوت باشد."
                msg.contains("Auth session missing") -> "نشست شما منقضی شده؛ دوباره وارد شوید."
                else -> msg
            }
        }
    }
}
