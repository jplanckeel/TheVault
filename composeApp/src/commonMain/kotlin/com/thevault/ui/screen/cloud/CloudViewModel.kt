package com.thevault.ui.screen.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Account
import com.thevault.domain.model.ShareLink
import com.thevault.domain.model.ShareScope
import com.thevault.domain.usecase.AccountUseCase
import com.thevault.domain.usecase.BackupVaultUseCase
import com.thevault.domain.usecase.EditShareUseCase
import com.thevault.domain.usecase.RestoreVaultUseCase
import com.thevault.domain.usecase.ShareLinksUseCase
import com.thevault.domain.usecase.SignInUseCase
import com.thevault.ui.screen.collection.VaultEntriesSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State of the account screen.
 *
 * @property busy Whether a request is running.
 * @property message Last result, null when none.
 * @property error Whether [message] is an error.
 * @property links Share links of the account.
 * @property created Link just created (to share it).
 */
data class CloudUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val error: Boolean = false,
    val links: List<ShareLink> = emptyList(),
    val created: ShareLink? = null,
)

/** ViewModel of the account, backup and share screen. */
@Suppress("LongParameterList")
class CloudViewModel(
    account: AccountUseCase,
    private val signIn: SignInUseCase,
    private val backup: BackupVaultUseCase,
    private val restore: RestoreVaultUseCase,
    private val shareLinks: ShareLinksUseCase,
    private val editShare: EditShareUseCase,
    private val source: VaultEntriesSource,
) : ViewModel() {
    private val state = MutableStateFlow(CloudUiState())

    /** Observable UI state. */
    val uiState: StateFlow<CloudUiState> = state.asStateFlow()

    /** Signed-in account, null when signed out. */
    val account: StateFlow<Account?> = account().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Signs in. */
    fun login(
        email: String,
        password: String,
    ) = run("Connexion…") {
        signIn(email, password).map { "Connecté." }.also { if (it.isSuccess) loadLinks() }
    }

    /** Signs out. */
    fun logout() {
        viewModelScope.launch {
            signIn(null)
            state.value = CloudUiState(message = "Déconnecté.")
        }
    }

    /** Uploads the whole collection. */
    fun backupNow() = run("Sauvegarde…") { upload().map { "Sauvegarde envoyée." } }

    /** Restores the backup of the account. */
    fun restoreNow() = run("Restauration…") { restore().map { "$it objets restaurés." } }

    /** Loads the share links. */
    fun loadLinks() {
        viewModelScope.launch { shareLinks().onSuccess { links -> state.update { it.copy(links = links) } } }
    }

    /** Uploads a fresh backup, then creates a link. */
    fun share(
        scope: ShareScope,
        title: String,
        hidePrices: Boolean,
    ) = run("Création du lien…") {
        upload().mapCatching { editShare(scope, title, hidePrices).getOrThrow() }.map { link ->
            state.update { it.copy(created = link) }
            loadLinks()
            "Lien créé."
        }
    }

    /** Revokes a link. */
    fun revoke(link: ShareLink) =
        run("Révocation…") {
            editShare(link.scope, "", false, revoke = link.token).map {
                loadLinks()
                "Lien révoqué."
            }
        }

    /** Forgets the link just created (after sharing it). */
    fun consumeCreated() = state.update { it.copy(created = null) }

    private suspend fun upload(): Result<String> {
        val values = source.entries().first().associate { it.id to it.value }
        return backup(values)
    }

    private fun run(
        pending: String,
        block: suspend () -> Result<String>,
    ) {
        state.update { it.copy(busy = true, message = pending, error = false) }
        viewModelScope.launch {
            val result = block()
            state.update {
                it.copy(
                    busy = false,
                    message = result.getOrElse { e -> "Échec : ${e.message ?: "serveur injoignable"}" },
                    error = result.isFailure,
                )
            }
        }
    }
}
