package com.gem.keeppeer.ui.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gem.keeppeer.R
import com.gem.keeppeer.domain.*
import com.gem.keeppeer.ui.components.*
import com.gem.keeppeer.ui.edit.SensitiveKeyboard
import com.gem.keeppeer.ui.theme.KeeppeerTheme

@Composable
fun categoryLabel(category: CategoryFilter): String = when (category) {
    CategoryFilter.All -> stringResource(R.string.all_categories)
    CategoryFilter.Uncategorized -> stringResource(R.string.uncategorized)
    is CategoryFilter.Named -> stringResource(R.string.named_category, category.value)
}

private val FabHeight = 56.dp

@Composable
fun PasswordListScreen(
    state: EntryListState, onSearch: (String) -> Unit, onRetry: () -> Unit,
    onAdd: () -> Unit, onEntry: (Long) -> Unit, onSettings: () -> Unit, onLock: () -> Unit,
    onCategory: (CategoryFilter) -> Unit, onFavoritesOnly: (Boolean) -> Unit,
    onClear: () -> Unit, onFavorite: (Long, Boolean) -> Unit,
    onCopyPassword: (Long) -> Unit,
) {
    val colors = KeeppeerTheme.colors
    val emptyVault = !state.loading && state.error == null && state.entries.isEmpty() && state.vaultEmpty
    val noResults = !state.loading && state.error == null && state.entries.isEmpty() && !state.vaultEmpty
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            // The empty vault shows its own centered Add entry action instead.
            if (!emptyVault) ExtendedFloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.heightIn(min = FabHeight),
                shape = VaultShapes.Button,
                containerColor = colors.buttonFill,
                contentColor = colors.onButtonFill,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add), style = MaterialTheme.typography.labelLarge) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()).consumeWindowInsets(padding)) {
            BuildBanner()
            // All controls scroll with content, including in landscape and at large fonts.
            // Bottom padding keeps the last row's Copy and star clear of the Add entry button.
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = padding.calculateBottomPadding() + FabHeight + 16.dp + 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "header") { Header(state, onLock, onSettings) }
                item(key = "search") { SearchField(state.query, onSearch, Modifier.padding(horizontal = 16.dp)) }
                item(key = "chips") { FilterChips(state, onCategory, onFavoritesOnly) }
                if (state.category is CategoryFilter.Named && state.category.value !in state.categories) item(key = "orphan") {
                    Text(stringResource(R.string.active_category_removed), Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // No-results shows its own Clear filters action; avoid two identical controls.
                if (state.filtered && !noResults) item(key = "clear") {
                    VaultTextButton(onClick = onClear, Modifier.padding(horizontal = 8.dp)) { Text(stringResource(R.string.clear_filters)) }
                }
                if (state.favoriteError) item(key = "favorite-error") {
                    Text(stringResource(R.string.favorite_error), Modifier.padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                when {
                    state.loading -> item { CircularProgressIndicator(Modifier.padding(24.dp)) }
                    state.error != null -> item { StatusMessage(stringResource(R.string.load_entries_error), onRetry) }
                    emptyVault -> item {
                        EmptyState(stringResource(R.string.empty_vault)) {
                            PrimaryButton(stringResource(R.string.add), onAdd, icon = Icons.Outlined.Add)
                        }
                    }
                    noResults -> item {
                        EmptyState(stringResource(R.string.no_results)) {
                            OutlineSteelButton(stringResource(R.string.clear_filters), onClear)
                        }
                    }
                    else -> {
                        if (state.sections.favorites.isNotEmpty()) {
                            item(key = "favorites") { ListSection(stringResource(R.string.favorites)) }
                            items(state.sections.favorites, key = { it.id }) { EntryRow(it, state, onEntry, onFavorite, onCopyPassword) }
                        }
                        if (state.sections.remaining.isNotEmpty()) {
                            item(key = "remaining") { ListSection(stringResource(if (state.sections.favorites.isEmpty()) R.string.all_entries else R.string.other_entries)) }
                            items(state.sections.remaining, key = { it.id }) { EntryRow(it, state, onEntry, onFavorite, onCopyPassword) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: EntryListState, onLock: () -> Unit, onSettings: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.app_title), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            Text(stringResource(R.string.vault_title), style = MaterialTheme.typography.headlineMedium, color = scheme.onSurface,
                modifier = Modifier.semantics { heading() })
            if (!state.loading && state.error == null) {
                val n = state.entries.size
                Text(
                    if (state.filtered) pluralStringResource(R.plurals.entry_match_count, n, n)
                    else pluralStringResource(R.plurals.entry_count, n, n),
                    style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onLock) { Icon(Icons.Outlined.Lock, contentDescription = stringResource(R.string.lock_now)) }
        IconButton(onClick = onSettings) { Icon(Icons.Outlined.Shield, contentDescription = stringResource(R.string.settings)) }
    }
}

@Composable
private fun SearchField(query: String, onSearch: (String) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(R.string.search)
    BasicTextField(
        value = query,
        onValueChange = onSearch,
        singleLine = true,
        keyboardOptions = SensitiveKeyboard,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
        cursorBrush = SolidColor(scheme.primary),
        // The label stays available to TalkBack once text is typed and the placeholder hides.
        modifier = modifier.fillMaxWidth().semantics { contentDescription = label },
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(VaultShapes.Field)
                    .background(scheme.surface)
                    .border(1.dp, scheme.outline, VaultShapes.Field)
                    .padding(start = 14.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                    if (query.isEmpty()) {
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { })
                    }
                    inner()
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onSearch("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.clear_search))
                    }
                } else Spacer(Modifier.width(12.dp))
            }
        },
    )
}

@Composable
private fun FilterChips(state: EntryListState, onCategory: (CategoryFilter) -> Unit, onFavoritesOnly: (Boolean) -> Unit) {
    val choices = listOf(CategoryFilter.All, CategoryFilter.Uncategorized) + state.categories.map { CategoryFilter.Named(it) }
    // Keep an orphaned active category visible, rather than silently broadening results.
    val visible = if (state.category !in choices) choices + state.category else choices
    val groupLabel = stringResource(R.string.filters)
    LazyRow(
        Modifier.fillMaxWidth().semantics { isTraversalGroup = true; contentDescription = groupLabel }.selectableGroup(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "favorites-only") {
            val label = stringResource(R.string.favorites_only)
            Chip(label, label, state.favoritesOnly, Modifier.toggleable(state.favoritesOnly, role = Role.Checkbox) { onFavoritesOnly(it) },
                icon = true)
        }
        items(visible, key = { it.toString() }) { choice ->
            val shown = when (choice) {
                CategoryFilter.All -> stringResource(R.string.all_categories)
                CategoryFilter.Uncategorized -> stringResource(R.string.uncategorized_short)
                is CategoryFilter.Named -> choice.value
            }
            val selected = state.category == choice
            Chip(shown, categoryLabel(choice), selected,
                Modifier.selectable(selected, role = Role.RadioButton) { onCategory(choice) })
        }
    }
}

/** Selected: filled onSurface with background-colored text. [spoken] is what TalkBack and tests read. */
@Composable
private fun Chip(shown: String, spoken: String, selected: Boolean, interaction: Modifier, icon: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val fg = if (selected) scheme.background else scheme.onSurface
    Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .heightIn(min = 40.dp)
                .clip(VaultShapes.Chip)
                .background(if (selected) scheme.onSurface else scheme.surface)
                .then(if (selected) Modifier else Modifier.border(1.dp, scheme.outline, VaultShapes.Chip))
                .then(interaction)
                .semantics(mergeDescendants = true) { text = AnnotatedString(spoken) }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon) {
                Icon(if (selected) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = null,
                    tint = fg, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(shown, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1,
                modifier = Modifier.clearAndSetSemantics { })
        }
    }
}

@Composable
private fun ListSection(label: String) {
    Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).semantics { heading() })
}

