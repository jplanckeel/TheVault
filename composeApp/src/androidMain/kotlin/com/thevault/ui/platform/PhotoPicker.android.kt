package com.thevault.ui.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Longest side of a saved photo, in pixels. */
private const val MAX_SIDE = 1600

/** JPEG quality of a saved photo. */
private const val JPEG_QUALITY = 85

/** Directory of the saved photos, under the app files. */
private const val PHOTO_DIR = "photos"

/** FileProvider authority suffix declared in the androidApp manifest. */
private const val AUTHORITY_SUFFIX = ".photos"

/** Android photo picker (no permission needed) and camera (system camera app). */
@Composable
actual fun rememberPhotoPicker(onPhoto: (String) -> Unit): PhotoPicker? {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(onPhoto)
    val capture = remember(context) { File(context.cacheDir, "captures").apply { mkdirs() }.resolve("capture.jpg") }

    fun store(source: Uri) {
        scope.launch {
            val saved = withContext(Dispatchers.IO) { runCatching { savePhoto(context, source) }.getOrNull() }
            if (saved != null) latest(saved)
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::store) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) store(Uri.fromFile(capture)) }
    return remember(context, gallery, camera) {
        PhotoPicker(
            pickFromGallery = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            takePhoto = {
                // No camera app: nothing happens rather than a crash.
                runCatching { camera.launch(FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, capture)) }
            },
        )
    }
}

/** Deletes a photo of the app storage (`…/files/photos/photo-*.jpg`). */
actual fun deleteLocalPhoto(uri: String) {
    val path = Uri.parse(uri).takeIf { it.scheme == "file" }?.path ?: return
    val file = File(path)
    if (file.parentFile?.name == PHOTO_DIR && file.name.startsWith("photo-")) file.delete()
}

/** Decodes [source], applies its EXIF rotation, shrinks it and saves it as a JPEG. */
private fun savePhoto(
    context: Context,
    source: Uri,
): String {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight) }
    val decoded =
        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("unreadable image")
    val rotation = resolver.openInputStream(source)?.use { exifRotation(ExifInterface(it)) } ?: 0
    val bitmap = transform(decoded, rotation)
    val dir = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }
    val file = File(dir, "photo-${System.currentTimeMillis()}.jpg")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
    return Uri.fromFile(file).toString()
}

/** Power-of-two decoding step keeping the longest side above [MAX_SIDE]. */
private fun sampleSize(
    width: Int,
    height: Int,
): Int {
    var sample = 1
    while (maxOf(width, height) / (sample * 2) >= MAX_SIDE) sample *= 2
    return sample
}

@Suppress("MagicNumber")
private fun exifRotation(exif: ExifInterface): Int =
    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

/** Rotates [bitmap] by [degrees] and scales it down to [MAX_SIDE]. */
private fun transform(
    bitmap: Bitmap,
    degrees: Int,
): Bitmap {
    val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height))
    if (degrees == 0 && scale == 1f) return bitmap
    val matrix =
        Matrix().apply {
            postScale(scale, scale)
            postRotate(degrees.toFloat())
        }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
