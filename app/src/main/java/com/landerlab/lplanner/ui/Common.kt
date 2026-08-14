package com.landerlab.lplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Common.kt — Lplanner Android v1.0.0
 *
 * Small monochrome controls matching the SwiftUI helpers in ZPlannerView.swift
 * (check / seg / row2 / editor), so both front ends read the same way.
 */

/** Checkbox + label, drawn as a bordered box like the SwiftUI `check`. */
@Composable
fun Check(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .alphaIf(enabled)
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 4.dp),
    ) {
        Icon(
            imageVector = if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = label.ifEmpty { if (checked) "checked" else "unchecked" },
            modifier = Modifier.size(20.dp),
        )
        if (label.isNotEmpty()) Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Two-value segmented control (the SwiftUI `.pickerStyle(.segmented)` equivalent). */
@Composable
fun Seg(
    options: List<String>,
    selectedIndex: Int,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    val outline = MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier
            .alphaIf(enabled)
            .border(1.dp, outline, RoundedCornerShape(4.dp)),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Text(
                text = label,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable(enabled = enabled) { onSelect(i) }
                    .padding(vertical = 8.dp, horizontal = 6.dp),
            )
        }
    }
}

/** Label + narrow single-line field (SwiftUI `row2`). */
@Composable
fun LabeledField(
    label: String,
    value: String,
    enabled: Boolean = true,
    fieldWidth: Int = 96,
    onChange: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.alphaIf(enabled),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it.replace("\n", "")) },
            singleLine = true,
            enabled = enabled,
            textStyle = MonoText,
            modifier = Modifier.width(fieldWidth.dp),
        )
    }
}

/** Multi-line monospace editor for rate / setpoint tables (SwiftUI `editor`). */
@Composable
fun RateEditor(
    value: String,
    enabled: Boolean = true,
    minHeight: Int = 84,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        textStyle = MonoText,
        modifier = Modifier
            .alphaIf(enabled)
            .fillMaxWidth()
            .heightIn(min = minHeight.dp),
    )
}

/** Config section: title, controls, grey help text, divider. */
@Composable
fun ConfigGroup(
    title: String,
    help: String,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        content()
        Text(
            help,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        androidx.compose.material3.HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Dim disabled controls the way SwiftUI's `.opacity(0.4)` does. */
fun Modifier.alphaIf(enabled: Boolean): Modifier =
    if (enabled) this else this.alpha(0.4f)
