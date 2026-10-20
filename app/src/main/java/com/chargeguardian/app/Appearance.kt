package com.chargeguardian.app

/** Stable preference keys; unknown keys safely fall back to the default palette. */
enum class Appearance(val key: String, val label: String, val accent: Long, val background: Long, val panel: Long) {
    ELECTRIC_NEON("electric_neon", "Electric Neon", 0xFF83FF00, 0xFF090C09, 0xFF141A12),
    NEON_MINT("neon_mint", "Neon Mint", 0xFF83F7BB, 0xFF0B1014, 0xFF141F22);

    companion object {
        fun fromKey(key: String?): Appearance = entries.firstOrNull { it.key == key } ?: ELECTRIC_NEON
    }
}
