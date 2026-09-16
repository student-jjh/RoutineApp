package com.example.routineapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URI
import javax.net.ssl.HttpsURLConnection

/** Public configuration, not a user session. Safe to package, but never log credentials. */
class SupabaseConfig(val url: String, val publishableKey: String) {
    init {
        require(Regex("https://[a-z0-9-]+\\.supabase\\.co").matches(url))
        require(Regex("sb_publishable_[A-Za-z0-9_-]+").matches(publishableKey))
    }
}

data class SupabaseConnectionStatus(val googleEnabled: Boolean)

object SupabaseConnection {
    /** An explicit, read-only settings check. Does not sign in, read records or upload anything. */
    suspend fun check(config: SupabaseConfig): SupabaseConnectionStatus = withContext(Dispatchers.IO) {
        val connection = URI("${config.url}/auth/v1/settings").toURL().openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", config.publishableKey)
            connection.setRequestProperty("Accept", "application/json")
            check(connection.responseCode == 200) { "Supabase settings request failed" }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val buffer = CharArray(16_385)
                var length = 0
                while (length < buffer.size) {
                    val count = reader.read(buffer, length, buffer.size - length)
                    if (count == -1) break
                    length += count
                }
                require(length <= 16_384)
                String(buffer, 0, length)
            }
            parseStatus(body)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseStatus(body: String): SupabaseConnectionStatus {
        val google = JSONObject(body).getJSONObject("external").get("google")
        require(google is Boolean)
        return SupabaseConnectionStatus(google)
    }
}
