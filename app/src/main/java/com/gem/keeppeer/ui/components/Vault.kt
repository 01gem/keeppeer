package com.gem.keeppeer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.gem.keeppeer.BuildConfig
import com.gem.keeppeer.R
import com.gem.keeppeer.ui.theme.KeeppeerTheme

/** Corner radii of the vault-steel system. Components pass these explicitly. */
object VaultShapes {
    val Field = RoundedCornerShape(16.dp)
    val Button = RoundedCornerShape(16.dp)
    val Card = RoundedCornerShape(24.dp)
    val Row = RoundedCornerShape(18.dp)
    val Tile = RoundedCornerShape(14.dp)
    val Chip = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(percent = 50)
}

// ---------------------------------------------------------------------------------------------
// Shield

/**
 * App mark: left half accent blue, right half steel, with a padlock (or check mark)
 * cut fully transparent, so it sits correctly on any background.
 * Decorative unless [contentDescription] is given.
 */
@Composable
fun KeeppeerShield(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showCheck: Boolean = false,
    contentDescription: String? = null,
) {
    val left = MaterialTheme.colorScheme.primary
    val right = KeeppeerTheme.colors.steel
    val a11y = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription; role = Role.Image }
    } else {
        Modifier.clearAndSetSemantics { }
    }
    Canvas(
        modifier
            .size(size)
            .then(a11y)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        val s = this.size.minDimension
        val ox = (this.size.width - s) / 2f
        val oy = (this.size.height - s) / 2f
        fun p(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)

        val shield = Path().apply {
            p(0.5f, 0.04f).let { moveTo(it.x, it.y) }
            p(0.88f, 0.17f).let { lineTo(it.x, it.y) }
            p(0.88f, 0.47f).let { lineTo(it.x, it.y) }
            val c1 = p(0.88f, 0.72f); val c2 = p(0.72f, 0.88f); val e = p(0.5f, 0.97f)
            cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
            val c3 = p(0.28f, 0.88f); val c4 = p(0.12f, 0.72f); val e2 = p(0.12f, 0.47f)
            cubicTo(c3.x, c3.y, c4.x, c4.y, e2.x, e2.y)
            p(0.12f, 0.17f).let { lineTo(it.x, it.y) }
            close()
        }
        val mid = ox + 0.5f * s
        clipRect(right = mid) { drawPath(shield, left) }
        clipRect(left = mid) { drawPath(shield, right) }

        if (showCheck) {
            val check = Path().apply {
                p(0.33f, 0.51f).let { moveTo(it.x, it.y) }
                p(0.46f, 0.64f).let { lineTo(it.x, it.y) }
                p(0.68f, 0.39f).let { lineTo(it.x, it.y) }
            }
            drawPath(
                check, Color.Black, blendMode = BlendMode.Clear,
                style = Stroke(width = 0.085f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        } else {
            // Shackle
            val stroke = 0.06f * s
            val shackleLeft = p(0.40f, 0.30f)
            drawArc(
                color = Color.Black, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = shackleLeft, size = Size(0.20f * s, 0.20f * s),
                style = Stroke(width = stroke), blendMode = BlendMode.Clear,
            )
            drawLine(Color.Black, p(0.40f, 0.40f), p(0.40f, 0.48f), stroke, blendMode = BlendMode.Clear)
            drawLine(Color.Black, p(0.60f, 0.40f), p(0.60f, 0.48f), stroke, blendMode = BlendMode.Clear)
            // Body
            val body = Rect(p(0.34f, 0.46f), p(0.66f, 0.71f))
            drawRoundRect(
                Color.Black, topLeft = body.topLeft, size = body.size,
                cornerRadius = CornerRadius(0.05f * s), blendMode = BlendMode.Clear,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Groups and rows

/** Rounded surface card that holds [VaultRow]s. Put [VaultDivider] between rows. */
@Composable
fun VaultGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = VaultShapes.Card,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), // decorative
    ) {
        Column(Modifier.padding(vertical = 6.dp), content = content)
    }
}

@Composable
fun VaultDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier.padding(start = 68.dp, end = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * Leading icon tile, title, optional subtitle, trailing slot.
 * Title and subtitle are one TalkBack node; anything in [trailing] (e.g. a Copy button)
 * stays separately focusable with its own label.
 */
@Composable
fun VaultRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .clip(VaultShapes.Row)
            .heightIn(min = 56.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(VaultShapes.Row)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(enabled = enabled, onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                    } else {
                        Modifier.semantics(mergeDescendants = true) { }
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(VaultShapes.Tile)
                        .background(KeeppeerTheme.colors.surfaceHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Segmented choice

/** Pill of options with radio semantics. Each segment is at least 48dp tall. */
@Composable
fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = KeeppeerTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(VaultShapes.Pill)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, VaultShapes.Pill)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(VaultShapes.Pill)
                    .background(if (isSelected) colors.buttonFill else Color.Transparent)
                    .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton) { onSelect(option) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) colors.onButtonFill else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Buttons

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 56.dp,
) {
    val colors = KeeppeerTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = height),
        enabled = enabled,
        shape = VaultShapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.buttonFill,
            contentColor = colors.onButtonFill,
            disabledContainerColor = colors.surfaceHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        ButtonContent(text, icon)
    }
}

@Composable
fun OutlineSteelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 56.dp,
    /** Tighter padding and 14sp text for half-width buttons; text wraps instead of clipping. */
    compact: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = height),
        enabled = enabled,
        shape = VaultShapes.Button,
        border = BorderStroke(1.dp, if (enabled) KeeppeerTheme.colors.steel else MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = if (compact) 12.dp else 20.dp, vertical = if (compact) 8.dp else 12.dp),
    ) {
        ButtonContent(text, icon, compact)
    }
}

