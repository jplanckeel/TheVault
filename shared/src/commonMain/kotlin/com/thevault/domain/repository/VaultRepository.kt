package com.thevault.domain.repository

import com.thevault.domain.model.Account
import com.thevault.domain.model.BackupVersion
import com.thevault.domain.model.ShareLink
import com.thevault.domain.model.ShareScope
import com.thevault.domain.model.VaultContent
import kotlinx.coroutines.flow.Flow

/** Backend account, backup and share links. Every call fails when signed out or offline. */
interface VaultRepository {
    /** Signed-in account, null when signed out. */
    fun observeAccount(): Flow<Account?>

    /** Signs in. */
    suspend fun login(
        email: String,
        password: String,
    ): Result<Account>

    /** Signs out (forgets the token). */
    suspend fun logout()

    /** Uploads [content]; returns the backup time. */
    suspend fun upload(
        content: VaultContent,
        force: Boolean = false,
    ): Result<String>

    /** Downloads the backup. */
    suspend fun download(version: Long? = null): Result<VaultContent>

    /** Backups saved on the account, newest first. */
    suspend fun backupVersions(): Result<List<BackupVersion>>

    /** Lists the share links. */
    suspend fun shares(): Result<List<ShareLink>>

    /** Creates a share link. */
    suspend fun createShare(
        scope: ShareScope,
        title: String,
        hidePrices: Boolean,
    ): Result<ShareLink>

    /** Revokes a share link. */
    suspend fun revokeShare(token: String): Result<Unit>
}
