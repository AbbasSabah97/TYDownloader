package com.example.youtubetomp3.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

class DownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val TAG = "DownloadWorker"

    override suspend fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()

        val downloadDir = File(
            applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            "YouTubeToMp3"
        ).apply { mkdirs() }

        val request = YoutubeDLRequest(url)
        request.addOption("-x")
        request.addOption("--audio-format", "mp3")
        request.addOption("--embed-thumbnail")
        request.addOption("--add-metadata")
      // استخدام android_vr كخيار أول (لا يتطلب PO Token)، مع tv و android كخطة بديلة
        request.addOption("--extractor-args", "youtube:player_client=android_vr,tv,android;player_skip=webpage")
        request.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")

        return try {
            Log.d(TAG, "بدء التحميل للرابط: $url")
            
            // تم إصلاح الخطأ هنا بإضافة المتغير الثالث line
            YoutubeDL.getInstance().execute(request) { progress, etaInSeconds, line ->
                Log.d(TAG, "التقدم: $progress% - الوقت المتبقي: $etaInSeconds ثانية")
            }
            
            Log.d(TAG, "تم التحميل بنجاح")
            showNotification("تم التحميل بنجاح", "تم حفظ الملف الصوتي في مجلد التطبيق.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "فشل التحميل", e)
            showNotification("فشل التحميل", "حدث خطأ: ${e.message}")
            Result.failure()
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "download_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Download Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
