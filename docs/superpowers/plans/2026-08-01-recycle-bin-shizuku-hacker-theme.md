# Recycle Bin Disable + Shizuku Refresh + Hacker Theme

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add three features: (1) a setting to disable the recycle bin, (2) a Shizuku availability refresh mechanism, and (3) a custom "Hacker" theme with green text on black background.

**Architecture:** All three features are independent and touch different subsystems. Feature 1 adds a boolean preference that gates recycle bin behavior across the delete dialog, home tab, and file tab. Feature 2 adds lifecycle listeners to `ShizukuManager` and a manual refresh button. Feature 3 extends the existing theme system with a new enum value and custom `ColorScheme`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, DataStore Preferences, Shizuku API

---

## Feature 1: Disable Recycle Bin Setting

### Task 1.1: Add `disableRecycleBin` preference

**Files:**
- Modify: `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/PreferencesManager.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: existing `prefMutableState` delegate pattern
- Produces: `preferencesManager.disableRecycleBin` boolean (default `false`)

- [ ] **Step 1: Add the preference to PreferencesManager.kt**

Add after the `moveToRecycleBin` preference (line ~173):

```kotlin
var disableRecycleBin by prefMutableState(
    keyName = "disableRecycleBin",
    defaultValue = false,
    getPreferencesKey = { booleanPreferencesKey(it) }
)
```

- [ ] **Step 2: Add string resources to strings.xml**

Add before the closing `</resources>` tag:

```xml
<string name="disable_recycle_bin">Disable Recycle Bin</string>
<string name="disable_recycle_bin_desc">Permanently delete files instead of moving to recycle bin</string>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/raival/compose/file/explorer/screen/preferences/PreferencesManager.kt app/src/main/res/values/strings.xml
git commit -m "feat: add disableRecycleBin preference"
```

---

### Task 1.2: Add toggle to Behavior settings UI

**Files:**
- Modify: `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/ui/BehaviorContainer.kt`

**Interfaces:**
- Consumes: `PreferencesManager.disableRecycleBin`
- Produces: none (UI only)

- [ ] **Step 1: Add the toggle to BehaviorContainer.kt**

Add after the "Confirm before exit" toggle (before the `useBuiltInViewer` section, around line 96), insert:

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

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/raival/compose/file/explorer/screen/preferences/ui/BehaviorContainer.kt
git commit -m "feat: add disable recycle bin toggle to settings"
```

---

### Task 1.3: Gate delete dialog recycle bin checkbox

**Files:**
- Modify: `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/ui/dialog/DeleteConfirmationDialog.kt`

**Interfaces:**
- Consumes: `PreferencesManager.disableRecycleBin`
- Produces: when disabled, skips recycle bin flow and always runs `DeleteTask`

- [ ] **Step 1: Modify DeleteConfirmationDialog.kt**

In the `DeleteConfirmationDialog` composable, the condition at line 105 that shows the "Move to Recycle Bin" checkbox needs to also check `disableRecycleBin`.

Change line 105 from:
```kotlin
                    if (!bottomOptionsBarState.value.showEmptyRecycleBinButton) {
```
To:
```kotlin
                    if (!bottomOptionsBarState.value.showEmptyRecycleBinButton && !preferencesManager.disableRecycleBin) {
```

Also change the confirm button onClick (line 66) to skip recycle bin when disabled. Change:
```kotlin
                            if (!bottomOptionsBarState.value.showEmptyRecycleBinButton && moveToRecycleBin) {
```
To:
```kotlin
                            if (!bottomOptionsBarState.value.showEmptyRecycleBinButton && moveToRecycleBin && !preferencesManager.disableRecycleBin) {
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/ui/dialog/DeleteConfirmationDialog.kt
git commit -m "feat: skip recycle bin in delete dialog when disabled"
```

---

### Task 1.4: Hide recycle bin section on home tab when disabled

**Files:**
- Modify: `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/home/ui/HomeTabContentView.kt`

**Interfaces:**
- Consumes: `PreferencesManager.disableRecycleBin`
- Produces: hides the Recycle Bin section when disabled

- [ ] **Step 1: Hide recycle bin section in HomeTabContentView.kt**

