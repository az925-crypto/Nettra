package com.zaaam.nettra.inspector.model

enum class ResourceType { All, FetchXHR, JS, CSS, Img, Media, Font, Doc, Other }

fun classifyResource(url: String, contentType: String? = null, isXhr: Boolean = false): ResourceType {
    if (isXhr) return ResourceType.FetchXHR
    val ct = contentType?.lowercase() ?: ""
    if (ct.contains("json") || ct.contains("xmlhttprequest")) return ResourceType.FetchXHR
    if (ct.contains("javascript")) return ResourceType.JS
    if (ct.contains("css")) return ResourceType.CSS
    if (ct.contains("image")) return ResourceType.Img
    if (ct.contains("video") || ct.contains("audio")) return ResourceType.Media
    if (ct.contains("font")) return ResourceType.Font
    if (ct.contains("text/html")) return ResourceType.Doc
    val ext = url.substringAfterLast('.', "").substringBefore('?').lowercase()
    return when (ext) {
        "js", "mjs" -> ResourceType.JS
        "css" -> ResourceType.CSS
        "png","jpg","jpeg","webp","gif","svg","ico","avif" -> ResourceType.Img
        "mp4","webm","mp3","wav","ogg" -> ResourceType.Media
        "woff","woff2","ttf","otf","eot" -> ResourceType.Font
        "html","htm" -> ResourceType.Doc
        "json" -> ResourceType.FetchXHR
        "" -> ResourceType.Other
        else -> ResourceType.Other
    }
}
