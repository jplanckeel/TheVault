package com.thevault.ui.screen.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.PlayLog
import com.thevault.domain.model.PlayStatus
import com.thevault.domain.model.Region
import com.thevault.domain.model.RemoteGame
import com.thevault.domain.usecase.GetGameByIdUseCase
import com.thevault.domain.usecase.SaveGameUseCase
import com.thevault.domain.usecase.SearchRemoteGamesUseCase
import com.thevault.ui.screen.copy.CopyDraft
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * State of the full game form.
 *
 * @property original Game being edited, null when adding.
 * @property title Title.
 * @property platform Platform.
 * @property genre Genre.
 * @property releaseYear Release year as typed.
 * @property region Region.
 * @property draft Parts, wear, edition, purchase, storage, loan and sale of the copy, as typed.
 * @property status Status.
 * @property currentValue Manual value as typed (used when there is no eBay quote).
 * @property rating Personal rating (0 = none).
 * @property developer Developer.
 * @property publisher Publisher.
 * @property notes Notes.
 * @property coverUrl Cover URL.
 * @property availablePlatforms Platforms of the IGDB game.
 * @property play Backlog status and time played.
 * @property playHours Hours played as typed.
 * @property remote IGDB game picked, if any.
 * @property isSaving Whether a save is running.
 * @property isSuccess Whether the game was saved (screen closes).
 * @property searchResults IGDB suggestions.
 * @property isSearching Whether a search is running.
 * @property searchError Search error message.
 */
data class AddItemUiState(
    val original: Game? = null,
    val title: String = "",
    val platform: String = "",
    val genre: String = "",
    val releaseYear: String = "",
    val region: Region = Region.PAL,
    val draft: CopyDraft = CopyDraft(),
    val status: ItemStatus = ItemStatus.OWNED,
    val currentValue: String = "",
    val rating: Int = 0,
    val developer: String = "",
    val publisher: String = "",
    val notes: String = "",
    val coverUrl: String? = null,
    val availablePlatforms: List<String> = emptyList(),
    val play: PlayStatus? = null,
    val playHours: String = "",
    val remote: RemoteGame? = null,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val searchResults: List<RemoteGame> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
) {
    /** Whether the form can be saved. */
    val canSave: Boolean get() = !isSaving && title.isNotBlank() && platform.isNotBlank() && draft.datesValid
}

/** ViewModel of the full game form (add or edit). */
class AddItemViewModel(
    private val saveGame: SaveGameUseCase,
    private val searchRemoteGames: SearchRemoteGamesUseCase,
    private val getGameById: GetGameByIdUseCase,
) : ViewModel() {
    private val state = MutableStateFlow(AddItemUiState())
    private var searchJob: Job? = null

    /** Observable UI state. */
    val uiState: StateFlow<AddItemUiState> = state.asStateFlow()

    /** Loads [gameId] for editing. */
    fun loadGame(gameId: String) {
        if (state.value.original?.id == gameId) return
        viewModelScope.launch {
            val game = getGameById(gameId) ?: return@launch
            state.value = game.toForm()
        }
    }

    /** Title typed: searches IGDB when adding. */
    fun onTitleChange(value: String) {
        state.update { it.copy(title = value) }
        if (state.value.original == null) search(value.trim())
    }

    private fun search(query: String) {
        searchJob?.cancel()
        if (query.length < MIN_QUERY) {
            state.update { it.copy(searchResults = emptyList(), isSearching = false, searchError = null) }
            return
        }
        searchJob =
            viewModelScope.launch {
                delay(DEBOUNCE_MS)
                state.update { it.copy(isSearching = true) }
                searchRemoteGames(query)
                    .onSuccess { r -> state.update { it.copy(searchResults = r, isSearching = false, searchError = null) } }
                    .onFailure { state.update { it.copy(searchResults = emptyList(), isSearching = false, searchError = SEARCH_ERROR) } }
            }
    }

    /** Fills the form from an IGDB result. */
    fun selectGame(remote: RemoteGame) =
        state.update {
            it.copy(
                title = remote.title,
                platform = remote.platformNames.firstOrNull() ?: it.platform,
                availablePlatforms = remote.platformNames,
                genre = remote.genres.firstOrNull() ?: it.genre,
                releaseYear = remote.releaseYear?.toString() ?: it.releaseYear,
                developer = remote.developer.orEmpty(),
                publisher = remote.publisher.orEmpty(),
                coverUrl = remote.coverUrl,
                remote = remote,
                searchResults = emptyList(),
            )
        }

    /** Applies a change of the form fields. */
    fun update(transform: (AddItemUiState) -> AddItemUiState) = state.update(transform)

    /** Saves the game. */
    @OptIn(ExperimentalUuidApi::class)
    fun saveItem() {
        val s = state.value
        if (!s.canSave) return
        state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val base =
                s.original ?: Game(Uuid.random().toString(), "", "", "", 0, s.region, status = s.status, acquiredAt = today)
            saveGame(s.applyTo(base))
            state.update { it.copy(isSaving = false, isSuccess = true) }
        }
    }

    private companion object {
        const val MIN_QUERY = 3
        const val DEBOUNCE_MS = 450L
        const val SEARCH_ERROR = "Recherche IGDB indisponible : tu peux remplir la fiche à la main."
    }
}