@Composable
private fun EmptyState(message: String, action: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KeeppeerShield(size = 72.dp)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        action()
    }
}

private fun copyStatusText(status: CopyStatus): Int = when (status) {
    CopyStatus.COPIED -> R.string.clipboard_copied
    CopyStatus.FAILED -> R.string.clipboard_failed
    CopyStatus.MISSING -> R.string.copy_missing
    CopyStatus.EMPTY -> R.string.copy_no_password
}

@Composable
private fun EntryRow(entry: EntrySummary, state: EntryListState, open: (Long) -> Unit,
    favorite: (Long, Boolean) -> Unit, copyPassword: (Long) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = VaultShapes.Row,
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant), // decorative card
    ) {
        Column {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 0.dp), verticalAlignment = Alignment.CenterVertically) {
                // Opening the entry: title and username are one TalkBack node; Copy and star stay separate.
                Row(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .clip(VaultShapes.Row)
                        .clickable(role = Role.Button) { open(entry.id) }
                        .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonogramTile(entry.title, size = 44.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.title, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        entry.username?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
                val copyDescription = stringResource(R.string.copy_password_for, entry.title)
                OutlinedButton(
                    onClick = { copyPassword(entry.id) },
                    enabled = entry.id !in state.copying,
                    modifier = Modifier.testTag("copy-${entry.id}")
                        .minimumInteractiveComponentSize()
                        .heightIn(min = 40.dp)
                        .semantics { contentDescription = copyDescription },
                    shape = VaultShapes.Button,
                    border = BorderStroke(1.dp, KeeppeerTheme.colors.steel),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.copy_short), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
                IconToggleButton(
                    checked = entry.isFavorite, enabled = entry.id !in state.updating,
                    modifier = Modifier.testTag("favorite-${entry.id}"),
                    onCheckedChange = { favorite(entry.id, it) },
                ) {
                    Icon(
                        if (entry.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = stringResource(if (entry.isFavorite) R.string.remove_favorite else R.string.add_favorite),
                        tint = if (entry.isFavorite) scheme.primary else scheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (state.copyStatusId == entry.id) state.copyStatus?.let {
                Text(stringResource(copyStatusText(it)),
                    Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it == CopyStatus.COPIED) scheme.onSurfaceVariant else scheme.error)
            }
        }
    }
}
