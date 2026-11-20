package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

private const val MAX_AVATAR_PX = 256

/**
 * Copies and downscales a Photo Picker / gallery URI into app-private internal storage.
 * Copies the URI stream ONCE to a temporary cache file so OEM ContentProviders that revoke
 * single-use URIs never fail or crash, and catches Throwable (including OutOfMemoryError).
 * Works cleanly from Android 8 (API 26) through Android 16 with zero storage permissions.
 */
fun savePickedAvatarToInternalStorage(
    context: Context,
    sourceUri: Uri,
    previousPath: String
): String? {
    var tempFile: File? = null
    return try {
        val resolver = context.contentResolver
        val cacheTemp = File(context.cacheDir, "masrofy_avatar_tmp_${System.currentTimeMillis()}.img")
        tempFile = cacheTemp

        // Single stream read from the external URI into app cache
        val copiedBytes = resolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(cacheTemp).use { output ->
                input.copyTo(output)
            }
        } ?: 0L
        if (copiedBytes <= 0L || !cacheTemp.exists() || cacheTemp.length() <= 0L) {
            return null
        }

        // Pass 1: measure bounds safely from local cache file
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(cacheTemp.absolutePath, boundsOpts)
        val origW = boundsOpts.outWidth
        val origH = boundsOpts.outHeight
        if (origW <= 0 || origH <= 0) return null

        var sampleSize = 1
        while (max(origW / sampleSize, origH / sampleSize) > MAX_AVATAR_PX * 2) {
            sampleSize *= 2
        }

        // Pass 2: decode downsampled bitmap from local cache file
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(cacheTemp.absolutePath, decodeOpts) ?: return null

        // Center-crop square and scale down cleanly to MAX_AVATAR_PX
        val size = minOf(decoded.width, decoded.height)
        if (size <= 0) {
            decoded.recycle()
            return null
        }
        val xOff = (decoded.width - size) / 2
        val yOff = (decoded.height - size) / 2
        val cropped = Bitmap.createBitmap(decoded, xOff, yOff, size, size)
        if (cropped != decoded) decoded.recycle()

        val finalBmp = if (size > MAX_AVATAR_PX) {
            Bitmap.createScaledBitmap(cropped, MAX_AVATAR_PX, MAX_AVATAR_PX, true).also {
                if (it != cropped) cropped.recycle()
            }
        } else {
            cropped
        }

        val outFile = File(context.filesDir, "masrofy_avatar_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { fos ->
            finalBmp.compress(Bitmap.CompressFormat.JPEG, 84, fos)
        }
        finalBmp.recycle()

        // Clean up previous avatar file so internal storage never accumulates stale images
        if (previousPath.isNotBlank() && previousPath != outFile.absolutePath) {
            runCatching { File(previousPath).delete() }
        }

        outFile.absolutePath
    } catch (_: Throwable) {
        null
    } finally {
        tempFile?.let { runCatching { it.delete() } }
    }
}

fun deleteLocalAvatarFile(path: String) {
    if (path.isNotBlank()) {
        runCatching { File(path).delete() }
    }
}

fun loadLocalAvatarImageBitmap(path: String): ImageBitmap? {
    if (path.isBlank()) return null
    val file = File(path)
    if (!file.exists() || !file.isFile) return null
    return try {
        val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
        BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
    } catch (_: Throwable) {
        null
    }
}
