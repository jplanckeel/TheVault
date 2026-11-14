package com.thevault.ui.screen.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.ItemStatus
import com.thevault.ui.component.EmptyState
import com.thevault.ui.component.KeyButton
import com.thevault.ui.component.ListRow
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.SearchField
import com.thevault.ui.component.formatEuroOrDash
import com.thevault.ui.component.shortPlatformName
import com.thevault.ui.screen.add.ownedKey
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ValueSource
import com.thevault.ui.screen.collection.VaultEntriesSource
import com.thevault.ui.theme.Vault
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.compose.viewmodel.koinViewModel

/**
 * One title found in the collection, all copies and statuses together.
 *
 * @property title Title.
 * @property platform Platform.
 * @property copies Every matching item (owned, wished, sold, loaned).
 */
data class CheckHit(
    val title: String,
    val platform: String,
    val copies: List<CollectionEntry>,
) {
    /** Owned copies. */
    val owned: Int get() = copies.count { it.status == ItemStatus.OWNED || it.status == ItemStatus.LOANED }

    /** Answer to « je l'ai déjà ? ». */
    val verdict: String
        get() =
            when {
                owned > 1 -> "Oui, $owned exemplaires"
                owned == 1 -> "Oui, déjà dans le coffre"
                copies.any { it.status == ItemStatus.WISHLIST } -> "Non — dans tes envies"
                copies.any { it.status == ItemStatus.SOLD } -> "Non — vendu"
                else -> "Non"
            }

    /** Best known local quote of the title. */
    val quote: CollectionEntry? get() = copies.filter { it.value != null }.maxByOrNull { if (it.valueSource == ValueSource.Ebay) 1 else 0 }
}

/** Titles of the collection matching [query] (any word order, case and punctuation ignored). */
fun quickCheck(
    query: String,
    entries: List<CollectionEntry>,
): List<CheckHit> {
    val words =
        query
            .lowercase()
            .split(' ')
            .map { w -> w.filter { it.isLetterOrDigit() } }
            .filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyList()
    return entries
        .filter { e ->
            val t = e.title.lowercase().filter { it.isLetterOrDigit() || it == ' ' }
            words.all { t.replace(" ", "").contains(it) }
        }.groupBy { ownedKey(it.title, it.platform) }
        .map { (_, list) -> CheckHit(list.first().title, list.first().platform, list) }
        .sortedWith(compareByDescending<CheckHit> { it.owned }.thenBy { it.title.lowercase() })
}

/** ViewModel of the « Je l'ai déjà ? » screen: everything is local, it works offline. */
class QuickCheckViewModel(
    source: VaultEntriesSource,
) : ViewModel() {
    /** Every item of the collection. */
    val entries: StateFlow<List<CollectionEntry>> =
        source.entries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/**
 * « Je l'ai déjà ? »: instant search in the collection, with copies and local quote.
 *
 * @param onBack Back navigation.
 * @param onOpen Opens an item.
 * @param viewModel Screen ViewModel.
 */
@Composable
fun QuickCheckScreen(
    onBack: () -> Unit,
    onOpen: (CollectionEntry) -> Unit,
    viewModel: QuickCheckViewModel = koinViewModel(),
) {
    val entries by viewModel.entries.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val hits = remember(query, entries) { quickCheck(query, entries) }
    LazyColumn(
        Modifier.fillMaxSize().imePadding().windowInsetsPadding(WindowInsets.navigationBars),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ScreenHeader(
                "Je l'ai déjà ?",
                eyebrow = "Hors ligne",
                leading = { KeyButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onBack) },
            )
        }
        item { SearchField(query, { query = it }, "Titre du jeu", Modifier.fillMaxWidth(), focus) }
        if (query.isNotBlank() && hits.isEmpty()) {
            item { EmptyState(Icons.Default.SearchOff, "Pas dans le coffre", message = "Ni possédé, ni en envies, ni vendu.") }
        }
        items(hits, key = { "${it.title}|${it.platform}" }) { hit ->
            val quote = hit.quote
            ListRow(
                hit.title,
                meta = "${shortPlatformName(hit.platform)} · ${hit.verdict}",
                trailing = (if (quote?.valueSource == ValueSource.UsIndicative) "≈ " else "") + formatEuroOrDash(quote?.value),
                cover = Triple(hit.copies.firstNotNullOfOrNull { it.coverUrl }, hit.platform, hit.copies.first().condition),
                onClick = { onOpen(hit.copies.first()) },
                trailingContent = {
                    Text(
                        if (hit.owned > 0) "✓" else "✗",
                        style = Vault.type.monoPrice,
                        color = if (hit.owned > 0) Vault.colors.ledSync else Vault.colors.ledPower,
                    )
                },
            )
        }
    }
}
