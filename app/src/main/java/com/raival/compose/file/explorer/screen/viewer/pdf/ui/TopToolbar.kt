package com.raival.compose.file.explorer.screen.viewer.pdf.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun TopToolbar(
    visible: Boolean,
    title: String,
    onBackClick: () -> Unit,
    onInfoClick: () -> Unit,
    isEditMode: Boolean,
    hasUnsavedChanges: Boolean,
    onEditModeToggle: () -> Unit,
    onSaveClick: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportOdf: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(500)
        ) + fadeIn(animationSpec = tween(500)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(500)
        ) + fadeOut(animationSpec = tween(500))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = colorScheme.surfaceContainer)
        ) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .windowInsetsPadding(WindowInsets.statusBars)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colorScheme.onSurface
                    )
                }

                if (isEditMode) {
                    AnimatedVisibility(visible = hasUnsavedChanges) {
                        IconButton(onClick = onSaveClick) {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = "Save",
                                tint = colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = {
                        onEditModeToggle()
                        onSaveClick()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Apply and save",
                            tint = colorScheme.primary
                        )
                    }
                }

                if (!isEditMode) {
                    IconButton(onClick = onEditModeToggle) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit",
                            tint = colorScheme.onSurface
                        )
                    }
                }

                var showMoreMenu by remember { mutableStateOf(false) }

                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = null,
                        tint = colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            showMoreMenu = false
                            onExportMarkdown()
                        },
                        text = { Text("Export as Markdown") }
                    )
                    DropdownMenuItem(
                        onClick = {
                            showMoreMenu = false
                            onExportOdf()
                        },
                        text = { Text("Export as ODF") }
                    )
                }

                IconButton(onClick = onInfoClick) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = colorScheme.onSurface
                    )
                }
            }
        }
    }
}
