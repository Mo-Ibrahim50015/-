package com.example.ui.components

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.util.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private const val SPLASH_MAX_DURATION_MS = 4500L

/**
 * Full-screen high-clarity splash video screen.
 * Preserves native aspect ratio and razor-sharp pixel quality (no aggressive 3x-4x zoom cropping)
 * inside a seamless full-screen deep-emerald cinema backdrop with gentle edge vignette.
 */
@Composable
fun SplashVideoScreen(
    isArabic: Boolean,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val latestOnFinished by rememberUpdatedState(onFinished)
    var finishedOnce by remember { mutableStateOf(false) }
    var startProgress by remember { mutableStateOf(false) }

    val finishSafely: () -> Unit = remember {
        {
            if (!finishedOnce) {
                finishedOnce = true
                latestOnFinished()
            }
        }
    }

    LaunchedEffect(Unit) {
        startProgress = true
        delay(SPLASH_MAX_DURATION_MS)
        finishSafely()
    }

    val progress by animateFloatAsState(
        targetValue = if (startProgress) 1f else 0f,
        animationSpec = tween(
            durationMillis = SPLASH_MAX_DURATION_MS.toInt(),
            easing = LinearEasing
        ),
        label = "splash_progress"
    )

    val playerHolder = remember { arrayOfNulls<MediaPlayer>(1) }
    val surfaceHolder = remember { arrayOfNulls<Surface>(1) }
    val videoSizeHolder = remember { intArrayOf(0, 0) }
    val isDisposed = remember { AtomicBoolean(false) }
    val isPrepared = remember { AtomicBoolean(false) }

    /**
     * Sharp aspect-ratio-preserving scaling: fills full width cleanly without distorting
     * or over-magnifying pixels (zoom strictly capped at 1.0x native fit so quality stays crystal clear).
     */
    fun applySharpFitTransform(tv: TextureView, viewW: Int, viewH: Int) {
        val vw = videoSizeHolder[0]
        val vh = videoSizeHolder[1]
        if (vw <= 0 || vh <= 0 || viewW <= 0 || viewH <= 0) return
        val viewRatio = viewW.toFloat() / viewH.toFloat()
        val videoRatio = vw.toFloat() / vh.toFloat()
        val scaleX: Float
        val scaleY: Float
        if (videoRatio > viewRatio) {
            scaleX = 1f
            scaleY = viewRatio / videoRatio
        } else {
            scaleX = videoRatio / viewRatio
            scaleY = 1f
        }
        runCatching {
            val matrix = Matrix().apply {
                setScale(scaleX, scaleY, viewW / 2f, viewH / 2f)
            }
            tv.setTransform(matrix)
        }
    }

    /**
     * Detaches references immediately on UI thread and performs blocking MediaPlayer.release()
     * on Dispatchers.IO so the main thread NEVER stalls or ANRs during splash exit.
     */
    fun releasePlayerResources() {
        val wasPrepared = isPrepared.getAndSet(false)
        val mp = synchronized(playerHolder) {
            val current = playerHolder[0]
            playerHolder[0] = null
            current
        }
        val surface = synchronized(surfaceHolder) {
            val current = surfaceHolder[0]
            surfaceHolder[0] = null
            current
        }
        if (mp != null || surface != null) {
            CoroutineScope(Dispatchers.IO).launch {
                if (mp != null) {
                    runCatching { mp.setOnPreparedListener(null) }
                    runCatching { mp.setOnCompletionListener(null) }
                    runCatching { mp.setOnErrorListener(null) }
                    runCatching { mp.setOnVideoSizeChangedListener(null) }
                    if (wasPrepared) {
                        runCatching { if (mp.isPlaying) mp.stop() }
                    }
                    runCatching { mp.setSurface(null) }
                    runCatching { mp.release() }
                }
                if (surface != null) {
                    runCatching { surface.release() }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isDisposed.set(true)
            releasePlayerResources()
        }
    }

    val backdropColor = Color(0xFF041522)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF03101B), backdropColor, Color(0xFF062030))
                )
            )
    ) {
        // Large Full-Width Crisp Video Surface (native resolution preserved without blurry upscaling)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                TextureView(ctx).apply {
                    isOpaque = false
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            st: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            if (isDisposed.get() || finishedOnce) return
                            releasePlayerResources()
                            val surface = runCatching { Surface(st) }.getOrNull() ?: return
                            synchronized(surfaceHolder) { surfaceHolder[0] = surface }
                            try {
                                val mp = MediaPlayer()
                                isPrepared.set(false)
                                synchronized(playerHolder) { playerHolder[0] = mp }
                                // setDataSource MUST be called first so native mPlayer leaves IDLE state (state 1)
                                context.resources.openRawResourceFd(R.raw.splash_intro).use { afd ->
                                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                }
                                mp.setSurface(surface)
                                mp.isLooping = false
                                mp.setOnVideoSizeChangedListener { _, vw, vh ->
                                    if (!isDisposed.get()) {
                                        videoSizeHolder[0] = vw
                                        videoSizeHolder[1] = vh
                                        applySharpFitTransform(this@apply, this@apply.width, this@apply.height)
                                    }
                                }
                                mp.setOnPreparedListener { player ->
                                    val stillActive = !isDisposed.get() && !finishedOnce &&
                                        synchronized(playerHolder) { playerHolder[0] === player }
                                    if (stillActive) {
                                        isPrepared.set(true)
                                        runCatching {
                                            player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                                        }
                                        runCatching { player.start() }
                                    }
                                }
                                mp.setOnCompletionListener {
                                    releasePlayerResources()
                                    finishSafely()
                                }
                                mp.setOnErrorListener { _, _, _ ->
                                    isPrepared.set(false)
                                    releasePlayerResources()
                                    finishSafely()
                                    true
                                }
                                mp.prepareAsync()
                            } catch (_: Throwable) {
                                isPrepared.set(false)
                                releasePlayerResources()
                                finishSafely()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(
                            st: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            if (!isDisposed.get()) {
                                applySharpFitTransform(this@apply, width, height)
                            }
                        }

                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            releasePlayerResources()
                            return true
                        }

                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                    }
                }
            }
        )

        // Subtle top & bottom dark scrims so Skip & progress bar remain legible on any frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF03101B).copy(alpha = 0.72f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF03101B).copy(alpha = 0.82f))
                    )
                )
        )

        // Skip Pill overlaid at top-end (accessible >=48dp touch target)
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(999.dp))
                .testTag("splash_skip_btn")
                .clickable {
                    releasePlayerResources()
                    finishSafely()
                },
            shape = RoundedCornerShape(999.dp),
            color = Color.Black.copy(alpha = 0.42f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = tr(isArabic, "تخطي", "Skip"),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Icon(
                    imageVector = if (isArabic) {
                        Icons.AutoMirrored.Outlined.KeyboardArrowLeft
                    } else {
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight
                    },
                    contentDescription = tr(isArabic, "تخطي المقدمة", "Skip intro"),
                    tint = Color(0xFF82F5C1),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Brand caption + 4-second progress bar overlaid at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = tr(isArabic, "مصروفي", "Masrofy"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = Color(0xFF82F5C1),
                trackColor = Color.White.copy(alpha = 0.24f)
            )
        }
    }
}
