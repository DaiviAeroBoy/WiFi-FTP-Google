package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural 3D Sci-Fi Holographic Server Cube Logo with glowing isometric facets,
 * orbital energy rings, pulsing core, and radar scan lines.
 */
@Composable
fun SciFi3DLogo(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scifi_logo")

    val rotationAngle by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "orbit_rotation"
        )
    } else {
        rememberInfiniteTransition(label = "static").animateFloat(0f, 0f, infiniteRepeatable(tween(1)), "0")
    }

    val pulseScale by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
    } else {
        rememberInfiniteTransition(label = "static_pulse").animateFloat(1f, 1f, infiniteRepeatable(tween(1)), "1")
    }

    val glowAlpha by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_alpha"
        )
    } else {
        rememberInfiniteTransition(label = "static_alpha").animateFloat(0.8f, 0.8f, infiniteRepeatable(tween(1)), "2")
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f

            // 1. Outer Hologram Glow Field
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00F0FF).copy(alpha = 0.28f * glowAlpha),
                        Color(0xFF0284C7).copy(alpha = 0.12f * glowAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.05f
                ),
                radius = baseRadius * 1.05f,
                center = center
            )

            // 2. Orbital Sci-Fi Rings (Elliptical / Rotating)
            rotate(degrees = rotationAngle, pivot = center) {
                drawCircle(
                    color = Color(0xFF00F0FF).copy(alpha = 0.45f),
                    radius = baseRadius * 0.90f,
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(16f, 12f, 4f, 12f),
                            0f
                        )
                    )
                )

                // Outer Node Orbiters
                val orbitRadius = baseRadius * 0.90f
                for (i in 0 until 3) {
                    val angleRad = Math.toRadians((i * 120.0))
                    val nodePos = Offset(
                        x = (center.x + orbitRadius * cos(angleRad)).toFloat(),
                        y = (center.y + orbitRadius * sin(angleRad)).toFloat()
                    )
                    drawCircle(
                        color = Color(0xFF00F0FF),
                        radius = 3.5.dp.toPx(),
                        center = nodePos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 1.8.dp.toPx(),
                        center = nodePos
                    )
                }
            }

            // Counter-rotating inner energy ring
            rotate(degrees = -rotationAngle * 1.5f, pivot = center) {
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.35f),
                    radius = baseRadius * 0.72f,
                    center = center,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(24f, 14f),
                            0f
                        )
                    )
                )
            }

            // 3. Central 3D Isometric Sci-Fi Server Cube
            val cubeRadius = baseRadius * 0.46f * pulseScale
            drawIsometricCube(center = center, radius = cubeRadius)

            // 4. Energy Beams & Core Spark
            drawCircle(
                color = Color.White.copy(alpha = glowAlpha),
                radius = 4.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color(0xFF00F0FF),
                radius = 6.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

/**
 * Draws a 3D isometric glowing server block with 3 visible facets: Top, Left, and Right.
 */
private fun DrawScope.drawIsometricCube(center: Offset, radius: Float) {
    val h = radius * 0.577f // 30-degree isometric projection ratio
    val r = radius

    // Points:
    // C = center
    // T = top vertex
    // BL = bottom left vertex
    // BR = bottom right vertex
    // TL = top left vertex
    // TR = top right vertex
    // B = bottom vertex
    val pCenter = center
    val pTop = Offset(center.x, center.y - r)
    val pBottom = Offset(center.x, center.y + r)
    val pTopLeft = Offset(center.x - r * 0.866f, center.y - h)
    val pTopRight = Offset(center.x + r * 0.866f, center.y - h)
    val pBottomLeft = Offset(center.x - r * 0.866f, center.y + h)
    val pBottomRight = Offset(center.x + r * 0.866f, center.y + h)

    // 1. Top Facet (Hologram Cyan Light)
    val topFacet = Path().apply {
        moveTo(pCenter.x, pCenter.y)
        lineTo(pTopRight.x, pTopRight.y)
        lineTo(pTop.x, pTop.y)
        lineTo(pTopLeft.x, pTopLeft.y)
        close()
    }
    drawPath(
        path = topFacet,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF00F0FF), Color(0xFF0284C7)),
            start = pTop,
            end = pCenter
        )
    )
    drawPath(
        path = topFacet,
        color = Color(0xFFE0F7FA),
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    )

    // 2. Left Facet (Deep Cyber Teal Shadow)
    val leftFacet = Path().apply {
        moveTo(pCenter.x, pCenter.y)
        lineTo(pTopLeft.x, pTopLeft.y)
        lineTo(pBottomLeft.x, pBottomLeft.y)
        lineTo(pBottom.x, pBottom.y)
        close()
    }
    drawPath(
        path = leftFacet,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF0369A1), Color(0xFF0F172A)),
            start = pTopLeft,
            end = pBottom
        )
    )
    drawPath(
        path = leftFacet,
        color = Color(0xFF38BDF8),
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    )

    // 3. Right Facet (Electric Indigo / Neon Blue)
    val rightFacet = Path().apply {
        moveTo(pCenter.x, pCenter.y)
        lineTo(pTopRight.x, pTopRight.y)
        lineTo(pBottomRight.x, pBottomRight.y)
        lineTo(pBottom.x, pBottom.y)
        close()
    }
    drawPath(
        path = rightFacet,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF0284C7), Color(0xFF1E1B4B)),
            start = pTopRight,
            end = pBottom
        )
    )
    drawPath(
        path = rightFacet,
        color = Color(0xFF00E5FF),
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    )

    // Server Slot Circuit Lines on Left & Right Facets
    val slotCount = 3
    for (i in 1..slotCount) {
        val fraction = i.toFloat() / (slotCount + 1)
        // Left facet line
        val p1 = Offset(
            x = pCenter.x + (pBottom.x - pCenter.x) * fraction,
            y = pCenter.y + (pBottom.y - pCenter.y) * fraction
        )
        val p2 = Offset(
            x = pTopLeft.x + (pBottomLeft.x - pTopLeft.x) * fraction,
            y = pTopLeft.y + (pBottomLeft.y - pTopLeft.y) * fraction
        )
        drawLine(
            color = Color(0xFF00F0FF).copy(alpha = 0.7f),
            start = p1,
            end = p2,
            strokeWidth = 1.dp.toPx()
        )

        // Right facet line
        val p3 = Offset(
            x = pTopRight.x + (pBottomRight.x - pTopRight.x) * fraction,
            y = pTopRight.y + (pBottomRight.y - pTopRight.y) * fraction
        )
        drawLine(
            color = Color(0xFF38BDF8).copy(alpha = 0.7f),
            start = p1,
            end = p3,
            strokeWidth = 1.dp.toPx()
        )
    }
}
