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
import com.thevault.domain.usecase.ObserveQuotesUseCase
import com.thevault.domain.usecase.SaveGameUseCase
import com.thevault.ui.screen.collection.effectiveValue
import com.thevault.ui.screen.collection.quoteFor
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
 * @property value Effective value (reliable quote, otherwise typed value).
 * @property grade Grade shown in the « Cote eBay » section.
 * @property history Price history, oldest first.
 */
data class GameDetailUiState(
    val game: Game? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val quotes: List<PriceQuote> = emptyList(),
    val value: Double? = null,
    val grade: PriceGrade = PriceGrade.CIB,
    val history: List<PricePoint> = emptyList(),
) {
    /** Quote of [grade], null when unknown. */
    val gradeQuote: PriceQuote? get() = quotes.find { it.grade == grade }
}

/** ViewModel of the game detail. */
class GameDetailViewModel(
    observeGames: ObserveGamesUseCase,
    observeQuotes: ObserveQuotesUseCase,
    private val getHistory: GetPriceHistoryUseCase,
    private val deleteGame: DeleteGameUseCase,
    private val saveGame: SaveGameUseCase,
) : ViewModel() {
    private val id = MutableStateFlow<String?>(null)
    private val grade = MutableStateFlow<PriceGrade?>(null)
    private val history = MutableStateFlow<List<PricePoint>>(emptyList())
    private val deleted = MutableStateFlow(false)

    /** Observable UI state. */
    val uiState: StateFlow<GameDetailUiState> =
        combine(id, observeGames(), observeQuotes(), grade, history) { gameId, games, quotes, g, h ->
            val game = games.find { it.id == gameId }
            val itemQuotes = gameId?.let { quotes[it] }.orEmpty()
            GameDetailUiState(
                game = game,
                isLoading = gameId == null,
                quotes = itemQuotes,
                value = game?.let { effectiveValue(it.currentValue, quoteFor(it.details.components, itemQuotes)) },
                grade = g ?: game?.details?.components?.priceGrade ?: PriceGrade.CIB,
                history = h,
            )
        }.combine(deleted) { s, d -> s.copy(isDeleted = d) }
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

    /** Deletes the game. */
    fun delete() {
        val gameId = id.value ?: return
        viewModelScope.launch {
            deleteGame(gameId)
            deleted.value = true
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
