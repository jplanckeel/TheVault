package com.thevault.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.usecase.CsvService
import com.thevault.domain.usecase.GetImportedPricesUseCase
import com.thevault.domain.usecase.ObserveSyncedIdsUseCase
import com.thevault.domain.usecase.RecordImportedPricesUseCase
import com.thevault.domain.usecase.RefreshCatalogUseCase
import com.thevault.domain.usecase.RefreshQuotesUseCase
import com.thevault.domain.usecase.SyncScheduler
import com.thevault.ui.screen.collection.ValueSource
import com.thevault.ui.screen.collection.VaultEntriesSource
import com.thevault.ui.screen.collection.owned
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Tone of a feedback message. */
enum class MessageTone {
    /** Action in progress. */
    Info,

    /** Action succeeded. */
    Success,

    /** Action failed. */
    Error,
}

/**
 * Feedback shown after an action.
 *
 * @property text Message text.
 * @property tone Message tone.
 */
data class SettingsMessage(
    val text: String,
    val tone: MessageTone,
)

/**
 * UI state of the Settings screen.
 *
 * @property isSyncing Whether an IGDB sync is running.
 * @property isImporting Whether a CSV import is running.
 * @property message Last feedback message, if any.
 * @property pending Items not pushed to the catalogue yet.
 * @property total Items in the collection.
 */
data class SettingsUiState(
    val isSyncing: Boolean = false,
    val isImporting: Boolean = false,
    val message: SettingsMessage? = null,
    val pending: Int = 0,
    val total: Int = 0,
    val valuation: ValuationSplit = ValuationSplit(),
)

/**
 * Where the value of the owned items comes from — shows the move from the Gameye US
 * prices to the eBay PAL quotes.
 *
 * @property ebay Items valued by a reliable eBay quote.
 * @property manual Items valued by the user.
 * @property usIndicative Items still valued by their Gameye US price.
 * @property none Items without any value.
 */
data class ValuationSplit(
    val ebay: Int = 0,
    val manual: Int = 0,
    val usIndicative: Int = 0,
    val none: Int = 0,
)

/**
 * ViewModel of the Settings screen: sync with the server, CSV import / export.
 */
@Suppress("LongParameterList")
class SettingsViewModel(
    private val csvService: CsvService,
    private val refreshCatalog: RefreshCatalogUseCase,
    private val source: VaultEntriesSource,
    observeSynced: ObserveSyncedIdsUseCase,
    private val scheduler: SyncScheduler,
    private val refreshQuotes: RefreshQuotesUseCase,
    private val recordImportedPrices: RecordImportedPricesUseCase,
    private val getImportedPrices: GetImportedPricesUseCase,
) : ViewModel() {
    private val state = MutableStateFlow(SettingsUiState())

    /** Observable UI state. */
    val uiState: StateFlow<SettingsUiState> =
        combine(state, source.entries(), observeSynced()) { s, entries, synced ->
            val owned = entries.owned()
            s.copy(
                pending = entries.count { it.id !in synced },
                total = entries.size,
                valuation =
                    ValuationSplit(
                        ebay = owned.count { it.valueSource == ValueSource.Ebay },
                        manual = owned.count { it.valueSource == ValueSource.Manual },
                        usIndicative = owned.count { it.valueSource == ValueSource.UsIndicative },
                        none = owned.count { it.valueSource == ValueSource.None },
                    ),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    /** Pushes the pending items, then downloads the catalogue sheets and the eBay quotes. */
    fun syncNow() {
        scheduler.request()
        state.update { it.copy(isSyncing = true, message = SettingsMessage("Mise à jour des fiches et des cotes…", MessageTone.Info)) }
        viewModelScope.launch {
            val sheets = refreshCatalog(force = true)
            val quotes = refreshQuotes(source.entries().first().map { it.id }, force = true)
            val message =
                if (sheets == null || quotes == null) {
                    SettingsMessage("Serveur injoignable : réessaie plus tard.", MessageTone.Error)
                } else {
                    SettingsMessage("$sheets fiches mises à jour, $quotes cotes eBay reçues.", MessageTone.Success)
                }
            state.update { it.copy(isSyncing = false, message = message) }
        }
    }

    /** Imports a CSV export (GameEye format). */
    fun importCsv(content: String) {
        state.update { it.copy(isImporting = true, message = SettingsMessage("Import en cours…", MessageTone.Info)) }
        viewModelScope.launch {
            val message =
                csvService.importCsv(content).fold(
                    onSuccess = {
                        recordImportedPrices(it.usPrices)
                        SettingsMessage(
                            "Import terminé : ${it.created} ajoutés, ${it.updated} mis à jour.",
                            MessageTone.Success,
                        )
                    },
                    onFailure = { SettingsMessage("Le fichier n'a pas pu être importé.", MessageTone.Error) },
                )
            scheduler.request()
            state.update { it.copy(isImporting = false, message = message) }
        }
    }

    /** Builds the CSV export; returns null and shows an error on failure. */
    suspend fun exportCsv(): String? =
        csvService.exportCsv(getImportedPrices()).fold(
            onSuccess = {
                state.update { s -> s.copy(message = SettingsMessage("Export enregistré.", MessageTone.Success)) }
                it
            },
            onFailure = {
                state.update { s -> s.copy(message = SettingsMessage("L'export a échoué.", MessageTone.Error)) }
                null
            },
        )

    /** Insurance inventory of the owned items. */
    suspend fun insuranceLines(): List<InsuranceLine> = insuranceLines(source.entries().first())

    /** Dismisses the feedback message. */
    fun clearMessage() = state.update { it.copy(message = null) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