At line 182-184, change:
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

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/home/ui/HomeTabContentView.kt
git commit -m "feat: hide recycle bin on home tab when disabled"
```

---

## Feature 2: Shizuku Refresh Check

### Task 2.1: Add lifecycle listeners to ShizukuManager

**Files:**
- Modify: `app/src/main/java/com/raival/compose/file/explorer/ShizukuManager.kt`

**Interfaces:**
- Consumes: `Shizuku` API (`addBinderReceivedListener`, `addServiceReceivedListener`, `addBinderDeadListener`)
- Produces: `refreshAvailability()` public method, automatic availability updates via StateFlow

- [ ] **Step 1: Add a public `refreshAvailability()` method**

Add after the `checkAvailability()` method (after line 83):

```kotlin
fun refreshAvailability() {
    checkAvailability()
    if (_shizukuAvailable.value) {
        checkPermissionState()
    } else {
        _shizukuGranted.value = false
        _isRoot.value = false
        _isShell.value = false
    }
}
```

- [ ] **Step 2: Add Shizuku lifecycle listeners**

Add listener fields in the class:

```kotlin
private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
    Log.d(TAG, "Shizuku binder received")
    refreshAvailability()
}

private val binderDeadListener = Shizuku.OnBinderDeadListener {
    Log.d(TAG, "Shizuku binder dead")
    _shizukuAvailable.value = false
    _shizukuGranted.value = false
    _isRoot.value = false
    _isShell.value = false
}
```

- [ ] **Step 3: Add `registerListeners()` and `unregisterListeners()` methods**

```kotlin
fun registerListeners() {
    try {
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
    } catch (_: Exception) { }
}

fun unregisterListeners() {
    try {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
    } catch (_: Exception) { }
}
```

- [ ] **Step 4: Call `registerListeners()` in `init` block**

Add at the end of the `init` block (after `checkPermissionState()`):

```kotlin
registerListeners()
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/raival/compose/file/explorer/ShizukuManager.kt
git commit -m "feat: add Shizuku lifecycle listeners and refresh mechanism"
```

---

### Task 2.2: Register/unregister listeners in BaseActivity

**Files:**
- Modify: `app/src/main/java/com/raival/compose/file/explorer/base/BaseActivity.kt`

**Interfaces:**
- Consumes: `ShizukuManager.registerListeners()`, `ShizukuManager.unregisterListeners()`
- Produces: lifecycle-aware listener registration

- [ ] **Step 1: Add listener registration in onCreate**

In `BaseActivity.onCreate()`, after the existing Shizuku permission listener registration (line ~67), add:

```kotlin
globalClass.shizukuManager.registerListeners()
```

- [ ] **Step 2: Add listener unregistration in onDestroy**

In `BaseActivity.onDestroy()`, after the existing Shizuku permission listener removal (line ~74), add:

```kotlin
globalClass.shizukuManager.unregisterListeners()
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/raival/compose/file/explorer/base/BaseActivity.kt
git commit -m "feat: register Shizuku lifecycle listeners in BaseActivity"
```

---

### Task 2.3: Add refresh button to Shizuku settings UI

**Files:**
- Modify: `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/ui/ShizukuContainer.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `ShizukuManager.refreshAvailability()`
- Produces: manual refresh button in the Shizuku section

- [ ] **Step 1: Add string resource for refresh**

Add to `strings.xml`:
```xml
<string name="refresh_shizuku_status">Refresh Status</string>
```

- [ ] **Step 2: Add refresh button to ShizukuContainer.kt**

Modify the `else` branch (when Shizuku is not available, lines 35-52) to add a refresh button below the "not installed" text. Add after the existing `PreferenceItem` and before the closing `}` of the Container:

```kotlin
            PreferenceItem(
                label = stringResource(R.string.refresh_shizuku_status),
                supportingText = emptyString,
                icon = Icons.Rounded.Refresh,
                onClick = {
                    globalClass.shizukuManager.refreshAvailability()
                }
            )
```

Add the import:
```kotlin
import androidx.compose.material.icons.rounded.Refresh
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/raival/compose/file/explorer/screen/preferences/ui/ShizukuContainer.kt app/src/main/res/values/strings.xml
git commit -m "feat: add Shizuku refresh button in settings"
```

---

## Feature 3: Hacker Theme

### Task 3.1: Add HACKER to ThemePreference enum

**Files:**
- Modify: `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/constant/ThemePreference.kt`

**Interfaces:**
- Consumes: none
- Produces: `ThemePreference.HACKER` enum value

- [ ] **Step 1: Add HACKER to the enum**

```kotlin
package com.raival.compose.file.explorer.screen.preferences.constant

enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM,
    HACKER
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/raival/compose/file/explorer/screen/preferences/constant/ThemePreference.kt
git commit -m "feat: add HACKER to ThemePreference enum"
```

---

### Task 3.2: Define Hacker color scheme

