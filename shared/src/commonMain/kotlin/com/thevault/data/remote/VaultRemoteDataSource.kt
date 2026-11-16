package com.thevault.data.remote

import com.thevault.data.remote.dto.BackupConflictDto
import com.thevault.data.remote.dto.BackupSavedDto
import com.thevault.data.remote.dto.BackupVersionDto
import com.thevault.data.remote.dto.LoginRequestDto
import com.thevault.data.remote.dto.LoginResponseDto
import com.thevault.data.remote.dto.ShareDto
import com.thevault.data.remote.dto.ShareRequestDto
import com.thevault.data.remote.dto.VaultBackupDto
import com.thevault.domain.model.BackupShrinkException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/** Account, backup and share links of the TheVault back-office. */
interface VaultRemoteDataSource {
    /** Signs in and returns the JWT and the account. */
    suspend fun login(
        email: String,
        password: String,
    ): LoginResponseDto

    /**
     * Stores the backup of the signed-in account.
     *
     * @param force Replaces a backup holding many more items.
     * @throws com.thevault.domain.model.BackupShrinkException When the server asks for a confirmation.
     */
    suspend fun putBackup(
        token: String,
        backup: VaultBackupDto,
        force: Boolean = false,
    ): BackupSavedDto

    /** Downloads the current backup, or the saved [version]. */
    suspend fun getBackup(
        token: String,
        version: Long? = null,
    ): VaultBackupDto

    /** Saved backups, newest first. */
    suspend fun backupVersions(token: String): List<BackupVersionDto>

    /** Creates a share link. */
    suspend fun createShare(
        token: String,
        request: ShareRequestDto,
    ): ShareDto

    /** Lists the share links. */
    suspend fun listShares(token: String): List<ShareDto>

    /** Revokes a share link. */
    suspend fun revokeShare(
        token: String,
        shareToken: String,
    )
}

/**
 * [VaultRemoteDataSource] over Ktor.
 *
 * @param httpClient HTTP client.
 * @param baseUrl API base URL.
 */
class TheVaultBoVaultDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : VaultRemoteDataSource {
    private fun HttpRequestBuilder.json(token: String?) {
        expectSuccess = true
        token?.let { bearerAuth(it) }
        contentType(ContentType.Application.Json)
    }

    override suspend fun login(
        email: String,
        password: String,
    ): LoginResponseDto =
        httpClient
            .post("$baseUrl/auth/login") {
                json(null)
                setBody(LoginRequestDto(email.trim(), password))
            }.body()

    override suspend fun putBackup(
        token: String,
        backup: VaultBackupDto,
        force: Boolean,
    ): BackupSavedDto {
        val response =
            httpClient.put("$baseUrl/backup") {
                json(token)
                expectSuccess = false
                if (force) parameter("force", "1")
                setBody(backup)
            }
        if (response.status == HttpStatusCode.Conflict) {
            val conflict = response.body<BackupConflictDto>()
            throw BackupShrinkException(conflict.currentCount, conflict.newCount)
        }
        if (!response.status.isSuccess()) throw IllegalStateException("Sauvegarde refusée (${response.status.value})")
        return response.body()
    }

    override suspend fun getBackup(
        token: String,
        version: Long?,
    ): VaultBackupDto =
        httpClient
            .get("$baseUrl/backup") {
                json(token)
                version?.let { parameter("version", it) }
            }.body()

    override suspend fun backupVersions(token: String): List<BackupVersionDto> =
        httpClient.get("$baseUrl/backup/versions") { json(token) }.body()

    override suspend fun createShare(
        token: String,
        request: ShareRequestDto,
    ): ShareDto =
        httpClient
            .post("$baseUrl/shares") {
                json(token)
                setBody(request)
            }.body()

    override suspend fun listShares(token: String): List<ShareDto> = httpClient.get("$baseUrl/shares") { json(token) }.body()

    override suspend fun revokeShare(
        token: String,
        shareToken: String,
    ) {
        httpClient.delete("$baseUrl/shares/$shareToken") { json(token) }
    }
}
