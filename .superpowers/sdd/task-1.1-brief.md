# Task 1.1: Add `disableRecycleBin` preference

## Files
- Modify: `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/PreferencesManager.kt`
- Modify: `app/src/main/res/values/strings.xml`

## What to do

### 1. Add preference to PreferencesManager.kt

After the `moveToRecycleBin` preference (line ~173), add:

```kotlin
var disableRecycleBin by prefMutableState(
    keyName = "disableRecycleBin",
    defaultValue = false,
    getPreferencesKey = { booleanPreferencesKey(it) }
)
```

### 2. Add string resources to strings.xml

Before the closing `</resources>` tag, add:

```xml
<string name="disable_recycle_bin">Disable Recycle Bin</string>
<string name="disable_recycle_bin_desc">Permanently delete files instead of moving to recycle bin</string>
```

## Verification
- The project should compile without errors after these changes.
