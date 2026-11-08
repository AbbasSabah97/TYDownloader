package com.example.youtubetomp3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.example.youtubetomp3.worker.DownloadWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var url by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("محول يوتيوب إلى MP3") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("أدخل رابط فيديو يوتيوب") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                            .setInputData(workDataOf("url" to url))
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build()
                            )
                            .build()
                        WorkManager.getInstance(context).enqueue(workRequest)
                        url = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تحويل إلى MP3")
            }
        }
    }
}
