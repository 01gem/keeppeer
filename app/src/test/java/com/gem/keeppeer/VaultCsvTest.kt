package com.gem.keeppeer

import com.gem.keeppeer.csv.*
import com.gem.keeppeer.domain.PasswordEntry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class VaultCsvTest {
    private val header = VaultCsv.header.joinToString(",")
    private fun csv(row: String, newline: String = "\r\n") = (header + newline + row + newline).byteInputStream()
    private suspend fun rejected(input: java.io.InputStream, category: String, limits: CsvLimits = CsvLimits()) {
        try { VaultCsv.parse(input, limits); fail("Expected rejection") }
        catch (e: CsvFailure) {
            assertTrue("Safe category", e.category == category)
            assertTrue("Safe location", e.record >= 1 && e.line >= 1)
            assertFalse("No field disclosure", e.message.orEmpty().contains("SYNTHETIC_PRIVATE"))
        }
    }
    @Test fun roundTripExactPasswordsMultilineUnicodeQuotesCommasAndFormulaValues() = runBlocking {
        val entries = listOf(PasswordEntry(title = "  雲, \"quoted\"  ", username = " User ",
            password = "  =SYNTHETIC_PRIVATE,\"\r\n🙂  ", url = "=1+1", notes = "α\nβ\r\nγ\rδ",
            category = "@formula", isFavorite = true, createdAt = 0, updatedAt = Long.MAX_VALUE),
            PasswordEntry(title = "duplicate", username = "", password = "", url = "", notes = "", category = "",
                createdAt = 1, updatedAt = 2))
        val output = ByteArrayOutputStream()
        VaultCsv.write(output, entries)
        val bytes = output.toByteArray()
        val restored = VaultCsv.parse(ByteArrayInputStream(bytes))
        assertTrue("Exact required values", restored[0] == entries[0])
        assertTrue("Empty optional normalization", restored[1] == entries[1].copy(username = null, url = null, notes = null, category = null))
        assertTrue("Literal header", bytes.toString(Charsets.UTF_8).startsWith(header + "\r\n"))
        assertTrue("Record separators", bytes.toString(Charsets.UTF_8).endsWith("\r\n"))
    }
    @Test fun bomLfAndTrailingEmptyOptionalCellsAreAccepted() = runBlocking {
        val result = VaultCsv.parse(("\uFEFF" + header + "\nT,, ,,,," + "false,0,0\n").byteInputStream())
        assertTrue(result.single().password == " ")
        assertNull(result.single().notes)
        assertNull(result.single().category)
    }
    @Test fun emptyTrailingFieldsAreParsedThenTypeValidated() = runBlocking {
        rejected(csv("T,,,,,,false,0,"), "timestamp")
        rejected(csv("T,,,,,,false,0,0,"), "column count")
    }
    @Test fun emptyPasswordsDuplicateRowsAndHeaderOnlyRemainValid() = runBlocking {
        val row = "T,,,,,,false,0,0"
        assertTrue(VaultCsv.parse(csv(row + "\r\n" + row)).size == 2)
        assertTrue(VaultCsv.parse(header.byteInputStream()).isEmpty())
    }
    @Test fun strictHeadersAndCounts() = runBlocking {
        rejected((header.uppercase() + "\n").byteInputStream(), "header")
        rejected((header + ",id\n").byteInputStream(), "column count")
        rejected(csv("T,p"), "column count")
        rejected(csv("T,,,,,,false,0,0,extra"), "column count")
        rejected(ByteArrayInputStream(byteArrayOf()), "missing header")
        rejected(csv("T,,,,,,false,0,0\n"), "column count")
    }
    @Test fun malformedQuotesAndBareCrAreRejected() = runBlocking {
        rejected(csv("T,a\"b,p,,,,false,0,0"), "quote")
        rejected(csv("\"T\"x,,p,,,,false,0,0"), "quote")
        rejected(csv("\"SYNTHETIC_PRIVATE,,p,,,,false,0,0"), "unterminated quote")
        rejected((header + "\rT,,,,,,false,0,0").byteInputStream(), "record separator")
    }
    @Test fun strictUtf8RejectsReplacementOfCredentialCharacters() = runBlocking {
        val bytes = (header + "\nT,,").toByteArray() + byteArrayOf(0xc3.toByte(), 0x28)
        rejected(ByteArrayInputStream(bytes), "UTF-8")
    }
    @Test fun requiredBooleanAndTimestampValidation() = runBlocking {
        rejected(csv(" ,,,,,,false,0,0"), "required title")
        for (flag in listOf("TRUE", "1", "", " false")) rejected(csv("T,,,,,,$flag,0,0"), "boolean")
        for (time in listOf("-1", "+1", " 1", "1.0", "9223372036854775808", "")) {
            rejected(csv("T,,,,,,false,$time,9"), "timestamp")
        }
        rejected(csv("T,,,,,,false,2,1"), "timestamp order")
    }
    @Test fun byteFieldRowAndAggregateLimitsAreEnforcedWithoutMetadata() = runBlocking {
        rejected(csv("T,,,,,,false,0,0"), "byte limit", CsvLimits(bytes = 10))
        rejected(csv("T,,,,,,false,0,0"), "character limit", CsvLimits(fieldChars = 3))
        rejected(csv("T,,,,,,false,0,0"), "character limit", CsvLimits(totalChars = 12))
        rejected(csv("T,,,,,,false,0,0"), "row limit", CsvLimits(rows = 0))
        rejected(csv("\"abcdefghijklmnop", "\n"), "character limit", CsvLimits(fieldChars = 12))
    }
    @Test fun parserCooperatesWithInFlightCancellation() = runBlocking {
        val job = Job()
        val input = object : ByteArrayInputStream((header + "\nT,,,,,,false,0,0").toByteArray()) {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val n = super.read(b, off, minOf(len, 4))
                job.cancel()
                return n
            }
        }
        var cancelled = false
        try { withContext(job) { VaultCsv.parse(input) } } catch (_: CancellationException) { cancelled = true }
        assertTrue(cancelled)
    }
    @Test fun writerLimitsAndMalformedUtf16FailRatherThanReplace() = runBlocking {
        var failed = false
        try { VaultCsv.write(ByteArrayOutputStream(), listOf(PasswordEntry(title = "T", password = "\uD800"))) }
        catch (_: java.nio.charset.CharacterCodingException) { failed = true }
        assertTrue(failed)
        failed = false
        try { VaultCsv.write(ByteArrayOutputStream(), emptyList(), CsvLimits(bytes = 2)) }
        catch (_: CsvFailure) { failed = true }
        assertTrue(failed)
        failed = false
        try { VaultCsv.write(ByteArrayOutputStream(), listOf(PasswordEntry(title = "T", password = "synthetic".repeat(100))),
            CsvLimits(bytes = header.length.toLong() + 12)) }
        catch (e: CsvFailure) { failed = e.category == "byte limit" }
        assertTrue(failed)
    }
}
