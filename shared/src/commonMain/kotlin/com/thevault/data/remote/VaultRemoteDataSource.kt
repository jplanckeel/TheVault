package com.thevault.data.remote

import com.thevault.data.remote.dto.BackupSavedDto
import com.thevault.data.remote.dto.LoginRequestDto
import com.thevault.data.remote.dto.LoginResponseDto
import com.thevault.data.remote.dto.ShareDto
import com.thevault.data.remote.dto.ShareRequestDto
import com.thevault.data.remote.dto.VaultBackupDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Account, backup and share links of the TheVault back-office. */
interface VaultRemoteDataSource {
    /** Signs in and returns the JWT and the account. */
    suspend fun login(
        email: String,
        password: String,
    ): LoginResponseDto

    /** Stores the backup of the signed-in account. */
    suspend fun putBackup(
        token: String,
        backup: VaultBackupDto,
    ): BackupSavedDto

    /** Downloads the backup of the signed-in account. */
    suspend fun getBackup(token: String): VaultBackupDto

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
    ): BackupSavedDto =
        httpClient
            .put("$baseUrl/backup") {
                json(token)
                setBody(backup)
            }.body()

    override suspend fun getBackup(token: String): VaultBackupDto = httpClient.get("$baseUrl/backup") { json(token) }.body()

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
