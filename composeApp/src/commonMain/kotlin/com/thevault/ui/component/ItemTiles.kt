package com.thevault.ui.component

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.thevault.domain.model.ItemCondition
import com.thevault.ui.theme.Error
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Outline
import com.thevault.ui.theme.Success
import com.thevault.ui.theme.VaultRadius

/**
 * Cover image with the manufacturer placeholder as fallback, framed like the design
 * (radius 10, 1dp border).
 *
 * @param url Cover URL, or null.
 * @param platform Platform used for the placeholder.
 * @param contentDescription Accessibility description.
 * @param modifier Modifier applied to the cover (size it from the caller).
 * @param shape Corner shape.
 */
@Composable
fun ItemCover(
    url: String?,
    platform: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = VaultRadius.md,
) {
    val framed = modifier.clip(shape).border(1.dp, Outline, shape)
    if (url.isNullOrBlank()) {
        BrandPlaceholder(platform, framed)
        return
    }
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = framed.background(coverTint(platform)),
        error = { BrandPlaceholder(platform, Modifier.fillMaxSize()) },
    )
}

/**
 * Gain or loss against the purchase price ("+179 €" green, "−20 €" orange).
 *
 * @param value Estimated value.
 * @param purchasePrice Purchase price.
 * @param modifier Modifier applied to the text.
 */
@Composable
fun ValueDelta(
    value: Double?,
    purchasePrice: Double?,
    modifier: Modifier = Modifier,
) {
    if (value == null || purchasePrice == null) return
    val delta = value - purchasePrice
    Text(
        formatSignedEuro(delta),
        style = MaterialTheme.typography.bodySmall,
        color = if (delta >= 0) Success else Error,
        maxLines = 1,
        modifier = modifier,
    )
}

/**
 * Data shown by a collection tile.
 *
 * @property title Item title.
 * @property platform Platform.
 * @property coverUrl Cover URL.
 * @property condition Physical condition.
 * @property value Estimated value.
 * @property purchasePrice Purchase price.
 */
data class TileData(
    val title: String,
    val platform: String,
    val coverUrl: String?,
    val condition: ItemCondition,
    val value: Double?,
    val purchasePrice: Double?,
)

/**
 * Game tile: 3:4 cover with the format badge, title on two lines, value and gain / loss.
 *
 * @param data Tile data.
 * @param onClick Click handler.
 * @param modifier Modifier applied to the tile.
 * @param square Square cover with the platform badge (consoles, accessories).
 */
@Composable
fun ItemTile(
    data: TileData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    square: Boolean = false,
) {
    Column(modifier.clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(if (square) 1f else 3f / 4f)) {
            ItemCover(data.coverUrl, data.platform, data.title, Modifier.fillMaxSize(), if (square) VaultRadius.lg else VaultRadius.md)
            FormatBadge(data.condition, Modifier.align(Alignment.TopStart).padding(6.dp), compact = true)
            if (square) PlatformBadge(data.platform, Modifier.align(Alignment.BottomStart).padding(6.dp), compact = true)
        }
        Text(
            data.title,
            style = MaterialTheme.typography.titleSmall,
            color = OnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 34.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatEuroOrDash(data.value, decimals = 2),
                style = MaterialTheme.typography.titleSmall,
                color = if (data.value != null) OnSurface else OnSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (!square) ValueDelta(data.value, data.purchasePrice)
        }
    }
}
