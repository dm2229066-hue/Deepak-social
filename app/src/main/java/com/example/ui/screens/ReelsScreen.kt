package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Post
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.components.ReportDialog
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerifiedCreatorBadge
import com.example.ui.components.VideoPlayerSurface
import com.example.ui.theme.RoseReaction
import com.example.ui.viewmodel.FeedViewModel
import com.example.ui.viewmodel.SocialViewModel
import com.example.util.MediaUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReelsScreen(
    currentUser: PublicProfile,
    blockedUserIds: List<String>,
    feedViewModel: FeedViewModel,
    socialViewModel: SocialViewModel,
    onOpenUserProfile: (String) -> Unit
) {
    val context = LocalContext.current
    val postsState by feedViewModel.publicPostsState.collectAsStateWithLifecycle()
    val userReactions by feedViewModel.userReactions.collectAsStateWithLifecycle()
    val followingIds by socialViewModel.followingIds.collectAsStateWithLifecycle()
    val activeProfilesState by socialViewModel.activeProfilesState.collectAsStateWithLifecycle()
    val selectedPostForComments by feedViewModel.selectedPostIdForComments.collectAsStateWithLifecycle()
    val commentsState by feedViewModel.commentsState.collectAsStateWithLifecycle()
    val isPublishing by feedViewModel.isPublishing.collectAsStateWithLifecycle()

    var showCreateReel by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var reportReelId by remember { mutableStateOf<String?>(null) }

    val profilesById = remember(activeProfilesState) {
        (activeProfilesState as? UiState.Success)?.data?.associateBy { it.uid } ?: emptyMap()
    }

    val reels = remember(postsState, blockedUserIds) {
        (postsState as? UiState.Success)?.data
            ?.filter { it.reelPost && it.authorId !in blockedUserIds }
            .orEmpty()
    }

    if (showCreateReel) {
        CreateReelDialog(
            isPublishing = isPublishing,
            onDismiss = { showCreateReel = false },
            onPublish = { caption, videoUrl, thumb ->
                feedViewModel.createPost(
                    author = currentUser,
                    text = caption,
                    mediaType = "video",
                    mediaData = videoUrl,
                    thumbnailData = thumb,
                    visibility = "public",
                    isReel = true,
                    onSuccess = { showCreateReel = false }
                )
            }
        )
    }

    reportReelId?.let { targetId ->
        ReportDialog(
            targetType = "reel",
            onDismiss = { reportReelId = null },
            onSubmit = { reason ->
                feedViewModel.submitReport("reel", targetId, reason) { reportReelId = null }
            }
        )
    }

    val commentReel = remember(selectedPostForComments, reels) {
        val id = selectedPostForComments ?: return@remember null
        reels.firstOrNull { it.id == id }
    }
    if (commentReel != null) {
        CommentsBottomSheet(
            post = commentReel,
            commentsState = commentsState,
            onDismiss = { feedViewModel.openComments(null) },
            onSendComment = { text, parentId ->
                feedViewModel.addComment(commentReel, currentUser, text, parentId)
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (postsState) {
            is UiState.Loading -> {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UiState.Error -> {
                Text(
                    text = (postsState as UiState.Error).message,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
            }
            is UiState.Success -> {
                if (reels.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayCircle,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Reels Yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Upload a short video Reel with #hashtags to start earning views!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showCreateReel = true },
                            modifier = Modifier.testTag("empty_reels_upload_button")
                        ) {
                            Text("Upload Reel")
                        }
                    }
                } else {
                    val pagerState = rememberPagerState(pageCount = { reels.size })
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val reel = reels[page]
                        val isCurrentPage = pagerState.currentPage == page
                        val authorProfile = profilesById[reel.authorId]
                        val isLiked = userReactions[reel.id] != null
                        val isFollowing = reel.authorId in followingIds

                        LaunchedEffect(isCurrentPage, reel.id) {
                            if (isCurrentPage) {
                                feedViewModel.recordViewOnce(reel)
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            VideoPlayerSurface(
                                videoSource = reel.mediaData,
                                thumbnailData = reel.thumbnailData,
                                isPlaying = isCurrentPage && isPlaying,
                                isMuted = isMuted,
                                onTogglePlay = { isPlaying = !isPlaying },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Bottom & right gradient overlay for legibility
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.28f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.78f)
                                            )
                                        )
                                    )
                            )

                            // Top bar overlay
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reels",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { isMuted = !isMuted }) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = if (isMuted) "Unmute" else "Mute",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Right action column
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 12.dp, bottom = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                ReelActionIcon(
                                    icon = Icons.Filled.Favorite,
                                    label = "${reel.likesCount}",
                                    tint = if (isLiked) RoseReaction else Color.White,
                                    onClick = {
                                        feedViewModel.toggleReaction(reel, "love", currentUser.displayName)
                                    }
                                )
                                ReelActionIcon(
                                    icon = Icons.Filled.ChatBubble,
                                    label = "${reel.commentsCount}",
                                    tint = Color.White,
                                    onClick = { feedViewModel.openComments(reel.id) }
                                )
                                ReelActionIcon(
                                    icon = Icons.Filled.Share,
                                    label = "${reel.sharesCount}",
                                    tint = Color.White,
                                    onClick = {
                                        feedViewModel.recordShare(reel.id)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Watch ${reel.authorName}'s Reel on Deepak Social: ${reel.text}")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Reel"))
                                    }
                                )
                                ReelActionIcon(
                                    icon = Icons.Filled.Visibility,
                                    label = "${reel.viewsCount}",
                                    tint = Color.White,
                                    onClick = {}
                                )
                                if (reel.authorId != currentUser.uid) {
                                    IconButton(onClick = { reportReelId = reel.id }) {
                                        Icon(Icons.Filled.Flag, contentDescription = "Report Reel", tint = Color.White)
                                    }
                                }
                            }

                            // Bottom-left creator & caption overlay
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 16.dp, end = 84.dp, bottom = 24.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onOpenUserProfile(reel.authorId) }
                                ) {
                                    UserAvatar(
                                        avatarData = authorProfile?.avatarData ?: reel.authorAvatar,
                                        displayName = reel.authorName,
                                        size = 40.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = reel.authorName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    VerifiedCreatorBadge(authorProfile?.monetizationStatus ?: "none")
                                    if (reel.authorId != currentUser.uid) {
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White.copy(alpha = 0.22f),
                                            modifier = Modifier.clickable {
                                                val target = authorProfile ?: PublicProfile(
                                                    uid = reel.authorId,
                                                    displayName = reel.authorName,
                                                    username = reel.authorUsername
                                                )
                                                socialViewModel.toggleFollow(currentUser, target)
                                            }
                                        ) {
                                            Text(
                                                text = if (isFollowing) "Following" else "Follow",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelMedium,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                                if (reel.text.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = reel.text,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (reel.hashtags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        reel.hashtags.forEach { tag ->
                                            Text(
                                                text = "#$tag",
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                style = MaterialTheme.typography.labelMedium
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

        FloatingActionButton(
            onClick = { showCreateReel = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 58.dp, end = 14.dp)
                .testTag("create_reel_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Upload Reel")
        }
    }
}

@Composable
private fun ReelActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint)
        }
        Text(text = label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CreateReelDialog(
    isPublishing: Boolean,
    onDismiss: () -> Unit,
    onPublish: (caption: String, videoSource: String, thumbnailData: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var caption by remember { mutableStateOf("") }
    var videoUriOrUrl by remember { mutableStateOf("") }
    var thumbData by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                videoUriOrUrl = uri.toString()
                thumbData = MediaUtils.extractVideoThumbnailBase64(context, uri)
                isProcessing = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish Reel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        videoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    enabled = !isProcessing,
                    modifier = Modifier.fillMaxWidth().testTag("pick_reel_video_button")
                ) {
                    Text(if (videoUriOrUrl.isBlank()) "Pick Short Video from Device" else "Change Selected Video")
                }

                OutlinedTextField(
                    value = videoUriOrUrl,
                    onValueChange = { videoUriOrUrl = it },
                    label = { Text("Or paste HTTPS video stream URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reel_video_url_input")
                )

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(500) },
                    label = { Text("Caption & #hashtags") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("reel_caption_input")
                )

                if (isProcessing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Extracting Reel thumbnail...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(caption, videoUriOrUrl.trim(), thumbData) },
                enabled = !isPublishing && !isProcessing && videoUriOrUrl.isNotBlank(),
                modifier = Modifier.testTag("publish_reel_button")
            ) {
                Text(if (isPublishing) "Publishing..." else "Publish Reel")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
