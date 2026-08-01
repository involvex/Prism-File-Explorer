# Task 2.2: Register/unregister listeners in BaseActivity — Report

## Summary

Added two lines to `BaseActivity.kt` to wire the Shizuku lifecycle listeners (from Task 2.1) to the activity lifecycle.

## Changes

**File:** `app/src/main/java/com/raival/compose/file/explorer/base/BaseActivity.kt`

1. **`onCreate()` (line 69):** Added `globalClass.shizukuManager.registerListeners()` after the existing `Shizuku.addRequestPermissionResultListener` call.

2. **`onDestroy()` (line 77):** Added `globalClass.shizukuManager.unregisterListeners()` after the existing `Shizuku.removeRequestPermissionResultListener` call.

Both additions follow the existing pattern in the file — placed after the try/catch blocks that handle Shizuku permission listener registration.

## Verification

- `./gradlew assembleDebug` completed successfully (BUILD SUCCESSFUL in 48s)
- No new warnings introduced

## Self-Review

- The code is minimal and matches the task spec exactly.
- `globalClass` is already imported at line 39.
- `ShizukuManager.registerListeners()` and `unregisterListeners()` were added in Task 2.1 and are available.
- The placement ensures listeners are registered after the activity is created and unregistered before the activity is destroyed, which is the correct lifecycle timing.
