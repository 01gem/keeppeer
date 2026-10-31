package com.gem.keeppeer.ui.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gem.keeppeer.R
import com.gem.keeppeer.csv.CsvTransfer
import com.gem.keeppeer.csv.SafCsvDocuments
import com.gem.keeppeer.ui.export.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gem.keeppeer.security.*
import com.gem.keeppeer.ui.components.PrototypePage
import com.gem.keeppeer.ui.navigation.KeeppeerApp
import com.gem.keeppeer.ui.privacy.ProtectDialogWindow
import javax.crypto.Cipher

typealias BiometricAction = (Boolean, Cipher, (BiometricResult) -> Unit) -> Unit

@Composable
fun VaultRoot(session: VaultSession, biometric: BiometricAction) {
    val context = LocalContext.current
    val picker: CsvPickerModel = viewModel()
    CsvPickers(picker)
    SensitiveClipboardProvider(session.clipboard) {
    val state by session.state.collectAsStateWithLifecycle()
    val access = state.access
    if (state.stage == VaultStage.UNLOCKED && access != null) {
        key(access.id) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides access) {
                val resolver = LocalContext.current.applicationContext.contentResolver
                val transfer = remember { CsvTransfer(SafCsvDocuments(resolver)) }
                val csv: CsvViewModel = viewModel(factory = viewModelFactory {
                    initializer { CsvViewModel(access, transfer) }
                })
                var settings by remember { mutableStateOf(false) }
                KeeppeerApp(access.repository, { settings = true }) { value ->
                    session.state.value.access === access && session.clipboard.copy(value)
                }
                if (settings) androidx.compose.ui.window.Dialog(
                    onDismissRequest = { settings = false },
                    // Full-screen dialog uses the existing Scaffold system/IME insets.
                    // A floating decor can pan its header above the screen on API24/25.
                    properties = androidx.compose.ui.window.DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false,
                    ),
                ) {
                    ProtectDialogWindow()
                    com.gem.keeppeer.ui.settings.SecuritySettings(session, biometric, csv, picker,
                        (context.applicationContext as com.gem.keeppeer.KeeppeerApplication).container.appearance
                    ) { settings = false }
                }
                CsvDialogs(csv, picker, transfer)
            }
        }
    } else {
        // No NavHost or entry ViewModel exists on the locked branch.
        key(state.stage, state.generation) { LockScreen(state, session, biometric) }
    }
    }
}

@Composable
private fun LockScreen(state: VaultState, session: VaultSession, biometric: BiometricAction) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    var biometricMessage by remember { mutableStateOf<Int?>(null) }
    PrototypePage(if (state.stage == VaultStage.FIRST_RUN) stringResource(R.string.create_vault_title) else stringResource(R.string.locked_title)) {
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (state.stage) {
                VaultStage.CHECKING -> { CircularProgressIndicator(); Text(stringResource(R.string.checking_vault)) }
                VaultStage.UNAVAILABLE -> {
                    Text(stringResource(R.string.vault_unavailable_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.vault_unavailable_description))
                }
                VaultStage.UNLOCKING -> { CircularProgressIndicator(); Text(stringResource(R.string.opening_vault)) }
                VaultStage.LEGACY_RESET -> {
                    Text(stringResource(R.string.legacy_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.legacy_explanation))
                    Button(onClick = { confirmReset = true }) { Text(stringResource(R.string.legacy_reset)) }
                }
                else -> {
                    if (state.stage == VaultStage.FIRST_RUN) {
                        Text(stringResource(R.string.master_guidance))
                        Text(stringResource(R.string.device_guidance))
                    }
                    PasswordInput(stringResource(R.string.master_password), password, { password = it })
                    if (state.stage == VaultStage.FIRST_RUN) PasswordInput(stringResource(R.string.confirm_master), confirmation, { confirmation = it })
                    Button(onClick = {
                        val p = password.toCharArray()
                        val c = confirmation.toCharArray()
                        password = ""; confirmation = ""
                        if (state.stage == VaultStage.FIRST_RUN) session.setup(p, c)
                        else { c.fill('\u0000'); session.unlock(p) }
                    }) { Text(if (state.stage == VaultStage.FIRST_RUN) stringResource(R.string.create_vault) else stringResource(R.string.unlock)) }
                    if (state.stage != VaultStage.FIRST_RUN && session.biometrics.enabled()) {
                        OutlinedButton(onClick = {
                            try {
                                val cipher = session.biometrics.decryptionCipher()
                                biometric(false, cipher) { result ->
                                    if (!result.success) biometricMessage = result.message()
                                }
                            } catch (error: Exception) {
                                biometricMessage = BiometricDiagnostics.failure(BiometricStage.DECRYPT_INIT, error).message()
                            }
                        }) { Text(stringResource(R.string.biometric_unlock)) }
                    }
                }
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            biometricMessage?.let { Text(stringResource(it)) }
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { ProtectDialogWindow(); Text(stringResource(R.string.reset_title)) },
        text = { Text(stringResource(R.string.reset_explanation)) },
        confirmButton = { TextButton(onClick = { confirmReset = false; session.resetLegacy() }) { Text(stringResource(R.string.reset_confirm)) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun PasswordInput(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = com.gem.keeppeer.ui.edit.SensitiveKeyboard.copy(keyboardType = KeyboardType.Password),
        singleLine = true, modifier = Modifier.fillMaxWidth())
}

/** Confirmation is local memory only and independent of the password-change form. */
@Composable
internal fun BiometricPasswordDialog(dismiss: () -> Unit, confirm: (CharArray) -> Unit) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = { password = ""; dismiss() },
        title = { ProtectDialogWindow(); Text(stringResource(R.string.biometric_enroll_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.biometric_enroll_explanation))
                PasswordInput(stringResource(R.string.biometric_current), password, { password = it; error = null })
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = {
            if (password.isEmpty()) error = BiometricResult(
                BiometricOutcome.EMPTY_PASSWORD, BiometricStage.PASSWORD_INPUT).message()
            else {
                val input = password.toCharArray()
                password = ""
                confirm(input)
            }
        }) { Text(stringResource(R.string.biometric_continue)) } },
        dismissButton = { TextButton(onClick = { password = ""; dismiss() }) { Text(stringResource(R.string.cancel)) } },
    )
}
