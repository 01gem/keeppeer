package com.gem.keeppeer.security

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import com.gem.keeppeer.data.local.EncryptedDatabaseProvider
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore

enum class InitializationPhase { STARTED, COMPLETE }

/** Inspection is read-only. Failures propagate: an unreadable path/key store is never fresh use. */
data class InitializationEvidence(
    val passwordEnvelope: Boolean,
    val encryptedEvidence: Boolean,
    val legacy: Boolean,
) {
    val fresh: Boolean get() = !passwordEnvelope && !encryptedEvidence && !legacy
}

interface VaultInitialization {
    fun inspect(): InitializationEvidence
    fun start()
    fun complete()
}

/** One record per resolved no-backup context, bound to its exact alias namespace. No key material. */
class FileVaultInitialization(
    private val context: Context,
    private val namespace: String,
    private val aliasExists: (String) -> Boolean = { alias ->
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias(alias)
    },
    private val beforeCommit: (InitializationPhase) -> Unit = {},
) : VaultInitialization {
    init { require(namespace.length in 1..200 && namespace.matches(Regex("[A-Za-z0-9_.-]+"))) }

    private val marker get() = File(context.noBackupFilesDir, MARKER_NAME)

    private fun regularFile(file: File): Boolean {
        // Check ancestors without following unexpected links. Only the requested context is inspected.
        val root = File(context.applicationInfo.dataDir).absoluteFile
        check(file.absolutePath.startsWith(root.path + File.separator))
        var parent = file.parentFile
        while (parent != null && parent.path.startsWith(root.path)) {
            try {
                check(OsConstants.S_ISDIR(Os.lstat(parent.path).st_mode))
                check(Os.access(parent.path, OsConstants.R_OK or OsConstants.X_OK))
            } catch (error: ErrnoException) {
                if (error.errno != OsConstants.ENOENT) throw error
            }
            parent = parent.parentFile
        }
        return try {
            check(OsConstants.S_ISREG(Os.lstat(file.path).st_mode)) { "Unexpected vault file type" }
            check(Os.access(file.path, OsConstants.R_OK)) { "Unreadable vault evidence" }
            true
        } catch (error: ErrnoException) {
            if (error.errno == OsConstants.ENOENT) false else throw error
        }
    }

    override fun inspect(): InitializationEvidence {
        val directory = context.noBackupFilesDir
        fun envelope(name: String) = listOf("", ".bak", ".new").map { regularFile(File(directory, name + it)) }
        val password = envelope("vault-envelope.bin")
        val biometric = envelope("biometric-envelope.bin")
        val markers = envelope(MARKER_NAME)
        // Do not call AtomicFile.openRead here: its rollback mutates files during inspection.
        if (markers.any { it }) {
            val source = if (markers[0]) marker else if (markers[1]) File(marker.path + ".bak") else null
            if (source != null) {
                val bytes = FileInputStream(source).use { input ->
                    val buffer = ByteArray(512)
                    val count = input.read(buffer)
                    check(count > 0 && input.read() == -1) { "Invalid initialization evidence" }
                    buffer.copyOf(count)
                }
                val text = bytes.toString(Charsets.UTF_8)
                check(InitializationPhase.entries.any { text == record(it) }) { "Invalid initialization evidence" }
            }
            // A lone .new is also evidence, even when a crash left no readable committed record.
        }
        fun database(name: String) = listOf("", "-wal", "-shm", "-journal").map {
            regularFile(File(context.getDatabasePath(name).path + it))
        }
        val active = database(EncryptedDatabaseProvider.VAULT_NAME)
        val legacy = database(EncryptedDatabaseProvider.LEGACY_NAME)
        // Inspect both exact aliases even if filesystem evidence exists. Exceptions remain errors.
        val storageKey = aliasExists("$namespace.storage.v1")
        val biometricKey = aliasExists("$namespace.biometric.v1")
        return InitializationEvidence(
            passwordEnvelope = password[0] || password[1],
            encryptedEvidence = markers.any { it } || password.any { it } || biometric.any { it } ||
                active.any { it } || storageKey || biometricKey,
            legacy = legacy.any { it },
        )
    }

    private fun record(phase: InitializationPhase) = "Keeppeer initialization v1\n$namespace\n${phase.name}\n"
    private fun write(phase: InitializationPhase) {
        AtomicEnvelopeFile(marker).write(record(phase).toByteArray(Charsets.UTF_8)) { beforeCommit(phase) }
        // Persist the directory entry too. A failure never permits provisioning/session publication.
        val descriptor = Os.open(marker.parentFile!!.path, OsConstants.O_RDONLY, 0)
        try { Os.fsync(descriptor) } finally { Os.close(descriptor) }
    }

    override fun start() = synchronized(creationGate) {
        check(inspect().fresh) { "Prior vault evidence prevents setup" }
        write(InitializationPhase.STARTED)
    }
    override fun complete() = write(InitializationPhase.COMPLETE)

    companion object {
        const val MARKER_NAME = "vault-initialization-v1.bin"
        private val creationGate = Any()
    }
}
