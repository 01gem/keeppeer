package com.gem.keeppeer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.gem.keeppeer.R

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
                navigationIcon = { if (onBack != null) TextButton(onClick = onBack) { Text("Back") } },
                actions = actions,
            )
        },
        floatingActionButton = floatingActionButton,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            Surface(color = MaterialTheme.colorScheme.errorContainer) {
                Text(
                    stringResource(R.string.development_notice),
                    Modifier.fillMaxWidth().padding(12.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            content()
        }
    }
}

@Composable
fun StatusMessage(message: String, retry: (() -> Unit)? = null) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message)
        if (retry != null) Button(onClick = retry) { Text("Retry") }
    }
}
