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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.thevault.ui.theme.CardBorderColor
import com.thevault.ui.theme.SurfaceContainerLow
import com.thevault.ui.theme.VaultRadius

/**
 * Card: « Carte » background, « Bordure » border, 18dp radius.
 *
 * @param modifier Modifier applied to the card.
 * @param shape Corner shape.
 * @param contentPadding Inner padding.
 * @param color Background color.
 * @param borderColor Border color.
 * @param onClick Optional click handler.
 * @param content Card content.
 */
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    shape: Shape = VaultRadius.card,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    color: Color = SurfaceContainerLow,
    borderColor: Color = CardBorderColor,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .clip(shape)
                .background(color)
                .border(1.dp, borderColor, shape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(contentPadding),
        content = content,
    )
}
