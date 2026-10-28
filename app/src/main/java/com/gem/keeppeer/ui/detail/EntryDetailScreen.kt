package com.gem.keeppeer.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gem.keeppeer.ui.components.PrototypePage
import com.gem.keeppeer.ui.components.StatusMessage
import java.text.DateFormat
import java.util.Date

@Composable
fun EntryDetailScreen(
    state: EntryDetailState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
) {
    var reveal by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    BackHandler(enabled = state.deleting) { }
    PrototypePage(title = "Entry details", onBack = if (state.deleting) null else onBack) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.entry == null -> StatusMessage(
                state.error ?: "This entry no longer exists. Return to the list.",
                if (state.error != null) onRetry else null,
            )
            else -> Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val entry = state.entry
                Text(entry.title, style = MaterialTheme.typography.headlineSmall)
                DetailValue("Username", entry.username)
                Text("Password", style = MaterialTheme.typography.labelLarge)
                Text(if (reveal) entry.password else "••••••••")
                TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Hide password" else "Show password") }
                DetailValue("URL", entry.url)
                DetailValue("Category", entry.category)
                DetailValue("Notes", entry.notes)
                Text(if (entry.isFavorite) "Favorite: Yes" else "Favorite: No")
                Text("Created: " + DateFormat.getDateTimeInstance().format(Date(entry.createdAt)))
                Text("Updated: " + DateFormat.getDateTimeInstance().format(Date(entry.updatedAt)))
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = onEdit, enabled = !state.deleting) { Text("Edit") }
                    OutlinedButton(onClick = { confirmDelete = true }, enabled = !state.deleting) {
                        Text(if (state.deleting) "Deleting…" else "Delete")
                    }
                }
            }
        }
    }
    if (confirmDelete && state.entry != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete entry?") },
            text = { Text("This permanently deletes this synthetic entry.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DetailValue(label: String, value: String?) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    Text(value ?: "—")
}
