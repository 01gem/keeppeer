package com.gem.keeppeer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun OptionToggle(label: String, checked: Boolean, enabled: Boolean = true, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = change)
        .padding(vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(label, Modifier.weight(1f).padding(start = 12.dp))
    }
}
@Composable
fun OptionRadio(label: String, selected: Boolean, enabled: Boolean = true, select: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = select).padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        RadioButton(selected, onClick = null, enabled = enabled)
        Text(label, Modifier.weight(1f).padding(start = 12.dp))
    }
}
@Composable
fun SectionHeading(label: String) {
    Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}
