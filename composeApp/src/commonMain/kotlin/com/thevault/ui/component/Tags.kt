@file:Suppress("MagicNumber") // Design tokens and geometry of the console mockups.

package com.thevault.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thevault.domain.model.Components
import com.thevault.domain.model.Region
import com.thevault.ui.theme.OverlayColors
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultRadius

/** LED colors. */
enum class LedColor {
    /** Green: fresh / synced. */
    Sync,

    /** Red: stale / error. */
    Power,

    /** Indigo: working. */
    Indigo,

    /** Off. */
    Off,
}

/**
 * Status LED with a glow and an optional engraved label.
 *
 * @param color LED color.
 * @param modifier Modifier.
 * @param label Optional label.
 * @param blink Blinks (work in progress).
 */
@Composable
fun StatusLed(
    color: LedColor,
    modifier: Modifier = Modifier,
    label: String? = null,
    blink: Boolean = false,
) {
    val c = Vault.colors
    val tint =
        when (color) {
            LedColor.Sync -> c.ledSync
            LedColor.Power -> c.ledPower
            LedColor.Indigo -> c.indigo
            LedColor.Off -> c.lineStrong
        }
    val alpha = rememberBlinkAlpha()
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(7.dp)
                .alpha(if (blink) alpha else 1f)
                .then(
                    if (color !=
                        LedColor.Off
                    ) {
                        Modifier.shadow(4.dp, VaultRadius.pill, ambientColor = tint, spotColor = tint)
                    } else {
                        Modifier
                    },
                ).clip(VaultRadius.pill)
                .background(tint),
        )
        if (label != null) Text(label.uppercase(), style = Vault.type.engrave.copy(letterSpacing = 1.4.sp), color = c.inkMuted)
    }
}

/** Alpha of a blinking LED (on 60 % of a 1.2 s cycle). */
@Composable
private fun rememberBlinkAlpha(): Float {
    val alpha by rememberInfiniteTransition(label = "led").animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                keyframes {
                    durationMillis = 1200
                    1f at 0
                    1f at 720
                    0.25f at 732
                    0.25f at 1200
                },
                RepeatMode.Restart,
                StartOffset(0),
            ),
        label = "blink",
    )
    return alpha
}

/**
 * Platform tag: manufacturer dot, short name in mono uppercase, optional region.
 *
 * @param platform Platform.
 * @param modifier Modifier.
 * @param region Optional region shown after a divider.
 * @param variant Optional region variant (FAH, UKV…) shown after the region.
 */
@Composable
fun PlatformTag(
    platform: String,
    modifier: Modifier = Modifier,
    region: Region? = null,
    variant: String? = null,
) {
    if (platform.isBlank()) return
    val c = Vault.colors
    Row(
        modifier
            .height(24.dp)
            .clip(VaultRadius.xs)
            .background(c.panel)
            .padding(start = 8.dp, end = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(7.dp).clip(VaultRadius.pill).background(platformColor(platform)))
        val style = Vault.type.monoTag.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Text(shortPlatformName(platform).uppercase(), style = style, color = c.ink, maxLines = 1)
        if (region != null && region != Region.OTHER) {
            Box(Modifier.width(1.dp).height(12.dp).background(c.line))
            Text(listOfNotNull(region.label, variant).joinToString(" · "), style = style, color = c.inkMuted)
        }
    }
}

/**
 * Four condition pips (Loose 1 · Complet 2 · Neuf 3 · Scellé 4).
 *
 * @param lit Lit pips.
 * @param modifier Modifier.
 * @param on Lit color.
 * @param off Empty color.
 */
@Composable
fun Pips(
    lit: Int,
    modifier: Modifier = Modifier,
    on: Color = Vault.colors.indigo,
    off: Color = Vault.colors.line,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(PIP_COUNT) { i ->
            Box(
                Modifier
                    .width(4.dp)
                    .height(10.dp)
                    .clip(VaultRadius.pill)
                    .background(if (i < lit) on else off),
            )
        }
    }
}

/**
 * Condition tag with pips; the overlay variant sits on covers.
 *
 * @param condition Condition.
 * @param modifier Modifier.
 * @param overlay Dark translucent variant for covers.
 * @param compact Hides the label (pips only).
 */
@Composable
fun ConditionTag(
    condition: Components,
    modifier: Modifier = Modifier,
    overlay: Boolean = false,
    compact: Boolean = false,
) {
    val c = Vault.colors
    val bg = if (overlay) OverlayColors.TagBackground else c.panel
    val fg = if (overlay) OverlayColors.TagContent else c.ink
    Row(
        modifier
            .height(
                if (overlay) 20.dp else 24.dp,
            ).clip(VaultRadius.xs)
            .background(bg)
            .padding(horizontal = if (overlay) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (overlay) Pips(condition.pips, on = OverlayColors.PipOn, off = OverlayColors.PipOff) else Pips(condition.pips)
        if (!compact) Text(condition.formatLabel, style = Vault.type.small.copy(fontSize = 11.sp), color = fg, maxLines = 1)
    }
}

/**
 * Data source with a LED: « eBay il y a 2 h », « Fiche IGDB ».
 *
 * @param text Source name.
 * @param modifier Modifier.
 * @param updated Optional age.
 * @param stale Red LED when the data is old or missing.
 */
@Composable
fun SourceBadge(
    text: String,
    modifier: Modifier = Modifier,
    updated: String? = null,
    stale: Boolean = false,
) {
    val c = Vault.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusLed(if (stale) LedColor.Power else LedColor.Sync)
        Text(text, style = Vault.type.small, color = c.inkMuted, maxLines = 1)
        if (updated != null) Text(updated, style = Vault.type.monoTag, color = c.inkFaint, maxLines = 1)
    }
}

/**
 * Price change: ▲ green / ▼ red in mono, e.g. « ▲ +312 € (6,8 %) 30 j ».
 *
 * @param text Change text without sign (the arrow and sign are added).
 * @param up Direction.
 * @param modifier Modifier.
 * @param period Optional muted period ("30 j").
 * @param pill Recessed pill variant.
 * @param small 11sp variant.
 */
@Composable
fun PriceDelta(
    text: String,
    up: Boolean,
    modifier: Modifier = Modifier,
    period: String? = null,
    pill: Boolean = false,
    small: Boolean = false,
) {
    val c = Vault.colors
    val style =
        Vault.type.monoTag.copy(
            fontSize = if (small) 11.sp else 13.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
    Row(
        modifier.then(
            if (pill) {
                Modifier
                    .height(28.dp)
                    .clip(VaultRadius.pill)
                    .background(c.well)
                    .padding(horizontal = 11.dp)
            } else {
                Modifier
            },
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text((if (up) "▲ +" else "▼ −") + text, style = style, color = if (up) c.gain else c.loss, maxLines = 1)
        if (period !=
            null
        ) {
            Text(period, style = style.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = c.inkMuted)
        }
    }
}

private const val PIP_COUNT = 4