/** Text field colors: surface fill; focused label uses the contrast-safe link color, not the accent. */
@Composable
fun vaultFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = KeeppeerTheme.colors.link,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
)

/**
 * Text button with a contrast-safe label. Material's default uses the accent blue,
 * which is only 3.6:1 on dark dialogs.
 */
@Composable
fun VaultTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = KeeppeerTheme.colors.link,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        content = content,
    )
}

/** Steel-filled button: ink text on silver in dark (12.2:1), white on slate in light (5.4:1). */
@Composable
fun SteelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 56.dp,
) {
    val colors = KeeppeerTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = height),
        enabled = enabled,
        shape = VaultShapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.steel,
            contentColor = if (colors.isDark) MaterialTheme.colorScheme.background else Color.White,
            disabledContainerColor = colors.surfaceHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        ButtonContent(text, icon)
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, compact: Boolean = false) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(if (compact) 18.dp else 20.dp))
        Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
    }
    // No line limit: at large font sizes labels wrap rather than clip.
    Text(text, style = if (compact) MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        else MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
}

// ---------------------------------------------------------------------------------------------
// Monogram

/**
 * First letter of [name] on a tile whose color is stable per name.
 * Hidden from TalkBack: the surrounding row already reads the title.
 */
@Composable
fun MonogramTile(name: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val c = KeeppeerTheme.colors
    // Every pair is at least 4.5:1 in its theme.
    val pairs = listOf(
        c.digit to c.digitBg,
        (if (c.isDark) MaterialTheme.colorScheme.background else Color.White) to c.steel,
        c.symbol to c.symbolBg,
        c.letter to c.letterBg,
    )
    val (fg, bg) = pairs[Math.floorMod(name.trim().lowercase().hashCode(), pairs.size)]
    val letter = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: '#'
    val fontSize = with(LocalDensity.current) { (size * 0.42f).toSp() } // fixed: ignores font scale
    Box(
        modifier
            .size(size)
            .clip(VaultShapes.Tile)
            .background(bg)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(letter.toString(), color = fg, fontSize = fontSize, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium.copy(lineHeight = fontSize))
    }
}

// ---------------------------------------------------------------------------------------------
// Build banner

/** Debug and validation builds only; release shows nothing. */
@Composable
fun BuildBanner() {
    val banner = when {
        BuildConfig.SHOW_DEVELOPMENT_BANNER -> stringResource(R.string.development_notice)
        BuildConfig.VALIDATION_BUILD -> stringResource(R.string.validation_notice)
        else -> null
    } ?: return
    Text(
        banner,
        Modifier.fillMaxWidth().background(KeeppeerTheme.colors.surfaceHigh).padding(horizontal = 12.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

// ---------------------------------------------------------------------------------------------
// Warning note

@Composable
fun WarningNote(text: String, modifier: Modifier = Modifier) {
    val c = KeeppeerTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(VaultShapes.Field)
            .background(c.warningBg)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = c.warningIcon, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.warningText, modifier = Modifier.weight(1f))
    }
}
