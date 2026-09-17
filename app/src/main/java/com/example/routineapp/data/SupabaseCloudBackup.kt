package com.example.routineapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI

class SupabaseCloudBackup(private val config: SupabaseConfig) {
    suspend fun hasBackup(session: SupabaseSession): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            request("HEAD", objectUrl(session.user.id), session, null, emptyMap())
            true
        }.getOrDefault(false)
    }

    suspend fun upload(session: SupabaseSession, content: String) = withContext(Dispatchers.IO) {
        request(
            method = "POST",
            url = objectUrl(session.user.id),
            session = session,
            body = content.toByteArray(Charsets.UTF_8),
            headers = mapOf("Content-Type" to "application/json", "x-upsert" to "true")
        )
    }

    suspend fun download(session: SupabaseSession): String = withContext(Dispatchers.IO) {
        request("GET", objectUrl(session.user.id), session, null, emptyMap())
            .toString(Charsets.UTF_8)
    }

    private fun objectUrl(userId: String): String =
        "${config.url}/storage/v1/object/routine-backups/$userId/latest.json"

    private fun request(
        method: String,
        url: String,
        session: SupabaseSession,
        body: ByteArray?,
        headers: Map<String, String>
    ): ByteArray {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", config.publishableKey)
            connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.use { readLimited(it) } ?: ByteArray(0)
            if (code !in 200..299) {
                val detail = response.toString(Charsets.UTF_8)
                val message = when (code) {
                    404 -> "클라우드 백업을 찾지 못했어요."
                    401, 403 -> "로그인이 만료됐거나 백업 권한이 없어요. 다시 로그인해 주세요."
                    else -> "클라우드 백업 요청에 실패했어요."
                }
                error(if (detail.isBlank()) message else "$message ($code)")
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun readLimited(input: java.io.InputStream): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            require(output.size() + count <= RoutineBackupCodec.MAX_BYTES) { "클라우드 백업이 너무 커요." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
