package com.example.youtubetomp3.worker

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.coroutines.resume

class ConvertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uri = inputData.getString(KEY_URI)?.let(Uri::parse) ?: return@withContext Result.failure()
        val bitrate = inputData.getInt(KEY_BITRATE, 320).coerceIn(128, 320)
        val title = inputData.getString(KEY_NAME)?.takeIf { it.isNotBlank() } ?: "Sonic audio"
        val input = File(applicationContext.cacheDir, "sonic-${UUID.randomUUID()}.media")
        val output = File(applicationContext.cacheDir, "sonic-${UUID.randomUUID()}.mp3")
        try {
            applicationContext.contentResolver.openInputStream(uri)?.use { source ->
                input.outputStream().use { target -> source.copyTo(target) }
            } ?: return@withContext Result.failure(workDataOf("error" to "Cannot read selected media"))

            val retriever = MediaMetadataRetriever()
            val durationMs = try {
                retriever.setDataSource(applicationContext, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } catch (_: Exception) { 0L } finally { retriever.release() }

            setProgress(workDataOf("progress" to 1, "phase" to "encoding"))
            val command = "-y -i " + input.absolutePath + " -map 0:a:0 -vn -c:a libmp3lame -b:a " + bitrate + "k -id3v2_version 3 " + output.absolutePath
            val session = suspendCancellableCoroutine { continuation ->
                val running = FFmpegKit.executeAsync(
                    command,
                    { completed -> if (continuation.isActive) continuation.resume(completed) },
                    { },
                    { stats ->
                        val percent = if (durationMs > 0L) ((stats.time * 100L / durationMs).toInt()).coerceIn(1, 99) else 0
                        setProgressAsync(workDataOf("progress" to percent, "phase" to "encoding"))
                    }
                )
                continuation.invokeOnCancellation { FFmpegKit.cancel(running.sessionId) }
            }
            if (!ReturnCode.isSuccess(session.returnCode) || !output.exists() || output.length() == 0L) {
                return@withContext Result.failure(workDataOf("error" to (session.failStackTrace ?: "MP3 encoding failed")))
            }

            val safeTitle = title.substringBeforeLast('.', title).replace(Regex("[^A-Za-z0-9 _-]"), "_").take(70)
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "$safeTitle.mp3")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Sonic MP3")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val resolver = applicationContext.contentResolver
            val destination = resolver.insert(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
                ?: return@withContext Result.failure(workDataOf("error" to "Cannot create MP3 output"))
            try {
                resolver.openOutputStream(destination, "w")?.use { target ->
                    output.inputStream().use { it.copyTo(target) }
                } ?: throw IllegalStateException("Cannot save MP3")
                resolver.update(destination, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            } catch (error: Exception) {
                resolver.delete(destination, null, null)
                throw error
            }
            setProgress(workDataOf("progress" to 100, "phase" to "completed"))
            Result.success(workDataOf("saved_uri" to destination.toString()))
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            Result.failure(workDataOf("error" to (error.message ?: "Conversion failed")))
        } finally {
            input.delete()
            output.delete()
        }
    }
    companion object {
        const val KEY_URI = "input_uri"
        const val KEY_BITRATE = "bitrate"
        const val KEY_NAME = "display_name"
        const val TAG = "sonic-conversion"
    }
}

class DirectDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val address = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val url = runCatching { URL(address) }.getOrNull() ?: return@withContext Result.failure()
        if (url.protocol !in listOf("http", "https")) return@withContext Result.failure()
        val host = url.host.lowercase()
        if (host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be" ||
            host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com")) {
            return@withContext Result.failure(workDataOf("error" to "Use platform-authorized YouTube download options."))
        }
        val folder = File(applicationContext.getExternalFilesDir(null), "SonicMP3/partials").apply { mkdirs() }
        val name = url.path.substringAfterLast('/').ifBlank { "downloaded-media" }.replace(Regex("[^A-Za-z0-9._-]"), "_").take(100)
        val partial = File(folder, "$name.part")
        var connection: HttpURLConnection? = null
        try {
            val oldSize = if (partial.exists()) partial.length() else 0L
            val activeConnection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000
                readTimeout = 30000
                instanceFollowRedirects = true
                if (oldSize > 0L) setRequestProperty("Range", "bytes=$oldSize-")
                connect()
            }
            connection = activeConnection
            val code = activeConnection.responseCode
            if (code !in 200..299) return@withContext Result.retry()
            val append = oldSize > 0L && code == HttpURLConnection.HTTP_PARTIAL
            val base = if (append) oldSize else 0L
            val total = if (append) activeConnection.getHeaderField("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
                ?: (base + activeConnection.contentLengthLong) else activeConnection.contentLengthLong
            activeConnection.inputStream.use { source ->
                partial.outputStream().use { target ->
                    val buffer = ByteArray(65536)
                    var copied = base
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = source.read(buffer)
                        if (count < 0) break
                        target.write(buffer, 0, count)
                        copied += count
                        val percent = if (total > 0L) (copied * 100L / total).toInt().coerceIn(0, 99) else 0
                        setProgressAsync(workDataOf("progress" to percent, "downloaded" to copied, "total" to total))
                    }
                }
            }
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, activeConnection.contentType?.substringBefore(';') ?: "application/octet-stream")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Sonic MP3")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = applicationContext.contentResolver
            val destination = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext Result.failure(workDataOf("error" to "Cannot create download destination"))
            try {
                resolver.openOutputStream(destination, "w")?.use { target ->
                    partial.inputStream().use { it.copyTo(target) }
                } ?: throw IllegalStateException("Cannot save downloaded file")
                resolver.update(destination, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            } catch (error: Exception) {
                resolver.delete(destination, null, null)
                throw error
            }
            partial.delete()
            setProgress(workDataOf("progress" to 100, "phase" to "completed"))
            Result.success(workDataOf("saved_uri" to destination.toString()))
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            Result.retry()
        } finally {
            connection?.disconnect()
        }
    }
    companion object {
        const val KEY_URL = "direct_url"
        const val TAG = "sonic-download"
    }
}
