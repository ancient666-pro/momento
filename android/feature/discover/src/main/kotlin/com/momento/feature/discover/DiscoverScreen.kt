package com.momento.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.momento.core.common.formatCompactCount
import com.momento.core.designsystem.components.CircularActionButton
import com.momento.core.designsystem.components.CornerBadge
import com.momento.core.designsystem.components.EmptyState
import com.momento.core.designsystem.components.ErrorState
import com.momento.core.designsystem.components.FilterChip
import com.momento.core.designsystem.components.GradientScrim
import com.momento.core.designsystem.components.LiveDot
import com.momento.core.designsystem.components.MomentoAvatar
import com.momento.core.designsystem.components.OverlappingAvatars
import com.momento.core.designsystem.components.PlayOverlay
import com.momento.core.designsystem.components.PostCardSkeleton
import com.momento.core.designsystem.components.SectionHeader
import com.momento.core.designsystem.components.StatusPill
import com.momento.core.designsystem.components.TagPill
import com.momento.core.designsystem.components.VerifiedBadge
import com.momento.core.designsystem.theme.ErrorRed
import com.momento.core.designsystem.theme.PrimaryYellow
import com.momento.core.designsystem.theme.SuccessGreen
import com.momento.core.designsystem.theme.SurfaceWhite
import com.momento.core.designsystem.theme.TextDark
import com.momento.core.designsystem.theme.TextMuted
import com.momento.core.designsystem.theme.TextPrimary
import com.momento.core.designsystem.theme.TextSecondary
import com.momento.domain.model.AudioRoom
import com.momento.domain.model.NearbyProfile
import com.momento.domain.model.SparkCard
import com.momento.domain.repository.SparkFilter

@Composable
fun DiscoverScreen(viewModel: DiscoverViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        DiscoverTopBar()

        when {
            state.isLoading -> Column { repeat(2) { PostCardSkeleton() } }
            state.errorMessage != null && state.nearby.isEmpty() -> ErrorState(
                message = state.errorMessage.orEmpty(),
                onRetry = viewModel::refresh,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            else -> DiscoverContent(
                state = state,
                onFilterSelected = viewModel::selectFilter,
                onAction = viewModel::actOnTopCard
            )
        }
    }
}

@Composable
private fun DiscoverTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Discover",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = "Explore",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = TextMuted
        )
        Spacer(Modifier.weight(1f))
        CircularActionButton(
            icon = Icons.Filled.Search,
            contentDescription = "Search",
            onClick = { /* Search screen arrives with the search phase */ },
            containerColor = PrimaryYellow,
            iconColor = TextDark,
            size = 42.dp,
            iconSize = 20.dp
        )
        Spacer(Modifier.width(8.dp))
        CircularActionButton(
            icon = Icons.Filled.Tune,
            contentDescription = "Filters",
            onClick = { /* Filter sheet arrives with the search phase */ },
            size = 42.dp,
            containerColor = SurfaceWhite,
            iconColor = TextPrimary,
            iconSize = 20.dp
        )
    }
}

@Composable
private fun DiscoverContent(
    state: DiscoverUiState,
    onFilterSelected: (SparkFilter) -> Unit,
    onAction: (NearbyAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "nearby-header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "People Nearby",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Spacer(Modifier.width(8.dp))
                StatusPill(
                    text = "SPOTLIGHT",
                    containerColor = Color(0xFFEFE4FF),
                    contentColor = Color(0xFF6D28D9)
                )
                Spacer(Modifier.weight(1f))
                state.nearby.firstOrNull()?.let {
                    Text(
                        text = "${it.distanceKm} km",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        val topCard = state.nearby.firstOrNull()
        if (topCard != null) {
            item(key = "nearby-card") {
                SpotlightCard(topCard, onAction)
            }
        } else {
            item(key = "nearby-empty") {
                EmptyState(
                    title = "You're all caught up",
                    message = "No more profiles nearby right now. Check back soon.",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                )
            }
        }

        item(key = "sparks-header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trending Sparks",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFF97316),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }

        item(key = "sparks-filters") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SparkFilter.entries) { filter ->
                    FilterChip(
                        label = filter.label(),
                        selected = state.selectedFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        icon = filter.icon()
                    )
                }
            }
        }

        item(key = "radar") {
            RadarBanner(state)
        }

        item(key = "sparks-grid") {
            SparkRow(state.sparks)
        }

        state.audioRooms.firstOrNull()?.let { room ->
            item(key = "audio-room") {
                AudioRoomBanner(room)
            }
        }
    }
}

private fun SparkFilter.label(): String = when (this) {
    SparkFilter.TRENDING -> "Trending"
    SparkFilter.NEW -> "New Sparks"
    SparkFilter.REELS -> "Reels"
    SparkFilter.CREATIVE -> "Creative"
    SparkFilter.AUDIO_SESSION -> "Audio Session"
}

