package com.zaaam.nettra.webview

enum class ThrottlingProfile(val delayMs: Long, val label: String) { OFF(0,"Off"), SLOW_3G(400,"Slow 3G"), FAST_3G(150,"Fast 3G"), OFFLINE(0,"Offline") }

object ThrottlingInterceptor {
    // kept for backward compat but not used as global source of truth; per-tab is authoritative
    @Volatile var profile: ThrottlingProfile = ThrottlingProfile.OFF

    fun maybeThrottle(profile: ThrottlingProfile) {
        val d = profile.delayMs
        if (d > 0) try { Thread.sleep(d) } catch (_: InterruptedException) {}
    }
    fun isOffline(profile: ThrottlingProfile): Boolean = profile == ThrottlingProfile.OFFLINE

    // legacy overloads delegate to global for callers not yet migrated
    fun maybeThrottle() = maybeThrottle(profile)
    fun isOffline(): Boolean = isOffline(profile)

    fun fromName(name: String?): ThrottlingProfile = try { ThrottlingProfile.valueOf(name ?: "OFF") } catch (_: Exception) { ThrottlingProfile.OFF }
}
