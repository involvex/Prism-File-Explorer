# Task 3.2 Report: Define Hacker color scheme in Theme.kt

## Changes Made

### 1. `app/src/main/java/com/raival/compose/file/explorer/theme/Theme.kt`
- Added `import androidx.compose.ui.graphics.Color`
- Added `HackerColorScheme` (30 color overrides using `darkColorScheme()` base, green-on-black palette with `#00FF41` matrix green)
- Added `ThemePreference.HACKER.ordinal` case to both branches of `getTheme()` (Android 12+ dynamic and pre-12 static)
- Updated `SideEffect` to force dark status bars (`isAppearanceLightStatusBars = false`) when Hacker theme is active

### 2. `app/src/main/res/values/strings.xml`
- Added `<string name="hacker">Hacker</string>` resource

## Verification
- `./gradlew assembleDebug` — BUILD SUCCESSFUL (2m 48s, only pre-existing deprecation warnings)
