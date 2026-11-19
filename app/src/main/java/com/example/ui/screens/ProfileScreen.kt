package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.components.MediaImage
import com.example.ui.components.ReportDialog
import com.example.ui.components.StatPill
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerifiedCreatorBadge
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldCreator
import com.example.ui.viewmodel.FeedViewModel
import com.example.ui.viewmodel.SocialViewModel
import com.example.util.MediaUtils
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    currentUser: PublicProfile,
    viewedUid: String?,
    isAdmin: Boolean,
    feedViewModel: FeedViewModel,
    socialViewModel: SocialViewModel,
    onBack: (() -> Unit)?,
    onOpenCreatorDashboard: () -> Unit,
    onOpenReferrals: () -> Unit,
    onOpenChatInbox: () -> Unit,
    onOpenDirectChat: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdminDashboard: () -> Unit
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    val activeProfilesState by socialViewModel.activeProfilesState.collectAsStateWithLifecycle()
    val postsState by feedViewModel.publicPostsState.collectAsStateWithLifecycle()
    val myPostsState by feedViewModel.myPostsState.collectAsStateWithLifecycle()
    val followingIds by socialViewModel.followingIds.collectAsStateWithLifecycle()

    val isOwnProfile = viewedUid == null || viewedUid == currentUser.uid
    val profile = remember(viewedUid, currentUser, activeProfilesState) {
        if (isOwnProfile) {
            currentUser
        } else {
            (activeProfilesState as? UiState.Success)?.data?.firstOrNull { it.uid == viewedUid } ?: currentUser
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    if (showEditDialog && isOwnProfile) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditDialog = false },
            onSave = { name, username, bio, avatar, cover ->
                socialViewModel.updateProfile(name, username, bio, avatar, cover) {
                    showEditDialog = false
                }
            }
        )
    }

    if (showReportDialog && !isOwnProfile) {
        ReportDialog(
            targetType = "user",
            onDismiss = { showReportDialog = false },
            onSubmit = { reason ->
                feedViewModel.submitReport("user", profile.uid, reason) {
                    showReportDialog = false
                }
            }
        )
    }

    val userPosts = remember(isOwnProfile, postsState, myPostsState, profile.uid) {
        val list = if (isOwnProfile) {
            (myPostsState as? UiState.Success)?.data.orEmpty()
        } else {
            (postsState as? UiState.Success)?.data?.filter { it.authorId == profile.uid }.orEmpty()
        }
        list
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "profile_header") {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Cover photo + overlapping avatar
                        Box(modifier = Modifier.fillMaxWidth().height(195.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    )
                            ) {
                                if (!profile.coverData.isNullOrBlank()) {
                                    MediaImage(
                                        mediaData = profile.coverData,
                                        contentDescription = "Cover photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            if (onBack != null) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            }

                            UserAvatar(
                                avatarData = profile.avatarData,
                                displayName = profile.displayName,
                                size = 94.dp,
                                showOnline = profile.online,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 16.dp)
                                    .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }

                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = profile.displayName,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        VerifiedCreatorBadge(profile.monetizationStatus)
                                    }
                                    Text(
                                        text = "@${profile.username}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isOwnProfile) {
                                    OutlinedButton(
                                        onClick = { showEditDialog = true },
                                        modifier = Modifier.testTag("edit_profile_button")
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Edit")
                                    }
                                }
                            }

                            if (profile.bio.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = profile.bio, style = MaterialTheme.typography.bodyMedium)
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatPill("Friends", "${profile.friendsCount}", Modifier.weight(1f))
                                StatPill("Followers", "${profile.followersCount}", Modifier.weight(1f))
                                StatPill("Posts", "${profile.postsCount}", Modifier.weight(1f))
                                StatPill("Views", "${profile.totalViews}", Modifier.weight(1f))
                            }

                            if (!isOwnProfile) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val isFollowing = profile.uid in followingIds
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { socialViewModel.toggleFollow(currentUser, profile) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isFollowing) "Following" else "Follow")
                                    }
                                    OutlinedButton(
                                        onClick = { socialViewModel.openChatWith(currentUser, profile, onOpenDirectChat) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Message")
                                    }
                                    TextButton(onClick = { showReportDialog = true }) {
                                        Text("Report", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isOwnProfile) {
                item(key = "creator_hub_actions") {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileHubTile(
                            icon = Icons.Filled.MonetizationOn,
                            tint = EmeraldSuccess,
                            title = "Creator Earning Dashboard",
                            subtitle = "Track ad, video, post & referral revenue, eligibility, and UPI/Bank withdrawals",
                            testTag = "open_creator_dashboard_button",
                            onClick = onOpenCreatorDashboard
                        )
                        ProfileHubTile(
                            icon = Icons.Filled.CardGiftcard,
                            tint = GoldCreator,
                            title = "Referral & Rewards (${profile.referralCode})",
                            subtitle = "Invite friends, track referrals, and earn verified rewards",
                            testTag = "open_referrals_button",
                            onClick = onOpenReferrals
                        )
                        ProfileHubTile(
                            icon = Icons.AutoMirrored.Filled.Chat,
                            tint = MaterialTheme.colorScheme.primary,
                            title = "1-to-1 Messages",
                            subtitle = "Real-time private chat with photo & video sharing",
                            testTag = "open_chat_inbox_button",
                            onClick = onOpenChatInbox
                        )
                        ProfileHubTile(
                            icon = Icons.Filled.Settings,
                            tint = MaterialTheme.colorScheme.secondary,
                            title = "Privacy, Theme & Security",
                            subtitle = "Dark mode, blocked users, Terms & Policy, Sign Out & Account Deletion",
                            testTag = "open_settings_button",
                            onClick = onOpenSettings
                        )
                        if (isAdmin) {
                            ProfileHubTile(
                                icon = Icons.Filled.AdminPanelSettings,
                                tint = MaterialTheme.colorScheme.error,
                                title = "Admin Control Center",
                                subtitle = "Manage users, content, reports, monetization, payouts, ads & announcements",
                                testTag = "open_admin_dashboard_button",
                                onClick = onOpenAdminDashboard
                            )
                        }
                    }
                }
            }

            item(key = "posts_section_header") {
                Text(
                    text = if (isOwnProfile) "Your Posts (${userPosts.size})" else "Posts (${userPosts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (userPosts.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = "No posts published yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(userPosts, key = { it.id }) { post ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (post.reelPost) "Reel • ${post.visibility}" else "Post • ${post.visibility}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = MediaUtils.formatRelativeTime(post.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (post.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(post.text, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (post.mediaType == "image" && !post.mediaData.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                MediaImage(
                                    mediaData = post.mediaData,
                                    contentDescription = "Post image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${post.likesCount} likes • ${post.commentsCount} comments • ${post.viewsCount} views",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isOwnProfile) {
                                    TextButton(onClick = { feedViewModel.deletePost(post, currentUser.postsCount) }) {
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    }
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
private fun ProfileHubTile(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: PublicProfile,
    onDismiss: () -> Unit,
    onSave: (name: String, username: String, bio: String, avatar: String?, cover: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(profile.displayName) }
    var username by remember { mutableStateOf(profile.username) }
    var bio by remember { mutableStateOf(profile.bio) }
    var avatarData by remember { mutableStateOf<String?>(null) }
    var coverData by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                avatarData = MediaUtils.uriToCompressedBase64(context, uri, maxDimension = 420, quality = 72)
                isProcessing = false
            }
        }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                coverData = MediaUtils.uriToCompressedBase64(context, uri, maxDimension = 900, quality = 70)
                isProcessing = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_name_input")
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.take(40) },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_username_input")
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(300) },
                    label = { Text("Bio") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("edit_bio_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (avatarData == null) "Avatar Photo" else "Avatar Ready")
                    }
                    OutlinedButton(
                        onClick = {
                            coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (coverData == null) "Cover Photo" else "Cover Ready")
                    }
                }
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, username, bio, avatarData, coverData) },
                enabled = !isProcessing && name.isNotBlank() && username.trim().length >= 2,
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
