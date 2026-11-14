package com.thevault.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.AchievementBadge
import com.thevault.ui.component.KeyButton
import com.thevault.ui.component.LevelMeter
import com.thevault.ui.component.LoadingBox
import com.thevault.ui.component.Panel
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.SectionHeader
import com.thevault.ui.component.StatTile
import com.thevault.ui.component.formatCount
import com.thevault.ui.navigation.tabBarClearance
import com.thevault.ui.theme.Vault
import org.koin.compose.viewmodel.koinViewModel

/** Profile: level, counters, stats link and achievements. */
@Composable
fun ProfileScreen(
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onSets: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = tabBarClearance()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader("Mon coffre", eyebrow = "Joueur 1") { KeyButton(Icons.Default.Settings, "Réglages", onSettings) }
        }
        val s = state
        if (s == null) {
            item { LoadingBox() }
            return@LazyColumn
        }
        item { Panel(Modifier.fillMaxWidth()) { LevelMeter(s.level.level, s.level.title, s.level.xpInLevel, s.level.xpForNext) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(formatCount(s.items.toLong()), "Objets", Modifier.weight(1f))
                StatTile("${s.unlocked}", "Succès", Modifier.weight(1f))
                StatTile("${s.platforms}", "Plateformes", Modifier.weight(1f))
            }
        }
        item { StatsLink("Voir mes statistiques", Icons.AutoMirrored.Filled.TrendingUp, onStats) }
        item { StatsLink("Listes de complétion", Icons.AutoMirrored.Filled.FormatListBulleted, onSets) }
        item { SectionHeader("Succès", Modifier.padding(top = 8.dp), action = "${s.unlocked} / ${s.achievements.size}") }
        items(s.achievements, key = { it.def.id }) { a ->
            AchievementBadge(a.def.title, a.def.description, a.unlocked, a.def.xp, a.progress, a.def.target)
        }
    }
}

@Composable
private fun StatsLink(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onStats: () -> Unit,
) {
    val c = Vault.colors
    Panel(Modifier.fillMaxWidth(), PaddingValues(horizontal = 16.dp, vertical = 14.dp), color = c.lavender, onClick = onStats) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = c.onLavender, modifier = Modifier.size(20.dp))
            Text(label, style = Vault.type.bodyStrong, color = c.onLavender, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.onLavender)
        }
    }
}
