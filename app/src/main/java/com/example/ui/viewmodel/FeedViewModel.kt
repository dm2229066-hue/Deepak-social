package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Comment
import com.example.data.model.Post
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.data.repository.FeedRepository
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

enum class FeedFilter {
    FOR_YOU, FOLLOWING, SAVED
}

class FeedViewModel(
    private val feedRepository: FeedRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private val _feedFilter = MutableStateFlow(FeedFilter.FOR_YOU)
    val feedFilter = _feedFilter.asStateFlow()

    private val _activeHashtag = MutableStateFlow<String?>(null)
    val activeHashtag = _activeHashtag.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _visibleLimit = MutableStateFlow(12)
    val visibleLimit = _visibleLimit.asStateFlow()

    private val _selectedPostIdForComments = MutableStateFlow<String?>(null)
    val selectedPostIdForComments = _selectedPostIdForComments.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing = _isPublishing.asStateFlow()

    val publicPostsState: StateFlow<UiState<List<Post>>> = feedRepository.observePublicPosts()
        .map<List<Post>, UiState<List<Post>>> { posts ->
            UiState.Success(posts.sortedByDescending { it.createdAt ?: Timestamp(0, 0) })
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load feed"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myPostsState: StateFlow<UiState<List<Post>>> = feedRepository.observeMyPosts(currentUserId)
        .map<List<Post>, UiState<List<Post>>> { posts ->
            UiState.Success(posts.sortedByDescending { it.createdAt ?: Timestamp(0, 0) })
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load your posts"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val userReactions: StateFlow<Map<String, String>> = feedRepository.observeUserReactions(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyMap())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val savedPostIds: StateFlow<Set<String>> = feedRepository.observeSavedPostIds(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptySet())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val commentsState: StateFlow<UiState<List<Comment>>> = _selectedPostIdForComments
        .flatMapLatest { postId ->
            if (postId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                feedRepository.observeComments(postId)
                    .map<List<Comment>, UiState<List<Comment>>> { comments ->
                        UiState.Success(comments.sortedBy { it.createdAt ?: Timestamp(0, 0) })
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load comments"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    fun setFeedFilter(filter: FeedFilter) {
        _feedFilter.value = filter
    }

    fun setActiveHashtag(tag: String?) {
        _activeHashtag.value = tag?.lowercase()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadMorePosts() {
        _visibleLimit.value += 10
    }

    fun openComments(postId: String?) {
        _selectedPostIdForComments.value = postId
    }

    fun createPost(
        author: PublicProfile,
        text: String,
        mediaType: String,
        mediaData: String?,
        thumbnailData: String?,
        visibility: String,
        isReel: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isPublishing.value = true
            val result = feedRepository.createPost(
                author = author,
                text = text,
                mediaType = mediaType,
                mediaData = mediaData,
                thumbnailData = thumbnailData,
                visibility = visibility,
                isReel = isReel
            )
            _isPublishing.value = false
            result.onSuccess {
                _events.tryEmit(if (isReel) "Reel published!" else "Post shared!")
                onSuccess()
            }.onFailure {
                _events.tryEmit(it.localizedMessage ?: "Failed to publish")
            }
        }
    }

    fun toggleReaction(post: Post, newReaction: String, actorName: String) {
        viewModelScope.launch {
            val current = userReactions.value[post.id]
            feedRepository.toggleReaction(post, current, newReaction, actorName)
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Reaction failed") }
        }
    }

    fun toggleSave(postId: String) {
        viewModelScope.launch {
            val currentlySaved = postId in savedPostIds.value
            feedRepository.toggleSavePost(postId, currentlySaved)
                .onSuccess {
                    _events.tryEmit(if (currentlySaved) "Removed from saved" else "Post saved")
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to save post") }
        }
    }

    fun addComment(post: Post, author: PublicProfile, text: String, parentCommentId: String? = null) {
        viewModelScope.launch {
            feedRepository.addComment(
                post = post,
                authorName = author.displayName.ifBlank { "Creator" },
                authorAvatar = author.avatarData,
                text = text,
                parentCommentId = parentCommentId
            ).onFailure {
                _events.tryEmit(it.localizedMessage ?: "Failed to post comment")
            }
        }
    }

    fun recordShare(postId: String) {
        viewModelScope.launch {
            feedRepository.incrementShare(postId)
        }
    }

    private val recordedViews = mutableSetOf<String>()

    fun recordViewOnce(post: Post) {
        if (!recordedViews.add(post.id)) return
        viewModelScope.launch {
            feedRepository.recordContentView(post)
        }
    }

    fun deletePost(post: Post, currentPostsCount: Long) {
        viewModelScope.launch {
            feedRepository.deletePost(post.id, currentPostsCount)
                .onSuccess { _events.tryEmit("Deleted successfully") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Delete failed") }
        }
    }

    fun submitReport(targetType: String, targetId: String, reason: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            feedRepository.submitReport(targetType, targetId, reason)
                .onSuccess {
                    _events.tryEmit("Report submitted for admin review")
                    onDone()
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Report failed") }
        }
    }
}
