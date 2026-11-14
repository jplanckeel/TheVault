package com.thevault.ui.screen.sets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemSet
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.domain.model.RemoteGame
import com.thevault.domain.model.SetEntry
import com.thevault.domain.usecase.DeleteSetUseCase
import com.thevault.domain.usecase.EditSetEntryUseCase
import com.thevault.domain.usecase.ObserveSetsUseCase
import com.thevault.domain.usecase.SaveGameUseCase
import com.thevault.domain.usecase.SaveSetUseCase
import com.thevault.domain.usecase.SearchRemoteGamesUseCase
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.VaultEntriesSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** ViewModel of the list overview. */
class SetsViewModel(
    source: VaultEntriesSource,
    observeSets: ObserveSetsUseCase,
    private val saveSet: SaveSetUseCase,
) : ViewModel() {
    /** Every list with its completion, null while loading. */
    val completions: StateFlow<List<SetCompletion>?> =
        combine(observeSets(), source.entries()) { sets, entries -> completions(sets, entries) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val entries = source.entries()

    /**
     * Creates a list named [name]; when [fromCollection] it starts with the owned and wished
     * games whose title contains the name (a series: « Zelda », « Final Fantasy »…).
     */
    @OptIn(ExperimentalUuidApi::class)
    fun create(
        name: String,
        fromCollection: Boolean,
        onCreated: (String) -> Unit,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val seeded =
                if (fromCollection) {
                    entries
                        .first()
                        .filter { it.kind == ItemKind.Game && it.title.contains(name.trim(), ignoreCase = true) }
                        .map { SetEntry(it.title, it.platform) }
                        .distinct()
                } else {
                    emptyList()
                }
            val id = Uuid.random().toString()
            saveSet(ItemSet(id, name.trim(), seeded))
            onCreated(id)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Which titles of a list are shown. */
enum class SetFilter(
    val label: String,
) {
    /** Every title. */
    All("Tous"),

    /** Owned titles. */
    Owned("Possédés"),

    /** Missing titles. */
    Missing("Manquants"),
}

/** ViewModel of one list. */
@Suppress("LongParameterList")
class SetDetailViewModel(
    source: VaultEntriesSource,
    observeSets: ObserveSetsUseCase,
    private val saveSet: SaveSetUseCase,
    private val deleteSet: DeleteSetUseCase,
    private val editEntry: EditSetEntryUseCase,
    private val searchRemote: SearchRemoteGamesUseCase,
    private val saveGame: SaveGameUseCase,
) : ViewModel() {
    private val setId = MutableStateFlow<String?>(null)
    private var searchJob: Job? = null

    /** Shown titles. */
    val filter = MutableStateFlow(SetFilter.All)

    /** IGDB results of the add search. */
    val results = MutableStateFlow<List<RemoteGame>>(emptyList())

    /** Completion of the list, null while loading or once deleted. */
    val completion: StateFlow<SetCompletion?> =
        combine(
            setId,
            observeSets(),
            source.entries(),
        ) { id, sets, entries -> completions(sets.filter { it.id == id }, entries).firstOrNull() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** Shows the list [id]. */
    fun load(id: String) {
        setId.value = id
    }

    /** Searches IGDB to add titles (debounced). */
    fun search(query: String) {
        searchJob?.cancel()
        if (query.trim().length < MIN_QUERY) {
            results.value = emptyList()
            return
        }
        searchJob =
            viewModelScope.launch {
                delay(DEBOUNCE_MS)
                results.value = searchRemote(query.trim()).getOrDefault(emptyList())
            }
    }

    /** Adds or removes a title. */
    fun toggle(
        entry: SetEntry,
        add: Boolean,
    ) {
        val id = setId.value ?: return
        viewModelScope.launch { editEntry(id, entry, add) }
    }

    /** Renames the list. */
    fun rename(name: String) {
        val set = completion.value?.set ?: return
        if (name.isBlank()) return
        viewModelScope.launch { saveSet(set.copy(name = name.trim())) }
    }

    /** Deletes the list. */
    fun delete(onDone: () -> Unit) {
        val id = setId.value ?: return
        viewModelScope.launch {
            deleteSet(id)
            onDone()
        }
    }

    /** Puts every missing title not tracked yet in the wishlist, so that it gets a quote. */
    @OptIn(ExperimentalUuidApi::class)
    fun wishMissing() {
        val missing = completion.value?.missing?.filter { it.wished == null } ?: return
        viewModelScope.launch {
            missing.forEach { line ->
                saveGame(
                    Game(
                        Uuid.random().toString(),
                        line.entry.title,
                        line.entry.platform,
                        "N/A",
                        0,
                        Region.PAL,
                        status = ItemStatus.WISHLIST,
                    ),
                )
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val MIN_QUERY = 3
        const val DEBOUNCE_MS = 450L
    }
}
