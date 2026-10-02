package com.example.ui.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ThumbnailCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    val lruCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }
}

/**
 * Extracts a representative frame from a local video file with in-memory LRU caching.
 */
suspend fun loadVideoThumbnail(videoPath: String): Bitmap? = withContext(Dispatchers.IO) {
    ThumbnailCache.lruCache.get(videoPath)?.let { return@withContext it }

    try {
        val retriever = MediaMetadataRetriever()
        val file = File(videoPath)
        if (file.exists()) {
            retriever.setDataSource(file.absolutePath)
        } else {
            retriever.setDataSource(videoPath)
        }

        // Retrieve frame at 1s (or closest sync frame at 0)
        val frame = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

        retriever.release()

        if (frame != null) {
            ThumbnailCache.lruCache.put(videoPath, frame)
        }
        frame
    } catch (_: Exception) {
        null
    }
}

@Composable
fun VideoThumbnailView(
    videoPath: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var thumbnail by remember(videoPath) { mutableStateOf<Bitmap?>(ThumbnailCache.lruCache.get(videoPath)) }
    var isLoading by remember(videoPath) { mutableStateOf(thumbnail == null) }

    LaunchedEffect(videoPath) {
        if (thumbnail == null) {
            isLoading = true
            thumbnail = loadVideoThumbnail(videoPath)
            isLoading = false
        }
    }

    Box(
        modifier = modifier.background(Color(0xFF161F2E)),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = "Miniaturka wideo",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle dark overlay so play badge stands out
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        } else if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }

        // Play badge icon
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.65f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Odtwórz",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun PhotoThumbnailView(
    imagePathOrUri: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val imageFile = remember(imagePathOrUri) { File(imagePathOrUri) }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(if (imageFile.exists()) imageFile else imagePathOrUri)
            .crossfade(true)
            .build(),
        contentDescription = "Miniaturka zdjęcia",
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E2430)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF252D3D))
            )
        }
    )
}
