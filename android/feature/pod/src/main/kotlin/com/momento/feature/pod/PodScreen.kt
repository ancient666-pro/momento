package com.momento.feature.pod

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.momento.core.common.formatCompactCount
import com.momento.core.common.formatDuration
import com.momento.core.designsystem.components.CircularActionButton
import com.momento.core.designsystem.components.CornerBadge
import com.momento.core.designsystem.components.EngageAction
import com.momento.core.designsystem.components.ErrorState
import com.momento.core.designsystem.components.FilterChip
import com.momento.core.designsystem.components.GradientScrim
import com.momento.core.designsystem.components.LiveDot
import com.momento.core.designsystem.components.MomentoAvatar
import com.momento.core.designsystem.components.OverlappingAvatars
import com.momento.core.designsystem.components.PlayOverlay
import com.momento.core.designsystem.components.PostCardSkeleton
import com.momento.core.designsystem.components.PrimaryButton
import com.momento.core.designsystem.components.SecondaryButton
import com.momento.core.designsystem.components.StatusPill
import com.momento.core.designsystem.components.VerifiedBadge
import com.momento.core.designsystem.components.WaveformBars
import com.momento.core.designsystem.theme.PrimaryYellow
import com.momento.core.designsystem.theme.SuccessGreen
import com.momento.core.designsystem.theme.SurfaceLow
import com.momento.core.designsystem.theme.SurfaceWhite
import com.momento.core.designsystem.theme.TextDark
import com.momento.core.designsystem.theme.TextMuted
import com.momento.core.designsystem.theme.TextPrimary
import com.momento.core.designsystem.theme.TextSecondary
import com.momento.domain.model.EditorialDispatchCard
import com.momento.domain.model.EssayCard
import com.momento.domain.model.LiveAudioPodCard
import com.momento.domain.model.MediaReelCard
import com.momento.domain.model.PodFeedItem
import com.momento.domain.model.PodPollCard
import com.momento.domain.model.PodPollOption
import com.momento.domain.model.PodTopic
import com.momento.domain.model.SoundtrackCard

@Composable
fun PodScreen(viewModel: PodViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        PodTopBar(liveListenerCount = state.liveListenerCount)

        when {
            state.isLoading -> Column { repeat(2) { PostCardSkeleton() } }
            state.errorMessage != null && state.feed.isEmpty() -> ErrorState(
                message = state.errorMessage.orEmpty(),
                onRetry = viewModel::refresh,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            else -> PodContent(
                state = state,
                onTopicSelected = viewModel::selectTopic,
                onVote = viewModel::vote,
                onJoin = viewModel::joinPod
            )
        }
    }
}

@Composable
private fun PodTopBar(liveListenerCount: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryYellow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = TextDark,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "Pod",
                style = MaterialTheme.typography.headlineSmall,
                color = TextDark,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Point of Discussion",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Spacer(Modifier.width(10.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SurfaceLow)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveDot(size = 7.dp)
            Spacer(Modifier.width(5.dp))
            Text(
                text = "${formatCompactCount(liveListenerCount)} live",
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.weight(1f))
        CircularActionButton(
            icon = Icons.Filled.Search,
            contentDescription = "Search Pod",
            onClick = { /* Pod search arrives with the search phase */ },
            size = 40.dp,
            containerColor = Color.Transparent,
            iconSize = 21.dp
        )
        Spacer(Modifier.width(4.dp))
        CircularActionButton(
            icon = Icons.Filled.Add,
            contentDescription = "Start Pod Broadcast",
            onClick = { /* Pod creation flow arrives with the live audio phase */ },
            size = 40.dp,
            containerColor = PrimaryYellow,
            iconColor = TextDark,
            iconSize = 22.dp
        )
    }
}

