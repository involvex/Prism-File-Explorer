# Task 2.3 Report: Add refresh button to Shizuku settings UI

## Status: DONE

## Changes Made

### 1. `app/src/main/res/values/strings.xml`
- Added `<string name="refresh_shizuku_status">Refresh Status</string>` before closing `</resources>` tag.

### 2. `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/ui/ShizukuContainer.kt`
- Added imports for `Icons.Rounded.Refresh` and `emptyString`.
- Added a second `PreferenceItem` in the `else` branch (when Shizuku is not available) with:
  - Label: `R.string.refresh_shizuku_status`
  - Icon: `Icons.Rounded.Refresh`
  - onClick: calls `globalClass.shizukuManager.refreshAvailability()`

## Verification
- `./gradlew assembleDebug` completed successfully (BUILD SUCCESSFUL).
