package com.gem.keeppeer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gem.keeppeer.BuildConfig
import com.gem.keeppeer.R
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrototypePage(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { if (onBack != null) TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
                actions = actions,
            )
        },
        floatingActionButton = floatingActionButton,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            val banner = when {
                BuildConfig.SHOW_DEVELOPMENT_BANNER -> stringResource(R.string.development_notice)
                BuildConfig.VALIDATION_BUILD -> stringResource(R.string.validation_notice)
                else -> null
            }
            if (banner != null) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        banner,
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            content()
        }
    }
}

@Composable
fun StatusMessage(message: String, retry: (() -> Unit)? = null) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message)
        if (retry != null) Button(onClick = retry) { Text(stringResource(R.string.retry)) }
    }
}
