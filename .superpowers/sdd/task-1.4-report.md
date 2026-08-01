# Task 1.4: Hide recycle bin section on home tab when disabled

## Summary

Added a conditional check around `RecycleBinSection` in the home tab's `when` block so it only renders when `globalClass.preferencesManager.disableRecycleBin` is `false`.

## Change

**File:** `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/home/ui/HomeTabContentView.kt`

At line 182-186, wrapped the `RecycleBinSection` composable call with:

```kotlin
if (!globalClass.preferencesManager.disableRecycleBin) {
    RecycleBinSection(mainActivityManager = mainActivityManager)
}
```

## Verification

- `./gradlew assembleDebug` completed successfully (BUILD SUCCESSFUL)
- No lint or compilation errors introduced
