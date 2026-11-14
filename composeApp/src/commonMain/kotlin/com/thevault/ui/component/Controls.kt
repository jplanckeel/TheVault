@file:Suppress("MagicNumber") // Design tokens and geometry of the console mockups.

package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultRadius
import com.thevault.ui.theme.molded
import com.thevault.ui.theme.well

/**
 * Filter chip: raised panel, inverted (ink) when selected, optional mono count.
 *
 * @param text Label.
 * @param selected Whether it is selected.
 * @param onClick Callback.
 * @param modifier Modifier.
 * @param count Optional count shown after the label.
 */
@Composable
fun VaultChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    val c = Vault.colors
    val shape = VaultRadius.pill
    val interaction = remember { MutableInteractionSource() }
    val surface = if (selected) Modifier.clip(shape).background(c.ink) else Modifier.molded(shape)
    Row(
        modifier
            .pressable(interaction, 0.95f)
            .height(36.dp)
            .then(surface)
            .clickable(interaction, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        val content = if (selected) c.shell else c.ink
        Text(
            text,
            style = Vault.type.label.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = content,
            maxLines = 1,
        )
        if (count != null) {
            Text("$count", style = Vault.type.monoTag, color = if (selected) c.shell.copy(alpha = 0.75f) else c.inkMuted)
        }
    }
}

/**
 * One option of a [SegmentedControl].
 *
 * @property label Label.
 * @property sub Optional mono line below (a price).
 */
data class Segment(
    val label: String,
    val sub: String? = null,
)

/**
 * Segmented control: recessed track, the active segment is a raised keycap.
 *
 * @param segments Options.
 * @param selected Index of the active option.
 * @param onSelect Callback with the index.
 * @param modifier Modifier.
 * @param large 52dp segments (prices).
 */
@Composable
fun SegmentedControl(
    segments: List<Segment>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val c = Vault.colors
    Row(modifier.fillMaxWidth().well(VaultRadius.md).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        segments.forEachIndexed { index, segment ->
            val on = index == selected
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = if (large) 52.dp else 36.dp)
                    .then(if (on) Modifier.shadow(1.dp, VaultRadius.segment).clip(VaultRadius.segment).background(c.keycap) else Modifier)
                    .clip(VaultRadius.segment)
                    .clickable(role = Role.Tab) { onSelect(index) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
            ) {
                val color = if (on) c.ink else c.inkMuted
                val labelStyle = if (large) Vault.type.small else Vault.type.label
                Text(segment.label, style = labelStyle, color = color, maxLines = 1)
                if (segment.sub !=
                    null
                ) {
                    Text(
                        segment.sub,
                        style = Vault.type.monoPrice.copy(fontSize = Vault.type.label.fontSize),
                        color = color,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Search field: recessed, 48dp, magnifier and clear button.
 *
 * @param query Text.
 * @param onQueryChange Callback.
 * @param placeholder Placeholder.
 * @param modifier Modifier.
 * @param focusRequester Optional focus requester.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val c = Vault.colors
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = Vault.type.body.copy(color = c.ink),
        cursorBrush = SolidColor(c.indigo),
        modifier =
            modifier
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner -> SearchFrame(query, placeholder, focused, onQueryChange, inner) },
    )
}

@Composable
private fun SearchFrame(
    query: String,
    placeholder: String,
    focused: Boolean,
    onQueryChange: (String) -> Unit,
    inner: @Composable () -> Unit,
) {
    val c = Vault.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .well(VaultRadius.md)
            .then(if (focused) Modifier.border(2.dp, c.focus, VaultRadius.md) else Modifier)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = c.inkMuted, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(placeholder, style = Vault.type.body, color = c.inkMuted, maxLines = 1)
            inner()
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Effacer",
                tint = c.inkMuted,
                modifier = Modifier.size(20.dp).clickableNoRipple { onQueryChange("") },
            )
        }
    }
}

/**
 * Form field: engraved label above a recessed input.
 *
 * @param label Label.
 * @param value Text.
 * @param onValueChange Callback.
 * @param modifier Modifier.
 * @param placeholder Placeholder.
 * @param keyboardOptions Keyboard options.
 * @param singleLine Single line input.
 * @param suffix Optional trailing text ("€").
 */
@Composable
fun VaultTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    suffix: String? = null,
) {
    val c = Vault.colors
    var focused by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Engrave(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            textStyle = Vault.type.body.copy(color = c.ink),
            cursorBrush = SolidColor(c.indigo),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .well(VaultRadius.md)
                        .then(if (focused) Modifier.border(2.dp, c.focus, VaultRadius.md) else Modifier)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, style = Vault.type.body, color = c.inkMuted)
                        inner()
                    }
                    if (suffix != null) Text(suffix, style = Vault.type.monoPrice, color = c.inkMuted)
                }
            },
        )
    }
}
