# Task 1.2: Add toggle to Behavior settings UI

## Files
- Modify: `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/ui/BehaviorContainer.kt`

## What to do

Add a "Disable Recycle Bin" toggle to the Behavior settings section.

### 1. Add the toggle to BehaviorContainer.kt

After the "Confirm before exit" toggle (around line 96, before the `useBuiltInViewer` section), insert:

```kotlin
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            thickness = 3.dp
        )

        PreferenceItem(
            label = stringResource(R.string.disable_recycle_bin),
            supportingText = stringResource(R.string.disable_recycle_bin_desc),
            icon = Icons.Rounded.DeleteSweep,
            switchState = prefs.disableRecycleBin,
            onSwitchChange = { prefs.disableRecycleBin = it }
        )
```

Add the import if missing:
```kotlin
import androidx.compose.material.icons.rounded.DeleteSweep
```

## Context

- `prefs` is already available as `globalClass.preferencesManager` at the top of the composable
- The string resources `R.string.disable_recycle_bin` and `R.string.disable_recycle_bin_desc` were added in Task 1.1
- Follow the exact same pattern as other `PreferenceItem` calls in this file (e.g., the "Confirm before exit" toggle above)

## Verification
- The project should compile without errors after these changes.
