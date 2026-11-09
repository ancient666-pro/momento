package com.momento.feature.home

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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
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
import com.momento.core.designsystem.components.CornerBadge
import com.momento.core.designsystem.components.EngageAction
import com.momento.core.designsystem.components.ErrorState
import com.momento.core.designsystem.components.GradientScrim
import com.momento.core.designsystem.components.MomentoAvatar
import com.momento.core.designsystem.components.PlayOverlay
import com.momento.core.designsystem.components.PostCardSkeleton
import com.momento.core.designsystem.components.SectionHeader
import com.momento.core.designsystem.components.StatusPill
import com.momento.core.designsystem.components.VerifiedBadge
import com.momento.core.designsystem.theme.PrimaryYellow
import com.momento.core.designsystem.theme.SuccessGreen
import com.momento.core.designsystem.theme.SurfaceWhite
import com.momento.core.designsystem.theme.TextDark
import com.momento.core.designsystem.theme.TextMuted
import com.momento.core.designsystem.theme.TextPrimary
import com.momento.core.designsystem.theme.TextSecondary
import com.momento.domain.model.MediaType
import com.momento.domain.model.Post
import com.momento.domain.model.Story
import com.momento.domain.model.StoryStatus

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        HomeTopBar(
            selectedTab = state.selectedTab,
            onTabSelected = viewModel::selectTab
        )

        when {
            state.isLoading -> LoadingFeed()
            state.errorMessage != null -> ErrorState(
                message = state.errorMessage.orEmpty(),
                onRetry = viewModel::refresh,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            else -> FeedContent(
                stories = state.stories,
                posts = state.posts,
                onToggleLike = viewModel::toggleLike,
                onToggleBookmark = viewModel::toggleBookmark
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    selectedTab: FeedTab,
    onTabSelected: (FeedTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FeedTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (tab == FeedTab.FOLLOWING) "Following" else "For You",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) TextDark else TextMuted
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (selected) PrimaryYellow else Color.Transparent)
                )
            }
        }
        Spacer(Modifier.weight(1f))
        CircularActionButton(
            icon = Icons.Filled.Search,
            contentDescription = "Search",
            onClick = { /* Search screen arrives with the Discover search phase */ },
            containerColor = PrimaryYellow,
            iconColor = TextDark,
            size = 42.dp,
            iconSize = 20.dp
        )
    }
}

@Composable
private fun LoadingFeed() {
    Column {
        repeat(2) { PostCardSkeleton() }
    }
}

@Composable
private fun FeedContent(
    stories: List<Story>,
    posts: List<Post>,
    onToggleLike: (Post) -> Unit,
    onToggleBookmark: (Post) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (stories.isNotEmpty()) {
            item(key = "stories-header") {
                SectionHeader(
                    title = "Stories",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
            item(key = "stories-carousel") {
                StoryCarousel(stories)
            }
        }
        item(key = "trending-header") {
            SectionHeader(
                title = "Trending",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                onToggleLike = { onToggleLike(post) },
                onToggleBookmark = { onToggleBookmark(post) }
            )
        }
    }
}

@Composable
fun StoryCarousel(stories: List<Story>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stories, key = { it.id }) { story ->
            StoryCard(story)
        }
    }
}

@Composable
private fun StoryCard(story: Story) {
    Box(
        modifier = Modifier
            .width(128.dp)
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(18.dp))
    ) {
        AsyncImage(
            model = story.mediaUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        GradientScrim(
            modifier = Modifier.align(Alignment.BottomCenter),
            heightFraction = 0.55f
        )
        when (story.status) {
            StoryStatus.LIVE -> StatusPill(
                text = "Live ${formatCompactCount(story.liveViewerCount)}",
                icon = null,
                containerColor = PrimaryYellow,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
            StoryStatus.PREMIERE -> StatusPill(
                text = "Premiere",
                containerColor = SurfaceWhite,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
            StoryStatus.NONE -> {}
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MomentoAvatar(url = story.author.avatarUrl, size = 24.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = story.author.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = SurfaceWhite,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PostCard(
    post: Post,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MomentoAvatar(url = post.author.avatarUrl, size = 42.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = post.author.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    if (post.author.verified) {
                        Spacer(Modifier.width(4.dp))
                        VerifiedBadge(size = 15.dp)
                    }
                }
                Text(
                    text = "@${post.author.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f)
                .clip(RoundedCornerShape(20.dp))
        ) {
            AsyncImage(
                model = post.mediaUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            GradientScrim(
                modifier = Modifier.align(Alignment.BottomCenter),
                heightFraction = 0.4f
            )
            if (post.mediaType == MediaType.VIDEO) {
                PlayOverlay(
                    modifier = Modifier.align(Alignment.Center),
                    size = 52.dp
                )
                post.videoDurationSeconds?.let {
                    CornerBadge(
                        text = "${it / 60}:${(it % 60).toString().padStart(2, '0')}",
                        containerColor = Color.Black.copy(alpha = 0.55f),
                        contentColor = SurfaceWhite,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EngagementOverlay(icon = Icons.Filled.Favorite, count = formatCompactCount(post.likeCount))
                EngagementOverlay(icon = Icons.Outlined.ChatBubbleOutline, count = formatCompactCount(post.commentCount))
                EngagementOverlay(icon = Icons.Outlined.Share, count = formatCompactCount(post.shareCount))
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            EngageAction(
                icon = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                count = formatCompactCount(post.likeCount),
                onClick = onToggleLike,
                tint = if (post.isLiked) Color(0xFFE0245E) else TextPrimary
            )
            EngageAction(
                icon = Icons.Outlined.ChatBubbleOutline,
                count = formatCompactCount(post.commentCount),
                onClick = { /* Comment sheet arrives with the comments phase */ }
            )
            EngageAction(
                icon = Icons.Outlined.Share,
                count = formatCompactCount(post.shareCount),
                onClick = { /* Share sheet arrives with the sharing phase */ }
            )
            Spacer(Modifier.weight(1f))
            EngageAction(
                icon = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                count = "",
                onClick = onToggleBookmark,
                tint = if (post.isBookmarked) PrimaryYellow else TextPrimary
            )
        }
    }
}

@Composable
private fun EngagementOverlay(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SurfaceWhite,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(text = count, style = MaterialTheme.typography.labelMedium, color = SurfaceWhite)
    }
}
