@file:Suppress("MagicNumber") // Design tokens and geometry of the console mockups.

package com.thevault.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thevault.ui.theme.Vault

/**
 * Sparkline drawn on a recessed « screen »: baseline, three ticks, indigo line and a red
 * square on the last point. Draws nothing with fewer than two points.
 *
 * @param points Values, oldest first.
 * @param modifier Modifier.
 * @param height Chart height.
 * @param description Accessibility description.
 */
@Composable
fun Sparkline(
    points: List<Double>,
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
    description: String = "Évolution de la valeur",
) {
    val line = Vault.colors.indigoInk
    val base = Vault.colors.line
    val tip = Vault.colors.snesA
    Canvas(modifier.fillMaxWidth().height(height).semantics { contentDescription = description }) {
        val w = size.width
        val h = size.height
        drawLine(base, Offset(0f, h - 0.5f), Offset(w, h - 0.5f), strokeWidth = 1.dp.toPx())
        for (i in 1 until TICKS) drawLine(base, Offset(w * i / TICKS, h - 6.dp.toPx()), Offset(w * i / TICKS, h), strokeWidth = 1.dp.toPx())
        if (points.size < 2) return@Canvas
        val pad = 6.dp.toPx()
        val min = points.min()
        val span = (points.max() - min).takeIf { it > 0.0 } ?: 1.0
        val coords =
            points.mapIndexed { i, p ->
                Offset(pad + i * (w - 2 * pad) / (points.size - 1), (pad + (1 - (p - min) / span) * (h - 2 * pad)).toFloat())
            }
        val path = Path().apply { coords.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
        drawPath(path, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val last = coords.last()
        val r = 4.dp.toPx()
        drawRoundRect(
            tip,
            Offset(last.x - r, last.y - r),
            Size(2 * r, 2 * r),
            androidx.compose.ui.geometry
                .CornerRadius(r),
        )
    }
}

private const val TICKS = 4
