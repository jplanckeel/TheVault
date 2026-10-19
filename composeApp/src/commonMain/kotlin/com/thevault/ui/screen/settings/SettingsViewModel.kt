package com.thevault.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.usecase.CsvService
import com.thevault.domain.usecase.SyncVaultWithIgdbUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 */
data class SettingsUiState(
    val isSyncing: Boolean = false,
    val isImporting: Boolean = false,
    val message: SettingsMessage? = null,
)

/**
 * ViewModel of the Settings screen: CSV import / export and IGDB enrichment.
 */
class SettingsViewModel(
    private val csvService: CsvService,
    private val syncVaultWithIgdb: SyncVaultWithIgdbUseCase,
) : ViewModel() {
    private val state = MutableStateFlow(SettingsUiState())

    /** Observable UI state. */
    val uiState: StateFlow<SettingsUiState> = state.asStateFlow()

    /** Imports a CSV export (GameEye format). */
    fun importCsv(content: String) {
        state.update { it.copy(isImporting = true, message = SettingsMessage("Import en cours…", MessageTone.Info)) }
        viewModelScope.launch {
            val message =
                if (csvService.importCsv(content).isSuccess) {
                    SettingsMessage("Import terminé.", MessageTone.Success)
                } else {
                    SettingsMessage("Le fichier n'a pas pu être importé.", MessageTone.Error)
                }
            state.update { it.copy(isImporting = false, message = message) }
        }
    }

    /** Builds the CSV export; returns null and shows an error on failure. */
    suspend fun exportCsv(): String? =
        csvService.exportCsv().fold(
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

    /** Dismisses the feedback message. */
    fun clearMessage() = state.update { it.copy(message = null) }
}
