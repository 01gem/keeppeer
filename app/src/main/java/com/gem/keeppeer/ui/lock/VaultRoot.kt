package com.gem.keeppeer.ui.lock

import com.gem.keeppeer.ui.components.vaultFieldColors
import com.gem.keeppeer.ui.components.VaultTextButton
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.gem.keeppeer.ui.components.BuildBanner
import com.gem.keeppeer.ui.components.KeeppeerShield
import com.gem.keeppeer.ui.components.OutlineSteelButton
import com.gem.keeppeer.ui.components.PrimaryButton
import com.gem.keeppeer.ui.components.VaultShapes
import com.gem.keeppeer.ui.components.WarningNote
import com.gem.keeppeer.ui.theme.KeeppeerTheme
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
                KeeppeerApp(access.repository, { settings = true }, onLock = session::lock) { value ->
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
                    com.gem.keeppeer.ui.theme.MatchSystemBars()
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
    val firstRun = state.stage == VaultStage.FIRST_RUN
    val (title, subtitle) = when (state.stage) {
        VaultStage.CHECKING -> stringResource(R.string.app_name) to stringResource(R.string.checking_vault)
        VaultStage.UNLOCKING -> stringResource(R.string.locked_title) to stringResource(R.string.opening_vault)
        VaultStage.UNAVAILABLE -> stringResource(R.string.vault_unavailable_title) to stringResource(R.string.vault_unavailable_description)
        VaultStage.LEGACY_RESET -> stringResource(R.string.legacy_title) to stringResource(R.string.legacy_explanation)
        VaultStage.FIRST_RUN -> stringResource(R.string.create_vault_title) to stringResource(R.string.create_vault_subtitle)
        else -> stringResource(R.string.locked_title) to stringResource(R.string.locked_subtitle)
    }
    val submit = {
        val p = password.toCharArray()
        val c = confirmation.toCharArray()
        password = ""; confirmation = ""
        if (firstRun) session.setup(p, c)
        else { c.fill('\u0000'); session.unlock(p) }
    }
    VaultShell(title, subtitle) {
        when (state.stage) {
            VaultStage.CHECKING, VaultStage.UNLOCKING -> CircularProgressIndicator()
            VaultStage.UNAVAILABLE -> Unit
            VaultStage.LEGACY_RESET -> OutlineSteelButton(
                stringResource(R.string.legacy_reset), { confirmReset = true }, Modifier.fillMaxWidth(),
            )
            else -> {
                val biometricsOn = !firstRun && session.biometrics.enabled()
                if (firstRun) {
                    WarningNote(stringResource(R.string.master_guidance) + "\n\n" + stringResource(R.string.device_guidance))
                }
                if (biometricsOn) {
                    PrimaryButton(
                        text = stringResource(R.string.biometric_unlock),
                        onClick = {
                            try {
                                val cipher = session.biometrics.decryptionCipher()
                                biometric(false, cipher) { result ->
                                    if (!result.success) biometricMessage = result.message()
                                }
                            } catch (error: Exception) {
                                biometricMessage = BiometricDiagnostics.failure(BiometricStage.DECRYPT_INIT, error).message()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Outlined.Fingerprint,
                        height = 64.dp,
                    )
                    OrDivider(stringResource(R.string.unlock_or_password))
                }
                LockPasswordField(stringResource(R.string.master_password), password) { password = it }
                if (firstRun) LockPasswordField(stringResource(R.string.confirm_master), confirmation) { confirmation = it }
                val action = if (firstRun) stringResource(R.string.create_vault) else stringResource(R.string.unlock)
                if (biometricsOn) OutlineSteelButton(action, submit, Modifier.fillMaxWidth())
                else PrimaryButton(action, submit, Modifier.fillMaxWidth())
            }
        }
        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite })
        }
        biometricMessage?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite })
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { ProtectDialogWindow(); Text(stringResource(R.string.reset_title)) },
        text = { Text(stringResource(R.string.reset_explanation)) },
        confirmButton = { VaultTextButton(onClick = { confirmReset = false; session.resetLegacy() }) { Text(stringResource(R.string.reset_confirm)) } },
        dismissButton = { VaultTextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Shared layout for every vault state: gradient, ringed shield, title, explanation, content, footer. */
@Composable
private fun VaultShell(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val glow = KeeppeerTheme.colors.surfaceHigh
    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(glow, scheme.background),
                        center = Offset(size.width / 2f, size.height * 0.2f),
                        radius = size.maxDimension * 0.6f,
                    ),
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                // adjustResize no longer resizes under edge-to-edge; keep the buttons above the keyboard.
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            BuildBanner()
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RingedShield()
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                content()
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.offline_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RingedShield() {
    val outer = MaterialTheme.colorScheme.outlineVariant // decorative rings
    val inner = MaterialTheme.colorScheme.primary
    Box(Modifier.size(152.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 1.dp.toPx()
            val r = size.minDimension / 2f - stroke
            drawCircle(outer, radius = r, style = Stroke(stroke))
            drawCircle(
                outer, radius = r * 0.8f,
                style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
            )
            drawCircle(inner.copy(alpha = 0.7f), radius = r * 0.6f, style = Stroke(stroke * 1.5f))
        }
        KeeppeerShield(size = 72.dp)
    }
}

@Composable
private fun OrDivider(label: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            label,
            Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Lock-screen password field with show/hide. Visibility resets whenever the vault state changes. */
@Composable
private fun LockPasswordField(label: String, value: String, change: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = change,
        label = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = com.gem.keeppeer.ui.edit.SensitiveKeyboard.copy(keyboardType = KeyboardType.Password),
        singleLine = true,
        shape = VaultShapes.Field,
        colors = vaultFieldColors(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(if (visible) R.string.hide_password else R.string.show_password),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun PasswordInput(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = com.gem.keeppeer.ui.edit.SensitiveKeyboard.copy(keyboardType = KeyboardType.Password),
        shape = VaultShapes.Field, colors = vaultFieldColors(),
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
        confirmButton = { VaultTextButton(onClick = {
            if (password.isEmpty()) error = BiometricResult(
                BiometricOutcome.EMPTY_PASSWORD, BiometricStage.PASSWORD_INPUT).message()
            else {
                val input = password.toCharArray()
                password = ""
                confirm(input)
            }
        }) { Text(stringResource(R.string.biometric_continue)) } },
        dismissButton = { VaultTextButton(onClick = { password = ""; dismiss() }) { Text(stringResource(R.string.cancel)) } },
    )
}
