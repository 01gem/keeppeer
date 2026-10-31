package com.gem.keeppeer.ui.export

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gem.keeppeer.csv.*
import com.gem.keeppeer.domain.PasswordEntry
import com.gem.keeppeer.security.VaultAccess
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CsvAction { IMPORT, EXPORT }
data class CsvSelection(val action: CsvAction, val uri: Uri)
data class CsvPickerState(
    val launching: CsvAction? = null, val awaiting: CsvAction? = null,
    val selection: CsvSelection? = null, val notice: Boolean = false,
)

/** Activity-owned nonsecret state only. Intentionally not saved across process death. */
class CsvPickerModel : ViewModel() {
    private val mutable = MutableStateFlow(CsvPickerState())
    val state = mutable.asStateFlow()
    fun request(action: CsvAction) {
        if (mutable.value.launching != null || mutable.value.awaiting != null || mutable.value.selection != null) return
        mutable.value = CsvPickerState(launching = action)
    }
    fun launched() {
        mutable.value = mutable.value.copy(awaiting = mutable.value.launching, launching = null)
    }
    fun result(uri: Uri?) {
        val action = mutable.value.awaiting
        mutable.value = if (uri != null && uri.scheme == "content" && action != null)
            CsvPickerState(selection = CsvSelection(action, uri))
        else CsvPickerState(notice = uri != null)
    }
    fun failed() { mutable.value = CsvPickerState(notice = true) }
    fun take(): CsvSelection? {
        val selected = mutable.value.selection
        mutable.value = CsvPickerState()
        return selected
    }
    fun notice() { mutable.value = mutable.value.copy(notice = true) }
    fun dismissNotice() { mutable.value = mutable.value.copy(notice = false) }
}

data class CsvUiState(
    val busy: Boolean = false, val warning: Boolean = false,
    val preview: ImportPreview? = null, val policy: DuplicatePolicy = DuplicatePolicy.SKIP,
    val message: String? = null, val counts: ImportCounts? = null,
)

/** Parsed rows live only in this unlock's ViewModelStore and are dropped at revoke. */
class CsvViewModel(private val access: VaultAccess, private val transfer: CsvTransfer) : ViewModel() {
    private val mutable = MutableStateFlow(CsvUiState())
    val state = mutable.asStateFlow()
    private var rows: List<PasswordEntry>? = null
    private var operation: Job? = null
    fun warning() { if (!mutable.value.busy) mutable.value = mutable.value.copy(warning = true) }
    fun dismiss() {
        if (mutable.value.busy) return
        rows = null
        mutable.value = CsvUiState()
    }
    fun policy(value: DuplicatePolicy) {
        if (!mutable.value.busy) mutable.value = mutable.value.copy(policy = value)
    }
    fun selected(selection: CsvSelection, exportInterrupted: () -> Unit) {
        if (mutable.value.busy) return
        mutable.value = CsvUiState(busy = true)
        operation = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                if (selection.action == CsvAction.EXPORT) {
                    transfer.export(access, selection.uri)
                    ensureActive()
                    mutable.value = CsvUiState(message = "exported")
                } else {
                    val parsed = transfer.read(access, selection.uri)
                    val preview = access.repository.previewCsv(parsed)
                    ensureActive()
                    rows = parsed
                    mutable.value = CsvUiState(preview = preview)
                }
            } catch (cancelled: CancellationException) {
                if (selection.action == CsvAction.EXPORT) exportInterrupted()
                throw cancelled
            } catch (failure: Exception) {
                ensureActive()
                rows = null
                mutable.value = CsvUiState(message = if (selection.action == CsvAction.EXPORT) "partial"
                    else if (failure is CsvFailure) failure.message else "failed")
            }
        }
    }
    fun submit() {
        val state = mutable.value
        val input = rows ?: return
        val preview = state.preview ?: return
        if (state.busy) return
        mutable.value = state.copy(busy = true)
        operation = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                when (val result = access.repository.importCsv(input, preview, state.policy)) {
                    is ImportResult.Changed -> {
                        ensureActive()
                        mutable.value = CsvUiState(preview = result.preview, message = "changed")
                    }
                    is ImportResult.Applied -> {
                        rows = null
                        ensureActive()
                        val c = result.counts
                        mutable.value = CsvUiState(message = "applied", counts = c)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                ensureActive()
                rows = null
                mutable.value = CsvUiState(message = "failed")
            }
        }
    }
    fun cancel() {
        val previous = operation ?: return
        previous.cancel()
        rows = null
        viewModelScope.launch {
            previous.join()
            if (operation === previous) mutable.value = CsvUiState(message = "interrupted")
        }
    }
    override fun onCleared() {
        rows = null
        mutable.value = CsvUiState()
        operation?.cancel()
    }
}
