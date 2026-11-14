package com.thevault.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thevault.ui.theme.Vault

/** Clickable without ripple (text links). */
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)

/**
 * Engraved label: Archivo Expanded 10/12, uppercase, wide tracking.
 *
 * @param text Label.
 * @param modifier Modifier.
 * @param color Text color (muted ink by default).
 */
@Composable
fun Engrave(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Vault.colors.inkMuted,
) {
    Text(text.uppercase(), style = Vault.type.engrave, color = color, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/**
 * Root screen header: engraved eyebrow, 28/800 title and trailing key buttons.
 *
 * @param title Title.
 * @param modifier Modifier.
 * @param eyebrow Optional engraved line above the title.
 * @param leading Optional leading button (back).
 * @param actions Trailing buttons.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    leading: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (eyebrow != null) Engrave(eyebrow)
            Text(title, style = Vault.type.displayL, color = Vault.colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * Section title (18/800) with an optional link on the right.
 *
 * @param title Title.
 * @param modifier Modifier.
 * @param action Optional link label.
 * @param onAction Link callback.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = Vault.type.displayM, color = Vault.colors.ink, modifier = Modifier.weight(1f))
        if (action != null) LinkText(action, onAction)
    }
}

/**
 * Indigo text link (« Tout voir »).
 *
 * @param text Label.
 * @param onClick Callback.
 * @param modifier Modifier.
 */
@Composable
fun LinkText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = Vault.type.label,
        color = Vault.colors.indigoInk,
        modifier = modifier.clickableNoRipple(onClick).padding(vertical = 4.dp),
    )
}
