package com.raival.compose.file.explorer.screen.viewer.pdf.ui

import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.common.dp
import com.raival.compose.file.explorer.common.isNot
import com.raival.compose.file.explorer.common.ui.Isolate
import com.raival.compose.file.explorer.screen.viewer.pdf.PdfViewerInstance
import com.raival.compose.file.explorer.screen.viewer.pdf.misc.PdfFormField
import com.raival.compose.file.explorer.screen.viewer.pdf.misc.PdfPageHolder
import net.engawapg.lib.zoomable.ZoomState
import kotlin.math.roundToInt

@Composable
fun PdfPageItem(
    page: PdfPageHolder,
    pageSize: Size,
    instance: PdfViewerInstance,
    zoomState: ZoomState,
    isEditMode: Boolean,
    formFields: List<PdfFormField>,
    onFormFieldChange: (PdfFormField, String) -> Unit
) {
    var bitmap by remember(page.index) { mutableStateOf(page.bitmap) }
    var isLoading by remember(page.index) { mutableStateOf(false) }

    DisposableEffect(page.index) {
        if (bitmap == null && !isLoading) {
            isLoading = true
            instance.renderPage(page) { readyPage ->
                bitmap = readyPage.bitmap
                isLoading = false
            }
        }
        onDispose {
            instance.recycle(page)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLowest
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pageSize.height.dp()),
            contentAlignment = Alignment.Center
        ) {
            when {
                bitmap isNot null -> {
                    AsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        model = bitmap!!,
                        contentDescription = stringResource(R.string.page, page.index + 1),
                        contentScale = ContentScale.Fit
                    )
                }

                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }

                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.page, page.index + 1),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (isEditMode && bitmap isNot null) {
                FormFieldOverlay(
                    formFields = formFields,
                    pageWidthPx = pageSize.width,
                    pageHeightPx = pageSize.height,
                    onFormFieldChange = onFormFieldChange
                )
            }

            Isolate {
                val showPageNumber by remember(page.index) {
                    derivedStateOf {
                        zoomState.scale < 1.2f
                    }
                }
                if (showPageNumber) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        color = colorScheme.primary.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(6.dp),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "${page.index + 1}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormFieldOverlay(
    formFields: List<PdfFormField>,
    pageWidthPx: Int,
    pageHeightPx: Int,
    onFormFieldChange: (PdfFormField, String) -> Unit
) {
    val density = LocalDensity.current

    formFields.forEach { field ->
        val rect = field.rect

        val leftPx = (rect.left * pageWidthPx).roundToInt()
        val topPx = (rect.top * pageHeightPx).roundToInt()
        val widthDp = with(density) { ((rect.right - rect.left) * pageWidthPx).roundToInt().dp }
        val heightDp = with(density) { ((rect.bottom - rect.top) * pageHeightPx).roundToInt().dp }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(leftPx, topPx) }
                .width(widthDp)
                .height(heightDp)
        ) {
            FieldEditor(
                field = field,
                onValueChange = { newValue -> onFormFieldChange(field, newValue) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldEditor(
    field: PdfFormField,
    onValueChange: (String) -> Unit
) {
    when (field.fieldType) {
        PdfFormField.FieldType.TEXT,
        PdfFormField.FieldType.UNKNOWN -> {
            var textValue by remember(field.fullyQualifiedName) {
                mutableStateOf(TextFieldValue(field.value))
            }
            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                    onValueChange(it.text)
                },
                modifier = Modifier.fillMaxSize(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    color = colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = colorScheme.onSurface,
                    unfocusedTextColor = colorScheme.onSurface,
                    focusedIndicatorColor = colorScheme.primary,
                    unfocusedIndicatorColor = colorScheme.outline
                ),
                singleLine = true
            )
        }

        PdfFormField.FieldType.CHECKBOX -> {
            var isChecked by remember(field.fullyQualifiedName) {
                mutableStateOf(field.value.equals("Yes", ignoreCase = true))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = {
                        isChecked = it
                        onValueChange(if (it) "Yes" else "Off")
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        PdfFormField.FieldType.RADIO_BUTTON -> {
            var selected by remember(field.fullyQualifiedName) {
                mutableStateOf(field.value)
            }
            val options = field.options.ifEmpty { listOf(field.value, if (field.value == "Yes") "Off" else "Yes") }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == option,
                            onClick = {
                                selected = option
                                onValueChange(option)
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        PdfFormField.FieldType.LIST -> {
            var expanded by remember(field.fullyQualifiedName) { mutableStateOf(false) }
            var selected by remember(field.fullyQualifiedName) { mutableStateOf(field.value) }
            val options = field.options

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = selected.ifEmpty { stringResource(R.string.select_option) },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { expanded = true }
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = colorScheme.surfaceContainer
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            onClick = {
                                selected = option
                                onValueChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        PdfFormField.FieldType.COMBO_BOX -> {
            var expanded by remember(field.fullyQualifiedName) { mutableStateOf(false) }
            var selected by remember(field.fullyQualifiedName) { mutableStateOf(field.value) }
            val options = field.options

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = selected.ifEmpty { stringResource(R.string.select_option) },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { expanded = true }
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = colorScheme.surfaceContainer
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            onClick = {
                                selected = option
                                onValueChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        PdfFormField.FieldType.SIGNATURE -> {
            Text(
                text = stringResource(R.string.signature_field),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
        }
    }
}
