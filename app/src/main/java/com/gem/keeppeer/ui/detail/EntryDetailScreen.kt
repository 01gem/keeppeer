package com.gem.keeppeer.ui.detail

import com.gem.keeppeer.ui.components.VaultTextButton
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gem.keeppeer.R
import com.gem.keeppeer.security.SensitiveClipboard
import com.gem.keeppeer.ui.components.BuildBanner
import com.gem.keeppeer.ui.components.MonogramTile
import com.gem.keeppeer.ui.components.OutlineSteelButton
import com.gem.keeppeer.ui.components.PrimaryButton
import com.gem.keeppeer.ui.components.StatusMessage
import com.gem.keeppeer.ui.components.VaultDivider
import com.gem.keeppeer.ui.components.VaultGroup
import com.gem.keeppeer.ui.components.VaultRow
import com.gem.keeppeer.ui.components.VaultShapes
import com.gem.keeppeer.ui.privacy.ProtectDialogWindow
import com.gem.keeppeer.ui.theme.KeeppeerTheme
import com.gem.keeppeer.ui.theme.PasswordCharStyle
import java.text.DateFormat
import java.util.Date
import kotlin.math.ceil

@Composable
fun EntryDetailScreen(
    state: EntryDetailState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onCopy: (String) -> Boolean,
    onFavorite: (Boolean) -> Unit,
) {
    var reveal by remember(com.gem.keeppeer.ui.theme.LocalThemeMode.current) { mutableStateOf(false) }
    var confirmDelete by remember(com.gem.keeppeer.ui.theme.LocalThemeMode.current) { mutableStateOf(false) }
    var copyFailed by remember { mutableStateOf(false) }
    // Display only: SensitiveClipboard restarts its clear timer on every copy, so the ring restarts too.
    var copiedAt by remember { mutableStateOf<Long?>(null) }
    val copy: (String) -> Unit = { value ->
        if (onCopy(value)) { copiedAt = SystemClock.uptimeMillis(); copyFailed = false }
        else { copiedAt = null; copyFailed = true }
    }
    BackHandler(enabled = state.deleting) { }
    val title = stringResource(R.string.entry_details)
    Scaffold(
        modifier = Modifier.semantics { paneTitle = title },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DetailTopBar(
                state = state,
                onBack = if (state.deleting) null else onBack,
                onEdit = onEdit,
                onFavorite = onFavorite,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            BuildBanner()
            when {
                state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
                state.entry == null -> StatusMessage(
                    stringResource(if (state.error != null) R.string.load_entry_error else R.string.missing_entry),
                    if (state.error != null) onRetry else null,
                )
                else -> Column(
                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val entry = state.entry
                    Header(entry.title, entry.category)
                    if (state.favoriteError) ErrorText(stringResource(R.string.favorite_error))
                    PasswordCard(
                        password = entry.password,
                        reveal = reveal,
                        onToggle = { reveal = !reveal },
                        onCopy = { copy(entry.password) },
                    )
                    copiedAt?.let { start -> CopiedStrip(start) { copiedAt = null } }
                    if (copyFailed) ErrorText(stringResource(R.string.clipboard_failed))
                    VaultGroup {
                        FieldRow(Icons.Outlined.Person, stringResource(R.string.username), entry.username,
                            stringResource(R.string.copy_username), copy)
                        VaultDivider()
                        FieldRow(Icons.Outlined.Language, stringResource(R.string.url), entry.url,
                            stringResource(R.string.copy_url), copy)
                        VaultDivider()
                        FieldRow(Icons.Outlined.Description, stringResource(R.string.notes), entry.notes,
                            stringResource(R.string.copy_notes), copy)
                    }
                    state.error?.let { ErrorText(stringResource(R.string.delete_entry_error)) }
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        enabled = !state.deleting,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = VaultShapes.Button,
                        border = BorderStroke(1.dp, if (state.deleting) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text(
                            if (state.deleting) stringResource(R.string.deleting) else stringResource(R.string.delete),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val format = DateFormat.getDateTimeInstance()
                        FooterText(stringResource(R.string.updated, format.format(Date(entry.updatedAt))))
                        FooterText(stringResource(R.string.created, format.format(Date(entry.createdAt))))
                    }
                }
            }
        }
    }
    if (confirmDelete && state.entry != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { ProtectDialogWindow(); Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_explanation)) },
            confirmButton = { VaultTextButton(onClick = { confirmDelete = false; onDelete() }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { VaultTextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun DetailTopBar(
    state: EntryDetailState,
    onBack: (() -> Unit)?,
    onEdit: () -> Unit,
    onFavorite: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        }
        Spacer(Modifier.weight(1f))
        val entry = state.entry
        if (entry != null) {
            val on = entry.isFavorite
            val stateText = stringResource(if (on) R.string.favorite_yes else R.string.favorite_no)
            IconButton(
                onClick = { onFavorite(!on) },
                enabled = !state.deleting && !state.favoriteUpdating,
                modifier = Modifier.semantics { stateDescription = stateText },
            ) {
                Icon(
                    if (on) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(if (on) R.string.remove_favorite else R.string.add_favorite),
                    tint = if (on) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            }
            IconButton(onClick = onEdit, enabled = !state.deleting) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit))
            }
        }
    }
}

