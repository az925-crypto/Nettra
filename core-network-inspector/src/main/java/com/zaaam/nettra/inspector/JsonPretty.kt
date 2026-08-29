package com.zaaam.nettra.inspector

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

object JsonPretty {
    private val json = Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true }
    fun prettyPrint(raw: String): String {
        return try {
            val el = json.parseToJsonElement(raw)
            json.encodeToString(JsonElement.serializer(), el)
        } catch (_: Exception) { raw }
    }
    fun isJson(contentType: String?): Boolean = contentType?.lowercase()?.contains("json") == true
    fun isJsonBody(body: String): Boolean {
        val t = body.trim()
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"))
    }
}
