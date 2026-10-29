package com.gem.keeppeer

import com.gem.keeppeer.security.*
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.io.IOException
import javax.crypto.AEADBadTagException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], manifest = Config.NONE)
class BiometricDiagnosticsTest {
    @Test fun resultPreservesFailureStageAndDoesNotConfusePasswordWithWrapperFailure() {
        assertEquals(BiometricOutcome.WRONG_PASSWORD,
            BiometricDiagnostics.failure(BiometricStage.PASSWORD_VERIFY, AEADBadTagException()).outcome)
        val encrypted = BiometricDiagnostics.failure(BiometricStage.COMPLETE,
            BiometricFailure(BiometricStage.ENCRYPT_FINAL, AEADBadTagException()))
        assertEquals(BiometricOutcome.CRYPTO_ERROR, encrypted.outcome)
        assertEquals(BiometricStage.ENCRYPT_FINAL, encrypted.stage)
        assertEquals(BiometricOutcome.IO_ERROR, BiometricDiagnostics.failure(
            BiometricStage.COMPLETE, BiometricFailure(BiometricStage.WRAPPER_WRITE, IOException())).outcome)
        assertEquals(BiometricOutcome.SESSION_EXPIRED, BiometricDiagnostics.failure(
            BiometricStage.APPROVAL_CHECK, CancellationException()).outcome)
        assertEquals(BiometricOutcome.KEYSTORE_ERROR, BiometricDiagnostics.failure(
            BiometricStage.COMPLETE, BiometricFailure(BiometricStage.KEY_LOAD, IllegalStateException())).outcome)
    }
    @Test fun diagnosticsOmitMessagesStacksAndNonAllowlistedCodes() {
        ShadowLog.clear()
        BiometricDiagnostics.record(BiometricStage.PROMPT_ERROR,
            IOException("synthetic credential must never reach logs"), 999_999)
        BiometricDiagnostics.record(BiometricStage.PROMPT_ERROR, code = 7)
        val logs = ShadowLog.getLogsForTag("KeeppeerBiometric")
        assertEquals(2, logs.size)
        assertEquals("stage=PROMPT_ERROR exception=IOException", logs[0].msg)
        assertNull(logs[0].throwable)
        assertEquals("stage=PROMPT_ERROR code=7", logs[1].msg)
    }
}
