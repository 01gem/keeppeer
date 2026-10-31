package com.gem.keeppeer.security

import android.os.Build
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest

/** Public Compose interception API; IMEs receive privacy requests, not an enforcement guarantee. */
@Composable
fun SensitiveInputProvider(content: @Composable () -> Unit) {
    val interceptor = remember {
        PlatformTextInputInterceptor { request, next ->
            next.startInputMethod(PlatformTextInputMethodRequest { info ->
                request.createInputConnection(info).also {
                    if (Build.VERSION.SDK_INT >= 26) {
                        info.imeOptions = info.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                    }
                    if (info.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT) {
                        info.inputType = (info.inputType and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT.inv()) or
                            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    }
                }
            })
        }
    }
    InterceptPlatformTextInput(interceptor, content)
}
