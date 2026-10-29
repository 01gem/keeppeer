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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gem.keeppeer.security.*
import com.gem.keeppeer.ui.components.PrototypePage
import com.gem.keeppeer.ui.navigation.KeeppeerApp
import javax.crypto.Cipher

typealias BiometricAction = (Boolean, Cipher, (Boolean) -> Unit) -> Unit

@Composable
fun VaultRoot(session: VaultSession, biometric: BiometricAction) {
    SensitiveClipboardProvider(session.clipboard) {
    val state by session.state.collectAsStateWithLifecycle()
    val access = state.access
    if (state.stage == VaultStage.UNLOCKED && access != null) {
        key(access.id) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides access) {
                var settings by remember { mutableStateOf(false) }
                if (settings) SecuritySettings(session, biometric) { settings = false }
                else KeeppeerApp(access.repository, { settings = true }, session.clipboard::copy)
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
    var biometricMessage by remember { mutableStateOf<String?>(null) }
    PrototypePage(if (state.stage == VaultStage.FIRST_RUN) "Create your vault" else "Keeppeer locked") {
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (state.stage) {
                VaultStage.UNLOCKING -> { CircularProgressIndicator(); Text("Opening encrypted vault…") }
                VaultStage.LEGACY_RESET -> {
                    Text("Phase 1 plaintext development data found", style = MaterialTheme.typography.titleLarge)
                    Text("Only synthetic prototype data can be reset here. This permanently deletes all Phase 1 entries and starts a new encrypted vault. Existing data will remain untouched unless you confirm. There is no automatic migration.")
                    Button(onClick = { confirmReset = true }) { Text("Reset synthetic development data…") }
                }
                else -> {
                    if (state.stage == VaultStage.FIRST_RUN) {
                        Text("Choose a strong master password of at least 12 characters. Forgetting it can permanently lose your entries. There is no account, server, or password reset.")
                        Text("Protection is also tied to this device's Android Keystore. Uninstalling, clearing app data, or losing device keys can make the vault unrecoverable, even with your password.")
                    }
                    PasswordInput("Master password", password, { password = it })
                    if (state.stage == VaultStage.FIRST_RUN) PasswordInput("Confirm master password", confirmation, { confirmation = it })
                    Button(onClick = {
                        val p = password.toCharArray()
                        val c = confirmation.toCharArray()
                        password = ""; confirmation = ""
                        if (state.stage == VaultStage.FIRST_RUN) session.setup(p, c)
                        else { c.fill('\u0000'); session.unlock(p) }
                    }) { Text(if (state.stage == VaultStage.FIRST_RUN) "Create vault" else "Unlock") }
                    if (state.stage != VaultStage.FIRST_RUN && session.biometrics.enabled()) {
                        OutlinedButton(onClick = {
                            try {
                                val cipher = session.biometrics.decryptionCipher()
                                biometric(false, cipher) { success ->
                                    if (!success) biometricMessage = "Biometrics cancelled or unavailable. Use your master password."
                                }
                            } catch (_: Exception) {
                                biometricMessage = "Biometric key unavailable or invalidated. Use your master password and re-enable in Security settings."
                            }
                        }) { Text("Unlock with biometrics") }
                    }
                }
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            biometricMessage?.let { Text(it) }
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Permanently delete synthetic Phase 1 data?") },
        text = { Text("All plaintext prototype entries will be removed. Removal cannot guarantee erasure from flash storage. Continue only if these entries are synthetic and disposable.") },
        confirmButton = { TextButton(onClick = { confirmReset = false; session.resetLegacy() }) { Text("Delete synthetic data") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
    )
}

@Composable
fun PasswordInput(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun SecuritySettings(session: VaultSession, biometric: BiometricAction, back: () -> Unit) {
    val timeout by session.timeout.collectAsStateWithLifecycle()
    var current by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(session.biometrics.enabled()) }
    BackHandler(enabled = busy) {}
    PrototypePage("Security settings", if (busy) null else back) {
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = session::lock) { Text("Lock now") }
            Text("Lock after app goes into background", style = MaterialTheme.typography.titleMedium)
            LockTimeout.entries.forEach { option ->
                Row {
                    RadioButton(selected = timeout == option, onClick = { session.setTimeout(option) })
                    Text(option.label, Modifier.padding(top = 12.dp))
                }
            }
            Text("Screen lock always locks the vault. File pickers will follow the same background timeout.")
            HorizontalDivider()
            Text("Change master password", style = MaterialTheme.typography.titleMedium)
            Text("Consider exporting a backup before changing your password. CSV export becomes available in Phase 3. Plaintext exports must be protected separately.")
            PasswordInput("Current master password", current, { current = it })
            PasswordInput("New master password", replacement, { replacement = it })
            PasswordInput("Confirm new password", confirmation, { confirmation = it })
            Button(enabled = !busy, onClick = {
                val old = current.toCharArray(); val fresh = replacement.toCharArray(); val confirm = confirmation.toCharArray()
                current = ""; replacement = ""; confirmation = ""; busy = true
                session.changePassword(old, fresh, confirm) { message = it; busy = false }
            }) { Text("Change password") }
            HorizontalDivider()
            Text("Biometric unlock", style = MaterialTheme.typography.titleMedium)
            Text(if (enabled) "Enabled. Master password fallback remains available."
                else if (session.biometrics.available()) "Available. Enter your current master password above to enable."
                else "Strong biometrics are unavailable or not enrolled. Master password unlock remains available.")
            if (enabled) OutlinedButton(enabled = !busy, onClick = {
                session.disableBiometric { removed ->
                    enabled = session.biometrics.enabled()
                    message = if (removed) "Biometric unlock disabled." else "Biometric setting could not be fully cleared. Password unlock remains available."
                }
            }) { Text("Disable biometrics") }
            else Button(enabled = !busy && session.biometrics.available(), onClick = {
                val password = current.toCharArray(); current = ""; busy = true
                session.prepareBiometricEnrollment(password) { cipher ->
                    if (cipher == null) { message = "Password verification or biometric setup failed."; busy = false }
                    else biometric(true, cipher) { success ->
                        enabled = session.biometrics.enabled()
                        message = if (success) "Biometric unlock enabled." else "Biometrics cancelled or unavailable. Password unlock remains available."
                        busy = false
                    }
                }
            }) { Text("Enable biometrics") }
            message?.let { Text(it) }
        }
    }
}
