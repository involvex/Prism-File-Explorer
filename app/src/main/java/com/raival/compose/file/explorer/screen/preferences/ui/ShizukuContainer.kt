package com.raival.compose.file.explorer.screen.preferences.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.common.emptyString

@Composable
fun ShizukuContainer() {
    val shizukuAvailable by globalClass.shizukuManager.shizukuAvailable.collectAsState()
    val shizukuEnabled by globalClass.shizukuManager.shizukuEnabled.collectAsState()
    val context = LocalContext.current

    Container(title = stringResource(R.string.shizuku_access)) {
        if (shizukuAvailable) {
            PreferenceItem(
                label = stringResource(R.string.enable_shizuku_access),
                supportingText = stringResource(R.string.shizuku_access_description),
                icon = Icons.Rounded.Security,
                switchState = shizukuEnabled,
                onSwitchChange = { enabled ->
                    globalClass.shizukuManager.toggleEnabled(enabled)
                }
            )
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
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        thickness = 3.dp
    )
}