@Composable
private fun PodContent(
    state: PodUiState,
    onTopicSelected: (String) -> Unit,
    onVote: (PodPollCard, String) -> Unit,
    onJoin: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (state.topics.isNotEmpty()) {
            item(key = "topics") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.topics, key = { it.id }) { topic ->
                        PodTopicChip(
                            topic = topic,
                            selected = topic.id == state.selectedTopicId,
                            onClick = { onTopicSelected(topic.id) }
                        )
                    }
                }
            }
        }
        items(state.feed, key = { it.id }) { item ->
            when (item) {
                is PodPollCard -> PollCard(item, onVote)
                is LiveAudioPodCard -> LiveAudioPodCard(item, onJoin)
                is MediaReelCard -> PodMediaReelCard(item)
                is EditorialDispatchCard -> EditorialCard(item)
                is SoundtrackCard -> SoundtrackCard(item)
                is EssayCard -> EssayCard(item)
            }
        }
    }
}

@Composable
private fun PodTopicChip(topic: PodTopic, selected: Boolean, onClick: () -> Unit) {
    val icon: ImageVector = when {
        topic.name == "Feed" -> Icons.Filled.GridOn
        topic.name.startsWith("#") && topic.live -> Icons.Filled.GraphicEq
        topic.name.startsWith("#") -> Icons.Filled.LocalFireDepartment
        else -> Icons.Filled.GridOn
    }
    FilterChip(
        label = topic.name,
        selected = selected,
        onClick = onClick,
        icon = icon
    )
}

