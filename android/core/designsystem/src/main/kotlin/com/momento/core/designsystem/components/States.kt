package com.momento.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.momento.core.designsystem.theme.SurfaceHigh
import com.momento.core.designsystem.theme.SurfaceLow
import com.momento.core.designsystem.theme.TextDark
import com.momento.core.designsystem.theme.TextMuted
import com.momento.core.designsystem.theme.TextPrimary
import com.momento.core.designsystem.theme.PrimaryYellow

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    Box(
        modifier = modifier
            .alpha(alpha)
            .clip(shape)
            .background(SurfaceHigh)
    )
}

@Composable
fun PostCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBox(Modifier.size(40.dp), CircleShape)
            Spacer(Modifier.width(10.dp))
            Column {
                SkeletonBox(Modifier.size(width = 120.dp, height = 12.dp))
                Spacer(Modifier.height(6.dp))
                SkeletonBox(Modifier.size(width = 80.dp, height = 10.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(320.dp), RoundedCornerShape(20.dp))
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(text = message, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = Icons.Outlined.CloudOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
        Text(
            text = "Couldn't load this right now",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Text(text = message, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = TextDark)
        ) {
            Text("Try again")
        }
    }
}
