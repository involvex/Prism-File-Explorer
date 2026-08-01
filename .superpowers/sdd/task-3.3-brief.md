# Task 3.3: Add Hacker to theme selector in settings

## Files
- Modify: `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/ui/AppearanceContainer.kt`

## What to do

Add the "Hacker" option to the theme selector dialog in the Appearance settings screen.

### 1. Update the theme label display

In `AppearanceContainer.kt`, the `supportingText` for the theme preference (lines 25-29) currently shows the current theme name. Add the HACKER case.

Change:
```kotlin
            supportingText = when (prefs.theme) {
                ThemePreference.LIGHT.ordinal -> stringResource(R.string.light)
                ThemePreference.DARK.ordinal -> stringResource(R.string.dark)
                else -> stringResource(R.string.follow_system)
            },
```
To:
```kotlin
            supportingText = when (prefs.theme) {
                ThemePreference.LIGHT.ordinal -> stringResource(R.string.light)
                ThemePreference.DARK.ordinal -> stringResource(R.string.dark)
                ThemePreference.HACKER.ordinal -> stringResource(R.string.hacker)
                else -> stringResource(R.string.follow_system)
            },
```

### 2. Update the choices list in the dialog

Change the `choices` list (lines 35-39) from:
```kotlin
                    choices = listOf(
                        globalClass.getString(R.string.light),
                        globalClass.getString(R.string.dark),
                        globalClass.getString(R.string.follow_system)
                    ),
```
To:
```kotlin
                    choices = listOf(
                        globalClass.getString(R.string.light),
                        globalClass.getString(R.string.dark),
                        globalClass.getString(R.string.follow_system),
                        globalClass.getString(R.string.hacker)
                    ),
```

## Context

- `ThemePreference.HACKER` was added in Task 3.1
- The string resource `R.string.hacker` was added in Task 3.2
- The enum ordinal for HACKER is 3 (0=LIGHT, 1=DARK, 2=SYSTEM, 3=HACKER), which matches the position in the choices list (index 3)
- The `prefs.singleChoiceDialog.show()` method takes `selectedChoice` as the current ordinal value, so the dialog will correctly highlight the current selection

## Verification
- The project should compile without errors after these changes.
