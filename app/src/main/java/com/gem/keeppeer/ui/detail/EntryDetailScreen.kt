package com.gem.keeppeer.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gem.keeppeer.R
import androidx.compose.ui.unit.dp
import com.gem.keeppeer.ui.components.PrototypePage
import com.gem.keeppeer.ui.components.StatusMessage
import com.gem.keeppeer.ui.privacy.ProtectDialogWindow
import java.text.DateFormat
import java.util.Date

@Composable
fun EntryDetailScreen(
    state: EntryDetailState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onCopy: (String) -> Boolean,
) {
    var reveal by remember(com.gem.keeppeer.ui.theme.LocalThemeMode.current) { mutableStateOf(false) }
    var confirmDelete by remember(com.gem.keeppeer.ui.theme.LocalThemeMode.current) { mutableStateOf(false) }
    var copyStatus by remember { mutableStateOf<Int?>(null) }
    BackHandler(enabled = state.deleting) { }
    PrototypePage(title = stringResource(R.string.entry_details), onBack = if (state.deleting) null else onBack) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.entry == null -> StatusMessage(
                stringResource(if (state.error != null) R.string.load_entry_error else R.string.missing_entry),
                if (state.error != null) onRetry else null,
            )
            else -> Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val entry = state.entry
                Text(entry.title, style = MaterialTheme.typography.headlineSmall)
                DetailValue(stringResource(R.string.username), entry.username)
                entry.username?.let { value -> TextButton(onClick = { copyStatus = if (onCopy(value)) R.string.clipboard_copied else R.string.clipboard_failed }) { Text(stringResource(R.string.copy_username)) } }
                Text(stringResource(R.string.password), style = MaterialTheme.typography.labelLarge)
                Text(if (reveal) entry.password else "••••••••")
                TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) stringResource(R.string.hide_password) else stringResource(R.string.show_password)) }
                TextButton(onClick = { copyStatus = if (onCopy(entry.password)) R.string.clipboard_copied else R.string.clipboard_failed }) { Text(stringResource(R.string.copy_password)) }
                DetailValue(stringResource(R.string.url), entry.url)
                entry.url?.let { value -> TextButton(onClick = { copyStatus = if (onCopy(value)) R.string.clipboard_copied else R.string.clipboard_failed }) { Text(stringResource(R.string.copy_url)) } }
                DetailValue(stringResource(R.string.category), entry.category)
                DetailValue(stringResource(R.string.notes), entry.notes)
                entry.notes?.let { value -> TextButton(onClick = { copyStatus = if (onCopy(value)) R.string.clipboard_copied else R.string.clipboard_failed }) { Text(stringResource(R.string.copy_notes)) } }
                copyStatus?.let { Text(stringResource(it)) }
                Text(if (entry.isFavorite) stringResource(R.string.favorite_yes) else stringResource(R.string.favorite_no))
                Text(stringResource(R.string.created, DateFormat.getDateTimeInstance().format(Date(entry.createdAt))))
                Text(stringResource(R.string.updated, DateFormat.getDateTimeInstance().format(Date(entry.updatedAt))))
                state.error?.let { Text(stringResource(R.string.delete_entry_error), color = MaterialTheme.colorScheme.error) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onEdit, enabled = !state.deleting) { Text(stringResource(R.string.edit)) }
                    OutlinedButton(onClick = { confirmDelete = true }, enabled = !state.deleting) {
                        Text(if (state.deleting) stringResource(R.string.deleting) else stringResource(R.string.delete))
                    }
                }
            }
        }
    }
    if (confirmDelete && state.entry != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { ProtectDialogWindow(); Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_explanation)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun DetailValue(label: String, value: String?) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    Text(value ?: stringResource(R.string.empty_value))
}
