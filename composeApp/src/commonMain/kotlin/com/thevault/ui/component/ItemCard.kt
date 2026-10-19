package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.thevault.ui.theme.CardBorderColor
import com.thevault.ui.theme.CoverScrimColor
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceContainerHigh
import com.thevault.ui.theme.SurfaceContainerLow
import com.thevault.ui.theme.TabularNumbers
import com.thevault.ui.theme.VaultRadius

/**
 * Cover image with the brand placeholder as fallback (no URL or loading error).
 *
 * @param url Cover URL, or null.
 * @param platform Platform used for the placeholder.
 * @param contentDescription Accessibility description.
 * @param modifier Modifier applied to the image (size it from the caller).
 */
@Composable
fun ItemCover(
    url: String?,
    platform: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    if (url.isNullOrBlank()) {
        BrandPlaceholder(platform, modifier)
        return
    }
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(SurfaceContainerHigh),
        error = { BrandPlaceholder(platform, Modifier.fillMaxSize()) },
    )
}

/**
 * Collection grid card — mirrors `GameCard.tsx`: 3:4 cover, condition badge,
 * platform label, title, price.
 *
 * @param title Item title.
 * @param platform Platform (shown uppercase in primary).
 * @param coverUrl Cover URL, or null.
 * @param onClick Click handler.
 * @param modifier Modifier applied to the card.
 * @param badge Optional badge displayed top-right.
 * @param price Optional estimated value.
 * @param trailingIcon Optional icon displayed next to the price (trend, wishlist…).
 */
@Composable
fun ItemCard(
    title: String,
    platform: String,
    coverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: ConditionBadge? = null,
    price: Double? = null,
    trailingIcon: ImageVector? = null,
) {
    Column(
        modifier =
            modifier
                .clip(VaultRadius.xxl)
                .background(SurfaceContainerLow)
                .border(1.dp, CardBorderColor, VaultRadius.xxl)
                .clickable(onClick = onClick),
    ) {
        CoverWithBadge(title, platform, coverUrl, badge)
        CardFooter(title, platform, price, trailingIcon)
    }
}

@Composable
private fun CoverWithBadge(
    title: String,
    platform: String,
    coverUrl: String?,
    badge: ConditionBadge?,
) {
    Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
        ItemCover(coverUrl, platform, title, Modifier.fillMaxSize())
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, CoverScrimColor))),
        )
        if (badge != null) {
            VaultChip(
                text = badge.label,
                tone = badge.tone,
                variant = ChipVariant.Solid,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
        }
    }
}

@Composable
private fun CardFooter(
    title: String,
    platform: String,
    price: Double?,
    trailingIcon: ImageVector?,
) {
    Column(Modifier.padding(12.dp)) {
        Text(
            platform.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(title, style = MaterialTheme.typography.titleSmall, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.size(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatEuroOrDash(price),
                style = MaterialTheme.typography.titleSmall.merge(TabularNumbers),
                color = if (price != null) OnSurface else OnSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (trailingIcon != null) {
                Icon(trailingIcon, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * Compact list row — thumbnail, title, subtitle and trailing value.
 * Used on the dashboard and in lists where a grid is too heavy.
 *
 * @param title Item title.
 * @param subtitle Secondary line (platform · condition).
 * @param platform Platform used for the placeholder.
 * @param coverUrl Thumbnail URL, or null.
 * @param onClick Click handler.
 * @param modifier Modifier applied to the row.
 * @param trailing Optional trailing content (price, chip…).
 */
@Composable
fun ItemRow(
    title: String,
    subtitle: String,
    platform: String,
    coverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(VaultRadius.lg)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ItemCover(coverUrl, platform, title, Modifier.size(48.dp).clip(VaultRadius.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            trailing()
        }
    }
}
