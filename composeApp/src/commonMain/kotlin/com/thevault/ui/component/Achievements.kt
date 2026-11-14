package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thevault.ui.theme.OverlayColors
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultRadius
import com.thevault.ui.theme.faceShadow
import com.thevault.ui.theme.floatShadow

/**
 * Level card content: « NIV. 12 » badge, title, XP and a 10-segment meter.
 *
 * @param level Level number.
 * @param title Level title.
 * @param xp XP inside the level.
 * @param xpNext XP needed for the next level.
 */
@Composable
fun LevelMeter(
    level: Int,
    title: String,
    xp: Long,
    xpNext: Long,
) {
    val c = Vault.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                Modifier
                    .size(52.dp)
                    .faceShadow(VaultRadius.badge)
                    .clip(VaultRadius.badge)
                    .background(c.indigo),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "NIV.",
                    style = Vault.type.engrave.copy(fontSize = 8.sp, letterSpacing = 1.2.sp),
                    color = c.onIndigo.copy(alpha = 0.8f),
                )
                Text("$level", style = Vault.type.displayM.copy(fontSize = 20.sp, lineHeight = 22.sp), color = c.onIndigo)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = Vault.type.displayM.copy(fontSize = 17.sp), color = c.ink)
                Text("${formatCount(xp)} / ${formatCount(xpNext)} XP", style = Vault.type.monoTag, color = c.inkFaint)
            }
        }
        val lit = if (xpNext > 0) (xp.toFloat() / xpNext * SEGMENTS).toInt().coerceIn(0, SEGMENTS) else 0
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(SEGMENTS) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(VaultRadius.pill)
                        .background(if (i < lit) c.indigo else c.well),
                )
            }
        }
    }
}

/**
 * Achievement row: medal (trophy or lock), title, description, progress or XP.
 *
 * @param title Title.
 * @param description Description.
 * @param unlocked Whether it is unlocked.
 * @param xp XP granted.
 * @param progress Current progress.
 * @param total Target.
 */
@Suppress("LongParameterList")
@Composable
fun AchievementBadge(
    title: String,
    description: String,
    unlocked: Boolean,
    xp: Int,
    progress: Long,
    total: Long,
) {
    val c = Vault.colors
    Panel(Modifier.fillMaxWidth(), PaddingValues(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(
                        46.dp,
                    ).clip(
                        androidx.compose.foundation.shape
                            .RoundedCornerShape(15.dp),
                    ).background(if (unlocked) c.indigo else c.well),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                    contentDescription = if (unlocked) "Débloqué" else "Verrouillé",
                    tint = if (unlocked) c.onIndigo else c.inkMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = Vault.type.bodyStrong, color = c.ink)
                Text(description, style = Vault.type.caption, color = c.inkMuted)
                if (!unlocked && total > 0) {
                    Row(
                        Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Meter(progress.toFloat() / total, c.indigo, Modifier.weight(1f), height = 6.dp)
                        Text("${progress.coerceAtMost(total)}/$total", style = Vault.type.monoTag, color = c.inkFaint)
                    }
                }
            }
            if (unlocked) Text("+$xp XP", style = Vault.type.monoTag.copy(fontSize = 12.sp), color = c.indigoInk)
        }
    }
}

/**
 * Floating « Succès débloqué » toast.
 *
 * @param title Achievement title.
 * @param xp XP granted.
 * @param modifier Modifier.
 */
@Composable
fun AchievementToast(
    title: String,
    xp: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .widthIn(max = 360.dp)
            .floatShadow(VaultRadius.pill)
            .clip(VaultRadius.pill)
            .background(OverlayColors.Toast)
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(VaultRadius.pill).background(OverlayColors.ToastMedal), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("SUCCÈS DÉBLOQUÉ", style = Vault.type.engrave.copy(fontSize = 9.sp, letterSpacing = 1.4.sp), color = OverlayColors.PipOn)
            Text(
                title,
                style = Vault.type.label.copy(fontSize = 14.sp),
                color = OverlayColors.TagContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text("+$xp XP", style = Vault.type.monoTag.copy(fontSize = 12.sp), color = OverlayColors.PipOn)
    }
}

private const val SEGMENTS = 10
