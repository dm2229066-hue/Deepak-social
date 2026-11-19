package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FriendRequest
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerifiedCreatorBadge
import com.example.ui.viewmodel.SocialViewModel

@Composable
fun FriendsScreen(
    currentUser: PublicProfile,
    blockedUserIds: List<String>,
    socialViewModel: SocialViewModel,
    onOpenChat: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit
) {
    val profilesState by socialViewModel.activeProfilesState.collectAsStateWithLifecycle()
    val requests by socialViewModel.friendRequests.collectAsStateWithLifecycle()
    val followingIds by socialViewModel.followingIds.collectAsStateWithLifecycle()
    val followerIds by socialViewModel.followerIds.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }

    val acceptedRequests = remember(requests) { requests.filter { it.status == "accepted" } }
    val incomingPending = remember(requests, currentUser.uid) {
        requests.filter { it.status == "pending" && it.toUid == currentUser.uid }
    }
    val sentPending = remember(requests, currentUser.uid) {
        requests.filter { it.status == "pending" && it.fromUid == currentUser.uid }
    }

    val friendUidSet = remember(acceptedRequests, currentUser.uid) {
        acceptedRequests.map { if (it.fromUid == currentUser.uid) it.toUid else it.fromUid }.toSet()
    }
    val requestByOtherUid = remember(requests, currentUser.uid) {
        requests.associateBy { if (it.fromUid == currentUser.uid) it.toUid else it.fromUid }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
        ) {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text(
                        text = "Friends & Creators",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search people by name or @username") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("friends_search_input")
                    )
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Discover") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Requests (${incomingPending.size})") }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Friends (${friendUidSet.size})") }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("Follow (${followingIds.size})") }
                        )
                    }
                }
            }

            when (val state = profilesState) {
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
                    val query = search.trim().lowercase()
                    val allOthers = state.data
                        .filter { it.uid != currentUser.uid && it.uid !in blockedUserIds }
                        .filter {
                            query.isBlank() ||
                                it.displayName.lowercase().contains(query) ||
                                it.username.lowercase().contains(query)
                        }

                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (selectedTab) {
                            0 -> {
                                if (allOthers.isEmpty()) {
                                    item {
                                        EmptyPeopleMessage("No other active users found yet. Invite friends with your referral code!")
                                    }
                                } else {
                                    items(allOthers, key = { it.uid }) { person ->
                                        val existingReq = requestByOtherUid[person.uid]
                                        PersonDiscoveryCard(
                                            person = person,
                                            existingRequest = existingReq,
                                            isFollowing = person.uid in followingIds,
                                            currentUid = currentUser.uid,
                                            onProfileClick = { onOpenUserProfile(person.uid) },
                                            onAddFriend = { socialViewModel.sendFriendRequest(currentUser, person) },
                                            onAcceptFriend = {
                                                existingReq?.let {
                                                    socialViewModel.respondFriendRequest(it, true, currentUser.displayName)
                                                }
                                            },
                                            onToggleFollow = { socialViewModel.toggleFollow(currentUser, person) },
                                            onMessage = {
                                                socialViewModel.openChatWith(currentUser, person, onOpenChat)
                                            },
                                            onBlock = { socialViewModel.toggleBlockUser(person.uid, true) }
                                        )
                                    }
                                }
                            }
                            1 -> {
                                if (incomingPending.isEmpty() && sentPending.isEmpty()) {
                                    item { EmptyPeopleMessage("No pending friend requests right now.") }
                                } else {
                                    items(incomingPending, key = { it.id }) { req ->
                                        IncomingRequestCard(
                                            request = req,
                                            onAccept = {
                                                socialViewModel.respondFriendRequest(req, true, currentUser.displayName)
                                            },
                                            onDecline = {
                                                socialViewModel.respondFriendRequest(req, false, currentUser.displayName)
                                            }
                                        )
                                    }
                                    items(sentPending, key = { it.id }) { req ->
                                        SentRequestCard(
                                            request = req,
                                            onCancel = { socialViewModel.removeFriendship(req) }
                                        )
                                    }
                                }
                            }
                            2 -> {
                                val friends = allOthers.filter { it.uid in friendUidSet }
                                if (friends.isEmpty()) {
                                    item { EmptyPeopleMessage("You haven't added friends yet. Check Discover or Requests!") }
                                } else {
                                    items(friends, key = { it.uid }) { friend ->
                                        val req = requestByOtherUid[friend.uid]
                                        FriendRowCard(
                                            person = friend,
                                            onProfileClick = { onOpenUserProfile(friend.uid) },
                                            onChat = {
                                                socialViewModel.openChatWith(currentUser, friend, onOpenChat)
                                            },
                                            onUnfriend = {
                                                req?.let { socialViewModel.removeFriendship(it) }
                                            },
                                            onBlock = { socialViewModel.toggleBlockUser(friend.uid, true) }
                                        )
                                    }
                                }
                            }
                            3 -> {
                                val followed = allOthers.filter { it.uid in followingIds || it.uid in followerIds }
                                if (followed.isEmpty()) {
                                    item { EmptyPeopleMessage("Follow creators to see their posts & Reels first!") }
                                } else {
                                    items(followed, key = { it.uid }) { person ->
                                        val isFollowing = person.uid in followingIds
                                        val isFollower = person.uid in followerIds
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onOpenUserProfile(person.uid) }
                                                    .padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                UserAvatar(person.avatarData, person.displayName, 48.dp, person.online)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(person.displayName, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        VerifiedCreatorBadge(person.monetizationStatus)
                                                    }
                                                    Text(
                                                        text = buildString {
                                                            append("@${person.username}")
                                                            if (isFollower) append(" • Follows you")
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                OutlinedButton(onClick = { socialViewModel.toggleFollow(currentUser, person) }) {
                                                    Text(if (isFollowing) "Unfollow" else "Follow Back")
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
        }
    }
}

@Composable
private fun PersonDiscoveryCard(
    person: PublicProfile,
    existingRequest: FriendRequest?,
    isFollowing: Boolean,
    currentUid: String,
    onProfileClick: () -> Unit,
    onAddFriend: () -> Unit,
    onAcceptFriend: () -> Unit,
    onToggleFollow: () -> Unit,
    onMessage: () -> Unit,
    onBlock: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onProfileClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(person.avatarData, person.displayName, 50.dp, person.online)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(person.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        VerifiedCreatorBadge(person.monetizationStatus)
                    }
                    Text(
                        text = "@${person.username} • ${person.followersCount} followers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (person.bio.isNotBlank()) {
                        Text(
                            text = person.bio,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
                IconButton(onClick = onBlock) {
                    Icon(
                        imageVector = Icons.Filled.Block,
                        contentDescription = "Block user",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    existingRequest == null || existingRequest.status == "declined" -> {
                        Button(
                            onClick = onAddFriend,
                            modifier = Modifier.weight(1f).testTag("add_friend_${person.uid}")
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Friend")
                        }
                    }
                    existingRequest.status == "pending" && existingRequest.toUid == currentUid -> {
                        Button(onClick = onAcceptFriend, modifier = Modifier.weight(1f)) {
                            Text("Accept Request")
                        }
                    }
                    existingRequest.status == "pending" -> {
                        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) {
                            Text("Requested")
                        }
                    }
                    else -> {
                        Button(onClick = onMessage, modifier = Modifier.weight(1f)) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Message")
                        }
                    }
                }
                OutlinedButton(
                    onClick = onToggleFollow,
                    modifier = Modifier.weight(1f).testTag("follow_person_${person.uid}")
                ) {
                    Text(if (isFollowing) "Following" else "Follow")
                }
                IconButton(onClick = onMessage) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat")
                }
            }
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(request.fromAvatar, request.fromName, 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(request.fromName, fontWeight = FontWeight.Bold)
                Text("Wants to be friends", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onAccept) {
                Icon(Icons.Filled.Check, contentDescription = "Accept", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDecline) {
                Icon(Icons.Filled.Close, contentDescription = "Decline", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SentRequestCard(
    request: FriendRequest,
    onCancel: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(request.toAvatar, request.toName, 44.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(request.toName, fontWeight = FontWeight.SemiBold)
                Text("Pending request sent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun FriendRowCard(
    person: PublicProfile,
    onProfileClick: () -> Unit,
    onChat: () -> Unit,
    onUnfriend: () -> Unit,
    onBlock: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onProfileClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(person.avatarData, person.displayName, 48.dp, person.online)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(person.displayName, fontWeight = FontWeight.Bold)
                Text(
                    text = if (person.online) "Online now" else "@${person.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onChat) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message", tint = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = onUnfriend) {
                Text("Unfriend")
            }
            IconButton(onClick = onBlock) {
                Icon(Icons.Filled.Block, contentDescription = "Block", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EmptyPeopleMessage(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp)
        )
    }
}
