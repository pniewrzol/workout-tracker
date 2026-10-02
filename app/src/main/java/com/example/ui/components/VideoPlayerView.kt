package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun VideoPlayerView(
    videoPathOrUri: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false
) {
    val context = LocalContext.current
    val audioFocusHelper = remember { AudioFocusHelper(context) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var mediaPlayerRef by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    val fileUri = remember(videoPathOrUri) {
        val file = File(videoPathOrUri)
        if (file.exists()) {
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.parse(videoPathOrUri)
            }
        } else {
            Uri.parse(videoPathOrUri)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
            audioFocusHelper.abandonFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val mediaController = MediaController(ctx)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)

                        val file = File(videoPathOrUri)
                        if (file.exists()) {
                            setVideoPath(file.absolutePath)
                        } else {
                            setVideoURI(fileUri)
                        }

                        setOnPreparedListener { mp ->
                            mediaPlayerRef = mp
                            isPrepared = true
                            mp.isLooping = false
                            if (isMuted) {
                                mp.setVolume(0f, 0f)
                            }
                            if (autoPlay) {
                                if (!isMuted) {
                                    audioFocusHelper.requestFocus()
                                }
                                start()
                                isPlaying = true
                            }
                        }

                        setOnCompletionListener {
                            isPlaying = false
                            audioFocusHelper.abandonFocus()
                        }

                        setOnErrorListener { _, _, _ ->
                            isPrepared = false
                            isPlaying = false
                            audioFocusHelper.abandonFocus()
                            true
                        }

                        videoViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!isPrepared) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            // Overlay play/pause button if desired
            if (isPrepared && !isPlaying) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.size(64.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (!isMuted) {
                                audioFocusHelper.requestFocus()
                            }
                            videoViewRef?.start()
                            isPlaying = true
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Odtwórz",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        // Playback control bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2430))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        videoViewRef?.let { vv ->
                            if (vv.isPlaying) {
                                vv.pause()
                                isPlaying = false
                                audioFocusHelper.abandonFocus()
                            } else {
                                if (!isMuted) {
                                    audioFocusHelper.requestFocus()
                                }
                                vv.start()
                                isPlaying = true
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pauza" else "Odtwórz",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        videoViewRef?.let { vv ->
                            vv.seekTo(0)
                            if (!isMuted) {
                                audioFocusHelper.requestFocus()
                            }
                            vv.start()
                            isPlaying = true
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Od nowa",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        mediaPlayerRef?.let { mp ->
                            if (isMuted) {
                                mp.setVolume(0f, 0f)
                                audioFocusHelper.abandonFocus()
                            } else {
                                mp.setVolume(1f, 1f)
                                if (isPlaying) {
                                    audioFocusHelper.requestFocus()
                                }
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Wyłącz wyciszenie" else "Wycisz",
                        tint = if (isMuted) MaterialTheme.colorScheme.error else Color.White
                    )
                }
            }

            // Button to open externally
            OutlinedButton(
                onClick = {
                    openVideoInSystemPlayer(context, fileUri)
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Odtwarzacz pełnoekranowy",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun openVideoInSystemPlayer(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w("VideoPlayerView", "Unable to open video in external player", e)
    }
}
