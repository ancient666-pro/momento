package com.momento.feature.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.momento.core.common.formatCompactCount
import com.momento.core.designsystem.components.CircularActionButton
import com.momento.core.designsystem.components.ErrorState
import com.momento.core.designsystem.components.MomentoAvatar
import com.momento.core.designsystem.components.OverlappingAvatars
import com.momento.core.designsystem.components.PostCardSkeleton
import com.momento.core.designsystem.components.VerifiedBadge
import com.momento.core.designsystem.theme.ErrorRed
import com.momento.core.designsystem.theme.PrimaryYellow
import com.momento.core.designsystem.theme.SurfaceHigh
import com.momento.core.designsystem.theme.SurfaceLow
import com.momento.core.designsystem.theme.SurfaceWhite
import com.momento.core.designsystem.theme.TextDark
import com.momento.core.designsystem.theme.TextMuted
import com.momento.core.designsystem.theme.TextPrimary
import com.momento.core.designsystem.theme.TextSecondary
import com.momento.domain.model.Moment
import com.momento.domain.model.UserProfile

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        ProfileTopBar()

        when {
            state.isLoading -> Column { repeat(2) { PostCardSkeleton() } }
            state.profile == null -> ErrorState(
                message = state.errorMessage.orEmpty(),
                onRetry = viewModel::refresh,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            else -> ProfileContent(
                profile = state.profile!!,
                moments = state.moments,
                posts = state.posts,
                selectedTab = state.selectedTab,
                isFollowing = state.isFollowing,
                onTabSelected = viewModel::selectTab,
                onToggleFollow = viewModel::toggleFollow
            )
        }
    }
}

@Composable
private fun ProfileTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularActionButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = { /* Profile is a root destination in this scaffold */ },
            size = 40.dp,
            containerColor = Color.Transparent,
            iconSize = 22.dp
        )
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineSmall,
            color = TextDark,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Box {
            CircularActionButton(
                icon = Icons.Filled.Notifications,
                contentDescription = "Notifications",
                onClick = { /* Notifications screen arrives with the notifications phase */ },
                size = 40.dp,
                containerColor = Color.Transparent,
                iconSize = 21.dp
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 7.dp, end = 9.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(ErrorRed)
            )
        }
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfile,
    moments: List<Moment>,
    posts: List<com.momento.domain.model.Post>,
    selectedTab: ProfileTab,
    isFollowing: Boolean,
    onTabSelected: (ProfileTab) -> Unit,
    onToggleFollow: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "profile-header") {
            ProfileHeader(profile, isFollowing, onToggleFollow)
        }
        item(key = "profile-tabs") {
            ProfileTabSelector(selectedTab, onTabSelected)
        }
        item(key = "highlights") {
            HighlightsRow(moments)
        }
        when (selectedTab) {
            ProfileTab.POST -> item(key = "posts") {
                Column {
                    posts.forEach { post ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceWhite)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = post.mediaUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = post.caption,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    maxLines = 2
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Filled.Favorite, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = formatCompactCount(post.likeCount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            ProfileTab.REELS -> item(key = "moments-grid") {
                MomentsGrid(moments)
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatBlock(value = formatCompactCount(profile.followerCount), label = "Followers")
            MomentoAvatar(
                url = profile.avatarUrl,
                size = 104.dp,
                ringColor = SurfaceLow
            )
            StatBlock(
                value = formatCompactCount(profile.followingCount),
                label = "Following",
                alignEnd = true
            )
        }

        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextDark
                )
                if (profile.verified) {
                    Spacer(Modifier.width(6.dp))
                    VerifiedBadge(size = 17.dp)
                }
            }
            Text(
                text = "@${profile.username}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            if (profile.bio.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = profile.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            val category = profile.category
            if (category != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularActionButton(
                icon = Icons.Filled.Favorite,
                contentDescription = if (isFollowing) "Unfollow" else "Follow",
                onClick = onToggleFollow,
                size = 40.dp,
                containerColor = if (isFollowing) SurfaceWhite else PrimaryYellow,
                iconColor = if (isFollowing) ErrorRed else TextDark,
                iconSize = 18.dp
            )
        }
    }
}

@Composable
private fun StatBlock(value: String, label: String, alignEnd: Boolean = false) {
    Column(
        modifier = Modifier.width(76.dp),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextDark
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
private fun ProfileTabSelector(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(50))
            .background(SurfaceLow)
            .padding(4.dp)
    ) {
        ProfileTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) SurfaceWhite else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (tab == ProfileTab.POST) Icons.Filled.GridOn else Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = if (selected) TextDark else TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (tab == ProfileTab.POST) "Post" else "Moments",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) TextDark else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun HighlightsRow(moments: List<Moment>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        moments.take(4).forEach { moment ->
            MomentoAvatar(
                url = moment.thumbnailUrl,
                size = 48.dp,
                modifier = Modifier.padding(end = 2.dp),
                ringColor = SurfaceHigh
            )
        }
        Spacer(Modifier.width(10.dp))
        HighlightCard(
            text = "${moments.size} videos in your drift",
            thumbnailUrl = moments.firstOrNull()?.thumbnailUrl,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        HighlightCard(
            text = "Take a look",
            thumbnailUrl = moments.getOrNull(1)?.thumbnailUrl,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HighlightCard(
    text: String,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceLow)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            maxLines = 2
        )
    }
}

@Composable
private fun MomentsGrid(moments: List<Moment>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        moments.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                row.forEach { moment ->
                    MomentCell(moment, Modifier.weight(1f))
                }
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MomentCell(moment: Moment, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(0.66f)
            .clip(RoundedCornerShape(4.dp))
    ) {
        AsyncImage(
            model = moment.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (moment.durationSeconds > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = SurfaceWhite,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "0:${moment.durationSeconds.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SurfaceWhite
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = SurfaceWhite,
                modifier = Modifier.size(11.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = formatCompactCount(moment.viewCount),
                style = MaterialTheme.typography.labelSmall,
                color = SurfaceWhite
            )
        }
    }
}