@Composable
private fun Header(title: String, category: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MonogramTile(title, size = 60.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            if (!category.isNullOrBlank()) {
                Text(category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Password card

private enum class Strength(val label: Int) { WEAK(R.string.strength_weak), FAIR(R.string.strength_fair), STRONG(R.string.strength_strong) }
private enum class CharClass { LETTER, DIGIT, SYMBOL }

/** One grapheme-ish unit per tile; surrogate pairs stay together. */
private fun codePoints(value: String): List<String> = buildList {
    var i = 0
    while (i < value.length) {
        val cp = value.codePointAt(i)
        add(String(Character.toChars(cp)))
        i += Character.charCount(cp)
    }
}

private fun classOf(unit: String): CharClass {
    val cp = unit.codePointAt(0)
    return when {
        Character.isLetter(cp) -> CharClass.LETTER
        Character.isDigit(cp) -> CharClass.DIGIT
        else -> CharClass.SYMBOL // includes space
    }
}

/** Weak < 8; Strong = 16+, or 12+ with 3+ classes; Fair otherwise. */
private fun strengthOf(password: String): Pair<Strength, Int> {
    val units = codePoints(password)
    var lower = false; var upper = false; var digit = false; var symbol = false
    for (u in units) {
        val cp = u.codePointAt(0)
        when {
            Character.isUpperCase(cp) -> upper = true
            Character.isLetter(cp) -> lower = true
            Character.isDigit(cp) -> digit = true
            else -> symbol = true
        }
    }
    val classes = listOf(lower, upper, digit, symbol).count { it }
    val n = units.size
    val strength = when {
        n < 8 -> Strength.WEAK
        n >= 16 || (n >= 12 && classes >= 3) -> Strength.STRONG
        else -> Strength.FAIR
    }
    return strength to n
}

private const val TILES_PER_ROW = 8
private val CharTile = RoundedCornerShape(10.dp)

@Composable
private fun PasswordCard(password: String, reveal: Boolean, onToggle: () -> Unit, onCopy: () -> Unit) {
    val colors = KeeppeerTheme.colors
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(VaultShapes.Card)
            .background(Brush.verticalGradient(listOf(colors.surfaceHigh, scheme.surface)))
            .border(1.dp, scheme.outlineVariant, VaultShapes.Card) // decorative
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.password), style = MaterialTheme.typography.titleMedium, color = scheme.onSurface,
                modifier = Modifier.weight(1f))
            if (password.isNotEmpty()) {
                val (strength, length) = strengthOf(password)
                val word = stringResource(strength.label)
                Text(
                    // Length is shown only while revealed, so the hidden card does not leak it.
                    if (reveal) pluralStringResource(R.plurals.strength_with_length, length, word, length) else word,
                    style = MaterialTheme.typography.labelMedium,
                    color = when (strength) {
                        Strength.WEAK -> scheme.error
                        Strength.FAIR -> colors.warningText
                        Strength.STRONG -> colors.success
                    },
                )
            }
        }
        if (password.isEmpty()) {
            Text(stringResource(R.string.empty_value), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
        } else {
            val description = stringResource(if (reveal) R.string.password_shown else R.string.password_hidden)
            Crossfade(
                targetState = reveal,
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                label = "password-reveal",
            ) { shown ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (shown) {
                        CharGrid(codePoints(password))
                        Legend()
                    } else {
                        HiddenRow()
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineSteelButton(
                text = stringResource(if (reveal) R.string.hide_short else R.string.show_short),
                onClick = onToggle,
                modifier = Modifier.weight(1f),
                enabled = password.isNotEmpty(),
                icon = if (reveal) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            )
            PrimaryButton(
                text = stringResource(R.string.copy_short),
                onClick = onCopy,
                modifier = Modifier.weight(1f),
                enabled = password.isNotEmpty(),
                icon = Icons.Outlined.ContentCopy,
            )
        }
    }
}

@Composable
private fun CharGrid(units: List<String>) {
    val c = KeeppeerTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        units.chunked(TILES_PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { unit ->
                    val (fg, bg) = when (classOf(unit)) {
                        CharClass.LETTER -> c.letter to c.letterBg
                        CharClass.DIGIT -> c.digit to c.digitBg
                        CharClass.SYMBOL -> c.symbol to c.symbolBg
                    }
                    CharTileBox(bg) {
                        Text(if (unit == " ") "␣" else unit, style = PasswordCharStyle, color = fg, maxLines = 1)
                    }
                }
                repeat(TILES_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HiddenRow() {
    val c = KeeppeerTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(TILES_PER_ROW) {
            CharTileBox(c.letterBg) {
                Text("•", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RowScope.CharTileBox(bg: Color, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.weight(1f).height(44.dp).clip(CharTile).background(bg),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun Legend() {
    val c = KeeppeerTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(c.letterBg, c.letter, stringResource(R.string.legend_letters))
        LegendItem(c.digitBg, c.digit, stringResource(R.string.legend_digits))
        LegendItem(c.symbolBg, c.symbol, stringResource(R.string.legend_symbols))
    }
}

@Composable
private fun LegendItem(bg: Color, fg: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(bg).border(1.dp, fg, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------------------------------
// Copied strip

@Composable
private fun CopiedStrip(start: Long, onExpired: () -> Unit) {
    val total = SensitiveClipboard.CLEAR_DELAY_MS
    var remaining by remember(start) { mutableLongStateOf(total) }
    LaunchedEffect(start) {
        while (true) {
            remaining = (total - (SystemClock.uptimeMillis() - start)).coerceAtLeast(0)
            if (remaining == 0L) { onExpired(); break }
            withFrameMillis { }
        }
    }
    val seconds = ceil(remaining / 1000.0).toInt()
    val progress = remaining.toFloat() / total
    val track = MaterialTheme.colorScheme.outlineVariant // decorative track; the arc carries the info
    val ring = MaterialTheme.colorScheme.primary
    val announce = stringResource(R.string.clipboard_copied)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(VaultShapes.Field)
            .background(KeeppeerTheme.colors.surfaceHigh)
            .clearAndSetSemantics { contentDescription = announce; liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(24.dp)) {
            val stroke = 3.dp.toPx()
            drawCircle(track, radius = size.minDimension / 2f - stroke / 2f, style = Stroke(stroke))
            drawArc(ring, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            pluralStringResource(R.plurals.clipboard_clears_in, seconds, seconds),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Fields

@Composable
private fun FieldRow(icon: ImageVector, label: String, value: String?, copyLabel: String, copy: (String) -> Unit) {
    VaultRow(
        title = label,
        subtitle = value ?: stringResource(R.string.empty_value),
        icon = icon,
        trailing = if (value == null) null else {
            {
                IconButton(onClick = { copy(value) }) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = copyLabel)
                }
            }
        },
    )
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
private fun FooterText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}
