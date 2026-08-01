# Task 1.4: Hide recycle bin section on home tab when disabled

## Files
- Modify: `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/home/ui/HomeTabContentView.kt`

## What to do

When `disableRecycleBin` is true, hide the Recycle Bin section from the home tab.

### Change needed

At line 182-184 in `HomeTabContentView.kt`, change:

```kotlin
                HomeSectionType.RECYCLE_BIN -> {
                    RecycleBinSection(mainActivityManager = mainActivityManager)
                }
```

To:

```kotlin
                HomeSectionType.RECYCLE_BIN -> {
                    if (!globalClass.preferencesManager.disableRecycleBin) {
                        RecycleBinSection(mainActivityManager = mainActivityManager)
                    }
                }
```

## Context

- This is inside a `when` block that iterates over home section types
- `globalClass` is imported from `com.raival.compose.file.explorer.App.Companion.globalClass`
- The `disableRecycleBin` preference was added in Task 1.1
- The RecycleBinSection composable is defined later in the same file (around line 589)

## Verification
- The project should compile without errors after these changes.