@Composable
private fun CardShell(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceWhite)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun AuthorRow(
    avatarUrl: String,
    displayName: String,
    username: String,
    verified: Boolean,
    metaText: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MomentoAvatar(url = avatarUrl, size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (verified) {
                    Spacer(Modifier.width(4.dp))
                    VerifiedBadge(size = 15.dp)
                }
            }
            Text(
                text = "@$username · $metaText",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun PollCard(poll: PodPollCard, onVote: (PodPollCard, String) -> Unit) {
    CardShell {
        AuthorRow(
            avatarUrl = poll.author.avatarUrl,
            displayName = poll.author.displayName,
            username = poll.author.username,
            verified = poll.author.verified,
            metaText = "42m ago"
        ) {
            StatusPill(
                text = poll.endsInLabel,
                icon = Icons.Filled.Schedule,
                containerColor = PrimaryYellow
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = poll.question,
            style = MaterialTheme.typography.titleLarge,
            color = TextDark
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = poll.description,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            poll.options.forEach { option ->
                PollOptionRow(
                    option = option,
                    selected = poll.myVoteOptionId == option.id,
                    enabled = poll.myVoteOptionId == null,
                    onClick = { onVote(poll, option.id) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatCompactCount(poll.totalVotes)} votes tallied",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${formatCompactCount(poll.replyCount)} Pod replies",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun PollOptionRow(
    option: PodPollOption,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) PrimaryYellow else SurfaceLow)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(TextDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = PrimaryYellow,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = option.text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${option.percentage}%",
                style = MaterialTheme.typography.titleMedium,
                color = TextDark,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LiveAudioPodCard(card: LiveAudioPodCard, onJoin: (String) -> Unit) {
    CardShell {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LiveDot(size = 9.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "LIVE AUDIO POD",
                style = MaterialTheme.typography.labelMedium,
                color = SuccessGreen,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Filled.Headphones,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${formatCompactCount(card.listenerCount)} tuned in",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = card.title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextDark
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OverlappingAvatars(
                urls = card.hosts.map { it.avatarUrl },
                size = 34.dp,
                extraCount = (card.hosts.size - 4).coerceAtLeast(0)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Co-hosting audio session",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayOverlay(size = 52.dp, onClick = { /* Live playback arrives with the Call SDK phase */ })
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                WaveformBars(
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    seed = card.id.hashCode().toLong()
                )
                Spacer(Modifier.height(6.dp))
                Row {
                    Text(
                        text = formatDuration(card.positionSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatDuration(card.durationSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            PrimaryButton(
                text = "Join In",
                onClick = { onJoin(card.id) },
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun PodMediaReelCard(card: MediaReelCard) {
    CardShell {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.3f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = card.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            CornerBadge(
                text = "MEDIA REEL",
                containerColor = SurfaceWhite,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            )
            CornerBadge(
                text = formatCompactCount(card.viewCount),
                containerColor = Color.Black.copy(alpha = 0.55f),
                contentColor = SurfaceWhite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            )
            PlayOverlay(
                modifier = Modifier.align(Alignment.Center),
                size = 48.dp
            )
            CornerBadge(
                text = card.hashtag,
                containerColor = PrimaryYellow,
                contentColor = TextDark,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        AuthorRow(
            avatarUrl = card.author.avatarUrl,
            displayName = card.author.displayName,
            username = card.author.username,
            verified = card.author.verified,
            metaText = "now"
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = card.caption,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(10.dp))
        Row {
            EngageAction(Icons.Filled.Favorite, formatCompactCount(card.likeCount), onClick = { })
            EngageAction(Icons.Outlined.ChatBubbleOutline, formatCompactCount(card.commentCount), onClick = { })
            EngageAction(Icons.Outlined.Share, formatCompactCount(card.shareCount), onClick = { })
            Spacer(Modifier.weight(1f))
            EngageAction(Icons.Outlined.BookmarkBorder, "", onClick = { })
        }
    }
}

@Composable
private fun EditorialCard(card: EditorialDispatchCard) {
    CardShell {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "EDITORIAL DISPATCH",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${card.readMinutes} min read",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = card.title,
            style = MaterialTheme.typography.titleLarge,
            color = TextDark
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = card.snippet,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceLow)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.domain,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
                Text(
                    text = "By ${card.authorName} · ${formatCompactCount(card.footnoteCount)} community footnotes",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatCompactCount(card.participantCount)} Pod participants",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton(text = "Discuss", onClick = { /* Discussion thread arrives with the comments phase */ })
        }
    }
}

@Composable
private fun SoundtrackCard(card: SoundtrackCard) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryYellow)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COMMUNITY SOUNDTRACK",
                style = MaterialTheme.typography.labelMedium,
                color = TextDark,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Filled.Equalizer,
                contentDescription = null,
                tint = TextDark,
                modifier = Modifier.size(18.dp)
            )
        }
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = card.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Now Echoing: ${card.title}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextDark
                )
                Text(
                    text = "${card.artist} · ${formatCompactCount(card.clipCount)} Pod clips",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "#${card.rank} ON SOUNDBOARD",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF97316),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecondaryButton(
                text = "Use in Pod",
                onClick = { /* Soundtrack attachment arrives with the media phase */ },
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                text = "Listen & Talk",
                onClick = { /* Playback arrives with the media phase */ },
                modifier = Modifier.weight(1f),
                dark = true
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun EssayCard(card: EssayCard) {
    CardShell {
        AuthorRow(
            avatarUrl = card.author.avatarUrl,
            displayName = card.author.displayName,
            username = card.author.username,
            verified = card.author.verified,
            metaText = card.postedAtLabel
        ) {
            StatusPill(
                text = "ESSAY",
                containerColor = PrimaryYellow
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = card.title,
            style = MaterialTheme.typography.titleLarge,
            color = TextDark
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = card.preview,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(10.dp))
        Row {
            EngageAction(Icons.Filled.Favorite, formatCompactCount(card.likeCount), onClick = { })
            EngageAction(Icons.Outlined.ChatBubbleOutline, formatCompactCount(card.commentCount), onClick = { })
            EngageAction(Icons.Outlined.Share, formatCompactCount(card.shareCount), onClick = { })
            Spacer(Modifier.weight(1f))
            EngageAction(Icons.Outlined.BookmarkBorder, "", onClick = { })
        }
    }
}
