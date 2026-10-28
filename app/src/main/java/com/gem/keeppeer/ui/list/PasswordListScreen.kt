package com.gem.keeppeer.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gem.keeppeer.ui.components.PrototypePage
import com.gem.keeppeer.ui.components.StatusMessage

@Composable
fun PasswordListScreen(
    state: EntryListState,
    onSearch: (String) -> Unit,
    onRetry: () -> Unit,
    onAdd: () -> Unit,
    onEntry: (Long) -> Unit,
) {
    PrototypePage(
        title = "Keeppeer",
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Text("Add", Modifier.padding(16.dp)) } },
    ) {
        OutlinedTextField(
            value = state.query, onValueChange = onSearch,
            label = { Text("Search title, username, URL or category") },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.error != null -> StatusMessage(state.error, onRetry)
            state.entries.isEmpty() -> StatusMessage(
                if (state.query.isBlank()) "No entries yet. Add a synthetic entry to get started."
                else "No matching entries.",
            )
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(state.entries, key = { it.id }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.title) },
                        supportingContent = {
                            Column {
                                entry.username?.let { Text(it) }
                                entry.url?.let { Text(it) }
                            }
                        },
                        modifier = Modifier.clickable { onEntry(entry.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
