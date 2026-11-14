package com.thevault.data.repository

import com.thevault.data.mapper.toDomain
import com.thevault.data.mapper.toDto
import com.thevault.data.remote.VaultRemoteDataSource
import com.thevault.data.remote.dto.ShareDto
import com.thevault.data.remote.dto.ShareRequestDto
import com.thevault.domain.model.Account
import com.thevault.domain.model.ShareLink
import com.thevault.domain.model.ShareScope
import com.thevault.domain.model.VaultContent
import com.thevault.domain.repository.SettingsRepository
import com.thevault.domain.repository.VaultRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Clock

/**
 * [VaultRepository] keeping the session in the local settings.
 *
 * @param remote Backend API.
 * @param settings Local settings (token, email, name).
 * @param webOrigin Origin of the web app serving `/share/{token}`.
 * @param clock Time source.
 */
class VaultRepositoryImpl(
    private val remote: VaultRemoteDataSource,
    private val settings: SettingsRepository,
    private val webOrigin: String,
    private val clock: Clock = Clock.System,
) : VaultRepository {
    override fun observeAccount(): Flow<Account?> =
        combine(settings.observe(KEY_TOKEN), settings.observe(KEY_EMAIL), settings.observe(KEY_NAME)) { token, email, name ->
            if (token.isNullOrBlank() || email == null) null else Account(email, name.orEmpty())
        }

    override suspend fun login(
        email: String,
        password: String,
    ): Result<Account> =
        attempt {
            val response = remote.login(email, password)
            settings.set(KEY_TOKEN, response.token)
            settings.set(KEY_EMAIL, response.user.email)
            settings.set(KEY_NAME, response.user.name)
            Account(response.user.email, response.user.name)
        }

    override suspend fun logout() {
        settings.set(KEY_TOKEN, "")
    }

    override suspend fun upload(content: VaultContent): Result<String> =
        attempt { remote.putBackup(token(), content.toDto(clock.now().toString())).updatedAt }

    override suspend fun download(): Result<VaultContent> = attempt { remote.getBackup(token()).toDomain() }

    override suspend fun shares(): Result<List<ShareLink>> = attempt { remote.listShares(token()).mapNotNull { it.toDomain() } }

    override suspend fun createShare(
        scope: ShareScope,
        title: String,
        hidePrices: Boolean,
    ): Result<ShareLink> =
        attempt {
            requireNotNull(remote.createShare(token(), ShareRequestDto(scope.key, title.trim(), hidePrices)).toDomain())
        }

    override suspend fun revokeShare(token: String): Result<Unit> = attempt { remote.revokeShare(token(), token) }

    private suspend fun token(): String = requireNotNull(settings.get(KEY_TOKEN)?.takeIf { it.isNotBlank() }) { "Non connecté" }

    private fun ShareDto.toDomain(): ShareLink? {
        val s = ShareScope.entries.find { it.key == scope } ?: return null
        return ShareLink(token, s, title, hidePrices, "$webOrigin/share/$token")
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> attempt(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private companion object {
        const val KEY_TOKEN = "account.token"
        const val KEY_EMAIL = "account.email"
        const val KEY_NAME = "account.name"
    }
}
