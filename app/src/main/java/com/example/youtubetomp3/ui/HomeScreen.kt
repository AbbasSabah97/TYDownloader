package com.example.youtubetomp3.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.URLUtil
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.youtubetomp3.ui.theme.YouTubeToMp3Theme
import java.net.URL

private val AppBackground = Color(0xFF101216)
private val Panel = Color(0xFF1B2027)
private val Mint = Color(0xFF43D6A0)
private val Muted = Color(0xFF9AA5B1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var isArabic by remember { mutableStateOf(true) }
    var selectedFile by remember { mutableStateOf<String?>(null) }
    var url by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf("320 kbps") }
    var qualityMenu by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("جاهز") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            selectedFile = getDisplayName(context, uri)
            status = if (isArabic) "تم اختيار الملف" else "File selected"
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Mint,
            background = AppBackground,
            surface = Panel,
            onPrimary = Color(0xFF06251A),
            onBackground = Color.White,
            onSurface = Color.White,
            secondary = Mint
        )
    ) {
        Scaffold(containerColor = AppBackground) { insets ->
            Column(
                modifier = Modifier.fillMaxSize().padding(insets).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(54.dp).background(Panel, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("♫", color = Mint, fontSize = 30.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Sonic MP3", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("AUDIO CONVERTER", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                    TextButton(onClick = { isArabic = !isArabic }) {
                        Text(if (isArabic) "EN" else "ع", color = Mint, fontWeight = FontWeight.Bold)
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (isArabic) "حوّل ملفاتك الصوتية" else "Convert your media", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (isArabic) "اختر فيديو أو ملفاً صوتياً من هاتفك. دعم التحويل الفعلي إلى MP3 قيد التطوير."
                            else "Choose a video or audio file from your phone. MP3 encoding is under development.",
                            color = Muted, fontSize = 13.sp
                        )
                        Button(
                            onClick = { picker.launch(arrayOf("audio/*", "video/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color(0xFF06251A))
                        ) { Text(if (isArabic) "اختيار ملف من الهاتف" else "Choose file from phone") }
                        selectedFile?.let {
                            Text((if (isArabic) "الملف المحدد: " else "Selected: ") + it, color = Mint, fontSize = 12.sp)
                        }
                        ExposedDropdownMenuBox(
                            expanded = qualityMenu,
                            onExpandedChange = { qualityMenu = !qualityMenu }
                        ) {
                            OutlinedTextField(
                                value = quality,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (isArabic) "جودة MP3 المطلوبة" else "Target MP3 quality") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = qualityMenu) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = qualityMenu, onDismissRequest = { qualityMenu = false }) {
                                listOf("128 kbps", "192 kbps", "256 kbps", "320 kbps").forEach { item ->
                                    DropdownMenuItem(text = { Text(item) }, onClick = { quality = item; qualityMenu = false })
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { status = if (isArabic) "محرك الترميز سيُفعّل في المرحلة التالية" else "Encoder will be enabled in the next development stage" },
                            enabled = selectedFile != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (isArabic) "تحويل إلى MP3" else "Convert to MP3") }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (isArabic) "تنزيل ملف من رابط مباشر" else "Download from direct URL", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (isArabic) "يدعم روابط الملفات المباشرة التي يسمح مالكها بتنزيلها. لا يستخرج الصوت من روابط YouTube."
                            else "Supports direct media URLs whose owner permits downloading. YouTube link extraction is not supported.",
                            color = Muted, fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("https://example.com/media.mp4") }
                        )
                        Button(
                            onClick = {
                                val parsed = runCatching { URL(url.trim()) }.getOrNull()
                                if (parsed == null || parsed.protocol !in listOf("http", "https")) {
                                    status = if (isArabic) "أدخل رابط HTTP أو HTTPS صحيحاً" else "Enter a valid HTTP or HTTPS URL"
                                } else {
                                    runCatching {
                                        enqueueDirectDownload(context, url.trim())
                                        status = if (isArabic) "بدأ التنزيل عبر مدير أندرويد" else "Download started"
                                        url = ""
                                    }.onFailure {
                                        status = if (isArabic) "تعذر بدء التنزيل" else "Could not start download"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color(0xFF06251A))
                        ) { Text(if (isArabic) "بدء التنزيل" else "Start download") }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(if (isArabic) "حالة التطبيق" else "App status", fontWeight = FontWeight.SemiBold)
                        Text(status, color = Muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("Sonic MP3 • v0.1.0", color = Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

private fun getDisplayName(context: Context, uri: Uri): String {
    var name = "media_file"
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) name = cursor.getString(index) ?: name
        }
    }
    return name
}

private fun enqueueDirectDownload(context: Context, url: String) {
    val fileName = URLUtil.guessFileName(url, null, null)
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(fileName)
        .setDescription("Sonic MP3 authorized direct download")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(false)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SonicMP3/$fileName")
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    manager.enqueue(request)
}
