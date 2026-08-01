# Task 3.3: Add Hacker to theme selector in settings — Report

## Changes Made

**File modified:** `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/ui/AppearanceContainer.kt`

1. **Supporting text (line 25-30):** Added `ThemePreference.HACKER.ordinal -> stringResource(R.string.hacker)` to the `when` expression so the current theme label displays "Hacker" when that theme is active.

2. **Choices list (line 36-41):** Added `globalClass.getString(R.string.hacker)` to the dialog choices list so users can select the Hacker theme from the picker.

## Verification

- `assembleDebug` completed successfully (BUILD SUCCESSFUL).
- Both changes match the task specification exactly — ordinals align with the `ThemePreference` enum positions (LIGHT=0, DARK=1, SYSTEM=2, HACKER=3).

## Commit

- `3cd7fcb` — `feat: add Hacker option to theme selector in settings`
