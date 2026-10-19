package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.thevault.ui.theme.CardBorderColor
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceContainerLow
import com.thevault.ui.theme.VaultRadius

/**
 * Base container of the UI — mirrors `Card.tsx` on the web.
 *
 * `surface-low` background, 16dp radius and a discreet `outline-variant` border.
 *
 * @param modifier Modifier applied to the card.
 * @param shape Corner shape, [VaultRadius.xl] by default.
 * @param contentPadding Inner padding.
 * @param highlighted Draws a primary border, used for the active/selected state.
 * @param onClick Optional click handler; makes the card clickable.
 * @param content Card content.
 */
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    shape: Shape = VaultRadius.xl,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val borderColor = if (highlighted) Primary.copy(alpha = 0.6f) else CardBorderColor
    Column(
        modifier =
            modifier
                .clip(shape)
                .background(SurfaceContainerLow)
                .border(1.dp, borderColor, shape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(contentPadding),
        content = content,
    )
}
