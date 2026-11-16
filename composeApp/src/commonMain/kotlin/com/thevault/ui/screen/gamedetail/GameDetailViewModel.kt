package com.thevault.ui.screen.gamedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.PriceGrade
import com.thevault.domain.model.PricePoint
import com.thevault.domain.model.PriceQuote
import com.thevault.domain.model.priceGrade
import com.thevault.domain.usecase.DeleteGameUseCase
import com.thevault.domain.usecase.GetPriceHistoryUseCase
import com.thevault.domain.usecase.ObserveGamesUseCase
import com.thevault.domain.usecase.ObservePriceSnapshotsUseCase
import com.thevault.domain.usecase.ObserveQuotesUseCase
import com.thevault.domain.usecase.SaveGameUseCase
import com.thevault.ui.screen.collection.ItemPhotos
import com.thevault.ui.screen.collection.ValueSource
import com.thevault.ui.screen.collection.effectiveValue
import com.thevault.ui.screen.collection.localHistory
import com.thevault.ui.screen.collection.mergeHistory
import com.thevault.ui.screen.collection.quoteFor
import com.thevault.ui.screen.collection.usPriceFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state of the game detail.
 *
 * @property game Game, null while loading or when missing.
 * @property isLoading Whether the game is loading.
 * @property isDeleted Whether the game was deleted (screen closes).
 * @property quotes eBay quotes of the game, all grades.
 * @property value Effective value (reliable PAL quote, typed value, or US indicative price).
 * @property valueSource Origin of [value].
 * @property usPrice Indicative US price of the import for the game's completeness.
 * @property grade Grade shown in the « Cote eBay » section.
 * @property history Price history, oldest first.
 * @property photo URI of the user's photo of the copy, shown instead of the cover.
 */
data class GameDetailUiState(
    val game: Game? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val quotes: List<PriceQuote> = emptyList(),
    val value: Double? = null,
    val valueSource: ValueSource = ValueSource.None,
    val usPrice: Double? = null,
    val grade: PriceGrade = PriceGrade.CIB,
    val history: List<PricePoint> = emptyList(),
    val photo: String? = null,
) {
    /** Quote of [grade], null when unknown. */
    val gradeQuote: PriceQuote? get() = quotes.find { it.grade == grade }
}

/** ViewModel of the game detail. */
@Suppress("LongParameterList")
class GameDetailViewModel(
    observeGames: ObserveGamesUseCase,
    observeQuotes: ObserveQuotesUseCase,
    observeSnapshots: ObservePriceSnapshotsUseCase,
    private val getHistory: GetPriceHistoryUseCase,
    private val deleteGame: DeleteGameUseCase,
    private val saveGame: SaveGameUseCase,
    private val photos: ItemPhotos,
) : ViewModel() {
    private val id = MutableStateFlow<String?>(null)
    private val grade = MutableStateFlow<PriceGrade?>(null)
    private val history = MutableStateFlow<List<PricePoint>>(emptyList())
    private val deleted = MutableStateFlow(false)

    /** Observable UI state. */
    val uiState: StateFlow<GameDetailUiState> =
        combine(id, observeGames(), observeQuotes(), observeSnapshots(), grade) { gameId, games, quotes, snaps, g ->
            val game = games.find { it.id == gameId }
            val itemQuotes = gameId?.let { quotes[it] }.orEmpty()
            val itemSnaps = gameId?.let { snaps[it] }.orEmpty()
            val components = game?.details?.components
            val us = components?.let { usPriceFor(it, itemSnaps) }
            val (value, source) = effectiveValue(game?.currentValue, components?.let { quoteFor(it, itemQuotes) }, us)
            GameDetailUiState(
                game = game,
                isLoading = gameId == null,
                quotes = itemQuotes,
                value = value,
                valueSource = source,
                usPrice = us,
                grade = g ?: components?.priceGrade ?: PriceGrade.CIB,
                history = localHistory(itemSnaps),
            )
        }.combine(history) { s, remote -> s.copy(history = mergeHistory(remote, s.history)) }
            .combine(deleted) { s, d -> s.copy(isDeleted = d) }
            .combine(photos.observe()) { s, p -> s.copy(photo = s.game?.id?.let(p::get)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GameDetailUiState())

    /** Shows the game [gameId] and loads its price history. */
    fun loadGame(gameId: String) {
        if (id.value == gameId) return
        id.value = gameId
        viewModelScope.launch { history.value = getHistory(gameId) }
    }

    /** Shows the quote of [value]. */
    fun onGradeSelected(value: PriceGrade) {
        grade.value = value
    }

    /** Changes the status (« Vendu / prêté »). */
    fun setStatus(status: ItemStatus) {
        val game = uiState.value.game ?: return
        viewModelScope.launch { saveGame(game.copy(status = status)) }
    }

    /** Makes [uri] the photo of the game. */
    fun setPhoto(uri: String) {
        val gameId = id.value ?: return
        viewModelScope.launch { photos.set(gameId, uri) }
    }

    /** Removes the photo of the game: the catalogue cover is shown again. */
    fun removePhoto() {
        val gameId = id.value ?: return
        viewModelScope.launch { photos.remove(gameId) }
    }

    /** Deletes the game. */
    fun delete() {
        val gameId = id.value ?: return
        viewModelScope.launch {
            photos.remove(gameId)
            deleteGame(gameId)
            deleted.value = true
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