**Files:**
- Modify: `app/src/main/java/com/raival/compose/file/explorer/theme/Theme.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `ThemePreference.HACKER`
- Produces: `HackerColorScheme` Material 3 `ColorScheme`

- [ ] **Step 1: Add Hacker color scheme to Theme.kt**

Add after the `LightColorScheme` definition (line 28):

```kotlin
private val HackerColorScheme = darkColorScheme(
    primary = Color(0xFF00FF41),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF003300),
    onPrimaryContainer = Color(0xFF00FF41),
    secondary = Color(0xFF00CC33),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF002200),
    onSecondaryContainer = Color(0xFF00FF41),
    tertiary = Color(0xFF33FF66),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF004400),
    onTertiaryContainer = Color(0xFF00FF41),
    background = Color(0xFF000000),
    onBackground = Color(0xFF00FF41),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFF00FF41),
    surfaceVariant = Color(0xFF111111),
    onSurfaceVariant = Color(0xFF00CC33),
    outline = Color(0xFF009922),
    outlineVariant = Color(0xFF003300),
    error = Color(0xFFFF0000),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF330000),
    onErrorContainer = Color(0xFFFF4444),
    inverseSurface = Color(0xFF00FF41),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = Color(0xFF003300),
    surfaceTint = Color(0xFF00FF41),
)
```

Add the import at the top of Theme.kt:
```kotlin
import androidx.compose.ui.graphics.Color
```

- [ ] **Step 2: Handle HACKER in the `getTheme()` function**

In the `FileExplorerTheme` composable's `getTheme()` function, add the HACKER case. Modify the when blocks:

For the Android 12+ branch (line 46-52):
```kotlin
when (manager.theme) {
    ThemePreference.LIGHT.ordinal -> dynamicLightColorScheme(context)
    ThemePreference.DARK.ordinal -> dynamicDarkColorScheme(context)
    ThemePreference.HACKER.ordinal -> HackerColorScheme
    else -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
```

For the pre-Android 12 branch (line 54-58):
```kotlin
when (manager.theme) {
    ThemePreference.LIGHT.ordinal -> LightColorScheme
    ThemePreference.DARK.ordinal -> DarkColorScheme
    ThemePreference.HACKER.ordinal -> HackerColorScheme
    else -> if (darkTheme) DarkColorScheme else LightColorScheme
}
```

- [ ] **Step 3: Force dark status bars for Hacker theme**

In the `SideEffect` block (line 72-78), the status bar light/dark is based on `darkTheme`. For HACKER theme, we always want dark status bars (light icons). Modify:

```kotlin
SideEffect {
    (view.context as? Activity)?.window?.let {
        val isHackerTheme = manager.theme == ThemePreference.HACKER.ordinal
        WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = 
            if (isHackerTheme) false else !darkTheme
    }
}
```

- [ ] **Step 4: Add string resource**

Add to `strings.xml`:
```xml
<string name="hacker">Hacker</string>
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/raival/compose/file/explorer/theme/Theme.kt app/src/main/res/values/strings.xml
git commit -m "feat: add Hacker theme with green-on-black color scheme"
```

---

### Task 3.3: Add Hacker to theme selector in settings

**Files:**
- Modify: `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/ui/AppearanceContainer.kt`

**Interfaces:**
- Consumes: `ThemePreference.HACKER`
- Produces: adds "Hacker" option to theme selection dialog

- [ ] **Step 1: Update the theme selector dialog**

In `AppearanceContainer.kt`, modify the theme preference item's `supportingText` (line 25-29) to include HACKER:

```kotlin
supportingText = when (prefs.theme) {
    ThemePreference.LIGHT.ordinal -> stringResource(R.string.light)
    ThemePreference.DARK.ordinal -> stringResource(R.string.dark)
    ThemePreference.HACKER.ordinal -> stringResource(R.string.hacker)
    else -> stringResource(R.string.follow_system)
},
```

Update the `choices` list in the dialog (line 35-39) to include Hacker:

```kotlin
choices = listOf(
    globalClass.getString(R.string.light),
    globalClass.getString(R.string.dark),
    globalClass.getString(R.string.follow_system),
    globalClass.getString(R.string.hacker)
),
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/raival/compose/file/explorer/screen/preferences/ui/AppearanceContainer.kt
git commit -m "feat: add Hacker option to theme selector dialog"
```

---

### Task 3.4: Verify build

- [ ] **Step 1: Run debug build**

Run: `./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Fix any compilation errors**

If there are errors, fix them and re-run.

- [ ] **Step 3: Final commit if needed**

```bash
git add -A
git commit -m "fix: resolve compilation errors for new features"
```
