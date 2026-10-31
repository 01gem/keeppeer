package com.gem.keeppeer.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.gem.keeppeer.R
import com.gem.keeppeer.domain.*
import com.gem.keeppeer.ui.components.OptionToggle
import com.gem.keeppeer.ui.privacy.ProtectDialogWindow
import com.gem.keeppeer.ui.theme.LocalThemeMode
import kotlin.math.roundToInt

@Composable
fun GeneratorDialog(state: EntryEditState, options: (GeneratorOptions) -> Unit,
    regenerate: () -> Unit, use: () -> Unit, dismiss: () -> Unit, copy: (String) -> Boolean) {
    var reveal by remember(state.candidate, LocalThemeMode.current) { mutableStateOf(false) }
    var copied by remember(state.candidate) { mutableStateOf<Boolean?>(null) }
    val o = state.generatorOptions
    val lengthLabel = stringResource(R.string.generator_length_label)
    AlertDialog(
        onDismissRequest = dismiss,
        title = { ProtectDialogWindow(); Text(stringResource(R.string.generator_title)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.generator_length, o.length))
                Slider(value = o.length.toFloat(), onValueChange = { options(o.copy(length = it.roundToInt())) },
                    valueRange = GeneratorPolicy.MIN_LENGTH.toFloat()..GeneratorPolicy.MAX_LENGTH.toFloat(),
                    steps = GeneratorPolicy.MAX_LENGTH - GeneratorPolicy.MIN_LENGTH - 1,
                    modifier = Modifier.semantics { contentDescription = lengthLabel })
                OptionToggle(stringResource(R.string.generator_lower), o.lowercase) { options(o.copy(lowercase = it)) }
                OptionToggle(stringResource(R.string.generator_upper), o.uppercase) { options(o.copy(uppercase = it)) }
                OptionToggle(stringResource(R.string.generator_digits), o.digits) { options(o.copy(digits = it)) }
                OptionToggle(stringResource(R.string.generator_symbols), o.symbols) { options(o.copy(symbols = it)) }
                OptionToggle(stringResource(R.string.generator_ambiguous), o.excludeAmbiguous) { options(o.copy(excludeAmbiguous = it)) }
                OutlinedTextField(value = state.candidate.orEmpty(), onValueChange = {}, readOnly = true,
                    label = { Text(stringResource(R.string.generator_candidate)) },
                    visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth())
                TextButton(enabled = state.candidate != null, onClick = { reveal = !reveal }) {
                    Text(stringResource(if (reveal) R.string.hide_password else R.string.show_password))
                }
                TextButton(enabled = state.candidate != null, onClick = { state.candidate?.let { copied = copy(it) } }) {
                    Text(stringResource(R.string.copy_password))
                }
                copied?.let { Text(stringResource(if (it) R.string.clipboard_copied else R.string.clipboard_failed)) }
                if (state.generationFailed) Text(stringResource(R.string.generator_failed), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = regenerate) { Text(stringResource(R.string.generator_regenerate)) }
                Text(stringResource(R.string.generator_replace))
            }
        },
        confirmButton = { TextButton(enabled = state.candidate != null && !state.saving, onClick = use) {
            Text(stringResource(R.string.generator_use))
        } },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
