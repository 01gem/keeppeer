package com.gem.keeppeer.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gem.keeppeer.BuildConfig
import com.gem.keeppeer.R
import com.gem.keeppeer.security.*
import com.gem.keeppeer.ui.components.*
import com.gem.keeppeer.ui.export.*
import com.gem.keeppeer.ui.lock.*
import com.gem.keeppeer.ui.theme.KeeppeerTheme

private enum class SettingsPage { MAIN, CHANGE_PASSWORD }

@OptIn(ExperimentalComposeUiApi::class)
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
    // The switch only ever shows the stored biometric state; it is re-read after every outcome.
    var enabled by remember { mutableStateOf(session.biometrics.enabled()) }
    var available by remember { mutableStateOf(session.biometrics.available()) }
    var confirmBiometric by remember { mutableStateOf(false) }
    // Display only: true while the typed master password is being checked before the fingerprint prompt.
    var verifying by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(SettingsPage.MAIN) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        enabled = session.biometrics.enabled()
        available = session.biometrics.available()
    }
    val leavePage = { current = ""; replacement = ""; confirmation = ""; message = null; page = SettingsPage.MAIN }
    BackHandler(enabled = page == SettingsPage.CHANGE_PASSWORD) { leavePage() }
    BackHandler(enabled = busy) {}
    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!busy) IconButton(onClick = { if (page == SettingsPage.MAIN) back() else leavePage() }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                } else Spacer(Modifier.size(48.dp))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            BuildBanner()
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(if (page == SettingsPage.MAIN) R.string.security_title else R.string.change_master),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                when (page) {
                    SettingsPage.MAIN -> MainPage(
                        session = session, timeout = timeout, theme = theme, appearance = appearance,
                        csv = csv, picker = picker, busy = busy, enabled = enabled, available = available,
                        confirming = confirmBiometric, verifying = verifying, message = message,
                        onToggleFingerprint = {
                            if (enabled) {
                                busy = true
                                session.disableBiometric { removed ->
                                    enabled = session.biometrics.enabled()
                                    message = resources.getString(if (removed) R.string.biometric_disabled else R.string.biometric_disable_failed)
                                    busy = false
                                }
                            } else confirmBiometric = true
                        },
                        onChangePassword = { message = null; page = SettingsPage.CHANGE_PASSWORD },
                    )
                    SettingsPage.CHANGE_PASSWORD -> {
                        WarningNote(stringResource(R.string.csv_password_nudge))
                        PasswordInput(stringResource(R.string.current_master), current, { current = it })
                        PasswordInput(stringResource(R.string.new_master), replacement, { replacement = it })
                        PasswordInput(stringResource(R.string.confirm_new), confirmation, { confirmation = it })
                        PrimaryButton(stringResource(R.string.change_password), enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                            val old = current.toCharArray(); val fresh = replacement.toCharArray(); val confirm = confirmation.toCharArray()
                            current = ""; replacement = ""; confirmation = ""; busy = true
                            session.changePassword(old, fresh, confirm) { message = it; busy = false }
                        })
                        message?.let { MessageText(it) }
                    }
                }
            }
        }
    }
    if (confirmBiometric) BiometricPasswordDialog(
        dismiss = { confirmBiometric = false },
        confirm = { password ->
            confirmBiometric = false
            busy = true
            verifying = true
            message = null
            session.prepareBiometricEnrollment(password) { cipher, result ->
                verifying = false
                if (cipher == null) {
                    enabled = session.biometrics.enabled()
                    message = resources.getString(result.message()); busy = false
                }
                else biometric(true, cipher) { outcome ->
                    enabled = session.biometrics.enabled()
                    message = resources.getString(if (outcome.success) R.string.biometric_enrolled else outcome.message())
                    busy = false
                }
            }
        },
    )
}

