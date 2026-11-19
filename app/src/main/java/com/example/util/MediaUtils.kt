package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

object MediaUtils {
    private const val DATA_IMAGE_PREFIX = "data:image/jpeg;base64,"
    private val bitmapCache = object : LruCache<String, Bitmap>(48) {}

    suspend fun uriToCompressedBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 820,
        quality: Int = 72
    ): String? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            var sampleSize = 1
            while (max(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > maxDimension * 2) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@withContext null

            val scale = minOf(
                maxDimension.toFloat() / decoded.width.toFloat(),
                maxDimension.toFloat() / decoded.height.toFloat(),
                1f
            )
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true
                ).also { if (it != decoded) decoded.recycle() }
            } else {
                decoded
            }

            var currentQuality = quality
            var bytes: ByteArray
            do {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, currentQuality, out)
                bytes = out.toByteArray()
                currentQuality -= 10
            } while (bytes.size > 240_000 && currentQuality >= 30)

            scaled.recycle()
            DATA_IMAGE_PREFIX + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun extractVideoThumbnailBase64(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val frame = retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
                    ?: return@withContext null
                val scale = minOf(640f / frame.width.toFloat(), 640f / frame.height.toFloat(), 1f)
                val scaled = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        frame,
                        (frame.width * scale).toInt().coerceAtLeast(1),
                        (frame.height * scale).toInt().coerceAtLeast(1),
                        true
                    ).also { if (it != frame) frame.recycle() }
                } else {
                    frame
                }
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
                scaled.recycle()
                DATA_IMAGE_PREFIX + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            } catch (_: Exception) {
                null
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }

    fun decodeBase64Bitmap(data: String?): Bitmap? {
        if (data.isNullOrBlank()) return null
        val key = data.hashCode().toString() + "_" + data.length
        bitmapCache.get(key)?.let { return it }
        return try {
            val raw = if (data.startsWith("data:image")) {
                data.substringAfter("base64,", "")
            } else {
                data
            }
            if (raw.isBlank() || data.startsWith("http")) return null
            val decodedBytes = Base64.decode(raw, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.also {
                bitmapCache.put(key, it)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun extractHashtags(text: String): List<String> {
        val regex = Regex("#([A-Za-z0-9_]{2,30})")
        return regex.findAll(text)
            .map { it.groupValues[1].lowercase(Locale.ROOT) }
            .distinct()
            .take(15)
            .toList()
    }

    fun formatRelativeTime(timestamp: Timestamp?): String {
        val ts = timestamp ?: return "Just now"
        val diffSeconds = ((System.currentTimeMillis() - ts.toDate().time) / 1000L).coerceAtLeast(0L)
        return when {
            diffSeconds < 60L -> "Just now"
            diffSeconds < 3600L -> "${diffSeconds / 60L}m"
            diffSeconds < 86400L -> "${diffSeconds / 3600L}h"
            diffSeconds < 604800L -> "${diffSeconds / 86400L}d"
            else -> "${diffSeconds / 604800L}w"
        }
    }

    fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return formatter.format(amount)
    }
}
