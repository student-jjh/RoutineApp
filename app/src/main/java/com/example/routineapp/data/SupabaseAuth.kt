package com.example.routineapp.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

data class SupabaseUser(val id: String, val email: String?)

data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val user: SupabaseUser
)

class SupabaseAuth(private val context: Context, private val config: SupabaseConfig) {
    private val preferences = context.getSharedPreferences("supabase_auth", Context.MODE_PRIVATE)

    fun currentSession(): SupabaseSession? {
        val access = preferences.getString("accessToken", null) ?: return null
        val refresh = preferences.getString("refreshToken", null) ?: return null
        val userId = preferences.getString("userId", null) ?: return null
        return SupabaseSession(access, refresh, SupabaseUser(userId, preferences.getString("email", null)))
    }

    suspend fun signUp(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        validateCredentials(email, password)
        val redirect = URLEncoder.encode(REDIRECT_URI, Charsets.UTF_8.name())
        val response = request("${config.url}/auth/v1/signup?redirect_to=$redirect", "POST", JSONObject()
            .put("email", email.trim())
            .put("password", password))
        val session = response.optJSONObject("session")?.let(::sessionFrom)
        if (session != null) save(session)
        AuthResult(session, if (session == null) "가입했어요. 이메일 인증 후 로그인해 주세요." else "가입 및 로그인이 완료됐어요.")
    }

    suspend fun signIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        validateCredentials(email, password)
        val response = request("${config.url}/auth/v1/token?grant_type=password", "POST", JSONObject()
            .put("email", email.trim())
            .put("password", password))
        val session = sessionFrom(response)
        save(session)
        AuthResult(session, "로그인했어요.")
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        currentSession()?.let { runCatching {
            request("${config.url}/auth/v1/logout", "POST", null, it.accessToken)
        } }
        preferences.edit().clear().apply()
    }

    suspend fun handleCallback(uri: Uri): SupabaseSession = withContext(Dispatchers.IO) {
        val values = parseCallbackValues(uri)
        values["error_description"]?.let { error(Uri.decode(it)) }
        val access = values["access_token"]?.takeIf { it.isNotBlank() }
            ?: error("인증 링크에서 로그인 정보를 받지 못했어요.")
        val refresh = values["refresh_token"]?.takeIf { it.isNotBlank() }
            ?: error("인증 링크에서 갱신 정보를 받지 못했어요.")
        val user = request("${config.url}/auth/v1/user", "GET", null, access)
        val id = user.optString("id").takeIf { it.isNotBlank() } ?: error("사용자 정보를 받지 못했어요.")
        SupabaseSession(access, refresh, SupabaseUser(id, user.optString("email").ifBlank { null })).also(::save)
    }

    private fun save(session: SupabaseSession) {
        preferences.edit()
            .putString("accessToken", session.accessToken)
            .putString("refreshToken", session.refreshToken)
            .putString("userId", session.user.id)
            .putString("email", session.user.email)
            .apply()
    }

    private fun sessionFrom(body: JSONObject): SupabaseSession {
        val access = body.optString("access_token").takeIf { it.isNotBlank() } ?: error("세션을 받지 못했어요.")
        val refresh = body.optString("refresh_token").takeIf { it.isNotBlank() } ?: error("세션을 받지 못했어요.")
        val user = body.optJSONObject("user") ?: error("사용자 정보를 받지 못했어요.")
        val id = user.optString("id").takeIf { it.isNotBlank() } ?: error("사용자 정보를 받지 못했어요.")
        return SupabaseSession(access, refresh, SupabaseUser(id, user.optString("email").ifBlank { null }))
    }

    private fun request(url: String, method: String, body: JSONObject?, accessToken: String? = null): JSONObject {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", config.publishableKey)
            connection.setRequestProperty("Accept", "application/json")
            if (accessToken != null) connection.setRequestProperty("Authorization", "Bearer $accessToken")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val result = StringBuilder()
                val buffer = CharArray(8_192)
                while (result.length <= 1_048_576) {
                    val count = reader.read(buffer)
                    if (count == -1) break
                    result.append(buffer, 0, count)
                }
                require(result.length <= 1_048_576) { "서버 응답이 너무 커요." }
                result.toString()
            }.orEmpty()
            if (connection.responseCode !in 200..299) {
                val error = runCatching { JSONObject(responseText).optString("msg").ifBlank {
                    JSONObject(responseText).optString("error_description")
                } }.getOrNull().orEmpty()
                error(if (error.isBlank()) "Supabase 요청에 실패했어요." else error)
            }
            return if (responseText.isBlank()) JSONObject() else JSONObject(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun validateCredentials(email: String, password: String) {
        require(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim())) { "이메일 주소를 확인해 주세요." }
        require(password.length >= 6) { "비밀번호는 6자 이상이어야 해요." }
        require(password.length <= 128) { "비밀번호가 너무 길어요." }
    }

    private fun parseCallbackValues(uri: Uri): Map<String, String> {
        val source = listOfNotNull(uri.fragment, uri.query).joinToString("&")
        return source.split('&').mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) null else Uri.decode(part.substring(0, separator)) to Uri.decode(part.substring(separator + 1))
        }.toMap()
    }

    companion object {
        const val REDIRECT_URI = "routive://auth/callback"
    }
}

data class AuthResult(val session: SupabaseSession?, val message: String)
