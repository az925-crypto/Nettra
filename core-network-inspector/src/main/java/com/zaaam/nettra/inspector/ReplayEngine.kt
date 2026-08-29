package com.zaaam.nettra.inspector

import com.zaaam.nettra.inspector.model.CapturedRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ReplayOverrides(val method: String? = null, val headers: Map<String,String>? = null, val body: String? = null, val url: String? = null)
data class ReplayResult(val status: Int, val headers: Map<String,String>, val body: String?, val durationMs: Long)

object ReplayEngine {
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).followRedirects(true).build()
    suspend fun replay(req: CapturedRequest, overrides: ReplayOverrides = ReplayOverrides()): ReplayResult = withContext(Dispatchers.IO) {
        val method = overrides.method ?: req.method
        val url = overrides.url ?: req.url
        // Resolve headers: prefer overrides, else use originalRequestHeaders to unmask, fallback to masked
        val baseHeaders: Map<String,String> = when {
            overrides.headers != null -> overrides.headers
            req.originalRequestHeaders != null -> {
                // merge: if masked placeholder, restore original value
                req.requestHeaders.mapValues { (k, v) ->
                    if (v == "••••••••") req.originalRequestHeaders[k] ?: v else v
                }
            }
            else -> req.requestHeaders
        }
        // If still contains masked placeholder and overrides provides value, prefer override
        val headers = if (overrides.headers == null && req.originalRequestHeaders != null) {
            baseHeaders.mapValues { (k,v) -> if (v == "••••••••") overrides.headers?.get(k) ?: req.originalRequestHeaders[k] ?: v else v }
        } else baseHeaders
        val bodyStr = overrides.body
        val builder = Request.Builder().url(url)
        headers.forEach { (k,v) -> builder.header(k, v) }
        val body = if (bodyStr != null && method !in listOf("GET","HEAD")) bodyStr.toRequestBody() else null
        builder.method(method, body)
        val start = System.currentTimeMillis()
        val resp = client.newCall(builder.build()).execute()
        val duration = System.currentTimeMillis() - start
        val respBody = try { resp.body?.string()?.take(1024*1024) } catch (_: Exception) { null }
        val respHeaders = resp.headers.toMultimap().mapValues { it.value.joinToString(",") }
        ReplayResult(resp.code, respHeaders, respBody, duration)
    }
}
