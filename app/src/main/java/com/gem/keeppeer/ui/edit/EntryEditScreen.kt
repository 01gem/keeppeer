package com.gem.keeppeer.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.gem.keeppeer.R
import com.gem.keeppeer.ui.components.*
import com.gem.keeppeer.ui.theme.JetBrainsMono
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
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!state.saving) IconButton(onClick = onCancel) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                } else Spacer(Modifier.size(48.dp))
                Spacer(Modifier.weight(1f))
                IconButton(enabled = !state.saving, onClick = onSettings) {
                    Icon(Icons.Outlined.Shield, contentDescription = stringResource(R.string.settings))
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            BuildBanner()
            when {
                state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
                state.missing -> StatusMessage(stringResource(R.string.missing_entry))
                state.loadFailed -> StatusMessage(stringResource(R.string.load_entry_error), onRetry)
                else -> Column(
                    // imePadding keeps Save and Cancel reachable while typing in Notes.
                    Modifier.fillMaxWidth().weight(1f).imePadding().verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(if (editing) R.string.edit_entry else R.string.add_entry),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    val draft = state.draft
                    val enabled = !state.saving && state.savedId == null
                    VaultField(draft.title, { value -> onChange { it.copy(title = value) } },
                        stringResource(R.string.title_required), enabled, singleLine = true,
                        isError = state.titleError,
                        supportingText = if (state.titleError) ({ Text(stringResource(R.string.title_error)) }) else null)
                    VaultField(draft.username, { value -> onChange { it.copy(username = value) } },
                        stringResource(R.string.username_optional), enabled, singleLine = true)
                    VaultField(draft.password, { value -> onChange { it.copy(password = value) } },
                        stringResource(R.string.password), enabled,
                        keyboard = SensitiveKeyboard.copy(keyboardType = KeyboardType.Password),
                        transformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                        // JetBrains Mono only for revealed password characters.
                        textStyle = if (reveal) MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMono) else MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlineSteelButton(
                            stringResource(if (reveal) R.string.hide_password else R.string.show_password),
                            { reveal = !reveal }, Modifier.weight(1f), enabled = enabled,
                            icon = if (reveal) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, compact = true,
                        )
                        OutlineSteelButton(stringResource(R.string.generate), onGenerate, Modifier.weight(1f),
                            enabled = enabled, icon = Icons.Outlined.AutoAwesome, compact = true)
                    }
                    VaultField(draft.url, { value -> onChange { it.copy(url = value) } },
                        stringResource(R.string.url_optional), enabled, singleLine = true,
                        keyboard = SensitiveKeyboard.copy(keyboardType = KeyboardType.Uri))
                    VaultField(draft.category, { value -> onChange { it.copy(category = value) } },
                        stringResource(R.string.category_optional), enabled, singleLine = true)
                    VaultField(draft.notes, { value -> onChange { it.copy(notes = value) } },
                        stringResource(R.string.notes_optional), enabled, minLines = 3)
                    OptionToggle(stringResource(R.string.favorite), draft.isFavorite, enabled) { value -> onChange { it.copy(isFavorite = value) } }
                    state.error?.let {
                        Text(stringResource(R.string.save_entry_error), color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    PrimaryButton(stringResource(if (state.saving) R.string.saving else R.string.save), onSave,
                        Modifier.fillMaxWidth(), enabled = enabled)
                    VaultTextButton(onClick = onCancel, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge)
                    }
                    Text(stringResource(R.string.draft_notice), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun VaultField(
    value: String, change: (String) -> Unit, label: String, enabled: Boolean,
    singleLine: Boolean = false, minLines: Int = 1, isError: Boolean = false,
    keyboard: KeyboardOptions = SensitiveKeyboard,
    transformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    supportingText: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value, onValueChange = change, label = { Text(label) }, enabled = enabled,
        isError = isError, supportingText = supportingText, keyboardOptions = keyboard,
        visualTransformation = transformation, singleLine = singleLine, minLines = minLines,
        textStyle = textStyle, shape = VaultShapes.Field,
        colors = vaultFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}
