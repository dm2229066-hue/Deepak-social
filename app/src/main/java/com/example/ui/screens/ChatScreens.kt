package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.components.MediaImage
import com.example.ui.components.UserAvatar
import com.example.ui.components.VideoPlayerSurface
import com.example.ui.viewmodel.SocialViewModel
import com.example.util.MediaUtils
import kotlinx.coroutines.launch

@Composable
fun ChatInboxScreen(
    currentUser: PublicProfile,
    blockedUserIds: List<String>,
    socialViewModel: SocialViewModel,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val chatsState by socialViewModel.chatsState.collectAsStateWithLifecycle()
    val profilesState by socialViewModel.activeProfilesState.collectAsStateWithLifecycle()

    val profilesById = remember(profilesState) {
        (profilesState as? UiState.Success)?.data?.associateBy { it.uid } ?: emptyMap()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
        ) {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_inbox_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("Messages", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }

            when (val state = chatsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Success -> {
                    val activeChats = state.data.filter { thread ->
                        val otherUid = thread.participantUids.firstOrNull { it != currentUser.uid }
                        otherUid != null && otherUid !in blockedUserIds
                    }
                    val otherUsers = profilesById.values.filter {
                        it.uid != currentUser.uid && it.uid !in blockedUserIds
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (activeChats.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Conversations",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(activeChats, key = { it.id }) { thread ->
                                val otherUid = thread.participantUids.firstOrNull { it != currentUser.uid } ?: ""
                                val otherProfile = profilesById[otherUid]
                                val otherName = otherProfile?.displayName
                                    ?: if (thread.user1Uid == otherUid) thread.user1Name else thread.user2Name
                                val otherAvatar = otherProfile?.avatarData
                                    ?: if (thread.user1Uid == otherUid) thread.user1Avatar else thread.user2Avatar

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            socialViewModel.setActiveChat(thread.id)
                                            onOpenChat(thread.id)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        UserAvatar(
                                            avatarData = otherAvatar,
                                            displayName = otherName,
                                            size = 48.dp,
                                            showOnline = otherProfile?.online == true
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(otherName, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = thread.lastMessage.ifBlank { "Tap to start chatting" },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            text = MediaUtils.formatRelativeTime(thread.lastMessageAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Start New Chat",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (otherUsers.isEmpty()) {
                            item {
                                Text(
                                    text = "No other users active yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(otherUsers, key = { "new_${it.uid}" }) { user ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            socialViewModel.openChatWith(currentUser, user, onOpenChat)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        UserAvatar(user.avatarData, user.displayName, 42.dp, user.online)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(user.displayName, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = if (user.online) "Online now" else "@${user.username}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text("Chat", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
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
fun ChatConversationScreen(
    chatId: String,
    currentUser: PublicProfile,
    socialViewModel: SocialViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(chatId) {
        socialViewModel.setActiveChat(chatId)
    }

    val chatsState by socialViewModel.chatsState.collectAsStateWithLifecycle()
    val messagesState by socialViewModel.messagesState.collectAsStateWithLifecycle()
    val profilesState by socialViewModel.activeProfilesState.collectAsStateWithLifecycle()

    val chatThread = remember(chatsState, chatId) {
        (chatsState as? UiState.Success)?.data?.firstOrNull { it.id == chatId }
    }
    val otherUid = chatThread?.participantUids?.firstOrNull { it != currentUser.uid }
    val otherProfile = remember(profilesState, otherUid) {
        (profilesState as? UiState.Success)?.data?.firstOrNull { it.uid == otherUid }
    }
    val otherName: String = otherProfile?.displayName
        ?: (if (chatThread?.user1Uid == otherUid) chatThread?.user1Name else chatThread?.user2Name)
        ?: "Chat"

    var messageText by remember { mutableStateOf("") }
    var isSendingMedia by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null && chatThread != null) {
            scope.launch {
                isSendingMedia = true
                val b64 = MediaUtils.uriToCompressedBase64(context, uri)
                if (b64 != null) {
                    socialViewModel.sendMessage(chatThread, currentUser.displayName, "", "image", b64)
                }
                isSendingMedia = false
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null && chatThread != null) {
            scope.launch {
                isSendingMedia = true
                socialViewModel.sendMessage(chatThread, currentUser.displayName, "", "video", uri.toString())
                isSendingMedia = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
        ) {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_thread_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    UserAvatar(
                        avatarData = otherProfile?.avatarData,
                        displayName = otherName,
                        size = 40.dp,
                        showOnline = otherProfile?.online == true
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(otherName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (otherProfile?.online == true) "Active now" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            when (val state = messagesState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.data.isEmpty()) {
                            item {
                                Text(
                                    text = "Say hello to $otherName!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(state.data, key = { it.id }) { msg ->
                                ChatMessageBubble(msg = msg, isMine = msg.senderId == currentUser.uid)
                            }
                        }
                    }
                }
            }

            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !isSendingMedia && chatThread != null
                    ) {
                        Icon(Icons.Filled.Photo, contentDescription = "Send photo", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = {
                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        enabled = !isSendingMedia && chatThread != null
                    ) {
                        Icon(Icons.Filled.Videocam, contentDescription = "Send video", tint = MaterialTheme.colorScheme.secondary)
                    }
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it.take(2000) },
                        placeholder = { Text("Message...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("chat_message_input")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            val thread = chatThread
                            if (thread != null && messageText.isNotBlank()) {
                                socialViewModel.sendMessage(thread, currentUser.displayName, messageText)
                                messageText = ""
                            }
                        },
                        enabled = chatThread != null && messageText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(msg: ChatMessage, isMine: Boolean) {
    var playVideo by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (msg.mediaType == "image" && !msg.mediaData.isNullOrBlank()) {
                    MediaImage(
                        mediaData = msg.mediaData,
                        contentDescription = "Shared photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    if (msg.text.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
                } else if (msg.mediaType == "video" && !msg.mediaData.isNullOrBlank()) {
                    VideoPlayerSurface(
                        videoSource = msg.mediaData,
                        thumbnailData = null,
                        isPlaying = playVideo,
                        isMuted = false,
                        onTogglePlay = { playVideo = !playVideo },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    if (msg.text.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
                }

                if (msg.text.isNotBlank()) {
                    Text(
                        text = msg.text,
                        color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = MediaUtils.formatRelativeTime(msg.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMine) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
