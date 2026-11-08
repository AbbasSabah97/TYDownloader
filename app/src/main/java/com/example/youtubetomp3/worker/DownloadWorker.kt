package com.example.youtubetomp3.worker

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

class DownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()

        val downloadDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "YouTubeToMp3"
        ).apply { mkdirs() }

        val request = YoutubeDLRequest(url)
        request.addOption("-x")
        request.addOption("--audio-format", "mp3")
        request.addOption("--embed-thumbnail")
        request.addOption("--add-metadata")
        request.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")

        return try {
            YoutubeDL.getInstance().execute(request)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
