@file:Suppress("MagicNumber") // Design tokens and geometry of the console mockups.

package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.thevault.domain.model.ItemCondition
import com.thevault.ui.theme.OverlayColors
import com.thevault.ui.theme.Vault

/**
 * Cartridge silhouette: rounded top corners, chamfered bottom corners.
 *
 * @param radius Top corner radius.
 * @param chamfer Bottom chamfer.
 */
class CartridgeShape(
    private val radius: Dp,
    private val chamfer: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = with(density) { radius.toPx() }
        val c = with(density) { chamfer.toPx() }
        val path =
            Path().apply {
                moveTo(0f, size.height - c)
                lineTo(0f, r)
                arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false)
                lineTo(size.width - r, 0f)
                arcTo(Rect(size.width - 2 * r, 0f, size.width, 2 * r), 270f, 90f, false)
                lineTo(size.width, size.height - c)
                lineTo(size.width - c, size.height)
                lineTo(c, size.height)
                close()
            }
        return Outline.Generic(path)
    }
}

/**
 * Cover sizes.
 *
 * @property radius Top radius.
 * @property chamfer Bottom chamfer.
 * @property band Height of the manufacturer band.
 * @property titleSize Title size of placeholder covers (0 hides it).
 */
enum class CoverSize(
    val radius: Dp,
    val chamfer: Dp,
    val band: Dp,
    val titleSize: TextUnit,
) {
    /** List rows. */
    Xs(6.dp, 5.dp, 3.dp, 0.sp),

    /** Grid cards. */
    M(10.dp, 9.dp, 4.dp, 11.sp),

    /** Detail screens. */
    L(14.dp, 14.dp, 4.dp, 15.sp),
}

/**
 * Game cover: manufacturer band on top, condition tag, cartridge corners, and a title strip
 * when there is no image.
 *
 * @param url Image URL.
 * @param title Title (accessibility and placeholder).
 * @param platform Platform (band color).
 * @param modifier Modifier (set the width).
 * @param size Cover size.
 * @param condition Optional condition shown as an overlay tag.
 * @param missing Greyed-out cover (not owned).
 */
@Composable
fun GameCover(
    url: String?,
    title: String,
    platform: String,
    modifier: Modifier = Modifier,
    size: CoverSize = CoverSize.M,
    condition: ItemCondition? = null,
    missing: Boolean = false,
) {
    val shape = remember(size) { CartridgeShape(size.radius, size.chamfer) }
    Box(
        modifier
            .aspectRatio(COVER_RATIO)
            .alpha(if (missing) MISSING_ALPHA else 1f)
            .clip(shape)
            .background(Vault.colors.coverBlank),
    ) {
        if (url.isNullOrBlank()) {
            CoverPlaceholder(title, size)
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = "Jaquette : $title",
                contentScale = ContentScale.Crop,
                colorFilter = if (missing) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                modifier = Modifier.fillMaxSize(),
                error = { CoverPlaceholder(title, size) },
            )
        }
        Box(Modifier.fillMaxWidth().height(size.band).background(platformColor(platform)))
        if (condition != null && size != CoverSize.Xs) {
            ConditionTag(
                condition,
                Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 6.dp),
                overlay = true,
                compact =
                    size == CoverSize.M,
            )
        }
    }
}

@Composable
private fun CoverPlaceholder(
    title: String,
    size: CoverSize,
) {
    Box(Modifier.fillMaxSize().background(Vault.colors.coverBlank)) {
        if (size.titleSize.value > 0f) {
            val bottom = if (size == CoverSize.L) 20.dp else 13.dp
            Text(
                title,
                style = Vault.type.displayM.copy(fontSize = size.titleSize, lineHeight = size.titleSize * TITLE_LINE_HEIGHT),
                color = OverlayColors.CoverTitle,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(OverlayColors.CoverStrip)
                        .padding(start = 9.dp, end = 9.dp, top = 7.dp, bottom = bottom),
            )
        }
    }
}

/**
 * Data of a collection card.
 *
 * @property title Title.
 * @property platform Platform.
 * @property coverUrl Cover URL.
 * @property condition Condition.
 * @property value Value, null when unknown.
 * @property changePercent 30-day change of the quote, null when unknown.
 * @property caption Mono line under the title (short platform, or manufacturer for consoles).
 */
data class CardData(
    val title: String,
    val platform: String,
    val coverUrl: String?,
    val condition: ItemCondition,
    val value: Double?,
    val changePercent: Double? = null,
    val caption: String = shortPlatformName(platform),
)

/**
 * Collection card: cover, title on two lines, platform and value in mono, 30-day change.
 *
 * @param data Card data.
 * @param onClick Callback.
 * @param modifier Modifier.
 * @param missing Greyed-out (wishlist).
 */
@Composable
fun CollectionCard(
    data: CardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    missing: Boolean = false,
) {
    val c = Vault.colors
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier.pressable(interaction, 0.97f).clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GameCover(data.coverUrl, data.title, data.platform, Modifier.fillMaxWidth(), CoverSize.M, data.condition, missing)
        Text(
            data.title,
            style = Vault.type.label.copy(lineHeight = 16.sp),
            color = c.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp).heightIn(min = 32.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                data.caption.uppercase(),
                style = Vault.type.monoTag,
                color = c.inkMuted,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(formatEuroOrDash(data.value), style = Vault.type.monoPrice.copy(fontSize = 13.sp), color = c.ink, maxLines = 1)
        }
        data.changePercent?.takeIf { it != 0.0 }?.let { PriceDelta(formatWholePercent(it), it > 0, small = true) }
    }
}

private const val COVER_RATIO = 3f / 4f
private const val MISSING_ALPHA = 0.45f
private const val TITLE_LINE_HEIGHT = 1.15f
