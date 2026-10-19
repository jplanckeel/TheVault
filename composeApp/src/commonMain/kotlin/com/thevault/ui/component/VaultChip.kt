package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.ItemCondition
import com.thevault.ui.theme.Error
import com.thevault.ui.theme.Info
import com.thevault.ui.theme.OnPrimary
import com.thevault.ui.theme.OnSecondaryContainer
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.OutlineVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.Secondary
import com.thevault.ui.theme.SecondaryContainer
import com.thevault.ui.theme.Success
import com.thevault.ui.theme.SurfaceContainerHighest
import com.thevault.ui.theme.VaultRadius
import com.thevault.ui.theme.Warning

/** Color tone of a chip — same set as `Chip.tsx`. */
enum class ChipTone {
    /** Grey, for neutral information. */
    Neutral,

    /** Lavender accent. */
    Primary,

    /** Secondary purple. */
    Secondary,

    /** Positive (green). */
    Success,

    /** Warning (amber). */
    Warning,

    /** Error / sealed (red). */
    Error,

    /** Informational (blue). */
    Info,
}

/** Fill style of a chip. */
enum class ChipVariant {
    /** Tinted translucent background. */
    Soft,

    /** Opaque background. */
    Solid,

    /** Transparent with a colored border. */
    Outline,
}

/**
 * Pill-shaped indicator: condition badges, platform tags, deltas, filters.
 *
 * @param text Label (rendered as given; pass uppercase text for badges).
 * @param modifier Modifier applied to the chip.
 * @param tone Color tone.
 * @param variant Fill style.
 * @param icon Optional leading icon.
 * @param onClick Optional click handler (filter chips).
 */
@Composable
fun VaultChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: ChipTone = ChipTone.Neutral,
    variant: ChipVariant = ChipVariant.Soft,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = chipColors(tone, variant)
    val borderModifier = colors.border?.let { Modifier.border(1.dp, it, VaultRadius.full) } ?: Modifier
    val clickModifier = onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier
    Row(
        modifier =
            modifier
                .height(24.dp)
                .clip(VaultRadius.full)
                .background(colors.container)
                .then(borderModifier)
                .then(clickModifier)
                .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = colors.content, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = colors.content, maxLines = 1)
    }
}

/**
 * Selectable filter chip (e.g. "Tous", "PS5", "CIB").
 *
 * @param text Label.
 * @param selected Whether the filter is active.
 * @param onClick Click handler.
 * @param modifier Modifier applied to the chip.
 */
@Composable
fun VaultFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) Primary.copy(alpha = 0.15f) else Color.Transparent
    val border = if (selected) Primary.copy(alpha = 0.6f) else OutlineVariant.copy(alpha = 0.6f)
    val content = if (selected) Primary else OnSurfaceVariant
    Row(
        modifier =
            modifier
                .height(36.dp)
                .clip(VaultRadius.full)
                .background(container)
                .border(1.dp, border, VaultRadius.full)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = content, maxLines = 1)
    }
}

/**
 * Badge describing an item condition.
 *
 * @property label Uppercase label.
 * @property tone Color tone.
 */
data class ConditionBadge(
    val label: String,
    val tone: ChipTone,
)

/** Maps a domain condition to its badge — see design/DESIGN_SYSTEM.md §6. */
fun ItemCondition.toBadge(): ConditionBadge =
    when (this) {
        ItemCondition.NEW_SEALED -> ConditionBadge("SEALED", ChipTone.Error)
        ItemCondition.CIB -> ConditionBadge("CIB", ChipTone.Neutral)
        ItemCondition.CIB_NO_MANUAL -> ConditionBadge("CIB-", ChipTone.Neutral)
        ItemCondition.LOOSE -> ConditionBadge("LOOSE", ChipTone.Neutral)
        ItemCondition.MINT -> ConditionBadge("MINT", ChipTone.Success)
        ItemCondition.VERY_GOOD -> ConditionBadge("TRÈS BON", ChipTone.Neutral)
        ItemCondition.GOOD -> ConditionBadge("BON", ChipTone.Neutral)
        ItemCondition.FAIR -> ConditionBadge("CORRECT", ChipTone.Neutral)
        ItemCondition.POOR -> ConditionBadge("ABÎMÉ", ChipTone.Warning)
    }

private data class ChipColors(
    val container: Color,
    val content: Color,
    val border: Color?,
)

private fun toneColor(tone: ChipTone): Color =
    when (tone) {
        ChipTone.Neutral -> OnSurfaceVariant
        ChipTone.Primary -> Primary
        ChipTone.Secondary -> Secondary
        ChipTone.Success -> Success
        ChipTone.Warning -> Warning
        ChipTone.Error -> Error
        ChipTone.Info -> Info
    }

private fun chipColors(
    tone: ChipTone,
    variant: ChipVariant,
): ChipColors {
    val base = toneColor(tone)
    return when (variant) {
        ChipVariant.Soft ->
            when (tone) {
                ChipTone.Neutral -> ChipColors(SurfaceContainerHighest.copy(alpha = 0.7f), OnSurfaceVariant, null)
                ChipTone.Secondary -> ChipColors(SecondaryContainer.copy(alpha = 0.4f), Secondary, null)
                else -> ChipColors(base.copy(alpha = 0.15f), base, null)
            }
        ChipVariant.Solid ->
            when (tone) {
                ChipTone.Neutral -> ChipColors(SurfaceContainerHighest, OnSurface, null)
                ChipTone.Primary -> ChipColors(Primary, OnPrimary, null)
                ChipTone.Secondary -> ChipColors(SecondaryContainer, OnSecondaryContainer, null)
                else -> ChipColors(base, Color.Black, null)
            }
        ChipVariant.Outline -> ChipColors(Color.Transparent, base, base.copy(alpha = 0.5f))
    }
}
