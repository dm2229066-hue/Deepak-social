package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.FriendRequest
import com.example.data.model.PrivateUser
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.data.model.UserNotification
import com.example.data.repository.SocialRepository
import com.example.data.repository.UserRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SocialViewModel(
    private val userRepository: UserRepository,
    private val socialRepository: SocialRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId = _activeChatId.asStateFlow()

    val myProfileState: StateFlow<UiState<PublicProfile?>> = userRepository.observeMyProfile(currentUserId)
        .map<PublicProfile?, UiState<PublicProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val privateUserState: StateFlow<PrivateUser?> = userRepository.observePrivateUser(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val activeProfilesState: StateFlow<UiState<List<PublicProfile>>> = userRepository.observeActiveProfiles()
        .map<List<PublicProfile>, UiState<List<PublicProfile>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load people"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val friendRequests: StateFlow<List<FriendRequest>> = socialRepository.observeFriendRequests(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val followingIds: StateFlow<Set<String>> = socialRepository.observeFollowing(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptySet())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val followerIds: StateFlow<Set<String>> = socialRepository.observeFollowers(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptySet())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val chatsState: StateFlow<UiState<List<ChatThread>>> = socialRepository.observeChats(currentUserId)
        .map<List<ChatThread>, UiState<List<ChatThread>>> { list ->
            UiState.Success(list.sortedByDescending { it.lastMessageAt ?: Timestamp(0, 0) })
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load chats"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val messagesState: StateFlow<UiState<List<ChatMessage>>> = _activeChatId
        .flatMapLatest { chatId ->
            if (chatId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                socialRepository.observeMessages(chatId, currentUserId)
                    .map<List<ChatMessage>, UiState<List<ChatMessage>>> { msgs ->
                        UiState.Success(msgs.sortedBy { it.createdAt ?: Timestamp(0, 0) })
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load messages"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    val notificationsState: StateFlow<UiState<List<UserNotification>>> = socialRepository.observeNotifications(currentUserId)
        .map<List<UserNotification>, UiState<List<UserNotification>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) })
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load notifications"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun setActiveChat(chatId: String?) {
        _activeChatId.value = chatId
    }

    fun sendFriendRequest(me: PublicProfile, target: PublicProfile) {
        viewModelScope.launch {
            socialRepository.sendFriendRequest(me, target)
                .onSuccess { _events.tryEmit("Friend request sent to ${target.displayName}") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Could not send request") }
        }
    }

    fun respondFriendRequest(request: FriendRequest, accept: Boolean, responderName: String) {
        viewModelScope.launch {
            socialRepository.respondFriendRequest(request, accept, responderName)
                .onSuccess { _events.tryEmit(if (accept) "Friend request accepted!" else "Request declined") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to update request") }
        }
    }

    fun removeFriendship(request: FriendRequest) {
        viewModelScope.launch {
            socialRepository.removeFriendship(request)
                .onSuccess { _events.tryEmit("Connection removed") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Action failed") }
        }
    }

    fun toggleFollow(me: PublicProfile, target: PublicProfile) {
        viewModelScope.launch {
            val isFollowing = target.uid in followingIds.value
            socialRepository.toggleFollow(me, target, isFollowing)
                .onSuccess {
                    _events.tryEmit(if (isFollowing) "Unfollowed ${target.displayName}" else "Following ${target.displayName}")
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Follow action failed") }
        }
    }

    fun openChatWith(me: PublicProfile, other: PublicProfile, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            socialRepository.openOrCreateChat(me, other)
                .onSuccess { chatId ->
                    _activeChatId.value = chatId
                    onChatReady(chatId)
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Unable to open chat") }
        }
    }

    fun sendMessage(chat: ChatThread, senderName: String, text: String, mediaType: String = "none", mediaData: String? = null) {
        viewModelScope.launch {
            socialRepository.sendMessage(chat, senderName, text, mediaType, mediaData)
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Message failed to send") }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            socialRepository.markNotificationRead(currentUserId, id)
        }
    }

    fun markAllNotificationsRead(unreadIds: List<String>) {
        viewModelScope.launch {
            socialRepository.markAllNotificationsRead(currentUserId, unreadIds)
                .onSuccess { _events.tryEmit("All notifications marked read") }
        }
    }

    fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        avatarData: String?,
        coverData: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            userRepository.updateProfile(displayName, username, bio, avatarData, coverData)
                .onSuccess {
                    _events.tryEmit("Profile updated")
                    onDone()
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Update failed") }
        }
    }

    fun updatePrivacyMode(mode: String) {
        viewModelScope.launch {
            userRepository.updatePrivacyMode(mode)
                .onSuccess { _events.tryEmit("Privacy updated to $mode") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to update privacy") }
        }
    }

    fun updatePayoutDetails(upiId: String, bankAccount: String, ifsc: String, holderName: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            userRepository.updatePayoutDetails(upiId, bankAccount, ifsc, holderName)
                .onSuccess {
                    _events.tryEmit("Payout details saved")
                    onDone()
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to save payout details") }
        }
    }

    fun toggleBlockUser(targetUid: String, block: Boolean) {
        viewModelScope.launch {
            val currentBlocked = privateUserState.value?.blockedUserIds ?: emptyList()
            userRepository.toggleBlockUser(targetUid, currentBlocked, block)
                .onSuccess { _events.tryEmit(if (block) "User blocked" else "User unblocked") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Action failed") }
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            userRepository.deleteAccount()
                .onSuccess {
                    _events.tryEmit("Account deleted")
                    onDeleted()
                }
                .onFailure {
                    _events.tryEmit(it.localizedMessage ?: "Re-sign in recently to delete account")
                }
        }
    }
}
