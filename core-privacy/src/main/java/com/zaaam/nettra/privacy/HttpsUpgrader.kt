package com.zaaam.nettra.privacy

object HttpsUpgrader {
    fun upgrade(url: String): String? = PrivacyEngine().shouldUpgradeToHttps(url)
    fun isHttps(url: String): Boolean = url.trim().lowercase().startsWith("https://")
    fun isHttp(url: String): Boolean = url.trim().lowercase().startsWith("http://")
}
