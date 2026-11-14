package com.thevault.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.usecase.CsvService
import com.thevault.domain.usecase.GetImportedPricesUseCase
import com.thevault.domain.usecase.ObserveSyncedIdsUseCase
import com.thevault.domain.usecase.RecordImportedPricesUseCase
import com.thevault.domain.usecase.RefreshQuotesUseCase
import com.thevault.domain.usecase.SyncScheduler
import com.thevault.domain.usecase.SyncVaultWithIgdbUseCase
import com.thevault.ui.screen.collection.VaultEntriesSource
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
)

/**
 * ViewModel of the Settings screen: CSV import / export and IGDB enrichment.
 */
@Suppress("LongParameterList")
class SettingsViewModel(
    private val csvService: CsvService,
    private val syncVaultWithIgdb: SyncVaultWithIgdbUseCase,
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
            s.copy(pending = entries.count { it.id !in synced }, total = entries.size)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    /** Pushes the pending items and downloads the latest eBay quotes. */
    fun syncNow() {
        scheduler.request()
        state.update { it.copy(message = SettingsMessage("Mise à jour des cotes eBay…", MessageTone.Info)) }
        viewModelScope.launch {
            val ids = source.entries().first().map { it.id }
            val message =
                refreshQuotes(ids, force = true)?.let { SettingsMessage("$it cotes eBay à jour.", MessageTone.Success) }
                    ?: SettingsMessage("Serveur injoignable : réessaie plus tard.", MessageTone.Error)
            state.update { it.copy(message = message) }
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

    /** Enriches the collection with IGDB metadata (covers, genres…). */
    fun syncIgdb() {
        state.update { it.copy(isSyncing = true, message = SettingsMessage("Synchronisation IGDB…", MessageTone.Info)) }
        viewModelScope.launch {
            val result = syncVaultWithIgdb()
            val message =
                result.fold(
                    onSuccess = { SettingsMessage("$it jeu(x) mis à jour.", MessageTone.Success) },
                    onFailure = { SettingsMessage("La synchronisation IGDB a échoué.", MessageTone.Error) },
                )
            state.update { it.copy(isSyncing = false, message = message) }
        }
    }

    /** Insurance inventory of the owned items. */
    suspend fun insuranceLines(): List<InsuranceLine> = insuranceLines(source.entries().first())

    /** Dismisses the feedback message. */
    fun clearMessage() = state.update { it.copy(message = null) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
