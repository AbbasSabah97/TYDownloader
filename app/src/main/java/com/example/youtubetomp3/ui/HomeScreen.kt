package com.example.youtubetomp3.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.work.*
import com.example.youtubetomp3.worker.ConvertWorker
import com.example.youtubetomp3.worker.DirectDownloadWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

private val Bg = Color(0xFF101216)
private val Panel = Color(0xFF1B2027)
private val Mint = Color(0xFF43D6A0)
private val Muted = Color(0xFF9AA5B1)
private data class Track(val id: Long, val title: String, val artist: String, val uri: Uri, val duration: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val work = remember { WorkManager.getInstance(context) }
    var arabic by remember { mutableStateOf(true) }
    var tab by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Uri?>(null) }
    var pickedName by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var bitrate by remember { mutableIntStateOf(320) }
    var menu by remember { mutableStateOf(false) }
    var tracks by remember { mutableStateOf(emptyList<Track>()) }
    var permission by remember { mutableStateOf(hasAudioPermission(context)) }
    var current by remember { mutableStateOf<Track?>(null) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var status by remember { mutableStateOf("جاهز") }

    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(player) { onDispose { player.release() } }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(current) {
        current?.let {
            player.setMediaItem(MediaItem.fromUri(it.uri))
            player.prepare()
            player.play()
        }
    }
    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.takeIf { it > 0L } ?: 0L
            delay(500)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            picked = uri
            pickedName = queryName(context, uri) ?: "Sonic audio"
        }
    }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permission = granted
        if (granted) tracks = queryTracks(context)
    }
    val taskList by produceState(initialValue = emptyList<WorkInfo>(), work) {
        work.getWorkInfosByTagFlow("sonic-task").collect { value = it }
    }
    LaunchedEffect(tab, permission) {
        if (tab == 1 && permission) tracks = queryTracks(context)
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint, secondary = Mint, background = Bg, surface = Panel,
        onPrimary = Color(0xFF06251A), onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(containerColor = Bg) { inset ->
            Column(Modifier.fillMaxSize().padding(inset).padding(horizontal = 18.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).background(Panel, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                        Text("♫", color = Mint, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Sonic MP3", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("AUDIO STUDIO", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                    TextButton(onClick = { arabic = !arabic }) { Text(if (arabic) "EN" else "ع", color = Mint) }
                }
                TabRow(selectedTabIndex = tab, containerColor = Bg, contentColor = Mint) {
                    listOf(if (arabic) "تحويل" else "Convert", if (arabic) "المكتبة" else "Library", if (arabic) "التنزيلات" else "Downloads").forEachIndexed { index, title ->
                        Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title, fontSize = 12.sp) })
                    }
                }
                when (tab) {
                    0 -> Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(if (arabic) "تحويل الفيديو والصوت" else "Video & audio converter", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                                Text(if (arabic) "استخرج المسار الصوتي من ملف محلي واحفظه بصيغة MP3 باستخدام LAME." else "Extract audio from local media and encode it as MP3 using LAME.", color = Muted, fontSize = 12.sp)
                                Button(onClick = { picker.launch(arrayOf("audio/*", "video/*")) }, modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color(0xFF06251A))) {
                                    Text(if (arabic) "اختيار فيديو أو صوت" else "Choose video or audio")
                                }
                                if (pickedName.isNotBlank()) Text(pickedName, color = Mint, fontSize = 12.sp)
                                ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
                                    OutlinedTextField(
                                        value = "$bitrate kbps", onValueChange = {}, readOnly = true,
                                        label = { Text(if (arabic) "جودة MP3" else "MP3 bitrate") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menu) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                        listOf(128, 192, 256, 320).forEach { rate ->
                                            DropdownMenuItem(text = { Text("$rate kbps") }, onClick = { bitrate = rate; menu = false })
                                        }
                                    }
                                }
                                Button(onClick = {
                                    val source = picked
                                    if (source != null) {
                                        val request = OneTimeWorkRequestBuilder<ConvertWorker>()
                                            .setInputData(workDataOf(ConvertWorker.KEY_URI to source.toString(), ConvertWorker.KEY_BITRATE to bitrate, ConvertWorker.KEY_NAME to pickedName))
                                            .addTag("sonic-task").addTag(ConvertWorker.TAG).build()
                                        work.enqueue(request)
                                        status = if (arabic) "بدأ التحويل في الخلفية" else "Conversion started"
                                        tab = 2
                                    }
                                }, enabled = picked != null, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (arabic) "بدء التحويل إلى MP3" else "Convert to MP3")
                                }
                            }
                        }
                        Text(status, color = Muted, fontSize = 12.sp)
                        Text(if (arabic) "يتم حفظ ملفات MP3 في Music/Sonic MP3." else "MP3 files are saved in Music/Sonic MP3.", color = Muted, fontSize = 11.sp)
                    }
                    1 -> Column(Modifier.fillMaxSize().padding(top = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (arabic) "مكتبة الموسيقى" else "Music library", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { tracks = queryTracks(context) }) { Text(if (arabic) "تحديث" else "Refresh", color = Mint) }
                        }
                        if (!permission) {
                            Text(if (arabic) "اسمح بالوصول إلى ملفات الصوت لعرض المكتبة." else "Grant audio access to view your library.", color = Muted)
                            Button(onClick = {
                                val p = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
                                permissionRequest.launch(p)
                            }) { Text(if (arabic) "منح الإذن" else "Grant permission") }
                        } else {
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(tracks, key = { it.id }) { track ->
                                    Card(Modifier.fillMaxWidth().clickable { current = track },
                                        colors = CardDefaults.cardColors(containerColor = if (current?.id == track.id) Color(0xFF243C35) else Panel),
                                        shape = RoundedCornerShape(14.dp)) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("♫", color = Mint, fontSize = 23.sp)
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(track.title, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                                Text(track.artist, color = Muted, maxLines = 1, fontSize = 11.sp)
                                            }
                                            Text(formatTime(track.duration), color = Muted, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                        current?.let { track ->
                            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Slider(value = if (duration > 0) position.toFloat().coerceIn(0f, duration.toFloat()) else 0f,
                                        onValueChange = { player.seekTo(it.toLong()) },
                                        valueRange = 0f..duration.coerceAtLeast(1L).toFloat())
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(formatTime(position), color = Muted, fontSize = 10.sp)
                                        Spacer(Modifier.weight(1f))
                                        TextButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
                                            Text(if (playing) "Ⅱ" else "▶", color = Mint, fontSize = 20.sp)
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Text(formatTime(duration), color = Muted, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                    else -> Column(Modifier.fillMaxSize().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(if (arabic) "تنزيل ملف من رابط مباشر" else "Direct file download", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(if (arabic) "روابط الملفات المباشرة المصرح بتنزيلها فقط." else "Only direct media URLs you are authorized to download.", color = Muted, fontSize = 11.sp)
                                OutlinedTextField(value = url, onValueChange = { url = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                                    placeholder = { Text("https://example.com/audio.mp3") })
                                Button(onClick = {
                                    val parsed = runCatching { java.net.URL(url.trim()) }.getOrNull()
                                    if (parsed == null || parsed.protocol !in listOf("http", "https")) {
                                        status = if (arabic) "أدخل رابط HTTP/HTTPS صالحاً" else "Enter a valid HTTP/HTTPS URL"
                                    } else {
                                        val request = OneTimeWorkRequestBuilder<DirectDownloadWorker>()
                                            .setInputData(workDataOf(DirectDownloadWorker.KEY_URL to url.trim()))
                                            .addTag("sonic-task").addTag(DirectDownloadWorker.TAG).build()
                                        work.enqueueUniqueWork("sonic-download-" + url.trim().hashCode(), ExistingWorkPolicy.REPLACE, request)
                                        url = ""
                                        status = if (arabic) "بدأ التنزيل" else "Download started"
                                    }
                                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color(0xFF06251A))) {
                                    Text(if (arabic) "بدء التنزيل" else "Start download")
                                }
                            }
                        }
                        Text(if (arabic) "مهام التحويل والتنزيل" else "Conversion and download tasks", fontWeight = FontWeight.Bold)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(taskList, key = { it.id }) { info ->
                                val percent = info.progress.getInt("progress", 0)
                                val conversion = info.tags.contains(ConvertWorker.TAG)
                                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(if (conversion) (if (arabic) "تحويل MP3" else "MP3 conversion") else (if (arabic) "تنزيل ملف" else "File download"), fontWeight = FontWeight.SemiBold)
                                        Text(stateText(info.state, arabic), color = Muted, fontSize = 11.sp)
                                        if (info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED) {
                                            LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth(), color = Mint)
                                            Text("$percent%", color = Mint, fontSize = 11.sp)
                                            TextButton(onClick = { work.cancelWorkById(info.id) }) { Text(if (arabic) "إلغاء" else "Cancel", color = Color(0xFFFF8585)) }
                                        } else if (info.state == WorkInfo.State.CANCELLED || info.state == WorkInfo.State.FAILED) {
                                            TextButton(onClick = {
                                                if (conversion) {
                                                    work.enqueue(OneTimeWorkRequestBuilder<ConvertWorker>().setInputData(info.progress)
                                                        .addTag("sonic-task").addTag(ConvertWorker.TAG).build())
                                                } else {
                                                    val oldUrl = info.progress.getString(DirectDownloadWorker.KEY_URL)
                                                    if (!oldUrl.isNullOrBlank()) work.enqueue(OneTimeWorkRequestBuilder<DirectDownloadWorker>()
                                                        .setInputData(workDataOf(DirectDownloadWorker.KEY_URL to oldUrl))
                                                        .addTag("sonic-task").addTag(DirectDownloadWorker.TAG).build())
                                                }
                                            }) { Text(if (conversion) (if (arabic) "إعادة التحويل" else "Restart conversion") else (if (arabic) "استئناف التنزيل" else "Resume download"), color = Mint) }
                                        }
                                        if (info.state == WorkInfo.State.SUCCEEDED) Text(if (arabic) "اكتملت العملية" else "Completed", color = Mint, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Sonic MP3 • v0.2.0", color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
            }
        }
    }
}

private fun hasAudioPermission(context: Context): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
private fun queryTracks(context: Context): List<Track> {
    val result = mutableListOf<Track>()
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val columns = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION)
    context.contentResolver.query(collection, columns, null, null, MediaStore.Audio.Media.DATE_ADDED + " DESC")?.use { cursor ->
        val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        while (cursor.moveToNext()) {
            val mediaId = cursor.getLong(id)
            result += Track(mediaId, cursor.getString(title) ?: "Unknown", cursor.getString(artist) ?: "Unknown artist",
                ContentUris.withAppendedId(collection, mediaId), cursor.getLong(duration))
        }
    }
    return result
}
private fun queryName(context: Context, uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) return it.getString(0)
    }
    return null
}
private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
private fun stateText(state: WorkInfo.State, ar: Boolean): String = when (state) {
    WorkInfo.State.ENQUEUED -> if (ar) "في قائمة الانتظار" else "Queued"
    WorkInfo.State.RUNNING -> if (ar) "قيد التنفيذ" else "Running"
    WorkInfo.State.SUCCEEDED -> if (ar) "مكتمل" else "Completed"
    WorkInfo.State.FAILED -> if (ar) "فشل" else "Failed"
    WorkInfo.State.CANCELLED -> if (ar) "ملغى" else "Cancelled"
    WorkInfo.State.BLOCKED -> if (ar) "بانتظار عملية أخرى" else "Blocked"
}
