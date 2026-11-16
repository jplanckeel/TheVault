package com.thevault.domain.usecase

import com.thevault.domain.model.Account
import com.thevault.domain.model.BackupVersion
import com.thevault.domain.model.ShareLink
import com.thevault.domain.model.ShareScope
import com.thevault.domain.model.VaultContent
import com.thevault.domain.repository.AccessoryRepository
import com.thevault.domain.repository.ConsoleRepository
import com.thevault.domain.repository.GameRepository
import com.thevault.domain.repository.SetRepository
import com.thevault.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Account of the device.
 *
 * @param repository Vault repository.
 */
class AccountUseCase(
    private val repository: VaultRepository,
) {
    /** Signed-in account, null when signed out. */
    operator fun invoke(): Flow<Account?> = repository.observeAccount()
}

/**
 * Signs in or out.
 *
 * @param repository Vault repository.
 */
class SignInUseCase(
    private val repository: VaultRepository,
) {
    /** Signs in with [email] and [password], or signs out when [email] is null. */
    suspend operator fun invoke(
        email: String?,
        password: String = "",
    ): Result<Account?> =
        if (email == null) {
            repository.logout()
            Result.success(null)
        } else {
            repository.login(email, password)
        }
}

/**
 * Uploads the whole collection to the account.
 *
 * @param games Games.
 * @param consoles Consoles.
 * @param accessories Accessories.
 * @param sets Completion lists.
 * @param repository Vault repository.
 */
class BackupVaultUseCase(
    private val games: GameRepository,
    private val consoles: ConsoleRepository,
    private val accessories: AccessoryRepository,
    private val sets: SetRepository,
    private val repository: VaultRepository,
) {
    /**
     * Uploads everything, with the effective [values] for share links; returns the backup time.
     * Fails with [com.thevault.domain.model.BackupShrinkException] when it would replace a much
     * bigger backup, unless [force].
     */
    suspend operator fun invoke(
        values: Map<String, Double?>,
        force: Boolean = false,
    ): Result<String> =
        repository.upload(
            VaultContent(
                games.observeAll().first(),
                consoles.observeAll().first(),
                accessories.observeAll().first(),
                sets.observe().first(),
                values,
            ),
            force,
        )
}

/**
 * Restores the backup of the account: every item and list is written back (same
 * identifiers, so nothing is duplicated); local items absent from the backup are kept.
 *
 * @param games Games.
 * @param consoles Consoles.
 * @param accessories Accessories.
 * @param sets Completion lists.
 * @param repository Vault repository.
 */
class RestoreVaultUseCase(
    private val games: GameRepository,
    private val consoles: ConsoleRepository,
    private val accessories: AccessoryRepository,
    private val sets: SetRepository,
    private val repository: VaultRepository,
) {
    /** Restores the current backup, or the saved [version]; returns the number of items written. */
    suspend operator fun invoke(version: Long? = null): Result<Int> =
        repository.download(version).map { content ->
            content.games.forEach { games.save(it) }
            content.consoles.forEach { consoles.save(it) }
            content.accessories.forEach { accessories.save(it) }
            content.sets.forEach { sets.save(it) }
            content.games.size + content.consoles.size + content.accessories.size
        }
}

/**
 * Share links of the account.
 *
 * @param repository Vault repository.
 */
class ShareLinksUseCase(
    private val repository: VaultRepository,
) {
    /** Lists the links. */
    suspend operator fun invoke(): Result<List<ShareLink>> = repository.shares()
}

/**
 * Creates or revokes a share link.
 *
 * @param repository Vault repository.
 */
class EditShareUseCase(
    private val repository: VaultRepository,
) {
    /** Creates a link of [scope], or revokes [revoke] when given. */
    suspend operator fun invoke(
        scope: ShareScope,
        title: String,
        hidePrices: Boolean,
        revoke: String? = null,
    ): Result<ShareLink?> =
        if (revoke != null) repository.revokeShare(revoke).map { null } else repository.createShare(scope, title, hidePrices)
}

/**
 * Backups saved on the account (the server keeps the latest ten).
 *
 * @param repository Vault repository.
 */
class BackupVersionsUseCase(
    private val repository: VaultRepository,
) {
    /** Saved backups, newest first. */
    suspend operator fun invoke(): Result<List<BackupVersion>> = repository.backupVersions()
}
