package com.gem.keeppeer.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.gem.keeppeer.R
import com.gem.keeppeer.domain.*
import com.gem.keeppeer.ui.components.*
import com.gem.keeppeer.ui.edit.SensitiveKeyboard
import com.gem.keeppeer.ui.privacy.ProtectDialogWindow

@Composable
fun categoryLabel(category: CategoryFilter): String = when (category) {
    CategoryFilter.All -> stringResource(R.string.all_categories)
    CategoryFilter.Uncategorized -> stringResource(R.string.uncategorized)
    is CategoryFilter.Named -> stringResource(R.string.named_category, category.value)
}

@Composable
fun PasswordListScreen(
    state: EntryListState, onSearch: (String) -> Unit, onRetry: () -> Unit,
    onAdd: () -> Unit, onEntry: (Long) -> Unit, onSettings: () -> Unit,
    onCategory: (CategoryFilter) -> Unit, onFavoritesOnly: (Boolean) -> Unit,
    onClear: () -> Unit, onFavorite: (Long, Boolean) -> Unit,
) {
    var filters by remember { mutableStateOf(false) }
    PrototypePage(title = stringResource(R.string.app_title),
        actions = { TextButton(onClick = onSettings) { Text(stringResource(R.string.settings)) } },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Text(stringResource(R.string.add), Modifier.padding(16.dp)) } },
    ) {
        // All controls scroll with content, including in landscape and at 2x fonts.
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                OutlinedTextField(value = state.query, onValueChange = onSearch,
                    label = { Text(stringResource(R.string.search)) }, keyboardOptions = SensitiveKeyboard,
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedButton(onClick = { filters = true }) { Text(stringResource(R.string.filters)) }
                Text(categoryLabel(state.category))
            }
            if (state.filtered) item { TextButton(onClick = onClear) { Text(stringResource(R.string.clear_filters)) } }
            if (state.favoriteError) item { Text(stringResource(R.string.favorite_error), Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            when {
                state.loading -> item { CircularProgressIndicator(Modifier.padding(24.dp)) }
                state.error != null -> item { StatusMessage(stringResource(R.string.load_entries_error), onRetry) }
                state.entries.isEmpty() -> item { StatusMessage(stringResource(if (state.vaultEmpty) R.string.empty_vault else R.string.no_results)) }
                else -> {
                    if (state.sections.favorites.isNotEmpty()) {
                        item(key = "favorites") { SectionHeading(stringResource(R.string.favorites)) }
                        items(state.sections.favorites, key = { it.id }) { EntryRow(it, state, onEntry, onFavorite) }
                    }
                    if (state.sections.remaining.isNotEmpty()) {
                        item(key = "remaining") { SectionHeading(stringResource(if (state.sections.favorites.isEmpty()) R.string.all_entries else R.string.other_entries)) }
                        items(state.sections.remaining, key = { it.id }) { EntryRow(it, state, onEntry, onFavorite) }
                    }
                }
            }
        }
    }
    if (filters) AlertDialog(
        onDismissRequest = { filters = false },
        title = { ProtectDialogWindow(); Text(stringResource(R.string.filters)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            OptionToggle(stringResource(R.string.favorites_only), state.favoritesOnly, change = onFavoritesOnly)
            SectionHeading(stringResource(R.string.category))
            Column(Modifier.selectableGroup()) {
                val choices = listOf(CategoryFilter.All, CategoryFilter.Uncategorized) +
                    state.categories.map { CategoryFilter.Named(it) }
                // Keep an orphaned active category visible, rather than silently broadening results.
                val visible = if (state.category !in choices) choices + state.category else choices
                visible.forEach { choice ->
                    OptionRadio(categoryLabel(choice), state.category == choice) { onCategory(choice) }
                }
                if (state.category is CategoryFilter.Named && state.category.value !in state.categories)
                    Text(stringResource(R.string.active_category_removed))
            }
        } },
        confirmButton = { TextButton(onClick = { filters = false }) { Text(stringResource(R.string.csv_continue)) } },
        dismissButton = { TextButton(onClick = onClear) { Text(stringResource(R.string.clear_filters)) } },
    )
}

@Composable
private fun EntryRow(entry: EntrySummary, state: EntryListState, open: (Long) -> Unit,
    favorite: (Long, Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        ListItem(headlineContent = { Text(entry.title) }, supportingContent = {
            Column { entry.username?.let { Text(it) }; entry.url?.let { Text(it) }; entry.category?.let { Text(it) } }
        }, modifier = Modifier.weight(1f).clickable { open(entry.id) })
        IconToggleButton(checked = entry.isFavorite, enabled = entry.id !in state.updating,
            modifier = Modifier.testTag("favorite-${entry.id}"),
            onCheckedChange = { favorite(entry.id, it) }) {
            Icon(if (entry.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = stringResource(if (entry.isFavorite) R.string.remove_favorite else R.string.add_favorite))
        }
    }
    HorizontalDivider()
}
