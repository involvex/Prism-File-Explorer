# Task 1.2 Report: Add toggle to Behavior settings UI

## Status: DONE

## Changes Made

Modified `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/ui/BehaviorContainer.kt`:

1. Added import for `Icons.Rounded.DeleteSweep`
2. Inserted a `HorizontalDivider` and `PreferenceItem` toggle for "Disable Recycle Bin" after the "Confirm before exit" toggle and before the "Use built-in viewers" section

The toggle uses the same pattern as all other toggles in the file, referencing `prefs.disableRecycleBin` with the string resources `R.string.disable_recycle_bin` and `R.string.disable_recycle_bin_desc` (added in Task 1.1).

## Verification

- `compileDebugKotlin` passed with no errors
- String resources and preference field were confirmed present from prior tasks

## Commits

- `80b7814` — `feat: add Disable Recycle Bin toggle to Behavior settings`
