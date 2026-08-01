# Task 3.1: Add HACKER to ThemePreference enum

## Files
- Modify: `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/constant/ThemePreference.kt`

## What to do

Add `HACKER` to the `ThemePreference` enum.

Change from:
```kotlin
package com.raival.compose.file.explorer.screen.preferences.constant

enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM
}
```

To:
```kotlin
package com.raival.compose.file.explorer.screen.preferences.constant

enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM,
    HACKER
}
```

## Verification
- The project should compile without errors after these changes.
