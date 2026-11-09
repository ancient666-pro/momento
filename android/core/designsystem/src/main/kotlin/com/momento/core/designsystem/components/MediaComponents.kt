package com.momento.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.momento.core.designsystem.theme.PrimaryYellow
import com.momento.core.designsystem.theme.ScrimBlack
import com.momento.core.designsystem.theme.SurfaceHigh
import com.momento.core.designsystem.theme.SurfaceLow
import com.momento.core.designsystem.theme.SurfaceWhite
import com.momento.core.designsystem.theme.TextDark
import com.momento.core.designsystem.theme.TextPrimary
import com.momento.core.designsystem.theme.TextSecondary

@Composable
fun MediaImage(
    url: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    aspectRatio: Float? = null,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    onClick: (() -> Unit)? = null
) {
    val base = modifier
        .clip(RoundedCornerShape(cornerRadius))
        .let { if (onClick != null) it.clickable { onClick() } else it }
    Box(modifier = base) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = if (aspectRatio != null) {
                Modifier.fillMaxWidth().aspectRatio(aspectRatio)
            } else {
                Modifier.fillMaxSize()
            }
        )
    }
}

@Composable
fun GradientScrim(
    modifier: Modifier = Modifier,
    heightFraction: Float = 0.55f
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxSize(fraction = heightFraction)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, ScrimBlack),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    )
}

@Composable
fun PlayOverlay(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(SurfaceWhite.copy(alpha = 0.92f))
            .let { if (onClick != null) it.clickable { onClick() } else it },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Play",
            tint = TextDark,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

@Composable
fun CornerBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceWhite,
    contentColor: Color = TextPrimary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = contentColor)
    }
}

@Composable
fun TagPill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceLow,
    contentColor: Color = TextSecondary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}

@Composable
fun LiveDot(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 8.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(com.momento.core.designsystem.theme.SuccessGreen)
    )
}

@Composable
fun WaveformBars(
    modifier: Modifier = Modifier,
    barCount: Int = 28,
    seed: Long = 7L,
    activeColor: Color = PrimaryYellow,
    inactiveColor: Color = SurfaceHigh
) {
    val heights = androidx.compose.runtime.remember(seed) { generateBarHeights(barCount, seed) }
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            heights.forEach { height ->
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = height)
                        .clip(RoundedCornerShape(2.dp))
                        .background(activeColor)
                )
            }
        }
    }
}

private fun generateBarHeights(count: Int, seed: Long): List<androidx.compose.ui.unit.Dp> {
    val heights = intArrayOf(10, 22, 34, 16, 28, 40, 12, 24, 36, 18, 30, 14, 26, 38, 20, 32)
    return List(count.coerceAtLeast(0)) { index ->
        // seed can be negative (hashCode), so use floorMod to avoid a negative index.
        val value = heights[Math.floorMod(index + seed.toInt(), heights.size)]
        androidx.compose.ui.unit.Dp(value.toFloat() * 0.9f)
    }
}
