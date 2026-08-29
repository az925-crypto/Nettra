package com.zaaam.nettra.inspector

object BodyCaptureHelper {
    private val blockedExts = setOf(
        "mp4","avi","mov","webm","mp3","wav","ogg","flac",
        "png","jpg","jpeg","webp","gif","svg","ico","avif",
        "woff","woff2","ttf","otf","eot",
        "mpg","mpeg","mkv","flv"
    )
    fun shouldRefetch(method: String, contentType: String?, url: String): Boolean {
        if (method.uppercase() != "GET") return false
        if (url.length > 2048) return false
        val ext = try {
            // Use MimeTypeMap and Uri path segment, not substringAfterLast '.' on full URL
            val fromMime = android.webkit.MimeTypeMap.getFileExtensionFromUrl(url)?.lowercase()
            if (!fromMime.isNullOrBlank()) {
                fromMime.substringBefore('?').substringBefore('#')
            } else {
                val path = android.net.Uri.parse(url).path ?: ""
                val lastSegment = path.substringAfterLast('/', "")
                lastSegment.substringAfterLast('.', "").lowercase().substringBefore('?').substringBefore('#')
            }
        } catch (_: Exception) {
            ""
        }
        if (ext in blockedExts) return false
        // fallback: also respect contentType if provided (case-insensitive), but URL ext is primary
        val ct = contentType?.lowercase() ?: ""
        if (ct.contains("image") || ct.contains("video") || ct.contains("audio") || ct.contains("font")) return false
        return true
    }

    /** case-insensitive header lookup helper */
    fun getHeaderIgnoreCase(headers: Map<String,String>?, name: String): String? {
        if (headers == null) return null
        val lower = name.lowercase()
        return headers.entries.firstOrNull { it.key.lowercase() == lower }?.value
    }
}
