package com.gem.keeppeer.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.gem.keeppeer.ui.components.PrototypePage
import com.gem.keeppeer.ui.components.StatusMessage

@Composable
fun EntryEditScreen(
    state: EntryEditState,
    editing: Boolean,
    onChange: ((EntryDraft) -> EntryDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    // Reveal state is deliberately not saved and resets on rotation/navigation.
    var reveal by remember { mutableStateOf(false) }
    BackHandler(enabled = state.saving) { }
    PrototypePage(
        title = if (editing) "Edit entry" else "Add entry",
        onBack = if (state.saving) null else onCancel,
    ) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.missing -> StatusMessage("This entry no longer exists. Return to the list.")
            state.loadFailed -> StatusMessage(state.error ?: "Could not load entry.", onRetry)
            else -> Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val draft = state.draft
                val enabled = !state.saving && state.savedId == null
                OutlinedTextField(
                    value = draft.title, onValueChange = { value -> onChange { it.copy(title = value) } },
                    label = { Text("Title *") }, isError = state.titleError, enabled = enabled,
                    supportingText = { if (state.titleError) Text("Enter a nonblank title.") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                OutlinedTextField(
                    value = draft.username, onValueChange = { value -> onChange { it.copy(username = value) } },
                    label = { Text("Username (optional)") }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                OutlinedTextField(
                    value = draft.password, onValueChange = { value -> onChange { it.copy(password = value) } },
                    label = { Text("Password") }, enabled = enabled,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                    visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Hide" else "Show") } },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.url, onValueChange = { value -> onChange { it.copy(url = value) } },
                    label = { Text("URL (optional)") }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                OutlinedTextField(
                    value = draft.category, onValueChange = { value -> onChange { it.copy(category = value) } },
                    label = { Text("Category (optional)") }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                OutlinedTextField(
                    value = draft.notes, onValueChange = { value -> onChange { it.copy(notes = value) } },
                    label = { Text("Notes (optional)") }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth(), minLines = 3,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Checkbox(
                        checked = draft.isFavorite, enabled = enabled,
                        onCheckedChange = { value -> onChange { it.copy(isFavorite = value) } },
                    )
                    Text("Favorite", Modifier.padding(top = 12.dp))
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = onSave, enabled = enabled) { Text(if (state.saving) "Saving…" else "Save") }
                    TextButton(onClick = onCancel, enabled = enabled) { Text("Cancel") }
                }
                Text(
                    "Drafts stay in memory across rotation. Unsaved edits are lost if the process ends.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
