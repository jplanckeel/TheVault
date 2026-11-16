package com.thevault.ui.component

import androidx.compose.runtime.Composable
import com.thevault.ui.platform.rememberPhotoPicker

/**
 * Menu entries to photograph one's own copy, pick a photo or remove it. Empty on
 * platforms without photo support.
 *
 * @param hasPhoto Whether the item already shows the user's photo.
 * @param onPhoto Receives the URI of the saved photo.
 * @param onRemove Removes the photo.
 */
@Composable
fun photoMenuEntries(
    hasPhoto: Boolean,
    onPhoto: (String) -> Unit,
    onRemove: () -> Unit,
): List<MenuEntry> {
    val picker = rememberPhotoPicker(onPhoto) ?: return emptyList()
    return listOfNotNull(
        MenuEntry(if (hasPhoto) "Reprendre la photo" else "Photographier mon exemplaire", onClick = picker.takePhoto),
        MenuEntry(if (hasPhoto) "Changer de photo" else "Choisir une photo", onClick = picker.pickFromGallery),
        MenuEntry("Retirer ma photo", onClick = onRemove).takeIf { hasPhoto },
    )
}
