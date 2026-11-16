package com.thevault.domain.usecase

import com.thevault.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes the photos of the user's copies.
 *
 * @param repository Photo repository.
 */
class ObserveItemPhotosUseCase(
    private val repository: PhotoRepository,
) {
    /** Photo URI keyed by item identifier. */
    operator fun invoke(): Flow<Map<String, String>> = repository.observe()
}

/**
 * Makes a photo the picture of an item; it is shown instead of the catalogue cover.
 *
 * @param repository Photo repository.
 */
class SetItemPhotoUseCase(
    private val repository: PhotoRepository,
) {
    /**
     * Sets the photo of [itemId].
     *
     * @return URIs of the replaced photos, whose files can be deleted.
     */
    suspend operator fun invoke(
        itemId: String,
        uri: String,
    ): List<String> = repository.set(itemId, uri)
}

/**
 * Removes the photo of an item; the catalogue cover is shown again.
 *
 * @param repository Photo repository.
 */
class RemoveItemPhotoUseCase(
    private val repository: PhotoRepository,
) {
    /**
     * Removes the photo of [itemId].
     *
     * @return URIs of the removed photos, whose files can be deleted.
     */
    suspend operator fun invoke(itemId: String): List<String> = repository.remove(itemId)
}
