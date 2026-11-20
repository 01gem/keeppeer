package com.gem.keeppeer.ui.export

import com.gem.keeppeer.ui.components.VaultTextButton
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Modifier
import com.gem.keeppeer.ui.components.OptionRadio
import com.gem.keeppeer.ui.components.VaultDivider
import com.gem.keeppeer.ui.components.VaultGroup
import com.gem.keeppeer.ui.components.VaultRow
import com.gem.keeppeer.ui.components.PrimaryButton
import com.gem.keeppeer.ui.components.WarningNote
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gem.keeppeer.R
import com.gem.keeppeer.csv.*
import com.gem.keeppeer.ui.privacy.ProtectDialogWindow
import kotlinx.coroutines.launch

/** Must be composed outside the unlocked-only subtree. Grants are not persisted. */
@Composable
fun CsvPickers(picker: CsvPickerModel) {
    val state by picker.state.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        picker.result(if (result.resultCode == Activity.RESULT_OK) result.data?.data else null)
    }
    LaunchedEffect(state.launching) {
        val action = state.launching ?: return@LaunchedEffect
        picker.launched()
        try {
            val intent = if (action == CsvAction.EXPORT) Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(Intent.EXTRA_TITLE, "keeppeer-backup.csv")
            } else Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values",
                    "application/csv", "text/plain", "application/octet-stream"))
            }
            launcher.launch(intent)
        } catch (_: Exception) { picker.failed() }
    }
}

@Composable
fun CsvActions(vm: CsvViewModel, picker: CsvPickerModel, enabled: Boolean) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pending by picker.state.collectAsStateWithLifecycle()
    val ready = enabled && !state.busy && state.preview == null && pending == CsvPickerState()
    VaultGroup {
        VaultRow(
            title = stringResource(R.string.csv_export),
            icon = Icons.Outlined.FileUpload,
            onClick = vm::warning,
            enabled = ready,
        )
        VaultDivider()
        VaultRow(
            title = stringResource(R.string.csv_import),
            icon = Icons.Outlined.FileDownload,
            onClick = { picker.request(CsvAction.IMPORT) },
            enabled = ready,
        )
    }
    Text(stringResource(R.string.csv_provider), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun CsvDialogs(vm: CsvViewModel, picker: CsvPickerModel, transfer: CsvTransfer) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pending by picker.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    if (state.warning) CsvDialog(R.string.csv_export, {
        WarningNote(stringResource(R.string.csv_plaintext))
        BodyText(stringResource(R.string.csv_provider))
        BodyText(stringResource(R.string.csv_formulas))
    }, dismiss = vm::dismiss, confirm = {
        vm.dismiss(); picker.request(CsvAction.EXPORT)
    })
    pending.selection?.let { selected ->
        CsvDialog(if (selected.action == CsvAction.EXPORT) R.string.csv_export else R.string.csv_import, {
            if (selected.action == CsvAction.EXPORT) WarningNote(stringResource(R.string.csv_plaintext))
            else BodyText(stringResource(R.string.csv_read_confirm))
            BodyText(stringResource(R.string.csv_provider))
            BodyText(stringResource(R.string.csv_formulas))
        }, dismiss = {
            val discarded = picker.take()
            if (discarded?.action == CsvAction.EXPORT) {
                picker.notice() // Until deletion completes, retain an honest nonsecret partial-file warning.
                scope.launch { transfer.discardCreated(discarded.uri) }
            }
        }, confirm = {
            picker.take()?.let { vm.selected(it, picker::notice) }
        })
    }
    state.preview?.let { preview ->
        CsvDialog(R.string.csv_import, {
            Text(stringResource(R.string.csv_counts, preview.total, preview.new, preview.conflicting),
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            BodyText(stringResource(R.string.csv_duplicates))
            DuplicatePolicy.entries.forEach { policy ->
                OptionRadio(stringResource(when(policy) {
                    DuplicatePolicy.SKIP -> R.string.csv_skip
                    DuplicatePolicy.UPDATE -> R.string.csv_update
                    DuplicatePolicy.KEEP_BOTH -> R.string.csv_keep_both
                }), state.policy == policy, !state.busy) { vm.policy(policy) }
            }
            if (state.message == "changed") WarningNote(stringResource(R.string.csv_changed))
        }, enabled = !state.busy, dismiss = vm::dismiss, confirm = vm::submit)
    }
    if (state.busy && state.preview == null) AlertDialog(
        onDismissRequest = {},
        title = { ProtectDialogWindow(); Text(stringResource(R.string.csv_working)) },
        text = { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) },
        confirmButton = { VaultTextButton(onClick = vm::cancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.csv_cancel)) } },
    )
    if (state.message != null && state.preview == null && !state.busy) {
        CsvDialog(R.string.csv_heading, {
            val result = when {
                state.message == "exported" -> stringResource(R.string.csv_exported)
                state.message == "partial" -> stringResource(R.string.csv_partial)
                state.message == "interrupted" -> stringResource(R.string.csv_interrupted)
                state.counts != null -> state.counts!!.let {
                    stringResource(R.string.csv_applied,
                        pluralStringResource(R.plurals.csv_inserted, it.inserted, it.inserted),
                        pluralStringResource(R.plurals.csv_updated, it.updated, it.updated),
                        pluralStringResource(R.plurals.csv_skipped, it.skipped, it.skipped))
                }
                state.problem != null -> state.problem!!.let { problem ->
                    val reason = when (problem.category) {
                        "header", "missing header" -> R.string.csv_reason_HEADER
                        "column count" -> R.string.csv_reason_COLUMNS
                        "byte limit", "character limit", "row limit", "header limit" -> R.string.csv_reason_LIMIT
                        "UTF-8" -> R.string.csv_reason_UTF8
                        "required title", "boolean", "timestamp", "timestamp order" -> R.string.csv_reason_VALUE
                        else -> R.string.csv_reason_SYNTAX
                    }
                    stringResource(R.string.csv_rejected_location, stringResource(reason), problem.record, problem.line)
                }
                else -> stringResource(R.string.csv_failed)
            }
            // Plaintext-at-rest outcomes are warnings; the rest are plain results.
            if (state.message == "partial" || state.message == "interrupted") WarningNote(result) else BodyText(result)
        }, dismiss = vm::dismiss, confirm = vm::dismiss)
    }
    if (pending.notice) CsvDialog(R.string.csv_heading, {
        WarningNote(stringResource(R.string.csv_partial))
        BodyText(stringResource(R.string.csv_expired))
    }, dismiss = picker::dismissNotice, confirm = picker::dismissNotice)
}

@Composable
private fun CsvDialog(
    title: Int, body: @Composable () -> Unit, enabled: Boolean = true,
    dismiss: () -> Unit, confirm: () -> Unit,
) {
    AlertDialog(onDismissRequest = { if (enabled) dismiss() },
        title = { ProtectDialogWindow(); Text(stringResource(title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(10.dp)) { body() }
        },
        confirmButton = { PrimaryButton(stringResource(R.string.csv_continue), confirm, enabled = enabled) },
        dismissButton = {
            VaultTextButton(enabled = enabled, onClick = dismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.csv_cancel))
            }
        })
}

@Composable
private fun BodyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
