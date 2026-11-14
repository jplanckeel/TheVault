package com.thevault.ui.screen.sets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.ButtonSize
import com.thevault.ui.component.ButtonVariant
import com.thevault.ui.component.EmptyState
import com.thevault.ui.component.KeyButton
import com.thevault.ui.component.LoadingBox
import com.thevault.ui.component.Meter
import com.thevault.ui.component.Panel
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultTextField
import com.thevault.ui.component.formatEuro
import com.thevault.ui.theme.Vault
import org.koin.compose.viewmodel.koinViewModel

/**
 * Completion lists: progress, missing titles and estimated cost of each list.
 *
 * @param onBack Back navigation.
 * @param onOpen Opens a list.
 * @param viewModel Screen ViewModel.
 */
@Composable
fun SetsScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: SetsViewModel = koinViewModel(),
) {
    val completions by viewModel.completions.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize().imePadding().windowInsetsPadding(WindowInsets.navigationBars),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                "Listes",
                eyebrow = "Complétion",
                leading = { KeyButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onBack) },
            )
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                VaultTextField("Nouvelle liste", name, { name = it }, placeholder = "Zelda, GameCube PAL, Top RPG…")
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VaultButton("Vide", {
                        viewModel.create(name, false) {
                            name = ""
                            onOpen(it)
                        }
                    }, Modifier.weight(1f), ButtonVariant.Secondary, ButtonSize.Small)
                    VaultButton("Depuis ma collection", {
                        viewModel.create(name, true) {
                            name = ""
                            onOpen(it)
                        }
                    }, Modifier.weight(1.6f), size = ButtonSize.Small)
                }
                Text(
                    "« Depuis ma collection » reprend les jeux dont le titre contient le nom (une série).",
                    style = Vault.type.caption,
                    color = Vault.colors.inkMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        val list = completions
        when {
            list == null -> item { LoadingBox() }
            list.isEmpty() ->
                item {
                    EmptyState(
                        Icons.AutoMirrored.Filled.FormatListBulleted,
                        "Aucune liste",
                        message = "Crée une liste pour suivre une série ou une plateforme.",
                    )
                }
            else -> items(list, key = { it.set.id }) { SetCard(it) { onOpen(it.set.id) } }
        }
    }
}

@Composable
private fun SetCard(
    c: SetCompletion,
    onClick: () -> Unit,
) {
    Panel(Modifier.fillMaxWidth(), onClick = onClick) {
        Row {
            Text(c.set.name, style = Vault.type.displayM, color = Vault.colors.ink, modifier = Modifier.weight(1f))
            Text("${c.ownedCount} / ${c.lines.size}", style = Vault.type.monoPrice, color = Vault.colors.ink)
        }
        Meter(c.ratio, Vault.colors.indigo, Modifier.padding(top = 10.dp).fillMaxWidth(), height = 6.dp)
        val cost =
            when {
                c.missing.isEmpty() -> "Liste complète"
                c.estimatedCost > 0 ->
                    "${c.missing.size} manquants · ≈ ${formatEuro(c.estimatedCost)} pour compléter" +
                        if (c.unquoted > 0) " (${c.unquoted} non cotés)" else ""
                else -> "${c.missing.size} manquants · pas encore cotés"
            }
        Text(cost, style = Vault.type.caption, color = Vault.colors.inkMuted, modifier = Modifier.padding(top = 8.dp))
    }
}
