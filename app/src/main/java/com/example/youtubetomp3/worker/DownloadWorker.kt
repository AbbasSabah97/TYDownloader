package com.example.youtubetomp3.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

class DownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()

        // تغيير مسار الحفظ إلى مجلد التطبيق الخاص لتجنب مشاكل الصلاحيات
        val downloadDir = File(
            applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            "YouTubeToMp3"
        ).apply { mkdirs() }

        val request = YoutubeDLRequest(url)
        request.addOption("-x")
        request.addOption("--audio-format", "mp3")
        request.addOption("--embed-thumbnail")
        request.addOption("--add-metadata")
        request.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")

        return try {
            // تنفيذ التحميل
            YoutubeDL.getInstance().execute(request)
            
            // إظهار إشعار بالنجاح
            showNotification("تم التحميل بنجاح", "تم حفظ الملف الصوتي في مجلد التطبيق.")
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // إظهار إشعار بالفشل
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
            .setSmallIcon(android.R.drawable.stat_sys_download_done) // أيقونة افتراضية
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
