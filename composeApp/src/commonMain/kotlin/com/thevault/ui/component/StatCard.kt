package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.TabularNumbers
import com.thevault.ui.theme.VaultRadius

/**
 * KPI card — mirrors `Stat.tsx`: icon badge, uppercase label, big Montserrat value.
 *
 * @param label Uppercase label (e.g. "JEUX").
 * @param value Formatted main value.
 * @param modifier Modifier applied to the card.
 * @param icon Optional icon shown in a tinted square.
 * @param hint Optional sub-text under the value.
 * @param onClick Optional click handler.
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    hint: String? = null,
    onClick: (() -> Unit)? = null,
) {
    VaultCard(
        modifier = modifier,
        shape = VaultRadius.xxl,
        contentPadding = PaddingValues(16.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) {
                Box(
                    Modifier.size(32.dp).clip(VaultRadius.md).background(Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(value, style = MaterialTheme.typography.displaySmall.merge(TabularNumbers), color = OnSurface, maxLines = 1)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, maxLines = 1)
        }
    }
}
