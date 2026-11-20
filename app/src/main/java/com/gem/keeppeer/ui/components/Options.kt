package com.gem.keeppeer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Checkbox row; the whole row toggles and TalkBack hears one checkbox. */
@Composable
fun OptionToggle(label: String, checked: Boolean, enabled: Boolean = true, change: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(VaultShapes.Row)
        .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = change)
        .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = scheme.primary, checkmarkColor = scheme.onPrimary,
                uncheckedColor = scheme.onSurfaceVariant))
        Text(label, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant)
    }
}

/** Radio row; use inside a selectableGroup. */
@Composable
fun OptionRadio(label: String, selected: Boolean, enabled: Boolean = true, select: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(VaultShapes.Row)
        .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = select).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick = null, enabled = enabled,
            colors = RadioButtonDefaults.colors(selectedColor = scheme.primary, unselectedColor = scheme.onSurfaceVariant))
        Text(label, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant)
    }
}
