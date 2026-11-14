@file:Suppress("MagicNumber") // Design tokens and geometry of the console mockups.

package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thevault.domain.model.ItemCondition
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultRadius
import com.thevault.ui.theme.molded
import com.thevault.ui.theme.well

/**
 * Molded panel (radius 22), optionally clickable.
 *
 * @param modifier Modifier.
 * @param padding Inner padding.
 * @param color Surface color (panel by default).
 * @param onClick Optional callback.
 * @param content Content.
 */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    color: Color = Vault.colors.panel,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .then(if (onClick != null) Modifier.pressable(interaction, 0.98f) else Modifier)
            .molded(VaultRadius.lg, color)
            .then(
                if (onClick !=
                    null
                ) {
                    Modifier.clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            ).padding(padding),
        content = content,
    )
}

/**
 * Counter tile: big number above a small label.
 *
 * @param value Number.
 * @param label Label.
 * @param modifier Modifier.
 * @param onClick Optional callback.
 */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Panel(modifier, PaddingValues(14.dp), onClick = onClick) {
        Text(
            value,
            style = Vault.type.displayXl.copy(fontSize = 24.sp, lineHeight = 26.sp, letterSpacing = (-0.6).sp),
            color = Vault.colors.ink,
            maxLines = 1,
        )
        Text(label, style = Vault.type.small, color = Vault.colors.inkMuted, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    }
}

/**
 * List row: small cover, title and metadata, trailing price.
 *
 * @param title Title.
 * @param modifier Modifier.
 * @param meta Secondary line.
 * @param trailing Trailing mono text.
 * @param cover Optional cover (url, platform, condition).
 * @param onClick Optional callback.
 * @param trailingContent Optional content under the trailing text.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    trailing: String? = null,
    cover: Triple<String?, String, ItemCondition?>? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val c = Vault.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(VaultRadius.md)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (cover != null) GameCover(cover.first, title, cover.second, Modifier.width(44.dp), CoverSize.Xs)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Vault.type.bodyStrong, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (meta != null) Text(meta, style = Vault.type.caption, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (trailing != null) Text(trailing, style = Vault.type.monoPrice, color = c.ink, maxLines = 1)
            trailingContent?.invoke()
        }
    }
}

/**
 * Platform tile: manufacturer band, name, metadata, optional share track, value and change.
 *
 * @param name Platform name.
 * @param meta Metadata line ("Nintendo · 42 jeux").
 * @param band Band color.
 * @param value Value text.
 * @param onClick Callback.
 * @param modifier Modifier.
 * @param share Optional fill of the track (0..1).
 * @param change Optional change below the value.
 */
@Composable
fun PlatformTile(
    name: String,
    meta: String,
    band: Color,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    share: Float? = null,
    change: @Composable (() -> Unit)? = null,
) {
    val c = Vault.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 64.dp)
            .pressable(interaction, 0.98f)
            .molded(VaultRadius.lg)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(6.dp).fillMaxHeight().background(band))
        Column(Modifier.weight(1f).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                name,
                style = Vault.type.displayM.copy(fontSize = 17.sp, lineHeight = 19.sp),
                color = c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(meta, style = Vault.type.small, color = c.inkMuted, maxLines = 1)
            if (share != null) Meter(share, band, Modifier.padding(top = 3.dp).width(150.dp))
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = Vault.type.monoPrice, color = c.ink, maxLines = 1)
            change?.invoke()
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = c.inkFaint,
            modifier = Modifier.padding(end = 10.dp).size(20.dp),
        )
    }
}

/**
 * Thin recessed track with a colored fill.
 *
 * @param fraction Filled part, 0..1.
 * @param color Fill color.
 * @param modifier Modifier.
 * @param height Track height.
 */
@Composable
fun Meter(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 4.dp,
) {
    Box(modifier.height(height).clip(VaultRadius.pill).background(Vault.colors.well)) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(VaultRadius.pill)
                .background(color),
        )
    }
}

/**
 * Empty state: recessed icon, title, explanation and optional action.
 *
 * @param icon Icon.
 * @param title Title.
 * @param modifier Modifier.
 * @param message Optional explanation.
 * @param action Optional action label.
 * @param onAction Action callback.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val c = Vault.colors
    Column(
        modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(72.dp).well(VaultRadius.lg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = c.indigoInk, modifier = Modifier.size(32.dp))
        }
        Text(title, style = Vault.type.displayM, color = c.ink, textAlign = TextAlign.Center)
        if (message != null) Text(message, style = Vault.type.caption, color = c.inkMuted, textAlign = TextAlign.Center)
        if (action != null) VaultButton(action, onAction, Modifier.padding(top = 8.dp))
    }
}

/** Centered progress indicator. */
@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Vault.colors.indigo, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
    }
}