private fun SparkFilter.icon(): androidx.compose.ui.graphics.vector.ImageVector = when (this) {
    SparkFilter.TRENDING -> Icons.Filled.LocalFireDepartment
    SparkFilter.NEW -> Icons.Filled.Add
    SparkFilter.REELS -> Icons.Filled.PlayArrow
    SparkFilter.CREATIVE -> Icons.Filled.Star
    SparkFilter.AUDIO_SESSION -> Icons.Filled.GraphicEq
}

@Composable
private fun SpotlightCard(
    profile: NearbyProfile,
    onAction: (NearbyAction) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f)
                .clip(RoundedCornerShape(24.dp))
        ) {
            AsyncImage(
                model = profile.profile.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            GradientScrim(
                modifier = Modifier.align(Alignment.BottomCenter),
                heightFraction = 0.5f
            )
            StatusPill(
                text = "${profile.matchPercent}% Match",
                icon = Icons.Filled.Favorite,
                iconTint = ErrorRed,
                containerColor = SurfaceWhite.copy(alpha = 0.92f),
                contentColor = ErrorRed,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiveDot()
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "ONLINE NOW",
                    style = MaterialTheme.typography.labelSmall,
                    color = SurfaceWhite
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.profile.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = SurfaceWhite
                    )
                    if (profile.age != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = profile.age.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = SurfaceWhite
                        )
                    }
                    if (profile.profile.verified) {
                        Spacer(Modifier.width(6.dp))
                        VerifiedBadge(size = 16.dp)
                    }
                }
                Text(
                    text = profile.profile.category ?: profile.profile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SurfaceWhite.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile.interests.take(3).forEach { interest ->
                        TagPill(
                            text = interest,
                            containerColor = SurfaceWhite.copy(alpha = 0.25f),
                            contentColor = SurfaceWhite
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularActionButton(
                icon = Icons.Filled.Close,
                contentDescription = "Pass",
                onClick = { onAction(NearbyAction.PASS) },
                size = 54.dp,
                containerColor = SurfaceWhite,
                iconColor = TextSecondary
            )
            Spacer(Modifier.width(22.dp))
            CircularActionButton(
                icon = Icons.Filled.Bolt,
                contentDescription = "Super Like",
                onClick = { onAction(NearbyAction.SUPER_LIKE) },
                size = 64.dp,
                containerColor = PrimaryYellow,
                iconColor = TextDark,
                iconSize = 30.dp
            )
            Spacer(Modifier.width(22.dp))
            CircularActionButton(
                icon = Icons.Filled.Favorite,
                contentDescription = "Like & Connect",
                onClick = { onAction(NearbyAction.LIKE) },
                size = 54.dp,
                containerColor = SurfaceWhite,
                iconColor = ErrorRed
            )
        }
    }
}

@Composable
private fun RadarBanner(state: DiscoverUiState) {
    val radarCount = (state.nearby.map { it.profile.id } + state.sparks.map { it.author.id })
        .distinct()
        .size
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OverlappingAvatars(
            urls = (state.nearby.map { it.profile.avatarUrl } + state.sparks.map { it.author.avatarUrl })
                .take(4),
            size = 36.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (radarCount > 0) {
                    "$radarCount creator ${if (radarCount == 1) "vibe" else "vibes"} near you today"
                } else {
                    "No creator vibes nearby yet"
                },
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = "In your radar",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        FilterChip(label = "Take a look", selected = false, onClick = { /* Opens the radar list in a later phase */ })
    }
}

@Composable
private fun SparkRow(sparks: List<SparkCard>) {
    if (sparks.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sparks.take(2).forEach { spark ->
            SparkCell(spark, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SparkCell(spark: SparkCard, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(18.dp))
        ) {
            AsyncImage(
                model = spark.mediaUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            GradientScrim(
                modifier = Modifier.align(Alignment.BottomCenter),
                heightFraction = 0.45f
            )
            spark.durationSeconds?.let {
                CornerBadge(
                    text = "0:${it.toString().padStart(2, '0')}",
                    containerColor = Color.Black.copy(alpha = 0.55f),
                    contentColor = SurfaceWhite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = spark.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = SurfaceWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatCompactCount(spark.viewCount)} views",
                    style = MaterialTheme.typography.labelSmall,
                    color = SurfaceWhite.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun AudioRoomBanner(room: AudioRoom) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TextDark)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))) {
            AsyncImage(
                model = room.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            StatusPill(
                text = "LIVE AUDIO ROOM",
                containerColor = PrimaryYellow,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = room.title,
                style = MaterialTheme.typography.titleMedium,
                color = SurfaceWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatCompactCount(room.listenerCount)} listeners tuned in",
                style = MaterialTheme.typography.bodySmall,
                color = SurfaceWhite.copy(alpha = 0.7f)
            )
        }
        Spacer(Modifier.width(10.dp))
        StatusPill(
            text = "Join",
            containerColor = SurfaceWhite,
            contentColor = TextDark
        )
    }
}
