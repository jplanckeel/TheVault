@file:Suppress("MagicNumber") // Design tokens and geometry of the console mockups.

package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultRadius

/** « La cartouche » mark: a cartridge with a vault dial (64×64 viewport). */
private const val MARK =
    "M16 4H48A6 6 0 0 1 54 10V48L48 58H16L10 48V10A6 6 0 0 1 16 4Z " +
        "M21 12H43A4 4 0 0 1 47 16V36A4 4 0 0 1 43 40H21A4 4 0 0 1 17 36V16A4 4 0 0 1 21 12Z " +
        "M23 26a9 9 0 1 0 18 0a9 9 0 1 0 -18 0Z M27 26a5 5 0 1 0 10 0a5 5 0 1 0 -10 0Z " +
        "M30 26a2 2 0 1 0 4 0a2 2 0 1 0 -4 0Z M21 45h4v6h-4Z M27 45h4v6h-4Z M33 45h4v6h-4Z M39 45h4v6h-4Z"

/** Vector of the mark, filled with [color]. */
fun logoMark(color: Color): ImageVector =
    ImageVector
        .Builder("TheVaultMark", 64.dp, 64.dp, 64f, 64f)
        .addPath(PathParser().parsePathString(MARK).toNodes(), fill = SolidColor(color), pathFillType = PathFillType.EvenOdd)
        .build()

/**
 * Logo: indigo mark, « TheVault » wordmark and the four button dots.
 *
 * @param modifier Modifier.
 * @param size Mark size.
 * @param wordmark Shows the wordmark and dots.
 */
@Composable
fun VaultLogo(
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    wordmark: Boolean = true,
) {
    val c = Vault.colors
    val mark = remember(c.indigo) { logoMark(c.indigo) }
    Row(
        modifier.semantics(mergeDescendants = true) { contentDescription = "TheVault" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(mark, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(size))
        if (wordmark) {
            Text(
                "TheVault",
                style = Vault.type.displayM.copy(fontSize = (size.value * WORD_RATIO).sp, letterSpacing = (-0.8).sp),
                color = c.ink,
            )
            SnesDots(size / 2)
        }
    }
}

/**
 * The four colored face-button dots (X top, Y left, A right, B bottom).
 *
 * @param size Size of the group.
 */
@Composable
fun SnesDots(size: Dp) {
    val c = Vault.colors
    val d = size * DOT_RATIO
    val m = (size - d) / 2
    Box(Modifier.size(size)) {
        listOf(
            Triple(c.snesX, m, 0.dp),
            Triple(c.snesY, 0.dp, m),
            Triple(c.snesA, size - d, m),
            Triple(c.snesB, m, size - d),
        ).forEach { (color, x, y) ->
            Box(
                Modifier
                    .offset(x, y)
                    .size(d)
                    .clip(VaultRadius.pill)
                    .background(color),
            )
        }
    }
}

private const val WORD_RATIO = 0.66f
private const val DOT_RATIO = 0.36f
