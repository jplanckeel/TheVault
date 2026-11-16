package com.thevault.ui.screen.collection

import com.thevault.domain.usecase.ObserveItemPhotosUseCase
import com.thevault.domain.usecase.RemoveItemPhotoUseCase
import com.thevault.domain.usecase.SetItemPhotoUseCase
import com.thevault.ui.platform.deleteLocalPhoto
import kotlinx.coroutines.flow.Flow

/**
 * Photos of the user's own copies: shown instead of the catalogue cover. Replaced or
 * removed photo files are deleted from the device.
 *
 * @param observePhotos Observes the photos.
 * @param setPhoto Sets the photo of an item.
 * @param removePhoto Removes the photo of an item.
 * @param deleteFile Deletes a photo file.
 */
class ItemPhotos(
    private val observePhotos: ObserveItemPhotosUseCase,
    private val setPhoto: SetItemPhotoUseCase,
    private val removePhoto: RemoveItemPhotoUseCase,
    private val deleteFile: (String) -> Unit = ::deleteLocalPhoto,
) {
    /** Photo URI keyed by item identifier. */
    fun observe(): Flow<Map<String, String>> = observePhotos()

    /** Makes [uri] the photo of [itemId]. */
    suspend fun set(
        itemId: String,
        uri: String,
    ) = setPhoto(itemId, uri).forEach(deleteFile)

    /** Removes the photo of [itemId], if any. */
    suspend fun remove(itemId: String) = removePhoto(itemId).forEach(deleteFile)
}
