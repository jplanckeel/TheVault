package com.thevault.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.thevault.ui.theme.CardBorderColor
import com.thevault.ui.theme.OnPrimary
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceContainerLowest
import com.thevault.ui.theme.VaultRadius

/**
 * Bottom navigation: four tabs and a central round "Add" button.
 * See design/DESIGN_SYSTEM.md §5.
 *
 * @param currentRoute Route of the current destination.
 * @param onTabSelected Called when a tab is tapped.
 * @param onAdd Called when the central button is tapped.
 */
@Composable
fun VaultBottomBar(
    currentRoute: String?,
    onTabSelected: (VaultTab) -> Unit,
    onAdd: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(SurfaceContainerLowest.copy(alpha = 0.96f))) {
        HorizontalDivider(color = CardBorderColor)
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(68.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            val tabs = VaultTab.entries
            tabs.take(2).forEach { TabItem(it, currentRoute == it.route, onTabSelected, Modifier.weight(1f)) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(VaultRadius.full)
                        .background(Primary)
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter un objet", tint = OnPrimary, modifier = Modifier.size(28.dp))
                }
            }
            tabs.drop(2).forEach { TabItem(it, currentRoute == it.route, onTabSelected, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun TabItem(
    tab: VaultTab,
    selected: Boolean,
    onTabSelected: (VaultTab) -> Unit,
    modifier: Modifier,
) {
    val color = if (selected) Primary else OnSurfaceVariant
    Column(
        modifier.clip(VaultRadius.lg).clickable { onTabSelected(tab) }.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(tab.label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}
