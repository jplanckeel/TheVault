package com.thevault.ui.screen.collection

import com.thevault.domain.usecase.RefreshCollectionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Outcome shown after a pull to refresh.
 *
 * @property text Message.
 * @property success False when the server could not be reached.
 */
data class RefreshMessage(
    val text: String,
    val success: Boolean,
)

/**
 * State of the pull to refresh of the collection screens.
 *
 * @property isRefreshing Whether a refresh is running.
 * @property message Outcome of the last refresh, cleared after a few seconds.
 */
data class RefreshUiState(
    val isRefreshing: Boolean = false,
    val message: RefreshMessage? = null,
)

/**
 * Runs the pull to refresh of the collection screens. Shared by every screen (a single
 * refresh at a time) and run in an application scope, so leaving the screen does not
 * cancel it.
 *
 * @param refreshCollection Downloads the sheets and quotes from the backend.
 * @param scope Application scope.
 */
class CollectionRefresher(
    private val refreshCollection: RefreshCollectionUseCase,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(RefreshUiState())
    private var clearJob: Job? = null

    /** Observable refresh state. */
    val state: StateFlow<RefreshUiState> = mutableState.asStateFlow()

    /** Starts a refresh, unless one is already running. */
    fun refresh() {
        if (mutableState.value.isRefreshing) return
        clearJob?.cancel()
        mutableState.value = RefreshUiState(isRefreshing = true)
        scope.launch {
            val message = refreshCollection().toMessage()
            mutableState.value = RefreshUiState(message = message)
            clearJob =
                scope.launch {
                    delay(MESSAGE_DURATION_MS)
                    mutableState.update { if (it.isRefreshing) it else it.copy(message = null) }
                }
        }
    }

    private fun com.thevault.domain.usecase.CollectionRefresh?.toMessage(): RefreshMessage =
        when {
            this == null -> RefreshMessage("Serveur injoignable : réessaie plus tard.", success = false)
            sheets == 0 -> RefreshMessage("Fiches à jour · $quotes cotes eBay reçues", success = true)
            else -> RefreshMessage("$sheets fiches mises à jour · $quotes cotes eBay reçues", success = true)
        }

    private companion object {
        const val MESSAGE_DURATION_MS = 4_000L
    }
}
