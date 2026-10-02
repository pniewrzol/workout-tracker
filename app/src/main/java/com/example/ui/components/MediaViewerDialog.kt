package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.ExerciseMedia
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MediaViewerDialog(
    media: ExerciseMedia,
    onDismiss: () -> Unit,
    onDelete: (ExerciseMedia) -> Unit
) {
    val context = LocalContext.current
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(media.timestamp))

    // True fullscreen dialog covering the entire screen
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (media.mediaType == "VIDEO") {
                FullscreenVideoPlayer(
                    videoPathOrUri = media.uriString,
                    caption = media.caption,
                    dateStr = dateStr,
                    onClose = onDismiss,
                    onDelete = { onDelete(media) }
                )
            } else {
                FullscreenPhotoViewer(
                    imagePathOrUri = media.uriString,
                    caption = media.caption,
                    dateStr = dateStr,
                    onClose = onDismiss,
                    onDelete = { onDelete(media) }
                )
            }
        }
    }
}

@Composable
fun FullscreenPhotoViewer(
    imagePathOrUri: String,
    caption: String,
    dateStr: String,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val imageFile = remember(imagePathOrUri) { File(imagePathOrUri) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var areControlsVisible by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    },
                    onTap = {
                        areControlsVisible = !areControlsVisible
                    }
                )
            }
    ) {
        // Zoomable & Pannable Image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            val maxOffsetX = (size.width * (scale - 1f)) / 2f
                            val maxOffsetY = (size.height * (scale - 1f)) / 2f
                            offsetX = (offsetX + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = (offsetY + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(if (imageFile.exists()) imageFile else imagePathOrUri)
                    .crossfade(true)
                    .build(),
                contentDescription = caption.ifBlank { "Zdjęcie w pełnym ekranie" },
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        // Floating Top Bar
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Zamknij",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Podgląd zdjęcia",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Text(
                                text = dateStr,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row {
                        if (scale > 1.1f) {
                            IconButton(onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOutMap,
                                    contentDescription = "Zresetuj zoom",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Usuń",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Floating Bottom Caption / Hint Bar
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (caption.isNotBlank()) {
                        Text(
                            text = caption,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = "Dotknij dwukrotnie lub uszczypnij, aby przybliżyć • Dotknij, aby ukryć pasek",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FullscreenVideoPlayer(
    videoPathOrUri: String,
    caption: String,
    dateStr: String,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val audioFocusHelper = remember { AudioFocusHelper(context) }

    var isPlaying by remember { mutableStateOf(true) }
    var isPrepared by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var mediaPlayerRef by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var areControlsVisible by remember { mutableStateOf(true) }

    // Video Zoom & Pan State
    var videoScale by remember { mutableFloatStateOf(1f) }
    var videoOffsetX by remember { mutableFloatStateOf(0f) }
    var videoOffsetY by remember { mutableFloatStateOf(0f) }

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

    // Progress polling loop while playing
    LaunchedEffect(isPlaying, isPrepared) {
        while (isPlaying && isPrepared) {
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    currentPositionMs = vv.currentPosition
                    if (durationMs == 0 && vv.duration > 0) {
                        durationMs = vv.duration
                    }
                }
            }
            delay(250)
        }
    }

    // Auto-hide controls after 4 seconds of playback
    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying) {
            delay(4000)
            areControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (videoScale > 1.2f) {
                            videoScale = 1f
                            videoOffsetX = 0f
                            videoOffsetY = 0f
                        } else {
                            videoScale = 2.5f
                        }
                    },
                    onTap = {
                        areControlsVisible = !areControlsVisible
                    }
                )
            }
    ) {
        // Video View with Pinch-to-Zoom & Pan
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        videoScale = (videoScale * zoom).coerceIn(1f, 4f)
                        if (videoScale > 1f) {
                            val maxOffsetX = (size.width * (videoScale - 1f)) / 2f
                            val maxOffsetY = (size.height * (videoScale - 1f)) / 2f
                            videoOffsetX = (videoOffsetX + pan.x * videoScale).coerceIn(-maxOffsetX, maxOffsetX)
                            videoOffsetY = (videoOffsetY + pan.y * videoScale).coerceIn(-maxOffsetY, maxOffsetY)
                        } else {
                            videoOffsetX = 0f
                            videoOffsetY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
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
                            durationMs = duration
                            if (isMuted) {
                                mp.setVolume(0f, 0f)
                            } else {
                                audioFocusHelper.requestFocus()
                            }
                            start()
                            isPlaying = true
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
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = videoScale,
                        scaleY = videoScale,
                        translationX = videoOffsetX,
                        translationY = videoOffsetY
                    )
            )
        }

        if (!isPrepared) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(54.dp)
                    .align(Alignment.Center)
            )
        }

        // Floating Top Header
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.8f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Zamknij",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Odtwarzacz wideo",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Text(
                                text = dateStr,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row {
                        if (videoScale > 1.1f) {
                            IconButton(onClick = {
                                videoScale = 1f
                                videoOffsetX = 0f
                                videoOffsetY = 0f
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOutMap,
                                    contentDescription = "Zresetuj zoom",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(onClick = {
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
                        }) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "Wyłącz wyciszenie" else "Wycisz",
                                tint = if (isMuted) MaterialTheme.colorScheme.error else Color.White
                            )
                        }
                        IconButton(onClick = { openVideoInSystemPlayer(context, fileUri) }) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Otwórz zewnętrznie",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Usuń",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Center Play / Pause Pulsing Action Button
        AnimatedVisibility(
            visible = areControlsVisible && isPrepared,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.size(72.dp)
            ) {
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
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pauza" else "Odtwórz",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        // Floating Bottom Playback & Scrubber Controls Bar
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.82f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (caption.isNotBlank()) {
                        Text(
                            text = caption,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Progress Scrubber Bar
                    val currentSec = (currentPositionMs / 1000).coerceAtLeast(0)
                    val totalSec = (durationMs / 1000).coerceAtLeast(0)
                    val timeDisplay = "${formatTime(currentSec)} / ${formatTime(totalSec)}"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentSec),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Slider(
                            value = if (durationMs > 0) currentPositionMs.toFloat() else 0f,
                            onValueChange = { newPos ->
                                currentPositionMs = newPos.toInt()
                                videoViewRef?.seekTo(newPos.toInt())
                            },
                            valueRange = 0f..(if (durationMs > 0) durationMs.toFloat() else 1f),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color(0xFF334155)
                            )
                        )

                        Text(
                            text = formatTime(totalSec),
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    // Quick Actions (Rewind 5s, Play/Pause, Forward 5s, Replay)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            videoViewRef?.let { vv ->
                                val target = (vv.currentPosition - 5000).coerceAtLeast(0)
                                vv.seekTo(target)
                                currentPositionMs = target
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Replay5,
                                contentDescription = "-5 sekund",
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = {
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
                        }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pauza" else "Odtwórz",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(onClick = {
                            videoViewRef?.let { vv ->
                                val target = (vv.currentPosition + 5000).coerceAtMost(vv.duration)
                                vv.seekTo(target)
                                currentPositionMs = target
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Forward5,
                                contentDescription = "+5 sekund",
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = {
                            videoViewRef?.let { vv ->
                                vv.seekTo(0)
                                vv.start()
                                currentPositionMs = 0
                                isPlaying = true
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Od nowa",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
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
