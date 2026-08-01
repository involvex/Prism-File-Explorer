# Task 1.3: Gate delete dialog recycle bin checkbox

## Files
- Modify: `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/ui/dialog/DeleteConfirmationDialog.kt`

## What to do

When `disableRecycleBin` is true, the delete confirmation dialog should:
1. NOT show the "Move to Recycle Bin" checkbox
2. NOT use the recycle bin flow — always permanently delete

### Changes needed

There are exactly two conditions to modify in `DeleteConfirmationDialog.kt`:

**Change 1 (line 105):** The condition that shows the "Move to Recycle Bin" checkbox.

Change from:
```kotlin
                    if (!bottomOptionsBarState.value.showEmptyRecycleBinButton) {
```
To:
```kotlin
                    if (!bottomOptionsBarState.value.showEmptyRecycleBinButton && !preferencesManager.disableRecycleBin) {
```

**Change 2 (line 66):** The condition in the confirm button's onClick that decides whether to use recycle bin or permanent delete.

Change from:
```kotlin
                            if (!bottomOptionsBarState.value.showEmptyRecycleBinButton && moveToRecycleBin) {
```
To:
```kotlin
                            if (!bottomOptionsBarState.value.showEmptyRecycleBinButton && moveToRecycleBin && !preferencesManager.disableRecycleBin) {
```

## Context

- `preferencesManager` is already available as a local variable at the top of the composable (line 36): `val preferencesManager = globalClass.preferencesManager`
- The `disableRecycleBin` preference was added in Task 1.1
- The existing logic: when `moveToRecycleBin` is true and we're not in the recycle bin, files are copied to `.prism/bin/` via `CopyTask`. When false, `DeleteTask` permanently deletes.
- When `disableRecycleBin` is true, we want to always take the `DeleteTask` path regardless of `moveToRecycleBin`

## Verification
- The project should compile without errors after these changes.
