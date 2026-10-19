package com.thevault.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.EmptyState
import com.thevault.ui.component.IconSquareButton
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.VaultLogo
import com.thevault.ui.screen.collection.HomeSummary
import com.thevault.ui.theme.Primary
import org.koin.compose.viewmodel.koinViewModel

/** Home screen (« Ma collection »). */
@Composable
fun HomeScreen(
    actions: HomeActions,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val summary by viewModel.summary.collectAsState()
    HomeContent(summary, actions)
}

/**
 * Stateless home content.
 *
 * @param summary Home data, null while loading.
 * @param actions Navigation callbacks.
 */
@Composable
fun HomeContent(
    summary: HomeSummary?,
    actions: HomeActions,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item { HomeHeader(actions) }
        when {
            summary == null -> item { Loading() }
            summary.totalCount == 0 ->
                item {
                    EmptyState(
                        Icons.Default.Inventory2,
                        "Votre vault est vide",
                        message = "Ajoutez votre premier objet.",
                        actionLabel = "Ajouter",
                        onAction = actions.onAdd,
                    )
                }
            else -> {
                item { ValueCard(summary, actions) }
                if (summary.recent.isNotEmpty()) item { RecentAdditions(summary.recent, actions) }
                if (summary.platforms.isNotEmpty()) item { PlatformBreakdown(summary.platforms, actions) }
                if (summary.masterpieces.isNotEmpty()) item { Masterpieces(summary.masterpieces, actions.onEntryClick) }
            }
        }
    }
}

@Composable
private fun HomeHeader(actions: HomeActions) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultLogo(size = 44.dp)
        HomeTitle(actions)
    }
}

@Composable
private fun HomeTitle(actions: HomeActions) {
    ScreenHeader(
        title = "Ma collection",
        eyebrow = "The Vault",
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconSquareButton(Icons.Default.Search, "Rechercher dans la collection", actions.onSearch)
                IconSquareButton(Icons.Default.Settings, "Réglages", actions.onSettings)
            }
        },
    )
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Primary, modifier = Modifier.width(32.dp))
        }
    }
}
