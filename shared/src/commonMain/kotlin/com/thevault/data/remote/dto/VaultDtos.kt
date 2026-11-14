package com.thevault.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body of POST /auth/login.
 *
 * @property email Email.
 * @property password Password.
 */
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

/**
 * Answer of POST /auth/login.
 *
 * @property token JWT.
 * @property user Account.
 */
@Serializable
data class LoginResponseDto(
    val token: String,
    val user: UserDto,
)

/**
 * Account.
 *
 * @property email Email.
 * @property name Display name.
 */
@Serializable
data class UserDto(
    val email: String = "",
    val name: String = "",
)

/**
 * Answer of PUT /backup.
 *
 * @property updatedAt Time the backup was stored.
 */
@Serializable
data class BackupSavedDto(
    @SerialName("updated_at") val updatedAt: String,
)

/**
 * Body of POST /shares.
 *
 * @property scope `collection` or `wishlist`.
 * @property title Title shown on the page.
 * @property hidePrices Hides the values.
 */
@Serializable
data class ShareRequestDto(
    val scope: String,
    val title: String,
    @SerialName("hide_prices") val hidePrices: Boolean,
)

/**
 * Share link.
 *
 * @property token Token of the public URL.
 * @property scope `collection` or `wishlist`.
 * @property title Title.
 * @property hidePrices Whether values are hidden.
 * @property createdAt Creation time.
 */
@Serializable
data class ShareDto(
    val token: String,
    val scope: String,
    val title: String = "",
    @SerialName("hide_prices") val hidePrices: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
)
