package com.thevault.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thevault.ui.component.FaceButton
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultRadius
import com.thevault.ui.theme.floatShadow

/** Space the floating tab bar takes at the bottom of a scrolling screen. */
@Composable
fun tabBarClearance(): Dp = TAB_BAR_HEIGHT + TAB_BAR_MARGIN + 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/**
 * Floating tab bar: four tabs around the indigo « + » face button.
 *
 * @param current Active tab.
 * @param onTabSelected Tab callback.
 * @param onAdd « + » callback.
 * @param modifier Modifier.
 */
@Composable
fun VaultBottomBar(
    current: VaultTab?,
    onTabSelected: (VaultTab) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 14.dp, end = 14.dp, bottom = TAB_BAR_MARGIN)
            .height(TAB_BAR_HEIGHT)
            .floatShadow(VaultRadius.xl)
            .clip(VaultRadius.xl)
            .background(Vault.colors.panel)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tabs = VaultTab.entries
        tabs.take(2).forEach { TabItem(it, it == current, onTabSelected) }
        FaceButton(Icons.Default.Add, "Ajouter un objet", onAdd)
        tabs.drop(2).forEach { TabItem(it, it == current, onTabSelected) }
    }
}

@Composable
private fun TabItem(
    tab: VaultTab,
    selected: Boolean,
    onTabSelected: (VaultTab) -> Unit,
) {
    val c = Vault.colors
    val color = if (selected) c.ink else c.inkMuted
    Column(
        Modifier
            .width(64.dp)
            .semantics { this.selected = selected }
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Tab) { onTabSelected(tab) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(tab.label, style = Vault.type.small.copy(fontSize = 10.sp, lineHeight = 12.sp), color = color, maxLines = 1)
        Box(Modifier.size(4.dp).clip(VaultRadius.pill).background(if (selected) c.indigo else Color.Transparent))
    }
}

private val TAB_BAR_HEIGHT = 68.dp
private val TAB_BAR_MARGIN = 22.dp
