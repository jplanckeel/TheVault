package com.thevault.ui.screen.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemCondition
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.domain.model.RemoteGame
import com.thevault.domain.usecase.SaveGameUseCase
import com.thevault.domain.usecase.GetGameByIdUseCase
import com.thevault.domain.usecase.SearchRemoteGamesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class AddItemUiState(
    val gameId: String? = null,
    val initialAcquiredAtDate: LocalDate? = null,
    val title: String = "",
    val platform: String = "",
    val genre: String = "",
    val releaseYear: String = "2024",
    val region: Region = Region.PAL,
    val condition: ItemCondition = ItemCondition.CIB,
    val status: ItemStatus = ItemStatus.OWNED,
    val purchasePrice: String = "",
    val currentValue: String = "",
    val rating: Int = 0,
    val developer: String = "",
    val publisher: String = "",
    val gameModes: String = "",
    val themes: String = "",
    val notes: String = "",
    val coverUrl: String? = null,
    val availablePlatforms: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val searchResults: List<RemoteGame> = emptyList(),
    val isSearching: Boolean = false,
)

class AddItemViewModel(
    private val saveGameUseCase: SaveGameUseCase,
    private val searchRemoteGamesUseCase: SearchRemoteGamesUseCase,
    private val getGameByIdUseCase: GetGameByIdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun loadGame(gameId: String) {
        viewModelScope.launch {
            val game = getGameByIdUseCase(gameId)
            if (game != null) {
                _uiState.update { 
                    it.copy(
                        gameId = game.id,
                        initialAcquiredAtDate = game.acquiredAt,
                        title = game.title,
                        platform = game.platform,
                        genre = game.genre,
                        releaseYear = game.releaseYear.toString(),
                        region = game.region,
                        condition = game.condition,
                        status = game.status,
                        purchasePrice = game.purchasePrice?.toString() ?: "",
                        currentValue = game.currentValue?.toString() ?: "",
                        rating = game.rating ?: 0,
                        developer = game.developer ?: "",
                        publisher = game.publisher ?: "",
                        gameModes = game.gameModes ?: "",
                        themes = game.themes ?: "",
                        notes = game.notes ?: "",
                        coverUrl = game.coverUrl,
                        availablePlatforms = listOf(game.platform) 
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
        triggerSearch(value)
    }

    private fun triggerSearch(query: String) {
        searchJob?.cancel()
        if (query.length < 3) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            _uiState.update { it.copy(isSearching = true) }
            val results = searchRemoteGamesUseCase(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun selectGame(remoteGame: RemoteGame) {
        _uiState.update { 
            it.copy(
                title = remoteGame.title,
                platform = remoteGame.platformNames.firstOrNull() ?: it.platform,
                availablePlatforms = remoteGame.platformNames,
                genre = remoteGame.genres.firstOrNull() ?: it.genre,
                releaseYear = remoteGame.releaseYear?.toString() ?: it.releaseYear,
                developer = remoteGame.developer ?: "",
                publisher = remoteGame.publisher ?: "",
                gameModes = remoteGame.gameModes.joinToString(", "),
                themes = remoteGame.themes.joinToString(", "),
                coverUrl = remoteGame.coverUrl,
                searchResults = emptyList() // Hide suggestions
            )
        }
    }

    fun onPlatformChange(value: String) = _uiState.update { it.copy(platform = value) }
    fun onGenreChange(value: String) = _uiState.update { it.copy(genre = value) }
    fun onYearChange(value: String) = _uiState.update { it.copy(releaseYear = value) }
    fun onRegionChange(value: Region) = _uiState.update { it.copy(region = value) }
    fun onConditionChange(itemCondition: ItemCondition) = _uiState.update { it.copy(condition = itemCondition) }
    fun onRatingChange(rating: Int) = _uiState.update { it.copy(rating = rating) }
    fun onStatusChange(itemStatus: ItemStatus) = _uiState.update { it.copy(status = itemStatus) }
    fun onPriceChange(value: String) = _uiState.update { it.copy(purchasePrice = value) }
    fun onValueChange(value: String) = _uiState.update { it.copy(currentValue = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun saveItem() {
        val state = _uiState.value
        if (state.title.isBlank() || state.platform.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            val game = Game(
                id = state.gameId ?: Clock.System.now().toEpochMilliseconds().toString(),
                title = state.title,
                platform = state.platform.ifBlank { "Autre" },
                genre = state.genre.ifBlank { "N/A" },
                releaseYear = state.releaseYear.toIntOrNull() ?: 0,
                region = state.region,
                condition = state.condition,
                status = state.status,
                coverUrl = state.coverUrl,
                purchasePrice = state.purchasePrice.toDoubleOrNull(),
                currentValue = state.currentValue.toDoubleOrNull(),
                rating = state.rating,
                developer = state.developer.ifBlank { null },
                publisher = state.publisher.ifBlank { null },
                gameModes = state.gameModes.ifBlank { null },
                themes = state.themes.ifBlank { null },
                notes = state.notes,
                acquiredAt = state.initialAcquiredAtDate ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            )
            
            saveGameUseCase(game)
            _uiState.update { it.copy(isSaving = false, isSuccess = true) }
        }
    }
}
