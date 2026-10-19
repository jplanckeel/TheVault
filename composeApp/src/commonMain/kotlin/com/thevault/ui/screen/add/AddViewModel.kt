package com.thevault.ui.screen.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Console
import com.thevault.domain.model.Game
import com.thevault.domain.model.RemoteGame
import com.thevault.domain.usecase.SaveAccessoryUseCase
import com.thevault.domain.usecase.SaveConsoleUseCase
import com.thevault.domain.usecase.SaveGameUseCase
import com.thevault.domain.usecase.SearchRemoteGamesUseCase
import com.thevault.ui.component.manufacturerOf
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.VaultEntriesSource
import com.thevault.ui.screen.collection.owned
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * UI state of the add screen.
 *
 * @property query Typed search (or console / accessory name).
 * @property kind Category being added.
 * @property platform Platform filter of the results, or null for all.
 * @property results IGDB results.
 * @property isSearching Whether a search is running.
 * @property candidate Item open in the confirmation sheet.
 * @property form Choices of the confirmation sheet.
 */
data class AddUiState(
    val query: String = "",
    val kind: ItemKind = ItemKind.Game,
    val platform: String? = null,
    val results: List<RemoteGame> = emptyList(),
    val isSearching: Boolean = false,
    val candidate: AddCandidate? = null,
    val form: QuickAddForm = QuickAddForm(),
)

/** ViewModel of the add flow (search + quick confirmation sheet). */
class AddViewModel(
    source: VaultEntriesSource,
    private val searchRemoteGames: SearchRemoteGamesUseCase,
    private val saveGame: SaveGameUseCase,
    private val saveConsole: SaveConsoleUseCase,
    private val saveAccessory: SaveAccessoryUseCase,
) : ViewModel() {
    private val state = MutableStateFlow(AddUiState())
    private var searchJob: Job? = null

    /** Observable UI state. */
    val uiState: StateFlow<AddUiState> = state.asStateFlow()

    /** Every collection entry, for the owned flags and the hints. */
    val entries: StateFlow<List<CollectionEntry>> =
        source.entries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** Updates the search and queries IGDB (debounced) for games. */
    fun onQueryChanged(query: String) {
        state.update { it.copy(query = query, platform = null) }
        searchJob?.cancel()
        if (state.value.kind != ItemKind.Game || query.trim().length < MIN_QUERY) {
            state.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        searchJob =
            viewModelScope.launch {
                delay(DEBOUNCE_MS)
                state.update { it.copy(isSearching = true) }
                val results = searchRemoteGames(query.trim())
                state.update { it.copy(results = results, isSearching = false) }
            }
    }

    /** Switches between game, console and accessory. */
    fun onKindSelected(kind: ItemKind) {
        state.update { it.copy(kind = kind, results = emptyList(), platform = null) }
        onQueryChanged(state.value.query)
    }

    /** Filters the results by platform (null = all). */
    fun onPlatformSelected(platform: String?) = state.update { it.copy(platform = platform) }

    /** Opens the confirmation sheet with sensible defaults. */
    fun open(candidate: AddCandidate) {
        val owned = entries.value.owned()
        val platform = state.value.platform?.takeIf { it in candidate.platforms } ?: candidate.platforms.firstOrNull().orEmpty()
        state.update {
            it.copy(candidate = candidate, form = QuickAddForm(platform = platform, region = preferredRegion(owned)))
        }
    }

    /** Closes the confirmation sheet. */
    fun dismiss() = state.update { it.copy(candidate = null) }

    /** Updates the sheet choices. */
    fun onFormChanged(form: QuickAddForm) = state.update { it.copy(form = form) }

    /** Saves the item; returns the game id when a game was added. */
    @OptIn(ExperimentalUuidApi::class)
    fun save(onSaved: (gameId: String?) -> Unit) {
        val candidate = state.value.candidate ?: return
        val form = state.value.form
        val id = Uuid.random().toString()
        viewModelScope.launch {
            when (candidate.kind) {
                ItemKind.Game -> saveGame(candidate.toGame(id, form))
                ItemKind.Console -> saveConsole(candidate.toConsole(id, form))
                ItemKind.Accessory -> saveAccessory(candidate.toAccessory(id, form))
            }
            state.update { it.copy(candidate = null) }
            onSaved(if (candidate.kind == ItemKind.Game) id else null)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val DEBOUNCE_MS = 400L
        const val MIN_QUERY = 3
    }
}

private fun today() = Clock.System.todayIn(TimeZone.currentSystemDefault())

private fun AddCandidate.toGame(
    id: String,
    form: QuickAddForm,
) = Game(
    id = id,
    title = title,
    platform = form.platform.ifBlank { "Autre" },
    genre = remote?.genres?.firstOrNull() ?: "N/A",
    releaseYear = remote?.releaseYear ?: 0,
    region = form.region,
    condition = form.condition,
    coverUrl = remote?.coverUrl,
    status = form.status,
    purchasePrice = form.price.toAmount(),
    developer = remote?.developer,
    publisher = remote?.publisher,
    gameModes = remote?.gameModes?.joinToString(", ")?.ifBlank { null },
    themes = remote?.themes?.joinToString(", ")?.ifBlank { null },
    acquiredAt = today(),
)

private fun AddCandidate.toConsole(
    id: String,
    form: QuickAddForm,
) = Console(
    id = id,
    name = title,
    manufacturer = manufacturerOf(form.platform.ifBlank { title }).label,
    region = form.region,
    color = "",
    condition = form.condition,
    productionYear = 0,
    status = form.status,
    purchasePrice = form.price.toAmount(),
    acquiredAt = today(),
)

private fun AddCandidate.toAccessory(
    id: String,
    form: QuickAddForm,
) = Accessory(
    id = id,
    name = title,
    type = "Accessoire",
    compatibleConsole = form.platform,
    condition = form.condition,
    status = form.status,
    purchasePrice = form.price.toAmount(),
    acquiredAt = today(),
)
