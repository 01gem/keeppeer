package com.gem.keeppeer.csv

import com.gem.keeppeer.domain.PasswordEntry
import java.io.*
import java.nio.charset.CodingErrorAction
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Errors contain locations/categories only, never field values or provider messages. */
class CsvFailure(val category: String, val record: Int, val line: Int) :
    IOException("CSV $category (record $record, line $line)")

data class CsvLimits(
    val bytes: Long = 8L * 1024 * 1024,
    val rows: Int = 10_000,
    val fieldChars: Int = 262_144,
    val totalChars: Int = 4 * 1024 * 1024,
)

object VaultCsv {
    val header = listOf("title", "username", "password", "url", "notes", "category",
        "favorite", "created_at", "updated_at")
    val limits = CsvLimits()

    /** Caller owns the stream. Decoder rejects malformed UTF-8 instead of replacing characters. */
    suspend fun parse(input: InputStream, limits: CsvLimits = this.limits): List<PasswordEntry> {
        val context = currentCoroutineContext()
        var record = 1
        var line = 1
        fun fail(category: String): Nothing = throw CsvFailure(category, record, line)
        val bounded = object : FilterInputStream(input) {
            var count = 0L
            override fun read(): Int {
                context.ensureActive()
                val value = super.read()
                if (value >= 0 && ++count > limits.bytes) fail("byte limit")
                return value
            }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                context.ensureActive()
                val n = `in`.read(b, off, minOf(len, 8192))
                if (n > 0) { count += n; if (count > limits.bytes) fail("byte limit") }
                return n
            }
        }
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
        val reader = PushbackReader(InputStreamReader(bounded, decoder), 1)
        val entries = ArrayList<PasswordEntry>()
        val field = StringBuilder()
        val cells = ArrayList<String>(9)
        var total = 0
        var quoted = false
        var afterQuote = false
        var started = false
        var first = true
        var previousCr = false
        fun append(c: Char) {
            if (field.length >= limits.fieldChars || total >= limits.totalChars) fail("character limit")
            field.append(c); total++
        }
        fun cell() {
            if (cells.size >= 9) fail("column count")
            cells.add(field.toString())
            field.setLength(0)
            afterQuote = false
        }
        fun row() {
            cell()
            if (cells.size != 9) fail("column count")
            if (record == 1) {
                if (cells != header) fail("header")
            } else {
                if (entries.size >= limits.rows) fail("row limit")
                if (cells[0].isBlank()) fail("required title")
                val favorite = when(cells[6]) { "true" -> true; "false" -> false; else -> fail("boolean") }
                fun timestamp(value: String): Long {
                    if (value.isEmpty() || value.any { it !in '0'..'9' }) fail("timestamp")
                    return value.toLongOrNull() ?: fail("timestamp")
                }
                val created = timestamp(cells[7])
                val updated = timestamp(cells[8])
                if (updated < created) fail("timestamp order")
                fun optional(i: Int) = cells[i].ifEmpty { null }
                entries.add(PasswordEntry(title = cells[0], username = optional(1), password = cells[2],
                    url = optional(3), notes = optional(4), category = optional(5),
                    isFavorite = favorite, createdAt = created, updatedAt = updated))
            }
            cells.clear(); record++; started = false; afterQuote = false
        }
        try {
            while (true) {
                context.ensureActive()
                val value = reader.read()
                if (value < 0) break
                val c = value.toChar()
                if (first) { first = false; if (c == '\uFEFF') continue }
                if (c == '\r') line++ else if (c == '\n' && !previousCr) line++
                previousCr = c == '\r'
                if (quoted) {
                    if (c == '"') { quoted = false; afterQuote = true } else append(c)
                    continue
                }
                if (afterQuote && c == '"') { append(c); quoted = true; afterQuote = false; continue }
                when (c) {
                    '"' -> {
                        if (afterQuote || field.isNotEmpty() || started) fail("quote")
                        quoted = true; started = true
                    }
                    ',' -> { cell(); started = false }
                    '\r', '\n' -> {
                        if (c == '\r') {
                            val next = reader.read()
                            if (next != '\n'.code) fail("record separator")
                            previousCr = false
                        }
                        row()
                    }
                    else -> {
                        if (afterQuote) fail("quote")
                        append(c); started = true
                    }
                }
            }
            if (quoted) fail("unterminated quote")
            if (started || afterQuote || field.isNotEmpty() || cells.isNotEmpty()) row()
            if (record == 1) fail("missing header")
            return entries
        } catch (bad: java.nio.charset.CharacterCodingException) {
            fail("UTF-8")
        } finally {
            field.setLength(0); cells.clear()
            // Strings cannot be reliably erased on the JVM. Reader storage is short lived.
        }
    }

    /** CRLF records, exact embedded newlines, deterministic repository order; no formula rewriting. */
    suspend fun write(output: OutputStream, entries: List<PasswordEntry>, limits: CsvLimits = this.limits) {
        val context = currentCoroutineContext()
        if (entries.size > limits.rows) throw CsvFailure("row limit", 1, 1)
        var bytes = 0L
        var chars = 0L
        // Deliberately no buffered plaintext file or full serialized CSV copy.
        suspend fun record(fields: List<String>, number: Int) {
            context.ensureActive()
            val line = StringBuilder()
            fields.forEachIndexed { index, value ->
                if (value.length > limits.fieldChars) throw CsvFailure("character limit", number, number)
                chars += value.length
                if (chars > limits.totalChars) throw CsvFailure("character limit", number, number)
                if (index > 0) line.append(',')
                line.append('"')
                value.forEach { c -> if (c == '"') line.append('"'); line.append(c) }
                line.append('"')
            }
            line.append("\r\n")
            // Strict encoder also rejects malformed UTF-16 rather than damaging a credential.
            val encoded = Charsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(java.nio.CharBuffer.wrap(line))
            line.setLength(0)
            val buffer = ByteArray(minOf(encoded.remaining(), 8192))
            try {
                bytes += encoded.remaining()
                if (bytes > limits.bytes) throw CsvFailure("byte limit", number, number)
                while (encoded.hasRemaining()) {
                    context.ensureActive()
                    val n = minOf(buffer.size, encoded.remaining())
                    encoded.get(buffer, 0, n)
                    output.write(buffer, 0, n)
                }
            } finally {
                buffer.fill(0)
                if (encoded.hasArray()) encoded.array().fill(0)
            }
        }
        val headerBytes = (header.joinToString(",") + "\r\n").toByteArray(Charsets.UTF_8)
        bytes += headerBytes.size
        chars += header.sumOf { it.length }
        if (bytes > limits.bytes || chars > limits.totalChars || header.any { it.length > limits.fieldChars })
            throw CsvFailure("header limit", 1, 1)
        context.ensureActive()
        output.write(headerBytes)
        entries.forEach { e ->
            context.ensureActive()
            if (e.createdAt < 0 || e.updatedAt < e.createdAt) throw CsvFailure("timestamp", 1, 1)
        }
        entries.forEachIndexed { i, e ->
            record(listOf(e.title, e.username.orEmpty(), e.password, e.url.orEmpty(), e.notes.orEmpty(),
                e.category.orEmpty(), e.isFavorite.toString(), e.createdAt.toString(), e.updatedAt.toString()), i + 2)
        }
    }
}
