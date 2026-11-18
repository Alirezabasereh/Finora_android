package com.finora.app.net

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.OffsetDateTime

class ApiException(val status: Int, message: String, val offline: Boolean = false) : Exception(message)

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,   // epoch seconds
    val userId: String,
    val email: String
) {
    fun isExpiring(nowSec: Long = System.currentTimeMillis() / 1000, marginSec: Long = 60): Boolean =
        expiresAt - nowSec < marginSec

    fun toJson(): String = JSONObject()
        .put("access_token", accessToken).put("refresh_token", refreshToken)
        .put("expires_at", expiresAt).put("user_id", userId).put("email", email)
        .toString()

    companion object {
        fun fromJson(raw: String?): Session? {
            if (raw.isNullOrBlank()) return null
            return try {
                val o = JSONObject(raw)
                Session(
                    o.getString("access_token"), o.getString("refresh_token"),
                    o.getLong("expires_at"), o.getString("user_id"), o.optString("email", "")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class RemoteData(val data: JSONObject?, val updatedAtMillis: Long?)

/** Thin REST client for Supabase Auth (GoTrue) + PostgREST — the same calls supabase-js makes for the web app. */
class SupabaseApi(
    private val baseUrl: String,
    private val anonKey: String,
    private val http: HttpTransport = UrlConnectionTransport(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    // ───────────── plumbing ─────────────

    private fun call(method: String, path: String, body: JSONObject?, bearer: String?, extra: Map<String, String> = emptyMap()): HttpResponse {
        val headers = LinkedHashMap<String, String>()
        headers["apikey"] = anonKey
        headers["Authorization"] = "Bearer " + (bearer ?: anonKey)
        headers["Content-Type"] = "application/json"
        headers["Accept"] = "application/json"
        headers.putAll(extra)
        val res = try {
            http.request(method, baseUrl + path, headers, body?.toString())
        } catch (e: IOException) {
            throw ApiException(0, e.message ?: "network error", offline = true)
        }
        if (res.code !in 200..299) throw ApiException(res.code, errorMessage(res))
        return res
    }

    private fun errorMessage(res: HttpResponse): String {
        return try {
            val o = JSONObject(res.body)
            for (k in listOf("msg", "message", "error_description", "error")) {
                val v = o.optString(k, "")
                if (v.isNotEmpty()) return v
            }
            "HTTP ${res.code}"
        } catch (e: Exception) {
            if (res.body.isNotBlank()) res.body.take(200) else "HTTP ${res.code}"
        }
    }

    private fun sessionFrom(o: JSONObject): Session {
        val nowSec = nowMillis() / 1000
        val expiresAt = when {
            o.has("expires_at") && !o.isNull("expires_at") -> o.getLong("expires_at")
            else -> nowSec + o.optLong("expires_in", 3600)
        }
        val user = o.getJSONObject("user")
        return Session(
            o.getString("access_token"), o.getString("refresh_token"),
            expiresAt, user.getString("id"), user.optString("email", "")
        )
    }

    // ───────────── auth ─────────────

    fun signIn(email: String, password: String): Session {
        val res = call("POST", "/auth/v1/token?grant_type=password",
            JSONObject().put("email", email).put("password", password), null)
        return sessionFrom(JSONObject(res.body))
    }

    /** Returns a session when the project auto-confirms e-mails, otherwise null (confirmation mail sent). */
    fun signUp(email: String, password: String): Session? {
        val res = call("POST", "/auth/v1/signup",
            JSONObject().put("email", email).put("password", password), null)
        val o = JSONObject(res.body)
        return if (o.has("access_token") && o.has("user")) sessionFrom(o) else null
    }

    fun recover(email: String, redirectTo: String) {
        val q = "?redirect_to=" + URLEncoder.encode(redirectTo, "UTF-8")
        call("POST", "/auth/v1/recover$q", JSONObject().put("email", email), null)
    }

    fun refresh(refreshToken: String): Session {
        val res = call("POST", "/auth/v1/token?grant_type=refresh_token",
            JSONObject().put("refresh_token", refreshToken), null)
        return sessionFrom(JSONObject(res.body))
    }

    fun updatePassword(accessToken: String, newPassword: String) {
        call("PUT", "/auth/v1/user", JSONObject().put("password", newPassword), accessToken)
    }

    fun logout(accessToken: String) {
        try { call("POST", "/auth/v1/logout", null, accessToken) } catch (_: ApiException) { /* best effort */ }
    }

    /** Builds a session from the fragment of finora://auth-callback#access_token=...&type=recovery */
    fun sessionFromFragment(fragment: Map<String, String>): Session {
        val access = fragment["access_token"] ?: throw ApiException(400, "missing access_token")
        val refreshTok = fragment["refresh_token"] ?: ""
        val res = call("GET", "/auth/v1/user", null, access)
        val u = JSONObject(res.body)
        val nowSec = nowMillis() / 1000
        val expiresAt = fragment["expires_at"]?.toLongOrNull()
            ?: (nowSec + (fragment["expires_in"]?.toLongOrNull() ?: 3600))
        return Session(access, refreshTok, expiresAt, u.getString("id"), u.optString("email", ""))
    }

    // ───────────── user_data table ─────────────

    private fun parseMillis(s: String?): Long? =
        if (s.isNullOrBlank()) null else try { OffsetDateTime.parse(s).toInstant().toEpochMilli() } catch (e: Exception) { null }

    fun fetchUserData(s: Session): RemoteData {
        val res = call("GET", "/rest/v1/user_data?select=data,updated_at&user_id=eq.${s.userId}", null, s.accessToken)
        val arr = JSONArray(res.body)
        if (arr.length() == 0) return RemoteData(null, null)
        val row = arr.getJSONObject(0)
        val data = row.optJSONObject("data")
        return RemoteData(data, parseMillis(row.optString("updated_at", "")))
    }

    fun fetchUpdatedAt(s: Session): Long? {
        val res = call("GET", "/rest/v1/user_data?select=updated_at&user_id=eq.${s.userId}", null, s.accessToken)
        val arr = JSONArray(res.body)
        if (arr.length() == 0) return null
        return parseMillis(arr.getJSONObject(0).optString("updated_at", ""))
    }

    /** upsert({user_id, data, updated_at}, {onConflict:'user_id'}) */
    fun upsertUserData(s: Session, data: JSONObject, updatedAtMillis: Long) {
        val body = JSONObject()
            .put("user_id", s.userId)
            .put("data", data)
            .put("updated_at", java.time.Instant.ofEpochMilli(updatedAtMillis).toString())
        call("POST", "/rest/v1/user_data?on_conflict=user_id", body, s.accessToken,
            mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"))
    }

    companion object {
        fun parseFragment(fragment: String?): Map<String, String> {
            if (fragment.isNullOrEmpty()) return emptyMap()
            val out = LinkedHashMap<String, String>()
            for (part in fragment.trimStart('#', '?').split('&')) {
                if (part.isEmpty()) continue
                val i = part.indexOf('=')
                val k = if (i < 0) part else part.substring(0, i)
                val v = if (i < 0) "" else part.substring(i + 1)
                out[URLDecoder.decode(k, "UTF-8")] = URLDecoder.decode(v, "UTF-8")
            }
            return out
        }
    }
}
