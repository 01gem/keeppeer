package com.gem.keeppeer.ui.edit

import com.gem.keeppeer.ui.components.vaultFieldColors
import com.gem.keeppeer.ui.components.VaultTextButton
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
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
import com.gem.keeppeer.ui.components.OutlineSteelButton
import com.gem.keeppeer.ui.components.PrimaryButton
import com.gem.keeppeer.ui.components.VaultShapes
import com.gem.keeppeer.ui.privacy.ProtectDialogWindow
import com.gem.keeppeer.ui.theme.JetBrainsMono
import com.gem.keeppeer.ui.theme.LocalThemeMode
import kotlin.math.roundToInt

@Composable
fun GeneratorDialog(state: EntryEditState, options: (GeneratorOptions) -> Unit,
    regenerate: () -> Unit, use: () -> Unit, dismiss: () -> Unit, copy: (String) -> Boolean) {
    var reveal by remember(state.candidate, LocalThemeMode.current) { mutableStateOf(false) }
    var copied by remember(state.candidate) { mutableStateOf<Boolean?>(null) }
    val o = state.generatorOptions
    val lengthLabel = stringResource(R.string.generator_length_label)
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = dismiss,
        title = { ProtectDialogWindow(); Text(stringResource(R.string.generator_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.generator_length, o.length), style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface)
                Slider(value = o.length.toFloat(), onValueChange = { options(o.copy(length = it.roundToInt())) },
                    valueRange = GeneratorPolicy.MIN_LENGTH.toFloat()..GeneratorPolicy.MAX_LENGTH.toFloat(),
                    steps = GeneratorPolicy.MAX_LENGTH - GeneratorPolicy.MIN_LENGTH - 1,
                    colors = SliderDefaults.colors(thumbColor = scheme.primary, activeTrackColor = scheme.primary,
                        inactiveTrackColor = scheme.outline),
                    modifier = Modifier.semantics { contentDescription = lengthLabel })
                OptionToggle(stringResource(R.string.generator_lower), o.lowercase) { options(o.copy(lowercase = it)) }
                OptionToggle(stringResource(R.string.generator_upper), o.uppercase) { options(o.copy(uppercase = it)) }
                OptionToggle(stringResource(R.string.generator_digits), o.digits) { options(o.copy(digits = it)) }
                OptionToggle(stringResource(R.string.generator_symbols), o.symbols) { options(o.copy(symbols = it)) }
                OptionToggle(stringResource(R.string.generator_ambiguous), o.excludeAmbiguous) { options(o.copy(excludeAmbiguous = it)) }
                OutlinedTextField(value = state.candidate.orEmpty(), onValueChange = {}, readOnly = true,
                    label = { Text(stringResource(R.string.generator_candidate)) },
                    visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                    // JetBrains Mono only for revealed password characters.
                    textStyle = if (reveal) MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMono) else MaterialTheme.typography.bodyLarge,
                    shape = VaultShapes.Field,
                    colors = vaultFieldColors(),
                    modifier = Modifier.fillMaxWidth())
                // Row 1: two equal buttons, no icons so labels get the full width. Row 2: Regenerate full width.
                // At large font sizes labels wrap to a second line instead of clipping.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlineSteelButton(stringResource(if (reveal) R.string.hide_password else R.string.show_password),
                        { reveal = !reveal }, Modifier.weight(1f), enabled = state.candidate != null,
                        compact = true)
                    OutlineSteelButton(stringResource(R.string.copy_password),
                        { state.candidate?.let { copied = copy(it) } }, Modifier.weight(1f), enabled = state.candidate != null,
                        compact = true)
                }
                OutlineSteelButton(stringResource(R.string.generator_regenerate), regenerate, Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.Refresh)
                copied?.let {
                    Text(stringResource(if (it) R.string.clipboard_copied else R.string.clipboard_failed),
                        style = MaterialTheme.typography.bodyMedium, color = if (it) scheme.onSurface else scheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                if (state.generationFailed) Text(stringResource(R.string.generator_failed), color = scheme.error,
                    style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.generator_replace), style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            PrimaryButton(stringResource(R.string.generator_use), use, enabled = state.candidate != null && !state.saving)
        },
        dismissButton = { VaultTextButton(onClick = dismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) } },
    )
}
