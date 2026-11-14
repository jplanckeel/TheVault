@file:Suppress("TooGenericExceptionCaught") // Network boundary: any failure means "offline, retry later".

package com.thevault.data.sync

import com.thevault.domain.usecase.SyncPendingItemsUseCase
import com.thevault.domain.usecase.SyncScheduler
import com.thevault.domain.usecase.SyncStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [SyncScheduler] running in an application-wide scope: saves return immediately and the
 * push happens shortly after (requests are coalesced).
 *
 * @param scope Application scope (outlives the screens).
 * @param syncPending Pushes the pending items.
 */
class BackgroundSyncScheduler(
    scope: CoroutineScope,
    private val syncPending: SyncPendingItemsUseCase,
) : SyncScheduler {
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val state = MutableStateFlow(SyncStatus())

    override val status: StateFlow<SyncStatus> = state.asStateFlow()

    init {
        scope.launch {
            requests.receiveAsFlow().collect {
                delay(DEBOUNCE_MS)
                state.update { it.copy(running = true) }
                val report =
                    try {
                        syncPending()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                state.update { SyncStatus(running = false, lastReport = report ?: it.lastReport) }
            }
        }
    }

    override fun request() {
        requests.trySend(Unit)
    }

    private companion object {
        const val DEBOUNCE_MS = 800L
    }
}
