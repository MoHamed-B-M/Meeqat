package com.meeqat.azan.ui.athan

import com.meeqat.azan.domain.model.Prayer

/**
 * Maps a prayer to its bundled background image in `assets/images/`.
 * Existing files use short names (fajer/doher/aser/magreb); keep them as-is
 * and normalize here so no asset rename is required.
 */
object AthanBackgrounds {
    const val DIR = "images"

    fun assetPathFor(prayerName: String): String {
        val p = runCatching { Prayer.valueOf(prayerName) }.getOrNull()
        return assetPathFor(p)
    }

    fun assetPathFor(prayer: Prayer?): String = when (prayer) {
        Prayer.Fajr -> "$DIR/fajer.jpg"
        Prayer.Sunrise -> "$DIR/default.jpg"
        Prayer.Dhuhr -> "$DIR/doher.jpg"
        Prayer.Asr -> "$DIR/aser.jpg"
        Prayer.Maghrib -> "$DIR/magreb.jpg"
        Prayer.Isha -> "$DIR/default.jpg"
        null -> "$DIR/default.jpg"
    }

    /** All bundled backgrounds; used for preloading / validation. */
    val all: List<String> = listOf(
        "$DIR/fajer.jpg",
        "$DIR/doher.jpg",
        "$DIR/aser.jpg",
        "$DIR/magreb.jpg",
        "$DIR/default.jpg",
    )
}
