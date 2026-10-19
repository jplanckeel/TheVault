package com.thevault.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LogoGradient = Brush.linearGradient(listOf(Color(0xFFA78BFF), Color(0xFF6A3FE0)))
private val CartridgeBody = Color(0xFFF8F5FF)
private val CartridgeGroove = Color(0xFFDCD2FA)
private val CartridgeLabel = Color(0xFF2B1675)
private val CartridgeV = Color(0xFFC4AEFF)
private val CartridgeContacts = Color(0xFFB7A6EE)
private const val GLYPH_RATIO = 0.86f
private const val VIEWPORT = 100f

/**
 * TheVault logo « La cartouche »: a light game cartridge marked with a V on the violet
 * gradient of the app icon.
 *
 * @param modifier Modifier applied to the logo.
 * @param size Logo size.
 */
@Composable
fun VaultLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.224f)).background(LogoGradient),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * GLYPH_RATIO)) { drawCartridge() }
    }
}

private fun DrawScope.drawCartridge() {
    val k = this.size.width / VIEWPORT
    fun rect(x: Float, y: Float, w: Float, h: Float, r: Float, color: Color) =
        drawRoundRect(color, Offset(x * k, y * k), Size(w * k, h * k), CornerRadius(r * k))
    val body =
        Path().apply {
            moveTo(32 * k, 16 * k)
            lineTo(64 * k, 16 * k)
            lineTo(72 * k, 24 * k)
            lineTo(72 * k, 84 * k)
            lineTo(28 * k, 84 * k)
            lineTo(28 * k, 16 * k)
            close()
        }
    drawPath(body, CartridgeBody)
    rect(36f, 20f, 26f, 2.2f, 1.1f, CartridgeGroove)
    rect(36f, 25f, 26f, 2.2f, 1.1f, CartridgeGroove)
    rect(34f, 32f, 32f, 32f, 6f, CartridgeLabel)
    val v =
        Path().apply {
            moveTo(42 * k, 41 * k)
            lineTo(50 * k, 56 * k)
            lineTo(58 * k, 41 * k)
        }
    drawPath(v, CartridgeV, style = Stroke(6 * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
    listOf(37f, 44f, 51f, 58f).forEach { x -> rect(x, 71f, 4f, 7f, 1.2f, CartridgeContacts) }
}
