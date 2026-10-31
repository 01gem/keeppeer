package com.gem.keeppeer.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.gem.keeppeer.R
import com.gem.keeppeer.ui.components.*
import com.gem.keeppeer.ui.theme.LocalThemeMode

// The enclosing SensitiveInputProvider adds no-suggestions/no-learning requests.
// Third-party IMEs still control their behavior.
val SensitiveKeyboard = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Text)

@Composable
fun EntryEditScreen(
    state: EntryEditState, editing: Boolean,
    onChange: ((EntryDraft) -> EntryDraft) -> Unit, onSave: () -> Unit,
    onCancel: () -> Unit, onRetry: () -> Unit,
    onGenerate: () -> Unit = {}, onSettings: () -> Unit = {},
) {
    var reveal by remember(LocalThemeMode.current, state.generatorOpen) { mutableStateOf(false) }
    BackHandler(enabled = state.saving) { }
    PrototypePage(
        title = stringResource(if (editing) R.string.edit_entry else R.string.add_entry),
        onBack = if (state.saving) null else onCancel,
        actions = { TextButton(enabled = !state.saving, onClick = onSettings) { Text(stringResource(R.string.settings)) } },
    ) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.missing -> StatusMessage(stringResource(R.string.missing_entry))
            state.loadFailed -> StatusMessage(stringResource(R.string.load_entry_error), onRetry)
            else -> Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val draft = state.draft
                val enabled = !state.saving && state.savedId == null
                OutlinedTextField(value = draft.title,
                    onValueChange = { value -> onChange { it.copy(title = value) } },
                    label = { Text(stringResource(R.string.title_required)) }, isError = state.titleError, enabled = enabled,
                    supportingText = { if (state.titleError) Text(stringResource(R.string.title_error)) },
                    keyboardOptions = SensitiveKeyboard, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = draft.username,
                    onValueChange = { value -> onChange { it.copy(username = value) } },
                    label = { Text(stringResource(R.string.username_optional)) }, enabled = enabled,
                    keyboardOptions = SensitiveKeyboard, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = draft.password,
                    onValueChange = { value -> onChange { it.copy(password = value) } },
                    label = { Text(stringResource(R.string.password)) }, enabled = enabled,
                    keyboardOptions = SensitiveKeyboard.copy(keyboardType = KeyboardType.Password),
                    visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth())
                TextButton(enabled = enabled, onClick = { reveal = !reveal }) {
                    Text(stringResource(if (reveal) R.string.hide_password else R.string.show_password))
                }
                OutlinedButton(enabled = enabled, onClick = onGenerate) { Text(stringResource(R.string.generate)) }
                OutlinedTextField(value = draft.url,
                    onValueChange = { value -> onChange { it.copy(url = value) } },
                    label = { Text(stringResource(R.string.url_optional)) }, enabled = enabled,
                    keyboardOptions = SensitiveKeyboard.copy(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = draft.category,
                    onValueChange = { value -> onChange { it.copy(category = value) } },
                    label = { Text(stringResource(R.string.category_optional)) }, enabled = enabled,
                    keyboardOptions = SensitiveKeyboard, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = draft.notes,
                    onValueChange = { value -> onChange { it.copy(notes = value) } },
                    label = { Text(stringResource(R.string.notes_optional)) }, enabled = enabled,
                    keyboardOptions = SensitiveKeyboard, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OptionToggle(stringResource(R.string.favorite), draft.isFavorite, enabled) { value -> onChange { it.copy(isFavorite = value) } }
                state.error?.let { Text(stringResource(R.string.save_entry_error), color = MaterialTheme.colorScheme.error) }
                Button(onClick = onSave, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (state.saving) R.string.saving else R.string.save))
                }
                TextButton(onClick = onCancel, enabled = enabled) { Text(stringResource(R.string.cancel)) }
                Text(stringResource(R.string.draft_notice), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
