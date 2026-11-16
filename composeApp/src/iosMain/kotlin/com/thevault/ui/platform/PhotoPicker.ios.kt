package com.thevault.ui.platform

import androidx.compose.runtime.Composable

/** Photos of copies are not available on iOS yet. */
@Composable
actual fun rememberPhotoPicker(onPhoto: (String) -> Unit): PhotoPicker? = null

/** Nothing to delete: iOS never saves photos. */
actual fun deleteLocalPhoto(uri: String) = Unit
