package com.gem.keeppeer.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gem.keeppeer.R
import com.gem.keeppeer.security.*
import com.gem.keeppeer.ui.components.*
import com.gem.keeppeer.ui.export.*
import com.gem.keeppeer.ui.lock.*
import com.gem.keeppeer.ui.privacy.ProtectDialogWindow

@Composable
fun SecuritySettings(session: VaultSession, biometric: BiometricAction, csv: CsvViewModel,
    picker: CsvPickerModel, appearance: ThemeSettings, back: () -> Unit) {
    val resources = androidx.compose.ui.platform.LocalResources.current
    val timeout by session.timeout.collectAsStateWithLifecycle()
    val theme by appearance.state.collectAsStateWithLifecycle()
    var current by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(session.biometrics.enabled()) }
    var available by remember { mutableStateOf(session.biometrics.available()) }
    var confirmBiometric by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        enabled = session.biometrics.enabled()
        available = session.biometrics.available()
    }
    BackHandler(enabled = busy) {}
    PrototypePage(stringResource(R.string.security_title), if (busy) null else back) {
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = session::lock) { Text(stringResource(R.string.lock_now)) }
            SectionHeading(stringResource(R.string.theme))
            if (theme.loading) Text(stringResource(R.string.theme_loading))
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { option ->
                    OptionRadio(stringResource(when (option) {
                        ThemeMode.SYSTEM -> R.string.theme_system
                        ThemeMode.LIGHT -> R.string.theme_light
                        ThemeMode.DARK -> R.string.theme_dark
                    }), theme.mode == option, !theme.loading && !theme.saving && !theme.readFailed) { appearance.select(option) }
                }
            }
            if (theme.saving) Text(stringResource(R.string.theme_saving))
            if (theme.readFailed) {
                Text(stringResource(R.string.theme_read_failed))
                TextButton(onClick = appearance::retry) { Text(stringResource(R.string.retry)) }
            }
            if (theme.writeFailed) Text(stringResource(R.string.theme_write_failed), color = MaterialTheme.colorScheme.error)
            HorizontalDivider()
            SectionHeading(stringResource(R.string.timeout_heading))
            Column(Modifier.selectableGroup()) {
                LockTimeout.entries.forEach { option ->
                    OptionRadio(stringResource(when (option) {
                        LockTimeout.IMMEDIATE -> R.string.timeout_immediate
                        LockTimeout.MINUTE -> R.string.timeout_minute
                        LockTimeout.FIVE_MINUTES -> R.string.timeout_five
                    }), timeout == option) { session.setTimeout(option) }
                }
            }
            Text(stringResource(R.string.timeout_note))
            HorizontalDivider()
            CsvActions(csv, picker, !busy)
            HorizontalDivider()
            SectionHeading(stringResource(R.string.change_master))
            Text(stringResource(R.string.csv_password_nudge))
            PasswordInput(stringResource(R.string.current_master), current, { current = it })
            PasswordInput(stringResource(R.string.new_master), replacement, { replacement = it })
            PasswordInput(stringResource(R.string.confirm_new), confirmation, { confirmation = it })
            Button(enabled = !busy, onClick = {
                val old = current.toCharArray(); val fresh = replacement.toCharArray(); val confirm = confirmation.toCharArray()
                current = ""; replacement = ""; confirmation = ""; busy = true
                session.changePassword(old, fresh, confirm) { message = it; busy = false }
            }) { Text(stringResource(R.string.change_password)) }
            HorizontalDivider()
            SectionHeading(stringResource(R.string.biometric_heading))
            Text(stringResource(if (enabled) R.string.biometric_enabled
                else if (available) R.string.biometric_available else R.string.biometric_unavailable))
            if (enabled && !available) Text(stringResource(R.string.biometric_unavailable))
            if (enabled) OutlinedButton(enabled = !busy, onClick = {
                busy = true
                session.disableBiometric { removed ->
                    enabled = session.biometrics.enabled()
                    message = resources.getString(if (removed) R.string.biometric_disabled else R.string.biometric_disable_failed)
                    busy = false
                }
            }) { Text(stringResource(R.string.biometric_disable)) }
            else Button(enabled = !busy && available, onClick = { confirmBiometric = true }) {
                Text(stringResource(R.string.biometric_enable))
            }
            message?.let { Text(it) }
            HorizontalDivider()
            OutlinedButton(onClick = { about = true }) { Text(stringResource(R.string.about)) }
        }
    }
    if (confirmBiometric) BiometricPasswordDialog(
        dismiss = { confirmBiometric = false },
        confirm = { password ->
            confirmBiometric = false
            busy = true
            session.prepareBiometricEnrollment(password) { cipher, result ->
                if (cipher == null) { message = resources.getString(result.message()); busy = false }
                else biometric(true, cipher) { outcome ->
                    enabled = session.biometrics.enabled()
                    message = resources.getString(if (outcome.success) R.string.biometric_enrolled else outcome.message())
                    busy = false
                }
            }
        },
    )
    if (about) AboutDialog { about = false }
}

@Composable
private fun AboutDialog(dismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    AlertDialog(onDismissRequest = dismiss,
        title = { ProtectDialogWindow(); Text(stringResource(R.string.about)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            @Suppress("DEPRECATION")
            Text(stringResource(R.string.about_version, version.versionName.orEmpty(), version.versionCode))
            Text(stringResource(R.string.about_offline))
            Text(stringResource(R.string.about_export))
            Text(stringResource(R.string.about_limits))
            Text(stringResource(R.string.about_development), style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.csv_continue)) } })
}
