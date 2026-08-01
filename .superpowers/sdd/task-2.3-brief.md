# Task 2.3: Add refresh button to Shizuku settings UI

## Files
- Modify: `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/ui/ShizukuContainer.kt`
- Modify: `app/src/main/res/values/strings.xml`

## What to do

Add a manual "Refresh Status" button to the Shizuku settings section so users can recheck availability without force-stopping the app.

### 1. Add string resource to strings.xml

Before the closing `</resources>` tag, add:
```xml
<string name="refresh_shizuku_status">Refresh Status</string>
```

### 2. Add refresh button to ShizukuContainer.kt

In the `else` branch (when Shizuku is not available, lines 35-52), add a second `PreferenceItem` after the existing "not installed" one and before the closing `}` of the Container.

The current code in the `else` block is:
```kotlin
        } else {
            PreferenceItem(
                label = stringResource(R.string.shizuku_not_installed),
                supportingText = stringResource(R.string.shizuku_install_prompt),
                icon = Icons.Rounded.Security,
                onClick = {
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/RikkaApps/Shizuku/releases")
                        )
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        globalClass.showMsg(R.string.shizuku_not_installed)
                    }
                }
            )
        }
```

Change it to:
```kotlin
        } else {
            PreferenceItem(
                label = stringResource(R.string.shizuku_not_installed),
                supportingText = stringResource(R.string.shizuku_install_prompt),
                icon = Icons.Rounded.Security,
                onClick = {
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/RikkaApps/Shizuku/releases")
                        )
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        globalClass.showMsg(R.string.shizuku_not_installed)
                    }
                }
            )

            PreferenceItem(
                label = stringResource(R.string.refresh_shizuku_status),
                supportingText = emptyString,
                icon = Icons.Rounded.Refresh,
                onClick = {
                    globalClass.shizukuManager.refreshAvailability()
                }
            )
        }
```

Add the import for `Icons.Rounded.Refresh` if missing:
```kotlin
import androidx.compose.material.icons.rounded.Refresh
```

Also add the import for `emptyString` if missing:
```kotlin
import com.raival.compose.file.explorer.common.emptyString
```

## Context

- `ShizukuManager.refreshAvailability()` was added in Task 2.1
- `globalClass` is already imported in ShizukuContainer.kt
- The `emptyString` utility is used elsewhere in the codebase (e.g., BehaviorContainer.kt)
- The `Icons.Rounded.Refresh` icon is already used in BehaviorContainer.kt

## Verification
- The project should compile without errors after these changes.
