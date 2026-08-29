package com.zaaam.nettra.privacy

/**
 * FR-7: Privacy Report per Situs — P1 (count derived from FR-4)
 * In-memory per-tab counter, reset on navigation — see 02_ARCHITECTURE.md
 */
data class PrivacyReport(
    val url: String,
    val blockedCount: Int,
    val isHttps: Boolean,
    val grade: String // A/B/C
) {
    companion object {
        fun gradeFor(blocked: Int, isHttps: Boolean): String {
            if (!isHttps) return "C"
            return if (blocked > 6) "B" else "A"
        }
    }
}
