package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.Comment
import com.example.data.model.Post
import com.example.data.model.PublicProfile
import com.example.data.model.SponsoredAd
import com.example.data.model.UiState
import com.example.ui.components.MediaImage
import com.example.ui.components.ReportDialog
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerifiedCreatorBadge
import com.example.ui.components.VideoPlayerSurface
import com.example.ui.theme.GoldCreator
import com.example.ui.theme.RoseReaction
import com.example.ui.viewmodel.CreatorViewModel
import com.example.ui.viewmodel.FeedFilter
import com.example.ui.viewmodel.FeedViewModel
import com.example.ui.viewmodel.SocialViewModel
import com.example.util.MediaUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    currentUser: PublicProfile,
    blockedUserIds: List<String>,
    feedViewModel: FeedViewModel,
    socialViewModel: SocialViewModel,
    creatorViewModel: CreatorViewModel,
    onOpenChats: () -> Unit,
    onOpenUserProfile: (String) -> Unit
) {
    val context = LocalContext.current
    val postsState by feedViewModel.publicPostsState.collectAsStateWithLifecycle()
    val feedFilter by feedViewModel.feedFilter.collectAsStateWithLifecycle()
    val activeHashtag by feedViewModel.activeHashtag.collectAsStateWithLifecycle()
    val searchQuery by feedViewModel.searchQuery.collectAsStateWithLifecycle()
    val visibleLimit by feedViewModel.visibleLimit.collectAsStateWithLifecycle()
    val userReactions by feedViewModel.userReactions.collectAsStateWithLifecycle()
    val savedPostIds by feedViewModel.savedPostIds.collectAsStateWithLifecycle()
    val followingIds by socialViewModel.followingIds.collectAsStateWithLifecycle()
    val activeProfilesState by socialViewModel.activeProfilesState.collectAsStateWithLifecycle()
    val activeAds by creatorViewModel.activeAds.collectAsStateWithLifecycle()
    val announcements by creatorViewModel.activeAnnouncements.collectAsStateWithLifecycle()
    val selectedPostForComments by feedViewModel.selectedPostIdForComments.collectAsStateWithLifecycle()
    val commentsState by feedViewModel.commentsState.collectAsStateWithLifecycle()
    val isPublishing by feedViewModel.isPublishing.collectAsStateWithLifecycle()

    var showSearchBar by remember { mutableStateOf(false) }
    var showComposerDialog by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    val profilesById = remember(activeProfilesState) {
        (activeProfilesState as? UiState.Success)?.data?.associateBy { it.uid } ?: emptyMap()
    }

    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 4 && lastVisible >= total - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            feedViewModel.loadMorePosts()
        }
    }

    if (showComposerDialog) {
        CreatePostDialog(
            isPublishing = isPublishing,
            onDismiss = { showComposerDialog = false },
            onPublish = { text, mediaType, mediaData, thumbData, visibility ->
                feedViewModel.createPost(
                    author = currentUser,
                    text = text,
                    mediaType = mediaType,
                    mediaData = mediaData,
                    thumbnailData = thumbData,
                    visibility = visibility,
                    isReel = false,
                    onSuccess = { showComposerDialog = false }
                )
            }
        )
    }

    reportTarget?.let { (type, id) ->
        ReportDialog(
            targetType = type,
            onDismiss = { reportTarget = null },
            onSubmit = { reason ->
                feedViewModel.submitReport(type, id, reason) { reportTarget = null }
            }
        )
    }

    val commentPost = remember(selectedPostForComments, postsState) {
        val id = selectedPostForComments ?: return@remember null
        (postsState as? UiState.Success)?.data?.firstOrNull { it.id == id }
    }

    if (commentPost != null) {
        CommentsBottomSheet(
            post = commentPost,
            commentsState = commentsState,
            onDismiss = { feedViewModel.openComments(null) },
            onSendComment = { text, parentId ->
                feedViewModel.addComment(commentPost, currentUser, text, parentId)
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
        ) {
            // Facebook-inspired top header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.img_app_icon_fg),
                                contentDescription = "Deepak Social",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Deepak Social",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row {
                            IconButton(
                                onClick = { showSearchBar = !showSearchBar },
                                modifier = Modifier.testTag("home_search_toggle")
                            ) {
                                Icon(
                                    imageVector = if (showSearchBar) Icons.Filled.Close else Icons.Filled.Search,
                                    contentDescription = "Search posts and hashtags"
                                )
                            }
                            IconButton(
                                onClick = onOpenChats,
                                modifier = Modifier.testTag("home_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "Open Messages"
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = showSearchBar) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { feedViewModel.setSearchQuery(it) },
                            placeholder = { Text("Search posts, creators, or #hashtags") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { feedViewModel.setSearchQuery("") }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("home_search_input")
                        )
                    }
                }
            }

            when (val state = postsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is UiState.Success -> {
                    val filteredPosts = remember(
                        state.data,
                        feedFilter,
                        activeHashtag,
                        searchQuery,
                        followingIds,
                        savedPostIds,
                        blockedUserIds,
                        visibleLimit
                    ) {
                        val base = state.data
                            .filter { it.authorId !in blockedUserIds }
                            .filter { post ->
                                when (feedFilter) {
                                    FeedFilter.FOR_YOU -> true
                                    FeedFilter.FOLLOWING -> post.authorId in followingIds || post.authorId == currentUser.uid
                                    FeedFilter.SAVED -> post.id in savedPostIds
                                }
                            }
                            .filter { post ->
                                val tag = activeHashtag
                                tag == null || post.hashtags.contains(tag)
                            }
                            .filter { post ->
                                val q = searchQuery.trim().lowercase()
                                q.isBlank() ||
                                    post.text.lowercase().contains(q) ||
                                    post.authorName.lowercase().contains(q) ||
                                    post.hashtags.any { it.contains(q.removePrefix("#")) }
                            }

                        val ranked = if (feedFilter == FeedFilter.FOR_YOU && searchQuery.isBlank() && activeHashtag == null) {
                            base.sortedByDescending { post ->
                                val followBoost = if (post.authorId in followingIds) 500_000L else 0L
                                val engagement = post.likesCount * 4000L + post.commentsCount * 6000L
                                val recency = post.createdAt?.seconds ?: 0L
                                recency + followBoost + engagement
                            }
                        } else {
                            base
                        }
                        ranked.take(visibleLimit)
                    }

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Active platform announcements
                        if (announcements.isNotEmpty()) {
                            item(key = "announcements_banner") {
                                AnnouncementBanner(announcements.first())
                            }
                        }

                        // Quick composer card
                        item(key = "composer_card") {
                            QuickComposerCard(
                                user = currentUser,
                                onOpenComposer = { showComposerDialog = true }
                            )
                        }

                        // Feed filters + active hashtag pill
                        item(key = "filter_chips") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = feedFilter == FeedFilter.FOR_YOU,
                                    onClick = { feedViewModel.setFeedFilter(FeedFilter.FOR_YOU) },
                                    label = { Text("For You") },
                                    modifier = Modifier.testTag("filter_for_you")
                                )
                                FilterChip(
                                    selected = feedFilter == FeedFilter.FOLLOWING,
                                    onClick = { feedViewModel.setFeedFilter(FeedFilter.FOLLOWING) },
                                    label = { Text("Following") },
                                    modifier = Modifier.testTag("filter_following")
                                )
                                FilterChip(
                                    selected = feedFilter == FeedFilter.SAVED,
                                    onClick = { feedViewModel.setFeedFilter(FeedFilter.SAVED) },
                                    label = { Text("Saved (${savedPostIds.size})") },
                                    modifier = Modifier.testTag("filter_saved")
                                )
                                activeHashtag?.let { tag ->
                                    AssistChip(
                                        onClick = { feedViewModel.setActiveHashtag(null) },
                                        label = { Text("#$tag ×") }
                                    )
                                }
                            }
                        }

                        if (filteredPosts.isEmpty()) {
                            item(key = "empty_feed") {
                                EmptyFeedCard(onCreateClick = { showComposerDialog = true })
                            }
                        } else {
                            itemsIndexed(
                                items = filteredPosts,
                                key = { _, post -> post.id }
                            ) { index, post ->
                                LaunchedEffect(post.id) {
                                    feedViewModel.recordViewOnce(post)
                                }

                                val authorProfile = profilesById[post.authorId]
                                val isFollowingAuthor = post.authorId in followingIds

                                PostCard(
                                    post = post,
                                    authorProfile = authorProfile,
                                    currentUserId = currentUser.uid,
                                    currentReaction = userReactions[post.id],
                                    isSaved = post.id in savedPostIds,
                                    isFollowingAuthor = isFollowingAuthor,
                                    onAuthorClick = { onOpenUserProfile(post.authorId) },
                                    onToggleFollow = {
                                        val target = authorProfile ?: PublicProfile(
                                            uid = post.authorId,
                                            displayName = post.authorName,
                                            username = post.authorUsername
                                        )
                                        socialViewModel.toggleFollow(currentUser, target)
                                    },
                                    onReact = { reaction ->
                                        feedViewModel.toggleReaction(post, reaction, currentUser.displayName)
                                    },
                                    onOpenComments = { feedViewModel.openComments(post.id) },
                                    onShare = {
                                        feedViewModel.recordShare(post.id)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "${post.authorName} on Deepak Social: ${post.text}"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share post"))
                                    },
                                    onToggleSave = { feedViewModel.toggleSave(post.id) },
                                    onHashtagClick = { tag -> feedViewModel.setActiveHashtag(tag) },
                                    onReport = { reportTarget = (if (post.reelPost) "reel" else "post") to post.id },
                                    onBlockAuthor = { socialViewModel.toggleBlockUser(post.authorId, block = true) },
                                    onDeletePost = { feedViewModel.deletePost(post, currentUser.postsCount) }
                                )

                                // Interleave real sponsored ads every 3 posts
                                if (activeAds.isNotEmpty() && (index + 1) % 3 == 0) {
                                    val ad = activeAds[((index + 1) / 3 - 1) % activeAds.size]
                                    LaunchedEffect(ad.id) {
                                        creatorViewModel.recordAdImpressionOnce(ad.id)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    SponsoredAdCard(
                                        ad = ad,
                                        onCtaClick = {
                                            creatorViewModel.recordAdClick(ad.id)
                                            if (ad.ctaUrl.startsWith("http")) {
                                                runCatching {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ad.ctaUrl)))
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementBanner(announcement: Announcement) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Campaign,
                contentDescription = "Announcement",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = announcement.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun QuickComposerCard(
    user: PublicProfile,
    onOpenComposer: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    avatarData = user.avatarData,
                    displayName = user.displayName,
                    size = 42.dp,
                    showOnline = true
                )
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onOpenComposer)
                        .testTag("open_post_composer_button")
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "What's on your mind, ${user.displayName.substringBefore(" ")}?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onOpenComposer, modifier = Modifier.testTag("composer_photo_shortcut")) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Photo")
                }
                TextButton(onClick = onOpenComposer, modifier = Modifier.testTag("composer_video_shortcut")) {
                    Icon(
                        imageVector = Icons.Filled.VideoCall,
                        contentDescription = null,
                        tint = RoseReaction,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Video")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostCard(
    post: Post,
    authorProfile: PublicProfile?,
    currentUserId: String,
    currentReaction: String?,
    isSaved: Boolean,
    isFollowingAuthor: Boolean,
    onAuthorClick: () -> Unit,
    onToggleFollow: () -> Unit,
    onReact: (String) -> Unit,
    onOpenComments: () -> Unit,
    onShare: () -> Unit,
    onToggleSave: () -> Unit,
    onHashtagClick: (String) -> Unit,
    onReport: () -> Unit,
    onBlockAuthor: () -> Unit,
    onDeletePost: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showReactionBar by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    avatarData = authorProfile?.avatarData ?: post.authorAvatar,
                    displayName = post.authorName,
                    size = 44.dp,
                    showOnline = authorProfile?.online == true,
                    modifier = Modifier.clickable(onClick = onAuthorClick)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onAuthorClick)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        VerifiedCreatorBadge(authorProfile?.monetizationStatus ?: "none")
                    }
                    Text(
                        text = "@${post.authorUsername} • ${MediaUtils.formatRelativeTime(post.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (post.authorId != currentUserId) {
                    TextButton(
                        onClick = onToggleFollow,
                        modifier = Modifier.testTag("follow_author_${post.authorId}")
                    ) {
                        Text(if (isFollowingAuthor) "Following" else "Follow")
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("post_menu_${post.id}")
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Post options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isSaved) "Unsave post" else "Save post") },
                            onClick = {
                                showMenu = false
                                onToggleSave()
                            }
                        )
                        if (post.authorId == currentUserId) {
                            DropdownMenuItem(
                                text = { Text("Delete post", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDeletePost()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Report post") },
                                onClick = {
                                    showMenu = false
                                    onReport()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Block user", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onBlockAuthor()
                                }
                            )
                        }
                    }
                }
            }

            if (post.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = post.text,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (post.hashtags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    post.hashtags.forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clickable { onHashtagClick(tag) }
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (post.mediaType == "image" && !post.mediaData.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                MediaImage(
                    mediaData = post.mediaData,
                    contentDescription = "Post photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else if (post.mediaType == "video") {
                Spacer(modifier = Modifier.height(10.dp))
                VideoPlayerSurface(
                    videoSource = post.mediaData,
                    thumbnailData = post.thumbnailData,
                    isPlaying = isVideoPlaying,
                    isMuted = false,
                    onTogglePlay = { isVideoPlaying = !isVideoPlaying },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${post.likesCount} reactions • ${post.viewsCount} views",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${post.commentsCount} comments • ${post.sharesCount} shares",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = showReactionBar) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            "like" to "Like",
                            "love" to "Love",
                            "haha" to "Haha",
                            "wow" to "Wow",
                            "sad" to "Sad"
                        ).forEach { (key, label) ->
                            TextButton(
                                onClick = {
                                    showReactionBar = false
                                    onReact(key)
                                }
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (currentReaction == key) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentReaction == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showReactionBar = !showReactionBar },
                    modifier = Modifier.testTag("react_button_${post.id}")
                ) {
                    Icon(
                        imageVector = when (currentReaction) {
                            "love" -> Icons.Filled.Favorite
                            null -> Icons.Outlined.ThumbUp
                            else -> Icons.Filled.ThumbUp
                        },
                        contentDescription = "React",
                        tint = when (currentReaction) {
                            "love" -> RoseReaction
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentReaction?.replaceFirstChar { it.uppercase() } ?: "Like",
                        color = if (currentReaction != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = onOpenComments,
                    modifier = Modifier.testTag("comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Comment", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                TextButton(
                    onClick = onShare,
                    modifier = Modifier.testTag("share_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.testTag("save_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) "Saved" else "Save post",
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SponsoredAdCard(
    ad: SponsoredAd,
    onCtaClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ad.sponsorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldCreator.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Sponsored",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = ad.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = ad.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = onCtaClick, modifier = Modifier.fillMaxWidth()) {
                Text(ad.ctaText)
            }
        }
    }
}

@Composable
private fun EmptyFeedCard(onCreateClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_social),
                contentDescription = "Empty feed illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your feed is ready for stories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Share the first photo, video, or update with #DeepakSocial.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(onClick = onCreateClick, modifier = Modifier.testTag("empty_feed_create_button")) {
                Text("Create First Post")
            }
        }
    }
}

@Composable
private fun CreatePostDialog(
    isPublishing: Boolean,
    onDismiss: () -> Unit,
    onPublish: (text: String, mediaType: String, mediaData: String?, thumbData: String?, visibility: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("none") }
    var mediaData by remember { mutableStateOf<String?>(null) }
    var thumbData by remember { mutableStateOf<String?>(null) }
    var visibility by remember { mutableStateOf("public") }
    var isProcessingMedia by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessingMedia = true
                val b64 = MediaUtils.uriToCompressedBase64(context, uri)
                if (b64 != null) {
                    mediaType = "image"
                    mediaData = b64
                    thumbData = null
                }
                isProcessingMedia = false
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessingMedia = true
                val thumb = MediaUtils.extractVideoThumbnailBase64(context, uri)
                mediaType = "video"
                mediaData = uri.toString()
                thumbData = thumb
                isProcessingMedia = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Post") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(2000) },
                    label = { Text("Share an update or #hashtag...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_post_text_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isProcessingMedia,
                        modifier = Modifier.weight(1f).testTag("pick_post_image_button")
                    ) {
                        Text("Photo")
                    }
                    OutlinedButton(
                        onClick = {
                            videoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        enabled = !isProcessingMedia,
                        modifier = Modifier.weight(1f).testTag("pick_post_video_button")
                    ) {
                        Text("Video")
                    }
                }

                if (isProcessingMedia) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Optimizing media...", style = MaterialTheme.typography.bodySmall)
                    }
                } else if (mediaType != "none") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (mediaType == "image") "Photo attached" else "Video attached",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = {
                            mediaType = "none"
                            mediaData = null
                            thumbData = null
                        }) {
                            Text("Remove")
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = visibility == "public",
                        onClick = { visibility = "public" },
                        label = { Text("Public") }
                    )
                    FilterChip(
                        selected = visibility == "friends",
                        onClick = { visibility = "friends" },
                        label = { Text("Friends Only") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(text, mediaType, mediaData, thumbData, visibility) },
                enabled = !isPublishing && !isProcessingMedia && (text.isNotBlank() || !mediaData.isNullOrBlank()),
                modifier = Modifier.testTag("publish_post_button")
            ) {
                Text(if (isPublishing) "Posting..." else "Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: Post,
    commentsState: UiState<List<Comment>>,
    onDismiss: () -> Unit,
    onSendComment: (text: String, parentCommentId: String?) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<Comment?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Comments (${post.commentsCount})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            when (commentsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Text(commentsState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    val topLevel = commentsState.data.filter { it.parentCommentId.isNullOrBlank() }
                    val repliesByParent = commentsState.data.filter { !it.parentCommentId.isNullOrBlank() }
                        .groupBy { it.parentCommentId!! }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (topLevel.isEmpty()) {
                            item {
                                Text(
                                    text = "No comments yet. Start the conversation!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 20.dp)
                                )
                            }
                        } else {
                            itemsIndexed(topLevel, key = { _, c -> c.id }) { _, comment ->
                                CommentItemRow(
                                    comment = comment,
                                    isReply = false,
                                    onReplyClick = { replyingTo = comment }
                                )
                                val replies = repliesByParent[comment.id].orEmpty()
                                replies.forEach { reply ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    CommentItemRow(
                                        comment = reply,
                                        isReply = true,
                                        onReplyClick = { replyingTo = comment }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            replyingTo?.let { parent ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Replying to ${parent.authorName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { replyingTo = null }) {
                        Text("Cancel")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it.take(1000) },
                    placeholder = { Text("Write a comment...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendComment(inputText, replyingTo?.id)
                            inputText = ""
                            replyingTo = null
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("send_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Post comment",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItemRow(
    comment: Comment,
    isReply: Boolean,
    onReplyClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 34.dp else 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        UserAvatar(
            avatarData = comment.authorAvatar,
            displayName = comment.authorName,
            size = if (isReply) 28.dp else 34.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = comment.authorName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = MediaUtils.formatRelativeTime(comment.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Reply",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onReplyClick)
                )
            }
        }
    }
}