private fun Game.toForm() =
    AddItemUiState(
        original = this,
        title = title,
        platform = platform,
        genre = genre.takeIf { it != "N/A" }.orEmpty(),
        releaseYear = releaseYear.takeIf { it > 0 }?.toString().orEmpty(),
        region = region,
        draft = CopyDraft.of(details, purchasePrice, acquiredAt),
        status = status,
        currentValue = currentValue?.toPlain().orEmpty(),
        rating = rating ?: 0,
        developer = developer.orEmpty(),
        publisher = publisher.orEmpty(),
        notes = notes.orEmpty(),
        coverUrl = coverUrl,
        availablePlatforms = listOf(platform),
        play = play.status,
        playHours =
            play.minutes
                ?.let { m ->
                    if (m % MINUTES ==
                        0
                    ) {
                        "${m / MINUTES}"
                    } else {
                        "${m / MINUTES},${m % MINUTES * 10 / MINUTES}"
                    }
                }.orEmpty(),
    )

private const val MINUTES = 60

/** Copies the form onto [base], keeping the fields the form does not show. */
private fun AddItemUiState.applyTo(base: Game): Game =
    base.copy(
        title = title.trim(),
        platform = platform.trim(),
        genre = genre.ifBlank { "N/A" },
        releaseYear = releaseYear.toIntOrNull() ?: 0,
        region = region,
        details = draft.toDetails(),
        play = PlayLog(play, playHours.toAmount()?.let { (it * MINUTES).toInt() }),
        status = status,
        coverUrl = coverUrl,
        purchasePrice = draft.purchasePrice,
        acquiredAt = draft.purchaseDate ?: base.acquiredAt,
        currentValue = currentValue.toAmount(),
        rating = rating.takeIf { it > 0 },
        developer = developer.ifBlank { null },
        publisher = publisher.ifBlank { null },
        gameModes = remote?.gameModes?.joinToString(", ")?.ifBlank { null } ?: base.gameModes,
        themes = remote?.themes?.joinToString(", ")?.ifBlank { null } ?: base.themes,
        notes = notes.ifBlank { null },
    )

/** "12.5" → "12,5", "40.0" → "40". */
private fun Double.toPlain(): String = if (this % 1.0 == 0.0) toLong().toString() else toString().replace('.', ',')

/** Parses an amount typed with either a comma or a dot as decimal separator. */
internal fun String.toAmount(): Double? = trim().replace(',', '.').replace(" ", "").toDoubleOrNull()
