package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.EmeraldRunning
import kotlinx.coroutines.delay

@Composable
fun SciFiSplashScreen(
    onComplete: () -> Unit
) {
    val progressAnim = remember { Animatable(0f) }
    var currentLogIndex by remember { mutableStateOf(0) }

    val systemLogs = listOf(
        "BOOTING LOCAL FTP ENGINE...",
        "DISCOVERING WLAN0 & HOTSPOT INTERFACES...",
        "MOUNTING HIGH-SPEED STORAGE SYSTEM...",
        "ACTIVATING RFC 959 PROTOCOL BRIDGE...",
        "LOCAL DATA LINK ESTABLISHED • SYSTEM READY"
    )

    // Infinite radar sweep & pulse
    val infiniteTransition = rememberInfiniteTransition(label = "splash_fx")
    val radarLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar"
    )

    LaunchedEffect(Unit) {
        // Step 1
        currentLogIndex = 0
        progressAnim.animateTo(0.25f, tween(350, easing = LinearEasing))
        delay(300)

        // Step 2
        currentLogIndex = 1
        progressAnim.animateTo(0.55f, tween(400, easing = LinearEasing))
        delay(350)

        // Step 3
        currentLogIndex = 2
        progressAnim.animateTo(0.80f, tween(400, easing = LinearEasing))
        delay(350)

        // Step 4
        currentLogIndex = 3
        progressAnim.animateTo(1.0f, tween(350, easing = LinearEasing))
        currentLogIndex = 4
        delay(300)

        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .clickable { onComplete() } // Allow one-tap fast skip
    ) {
        // Sci-Fi Tech Grid & Radar Sweep in Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridStep = 48.dp.toPx()
            val w = size.width
            val h = size.height

            // Subtle vertical grid lines
            var x = 0f
            while (x < w) {
                drawLine(
                    color = Color(0xFF1E293B).copy(alpha = 0.25f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
                x += gridStep
            }

            // Subtle horizontal grid lines
            var y = 0f
            while (y < h) {
                drawLine(
                    color = Color(0xFF1E293B).copy(alpha = 0.25f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }

            // Radar scan beam
            val scanY = h * radarLineY
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF00F0FF).copy(alpha = 0.25f),
                        Color(0xFF00F0FF).copy(alpha = 0.6f)
                    ),
                    startY = scanY - 60.dp.toPx(),
                    endY = scanY
                ),
                start = Offset(0f, scanY),
                end = Offset(w, scanY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Center Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Futuristic 3D Logo
            SciFi3DLogo(
                size = 140.dp,
                animated = true
            )

            Spacer(modifier = Modifier.height(28.dp))

            // App Title with Cyberpunk / Sci-Fi Styling
            Text(
                text = "WIFI FTP SERVER",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CyanPrimary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "QUANTUM LOCAL FILE BRIDGE",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    ),
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Cyber Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnim.value)
                        .height(4.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(CyanPrimary, CyanAccent, Color.White)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Terminal HUD Boot Logs
            Surface(
                modifier = Modifier.fillMaxWidth(0.9f),
                color = Color(0xFF111827).copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "> ${systemLogs[currentLogIndex]}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (currentLogIndex == 4) EmeraldRunning else CyanAccent,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Tap to skip hint at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "TAP ANYWHERE TO SKIP",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF64748B)
            )
        }
    }
}
