package com.chargeguardian.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun GuardianTheme(appearance: Appearance, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(appearance.accent),
            onPrimary = Color(0xFF102008),
            primaryContainer = Color(appearance.panel),
            onPrimaryContainer = Color(appearance.accent),
            secondary = Color(appearance.accent),
            onSecondary = Color(0xFF102008),
            background = Color(appearance.background),
            onBackground = Color(0xFFF1F5EE),
            surface = Color(appearance.panel),
            onSurface = Color(0xFFF1F5EE),
            surfaceVariant = Color(appearance.panel),
            onSurfaceVariant = Color(if (appearance == Appearance.NEON_MINT) 0xFFB9CDCA else 0xFFBCC8B8),
            surfaceContainer = Color(appearance.panel),
            surfaceContainerHigh = Color(appearance.panel),
            outline = Color(if (appearance == Appearance.NEON_MINT) 0xFF7B9391 else 0xFF7E8C79),
            outlineVariant = Color(if (appearance == Appearance.NEON_MINT) 0xFF304148 else 0xFF354132)
        ),
        content = content
    )
}
