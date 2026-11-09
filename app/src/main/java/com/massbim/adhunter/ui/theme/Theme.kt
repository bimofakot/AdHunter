/*
 * Tema Material 3 AdHunter.
 * Palet tetap dipakai agar warna badge izin berisiko konsisten di semua device,
 * sementara mode terang/gelap mengikuti pengaturan sistem.
 */
package com.massbim.adhunter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF0A1C4A),
    inversePrimary = Color(0xFF9DB8FF),
    secondary = Color(0xFF0F766E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFF7F1),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFFB45309),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE7C2),
    onTertiaryContainer = Color(0xFF3A1B00),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFF6F8FC),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE3E8F0),
    onSurfaceVariant = Color(0xFF475467),
    surfaceTint = Color(0xFF1D4ED8),
    inverseSurface = Color(0xFF27313F),
    inverseOnSurface = Color(0xFFF2F5FA),
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE1DE),
    onErrorContainer = Color(0xFF5C0B06),
    outline = Color(0xFF8A97AB),
    outlineVariant = Color(0xFFC9D2DF),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE6EAF1),
    surfaceContainer = Color(0xFFEFF3F8),
    surfaceContainerHigh = Color(0xFFE9EEF5),
    surfaceContainerHighest = Color(0xFFE3E8F0),
    surfaceContainerLow = Color(0xFFF9FBFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C2FF),
    onPrimary = Color(0xFF06215B),
    primaryContainer = Color(0xFF183B7A),
    onPrimaryContainer = Color(0xFFDCE7FF),
    inversePrimary = Color(0xFF1D4ED8),
    secondary = Color(0xFF6FE6D8),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF0B4F49),
    onSecondaryContainer = Color(0xFFCFF7F1),
    tertiary = Color(0xFFFFC46B),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF673D00),
    onTertiaryContainer = Color(0xFFFFE7C2),
    background = Color(0xFF0B111A),
    onBackground = Color(0xFFE8EDF5),
    surface = Color(0xFF0B111A),
    onSurface = Color(0xFFE8EDF5),
    surfaceVariant = Color(0xFF3A4556),
    onSurfaceVariant = Color(0xFFC2CCDA),
    surfaceTint = Color(0xFFA9C2FF),
    inverseSurface = Color(0xFFE8EDF5),
    inverseOnSurface = Color(0xFF1B2534),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8B98AB),
    outlineVariant = Color(0xFF3A4556),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF303B4C),
    surfaceDim = Color(0xFF0B111A),
    surfaceContainer = Color(0xFF111A26),
    surfaceContainerHigh = Color(0xFF172230),
    surfaceContainerHighest = Color(0xFF1F2B3A),
    surfaceContainerLow = Color(0xFF0F1722),
    surfaceContainerLowest = Color(0xFF070C13),
)

@Composable
fun AdHunterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
