package com.vibeprayer.app.utils

object InAppUpdater {
    const val VERSION = "v1.0.0-N-OS"

    suspend fun checkForUpdates(): String? {
        // GitHub release fallback is wired in CI notes; in-app check is a stub
        // until a releases endpoint is configured. Returning null = up to date.
        return null
    }
}
