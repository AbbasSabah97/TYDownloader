package com.example.youtubetomp3.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

/**
 * Downloads only a direct HTTP(S) media URL supplied by the user.
 * This worker intentionally does not extract content from streaming platforms.
 */
class DownloadWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val source = inputData.getString("url") ?: return@withContext Result.failure()
        val parsed = runCatching { URL(source) }.getOrNull()
            ?: return@withContext Result.failure()
        if (parsed.protocol !in listOf("http", "https")) return@withContext Result.failure()

        try {
            val directory = File(applicationContext.getExternalFilesDir(null), "SonicMP3").apply { mkdirs() }
            val fileName = parsed.path.substringAfterLast('/').ifBlank { "downloaded_media" }
            val target = File(directory, fileName)
            parsed.openConnection().apply {
                connectTimeout = 20_000
                readTimeout = 30_000
            }.getInputStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
