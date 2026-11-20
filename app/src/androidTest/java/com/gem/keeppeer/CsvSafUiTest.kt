package com.gem.keeppeer

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.gem.keeppeer.csv.*
import com.gem.keeppeer.domain.PasswordEntry
import com.gem.keeppeer.security.*
import com.gem.keeppeer.ui.export.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Synthetic-only real DocumentsUI journey. Host must guard exact new AVD before instrumentation. */
class CsvSafUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val session get() = (compose.activity.application as KeeppeerApplication).container.session
    private fun unlock() {
        compose.waitUntil(30_000) { session.state.value.stage !in listOf(VaultStage.CHECKING, VaultStage.UNLOCKING) }
        compose.runOnUiThread {
            if (session.state.value.stage == VaultStage.FIRST_RUN)
                session.setup(MASTER.toCharArray(), MASTER.toCharArray())
            else if (session.state.value.access == null) session.unlock(MASTER.toCharArray())
        }
        compose.waitUntil(120_000) { session.state.value.access != null }
    }
    private fun click(text: String) {
        compose.onNodeWithText(text, useUnmergedTree = true).performClick()
        compose.waitForIdle() // Drive Compose effects before waiting on an external system window.
    }
    private fun settings() {
        compose.onNodeWithContentDescription(str(R.string.settings)).performClick()
        compose.waitForIdle()
    }
    private fun action(text: String) {
        compose.onNodeWithText(text, useUnmergedTree = true).performScrollTo().performClick()
        compose.waitForIdle()
    }
    private fun dialogRegistered() {
        compose.waitForIdle()
        compose.runOnUiThread {
            val field = compose.activity.privacyWindows.javaClass.getDeclaredField("windows").apply { isAccessible = true }
            val windows = field.get(compose.activity.privacyWindows) as Map<*, *>
            assertTrue("CSV dialog registered", windows.size >= 2)
            windows.keys.forEach {
                assertTrue("Every CSV owned window secure",
                    (it as android.view.Window).attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            }
        }
    }
    private fun systemNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        fun visit(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (predicate(node)) return node
            for (i in 0 until node.childCount) { node.getChild(i)?.let { visit(it)?.let { found -> return found } } }
            return null
        }
        return instrumentation.uiAutomation.rootInActiveWindow?.let(::visit)
    }
    private fun waitSystem(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val deadline = SystemClock.elapsedRealtime() + 30_000
        while (SystemClock.elapsedRealtime() < deadline) {
            systemNode(predicate)?.let { return it }
            SystemClock.sleep(100)
        }
        error("Expected system picker control unavailable")
    }
    private fun systemClick(predicate: (AccessibilityNodeInfo) -> Boolean) {
        var node: AccessibilityNodeInfo? = waitSystem(predicate)
        while (node != null && !node.isClickable) node = node.parent
        check(node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) { "System click failed" }
    }
    private fun pickerReady() {
        waitSystem { it.packageName?.toString()?.contains("documentsui") == true }
    }
    @Test fun realSafExportReimportSkipAndImmediateLockReturnPreserveExactValues() = runBlocking {
        unlock()
        val synthetic = PasswordEntry(title = "CSV 雲, \"title\"", username = "  user  ",
            password = "  =synthetic,\"value\"  ", notes = "first\n第二行 🙂\r\nthird", category = "Synthetic")
        val originalAccess = requireNotNull(session.state.value.access)
        val id = originalAccess.repository.create(synthetic)
        val stored = requireNotNull(originalAccess.repository.getById(id))
        settings()
        action(str(R.string.csv_export))
        compose.onNodeWithText(str(R.string.csv_plaintext)).assertIsDisplayed()
        dialogRegistered()
        click(str(R.string.csv_continue))
        pickerReady()
        // Default immediate lock must revoke the original repository while DocumentsUI is visible.
        compose.waitUntil(30_000) { session.state.value.access == null }
        val filename = "keeppeer-csv-synthetic-" + SystemClock.elapsedRealtime() + ".csv"
        val field = waitSystem { it.isEditable && it.packageName?.toString()?.contains("documentsui") == true }
        if (!field.isFocused) field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val focusedField = waitSystem { it.isEditable && it.isFocused &&
            it.packageName?.toString()?.contains("documentsui") == true }
        check(focusedField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,
            android.os.Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, filename) }))
        systemClick { it.text?.toString()?.equals("Save", ignoreCase = true) == true && it.isEnabled }
        compose.waitUntil(30_000) { session.state.value.stage == VaultStage.LOCKED }
        compose.onNodeWithText(str(R.string.locked_title)).assertIsDisplayed()
        val picker = syntheticOnMain { ViewModelProvider(compose.activity)[CsvPickerModel::class.java] }
        compose.waitUntil(30_000) { picker.state.value.selection != null }
        val exportUri = requireNotNull(picker.state.value.selection).uri
        unlock()
        // A renewed plaintext approval is mandatory in the new session; no automatic writing.
        compose.onNodeWithText(str(R.string.csv_plaintext)).assertIsDisplayed()
        dialogRegistered()
        click(str(R.string.csv_continue))
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText(str(R.string.csv_exported)).fetchSemanticsNodes().isNotEmpty()
        }
        dialogRegistered()
        val decoded = compose.activity.contentResolver.openInputStream(exportUri)!!.use { VaultCsv.parse(it) }
        assertTrue("Synthetic roundtrip exact", decoded.single() == stored.copy(id = 0))
        click(str(R.string.csv_continue))
        settings()
        action(str(R.string.csv_import))
        pickerReady()
        systemClick { it.text?.toString() == filename }
        compose.waitUntil(30_000) { picker.state.value.selection != null }
        unlock()
        click(str(R.string.csv_continue))
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText(str(R.string.csv_counts, 1, 0, 1)).fetchSemanticsNodes().isNotEmpty()
        }
        dialogRegistered()
        val beforeRotation = requireNotNull(session.state.value.access).id
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        assertTrue("Rotation keeps current session", session.state.value.access?.id == beforeRotation)
        compose.onNodeWithText(str(R.string.csv_counts, 1, 0, 1)).assertIsDisplayed()
        dialogRegistered()
        compose.onNodeWithText(str(R.string.csv_skip)).assertIsDisplayed()
        click(str(R.string.csv_continue))
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText(compose.activity.getString(R.string.csv_applied,
                compose.activity.resources.getQuantityString(R.plurals.csv_inserted, 0, 0),
                compose.activity.resources.getQuantityString(R.plurals.csv_updated, 0, 0),
                compose.activity.resources.getQuantityString(R.plurals.csv_skipped, 1, 1))).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue("Existing values and count unchanged", requireNotNull(session.state.value.access).repository.exportCsv() == listOf(stored))
        click(str(R.string.csv_continue))
        settings()
        action(str(R.string.csv_import))
        pickerReady()
        check(instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK))
        compose.waitUntil(30_000) { picker.state.value.awaiting == null }
        unlock()
        assertNull(picker.state.value.selection)
        assertTrue(requireNotNull(session.state.value.access).repository.exportCsv().size == 1)
        // This document was created by this test and contains synthetic values only.
        withContext(Dispatchers.IO) { SafCsvDocuments(compose.activity.contentResolver).deleteCreated(exportUri) }
        Unit
    }

    @Test fun lockDuringCsvWorkRevokesInFlightStreamAndStaleCallbackNeedsNewApproval() = runBlocking {
        unlock()
        val access = requireNotNull(session.state.value.access)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        var closed = false
        var deletes = 0
        val output = object : java.io.OutputStream() {
            override fun write(b: Int) {}
            override fun write(b: ByteArray, off: Int, len: Int) {
                started.countDown(); check(release.await(20, TimeUnit.SECONDS))
            }
            override fun close() { closed = true }
        }
        val documents = object : CsvDocuments {
            override fun read(uri: android.net.Uri): java.io.InputStream? = null
            override fun write(uri: android.net.Uri) = output
            override fun deleteCreated(uri: android.net.Uri): Boolean { deletes++; return true }
        }
        val transfer = CsvTransfer(documents)
        val work = async {
            try { transfer.export(access, android.net.Uri.parse("content://synthetic/document/new")); false }
            catch (_: CancellationException) { true }
        }
        withContext(Dispatchers.IO) { check(started.await(20, TimeUnit.SECONDS)) }
        compose.runOnUiThread { session.lock() }
        release.countDown()
        assertTrue(work.await())
        assertTrue(closed && deletes == 1)
        val picker = syntheticOnMain { ViewModelProvider(compose.activity)[CsvPickerModel::class.java] }
        compose.runOnUiThread {
            picker.request(CsvAction.IMPORT); picker.launched()
            picker.result(android.net.Uri.parse("content://synthetic/document/stale"))
        }
        compose.onNodeWithText(str(R.string.csv_read_confirm)).assertDoesNotExist()
        unlock()
        compose.onNodeWithText(str(R.string.csv_read_confirm)).assertIsDisplayed()
        dialogRegistered()
        click(str(R.string.csv_cancel))
        assertNull(picker.state.value.selection)
    }
    companion object { private const val MASTER = "synthetic test master" }
}

private fun str(id: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)
