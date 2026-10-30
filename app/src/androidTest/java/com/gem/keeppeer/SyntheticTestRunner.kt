package com.gem.keeppeer

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import com.gem.keeppeer.di.AppContainer
import com.gem.keeppeer.security.SensitiveClipboard
import com.gem.keeppeer.security.VaultSession
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first

/** API 24/25 ClipboardManager constructs a Handler on the requesting thread. */
internal fun <T : Any> syntheticOnMain(block: () -> T): T {
    if (Looper.myLooper() == Looper.getMainLooper()) return block()
    var value: T? = null
    InstrumentationRegistry.getInstrumentation().runOnMainSync { value = block() }
    return checkNotNull(value)
}

/** Wait on the instrumentation thread; availability inspection must never block Android main. */
internal fun awaitVaultInspection(session: VaultSession) = kotlinx.coroutines.runBlocking {
    kotlinx.coroutines.withTimeout(30_000) {
        session.state.first { it.stage != com.gem.keeppeer.security.VaultStage.CHECKING }
    }
}

/** Instrumentation never uses a developer's production vault files or key aliases. */
class SyntheticTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, SyntheticTestApplication::class.java.name, context)
}
class SyntheticTestApplication : KeeppeerApplication() {
    override fun createContainer(): AppContainer {
        val synthetic = SyntheticVaultContext(this)
        return object : AppContainer {
            override val session = VaultSession(synthetic, SensitiveClipboard(synthetic), synthetic.namespace)
        }
    }
}
class SyntheticVaultContext(base: Context) : ContextWrapper(base) {
    val namespace = "keeppeer.test." + UUID.randomUUID()
    private val root = File(base.cacheDir, namespace).apply { mkdirs() }
    override fun getApplicationContext(): Context = this
    override fun getNoBackupFilesDir(): File = File(root, "no-backup").apply { mkdirs() }
    override fun getDatabasePath(name: String): File = File(root, name)
    override fun deleteDatabase(name: String): Boolean = SQLiteDatabase.deleteDatabase(getDatabasePath(name))
    override fun getSharedPreferences(name: String, mode: Int) =
        super.getSharedPreferences(namespace + "." + name, mode)
}
