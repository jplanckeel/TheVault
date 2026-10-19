package com.thevault.ui.screen.additem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.RemoteGame
import com.thevault.ui.component.ItemCover
import com.thevault.ui.component.VaultFilterChip
import com.thevault.ui.theme.CardBorderColor
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceContainerHigh
import com.thevault.ui.theme.VaultRadius

/**
 * Labelled text input — `surface-high`, 12dp radius, primary border on focus-less edit.
 *
 * @param label Uppercase label.
 * @param value Current value.
 * @param onValueChange Value change callback.
 * @param modifier Modifier applied to the field.
 * @param placeholder Optional placeholder.
 * @param keyboardType Keyboard type.
 * @param singleLine Whether the field is single line.
 * @param trailing Optional trailing content (progress indicator…).
 */
@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = OnSurface),
            cursorBrush = SolidColor(Primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(VaultRadius.lg)
                        .background(SurfaceContainerHigh)
                        .border(1.dp, CardBorderColor, VaultRadius.lg)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant.copy(alpha = 0.6f))
                        }
                        inner()
                    }
                    trailing?.invoke()
                }
            },
        )
    }
}

/**
 * Uppercase field label.
 *
 * @param text Label text.
 */
@Composable
fun FieldLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
}

/**
 * Single choice among a few options, rendered as filter chips.
 *
 * @param T Option type.
 * @param label Uppercase label.
 * @param options Available options.
 * @param selected Selected option.
 * @param optionLabel Label of an option.
 * @param onSelected Selection callback.
 */
@Composable
fun <T> ChoiceField(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option ->
                VaultFilterChip(optionLabel(option), option == selected, { onSelected(option) })
            }
        }
    }
}

/**
 * Five-star rating selector; tapping the current rating clears it.
 *
 * @param rating Current rating (0-5).
 * @param onRatingChange Rating change callback.
 */
@Composable
fun RatingField(
    rating: Int,
    onRatingChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Note personnelle")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (star in 1..MAX_RATING) {
                val filled = star <= rating
                Icon(
                    if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "$star étoile(s)",
                    tint = if (filled) Primary else OnSurfaceVariant.copy(alpha = 0.5f),
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(VaultRadius.md)
                            .clickable { onRatingChange(if (rating == star) 0 else star) }
                            .padding(6.dp),
                )
            }
        }
    }
}

private const val MAX_RATING = 5

/**
 * Horizontal list of IGDB search results.
 *
 * @param results Search results.
 * @param onSelect Called when a result is picked.
 */
@Composable
fun RemoteResults(
    results: List<RemoteGame>,
    onSelect: (RemoteGame) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Résultats IGDB")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(results) { game -> RemoteGameCard(game) { onSelect(game) } }
        }
    }
}

@Composable
private fun RemoteGameCard(
    game: RemoteGame,
    onClick: () -> Unit,
) {
    val platform = game.platformNames.firstOrNull().orEmpty()
    Column(Modifier.width(112.dp).clip(VaultRadius.lg).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(VaultRadius.lg)) {
            ItemCover(game.coverUrl, platform, game.title, Modifier.fillMaxSize())
        }
        Text(
            game.title,
            style = MaterialTheme.typography.titleSmall,
            color = OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            listOfNotNull(platform.ifBlank { null }, game.releaseYear?.toString()).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(Modifier.height(4.dp))
    }
}
