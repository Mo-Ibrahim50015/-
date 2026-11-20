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
import kotlinx.coroutines.delay

private const val SPLASH_MAX_DURATION_MS = 4000L

/**
 * Full-screen responsive vertical/portrait splash video filling any phone screen edge-to-edge
 * with CENTER_CROP scaling, hardware-accelerated TextureView, and strict 4-second ceiling.
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

    fun applyCenterCropTransform(tv: TextureView, viewW: Int, viewH: Int) {
        val vw = videoSizeHolder[0]
        val vh = videoSizeHolder[1]
        if (vw <= 0 || vh <= 0 || viewW <= 0 || viewH <= 0) return
        val viewRatio = viewW.toFloat() / viewH.toFloat()
        val videoRatio = vw.toFloat() / vh.toFloat()
        val scaleX: Float
        val scaleY: Float
        if (videoRatio > viewRatio) {
            // Video is wider than portrait screen -> scale X so height fills 100% edge-to-edge
            scaleX = videoRatio / viewRatio
            scaleY = 1f
        } else {
            // Video is taller than screen -> scale Y so width fills 100% edge-to-edge
            scaleX = 1f
            scaleY = viewRatio / videoRatio
        }
        val matrix = Matrix().apply {
            setScale(scaleX, scaleY, viewW / 2f, viewH / 2f)
        }
        tv.setTransform(matrix)
    }

    fun releasePlayerResources() {
        playerHolder[0]?.let { mp ->
            runCatching { if (mp.isPlaying) mp.stop() }
            runCatching { mp.reset() }
            runCatching { mp.release() }
        }
        playerHolder[0] = null
        surfaceHolder[0]?.let { s -> runCatching { s.release() } }
        surfaceHolder[0] = null
    }

    DisposableEffect(Unit) {
        onDispose {
            releasePlayerResources()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF041522))
    ) {
        // Full-Screen Portrait Edge-to-Edge Video Surface
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                TextureView(ctx).apply {
                    isOpaque = true
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            st: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            releasePlayerResources()
                            val surface = Surface(st)
                            surfaceHolder[0] = surface
                            try {
                                val afd = context.resources.openRawResourceFd(R.raw.splash_intro)
                                val mp = MediaPlayer()
                                playerHolder[0] = mp
                                mp.setSurface(surface)
                                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                afd.close()
                                mp.isLooping = false
                                mp.setOnVideoSizeChangedListener { _, vw, vh ->
                                    videoSizeHolder[0] = vw
                                    videoSizeHolder[1] = vh
                                    applyCenterCropTransform(this@apply, this@apply.width, this@apply.height)
                                }
                                mp.setOnPreparedListener { player ->
                                    if (!finishedOnce) {
                                        player.start()
                                    }
                                }
                                mp.setOnCompletionListener {
                                    releasePlayerResources()
                                    finishSafely()
                                }
                                mp.setOnErrorListener { _, _, _ ->
                                    releasePlayerResources()
                                    finishSafely()
                                    true
                                }
                                mp.prepareAsync()
                            } catch (_: Exception) {
                                releasePlayerResources()
                                finishSafely()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(
                            st: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            applyCenterCropTransform(this@apply, width, height)
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
                .height(120.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.58f))
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
            color = Color.Black.copy(alpha = 0.36f)
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
