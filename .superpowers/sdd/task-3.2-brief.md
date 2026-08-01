# Task 3.2: Define Hacker color scheme in Theme.kt

## Files
- Modify: `app/src/main/java/com/raival/compose/file/explorer/theme/Theme.kt`
- Modify: `app/src/main/res/values/strings.xml`

## What to do

### 1. Add Hacker color scheme to Theme.kt

Add the `Color` import at the top of Theme.kt (if not already present):
```kotlin
import androidx.compose.ui.graphics.Color
```

Add after the `LightColorScheme` definition (after line 28):

```kotlin
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
```

### 2. Handle HACKER in the `getTheme()` function

In the `FileExplorerTheme` composable's `getTheme()` function, add the `HACKER` case to both branches.

For the Android 12+ branch (lines 46-52), change:
```kotlin
            when (manager.theme) {
                ThemePreference.LIGHT.ordinal -> dynamicLightColorScheme(context)
                ThemePreference.DARK.ordinal -> dynamicDarkColorScheme(context)
                else -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(
                    context
                )
            }
```
To:
```kotlin
            when (manager.theme) {
                ThemePreference.LIGHT.ordinal -> dynamicLightColorScheme(context)
                ThemePreference.DARK.ordinal -> dynamicDarkColorScheme(context)
                ThemePreference.HACKER.ordinal -> HackerColorScheme
                else -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(
                    context
                )
            }
```

For the pre-Android 12 branch (lines 54-58), change:
```kotlin
            when (manager.theme) {
                ThemePreference.LIGHT.ordinal -> LightColorScheme
                ThemePreference.DARK.ordinal -> DarkColorScheme
                else -> if (darkTheme) DarkColorScheme else LightColorScheme
            }
```
To:
```kotlin
            when (manager.theme) {
                ThemePreference.LIGHT.ordinal -> LightColorScheme
                ThemePreference.DARK.ordinal -> DarkColorScheme
                ThemePreference.HACKER.ordinal -> HackerColorScheme
                else -> if (darkTheme) DarkColorScheme else LightColorScheme
            }
```

### 3. Force dark status bars for Hacker theme

In the `SideEffect` block (lines 72-78), change:
```kotlin
        SideEffect {
            (view.context as? Activity)?.window?.let {
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !darkTheme
            }

        }
```
To:
```kotlin
        SideEffect {
            (view.context as? Activity)?.window?.let {
                val isHackerTheme = manager.theme == ThemePreference.HACKER.ordinal
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars =
                    if (isHackerTheme) false else !darkTheme
            }
        }
```

### 4. Add string resource

Add to `strings.xml` before the closing `</resources>` tag:
```xml
<string name="hacker">Hacker</string>
```

## Context

- `ThemePreference` now has `HACKER` as the 4th enum value (added in Task 3.1)
- The color scheme uses `darkColorScheme()` as the base, overriding with green-on-black colors
- `#00FF41` is the classic "matrix green" / terminal green color
- `#000000` is pure black background
- The status bar should show light (white/green) icons on the dark background

## Verification
- The project should compile without errors after these changes.
