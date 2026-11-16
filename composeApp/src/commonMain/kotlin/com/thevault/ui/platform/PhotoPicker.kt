package com.thevault.ui.platform

import androidx.compose.runtime.Composable

/**
 * Opens the system photo picker or the camera. The chosen photo is resized, saved in the
 * app storage and handed back as a local URI.
 *
 * @property pickFromGallery Opens the photo picker.
 * @property takePhoto Opens the camera.
 */
data class PhotoPicker(
    val pickFromGallery: () -> Unit,
    val takePhoto: () -> Unit,
)

/**
 * Photo picker calling [onPhoto] with the URI of the saved photo, or null when the
 * platform does not support photos yet.
 */
@Composable
expect fun rememberPhotoPicker(onPhoto: (String) -> Unit): PhotoPicker?

/** Deletes a photo file saved by [rememberPhotoPicker]; other URIs are ignored. */
expect fun deleteLocalPhoto(uri: String)
