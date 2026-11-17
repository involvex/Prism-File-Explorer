package com.raival.compose.file.explorer.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.raival.compose.file.explorer.App.Companion.globalClass
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.raival.compose.file.explorer.screen.preferences.constant.ThemePreference
import com.raival.compose.file.explorer.screen.preferences.misc.PrefSnapshot

private val DarkColorScheme = darkColorScheme()

private val LightColorScheme = lightColorScheme()

private val HackerColorScheme = darkColorScheme(
    primary = Color(0xFF00FF41),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF003300),
    onPrimaryContainer = Color(0xFF00FF41),
    secondary = Color(0xFF00CC33),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF002200),
    onSecondaryContainer = Color(0xFF00FF41),
    tertiary = Color(0xFF33FF66),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF004400),
    onTertiaryContainer = Color(0xFF00FF41),
    background = Color(0xFF000000),
    onBackground = Color(0xFF00FF41),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFF00FF41),
    surfaceVariant = Color(0xFF111111),
    onSurfaceVariant = Color(0xFF00CC33),
    outline = Color(0xFF009922),
    outlineVariant = Color(0xFF003300),
    error = Color(0xFFFF0000),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF330000),
    onErrorContainer = Color(0xFFFF4444),
    inverseSurface = Color(0xFF00FF41),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = Color(0xFF003300),
    surfaceTint = Color(0xFF00FF41),
)

@Composable
fun FileExplorerTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Collect the preference snapshot as a StateFlow instead of reaching into
    // mutable property getters during composition.
    val prefs by PrefSnapshot.flow.collectAsState()
    val theme = prefs[intPreferencesKey("theme")] ?: ThemePreference.SYSTEM.ordinal
    val dynamicColor = prefs[booleanPreferencesKey("dynamicColor")] ?: true
    val darkTheme: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        if (theme == ThemePreference.SYSTEM.ordinal) {
            isSystemInDarkTheme()
        } else theme == ThemePreference.DARK.ordinal
    } else {
        theme == ThemePreference.DARK.ordinal
    }

    fun getTheme(): ColorScheme {
        val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        return if (useDynamic) {
            when (theme) {
                ThemePreference.LIGHT.ordinal -> dynamicLightColorScheme(context)
                ThemePreference.DARK.ordinal -> dynamicDarkColorScheme(context)
                ThemePreference.HACKER.ordinal -> HackerColorScheme
                else -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(
                    context
                )
            }
        } else {
            when (theme) {
                ThemePreference.LIGHT.ordinal -> LightColorScheme
                ThemePreference.DARK.ordinal -> DarkColorScheme
                ThemePreference.HACKER.ordinal -> HackerColorScheme
                else -> if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
    }

    var colorScheme by remember {
        mutableStateOf(getTheme())
    }

    LaunchedEffect(theme, dynamicColor) {
        colorScheme = getTheme()
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let {
                val isHackerTheme = theme == ThemePreference.HACKER.ordinal
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars =
                    if (isHackerTheme) false else !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}