@Composable
private fun ColumnScope.MainPage(
    session: VaultSession, timeout: LockTimeout, theme: ThemeState, appearance: ThemeSettings,
    csv: CsvViewModel, picker: CsvPickerModel, busy: Boolean, enabled: Boolean, available: Boolean,
    confirming: Boolean, verifying: Boolean, message: String?, onToggleFingerprint: () -> Unit, onChangePassword: () -> Unit,
) {
    // 1. Status
    StatusCard(enabled, timeout, onLock = session::lock)

    // 2. Unlocking
    SectionLabel(stringResource(R.string.section_unlocking))
    VaultGroup {
        SwitchRow(
            icon = Icons.Outlined.Fingerprint,
            title = stringResource(R.string.biometric_heading),
            subtitle = stringResource(if (verifying) R.string.biometric_verifying else if (enabled) R.string.biometric_enabled
                else if (available) R.string.biometric_available else R.string.biometric_unavailable),
            progress = verifying,
            checked = enabled,
            enabled = !busy && !confirming && (enabled || available),
            actionLabel = stringResource(if (enabled) R.string.biometric_disable else R.string.biometric_enable),
            onToggle = onToggleFingerprint,
        )
        VaultDivider()
        VaultRow(
            title = stringResource(R.string.change_master),
            icon = Icons.Outlined.Key,
            onClick = onChangePassword,
            enabled = !busy,
            trailing = {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
            },
        )
    }
    if (enabled && !available) MessageText(stringResource(R.string.biometric_unavailable))
    message?.let { MessageText(it) }

    // 3. Auto-lock
    SectionLabel(stringResource(R.string.section_auto_lock))
    Text(stringResource(R.string.timeout_heading), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    SegmentedChoice(
        options = LockTimeout.entries,
        selected = timeout,
        onSelect = session::setTimeout,
        label = { stringResource(timeoutLabel(it)) },
    )
    Note(stringResource(R.string.timeout_note))

    // 4. Backup
    SectionLabel(stringResource(R.string.csv_heading))
    CsvActions(csv, picker, !busy)
    WarningNote(stringResource(R.string.about_export))

    // 5. Appearance
    SectionLabel(stringResource(R.string.theme))
    if (theme.loading) Note(stringResource(R.string.theme_loading))
    SegmentedChoice(
        options = ThemeMode.entries,
        selected = theme.mode,
        onSelect = appearance::select,
        enabled = !theme.loading && !theme.saving && !theme.readFailed,
        label = {
            stringResource(when (it) {
                ThemeMode.SYSTEM -> R.string.theme_system
                ThemeMode.LIGHT -> R.string.theme_light
                ThemeMode.DARK -> R.string.theme_dark
            })
        },
    )
    if (theme.saving) Note(stringResource(R.string.theme_saving))
    if (theme.readFailed) {
        Note(stringResource(R.string.theme_read_failed))
        VaultTextButton(onClick = appearance::retry) { Text(stringResource(R.string.retry)) }
    }
    if (theme.writeFailed) MessageText(stringResource(R.string.theme_write_failed), error = true)

    // 6. About
    SectionLabel(stringResource(R.string.about))
    AboutGroup()
    Note(stringResource(R.string.about_offline))
    Note(stringResource(when {
        BuildConfig.SHOW_DEVELOPMENT_BANNER -> R.string.about_development
        BuildConfig.VALIDATION_BUILD -> R.string.about_validation
        else -> R.string.about_release
    }))
}

private fun timeoutLabel(value: LockTimeout) = when (value) {
    LockTimeout.IMMEDIATE -> R.string.timeout_immediate
    LockTimeout.MINUTE -> R.string.timeout_minute
    LockTimeout.FIVE_MINUTES -> R.string.timeout_five
}

@Composable
private fun StatusCard(fingerprintOn: Boolean, timeout: LockTimeout, onLock: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth(),
        shape = VaultShapes.Card,
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant), // decorative card
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) { }) {
                KeeppeerShield(size = 52.dp, showCheck = true)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.status_protected), style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    val summary = stringResource(if (fingerprintOn) R.string.status_fingerprint_on else R.string.status_fingerprint_off) +
                        " " + stringResource(when (timeout) {
                            LockTimeout.IMMEDIATE -> R.string.status_locks_immediate
                            LockTimeout.MINUTE -> R.string.status_locks_minute
                            LockTimeout.FIVE_MINUTES -> R.string.status_locks_five
                        })
                    Text(summary, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
            }
            // Locking drops the unlocked subtree, which closes this dialog and shows the lock screen.
            SteelButton(stringResource(R.string.lock_now), onLock, Modifier.fillMaxWidth().testTag("settings-lock-now"),
                icon = Icons.Outlined.Lock)
        }
    }
}

/** Whole row toggles; the Switch is display-only so TalkBack hears one switch with its state. */
@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, enabled: Boolean,
    actionLabel: String, onToggle: () -> Unit, progress: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .clip(VaultShapes.Row)
            .clickable(enabled = enabled, onClickLabel = actionLabel, role = Role.Switch, onClick = onToggle)
            .semantics { toggleableState = ToggleableState(checked) }
            .heightIn(min = 56.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(VaultShapes.Tile).background(KeeppeerTheme.colors.surfaceHigh), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Spacer(Modifier.width(8.dp))
        if (progress) {
            // Same footprint as the switch, so the row does not jump.
            Box(Modifier.width(52.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp, color = scheme.primary)
            }
        } else Switch(
            checked = checked, onCheckedChange = null, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = scheme.primary, checkedThumbColor = scheme.onPrimary),
        )
    }
}

@Composable
private fun AboutGroup() {
    val context = LocalContext.current
    val version = remember {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    VaultGroup {
        @Suppress("DEPRECATION")
        VaultRow(
            title = stringResource(R.string.version_label),
            subtitle = stringResource(R.string.about_version, version.versionName.orEmpty(), version.versionCode),
            icon = Icons.Outlined.Info,
        )
        VaultDivider()
        ExpandRow(Icons.Outlined.WarningAmber, stringResource(R.string.about_limits_title)) {
            DetailText(stringResource(R.string.about_limits))
        }
        VaultDivider()
        // Full OFL texts ship as res/raw/onest_ofl.txt and res/raw/jetbrains_mono_ofl.txt.
        ExpandRow(Icons.Outlined.Description, stringResource(R.string.licenses_title)) {
            DetailText(stringResource(R.string.license_onest))
            DetailText(stringResource(R.string.license_jetbrains_mono))
            DetailText(stringResource(R.string.license_ofl))
        }
    }
}

@Composable
private fun ExpandRow(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .clip(VaultShapes.Row)
            .clickable(role = Role.Button) { expanded = !expanded }
            .semantics {
                if (expanded) collapse { expanded = false; true } else expand { expanded = true; true }
            }
            .heightIn(min = 56.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(VaultShapes.Tile).background(KeeppeerTheme.colors.surfaceHigh), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface, modifier = Modifier.weight(1f))
        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null,
            tint = scheme.onSurfaceVariant)
    }
    if (expanded) {
        Column(Modifier.padding(start = 68.dp, end = 16.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content)
    }
}

@Composable
private fun DetailText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp).semantics { heading() })
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun MessageText(text: String, error: Boolean = false) {
    Text(text, style = MaterialTheme.typography.bodyMedium,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite })
}
