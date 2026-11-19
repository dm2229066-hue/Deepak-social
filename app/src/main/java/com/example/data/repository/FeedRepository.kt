package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.Comment
import com.example.data.model.Post
import com.example.data.model.PublicProfile
import com.example.data.model.UserPostAction
import com.example.util.MediaUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FeedRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observePublicPosts(): Flow<List<Post>> =
        db.collection("posts")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "posts")
                }
                throw error
            }

    fun observeMyPosts(uid: String): Flow<List<Post>> =
        db.collection("posts")
            .whereEqualTo("authorId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "posts")
                }
                throw error
            }

    fun observeUserReactions(uid: String): Flow<Map<String, String>> =
        db.collection("users").document(uid).collection("liked_posts")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserPostAction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .associate { it.postId to (it.reactionType ?: "like") }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "users/$uid/liked_posts")
                }
                throw error
            }

    fun observeSavedPostIds(uid: String): Flow<Set<String>> =
        db.collection("users").document(uid).collection("saved_posts")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserPostAction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .map { it.postId }
                    .toSet()
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "users/$uid/saved_posts")
                }
                throw error
            }

    fun observeComments(postId: String): Flow<List<Comment>> =
        db.collection("posts").document(postId).collection("comments")
            .whereEqualTo("postVisibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Comment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "posts/$postId/comments")
                }
                throw error
            }

    suspend fun createPost(
        author: PublicProfile,
        text: String,
        mediaType: String,
        mediaData: String?,
        thumbnailData: String?,
        visibility: String,
        isReel: Boolean
    ): Result<Unit> {
        var path = "posts"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(2000)
            require(cleanText.isNotBlank() || !mediaData.isNullOrBlank()) {
                "Post must contain text or media"
            }
            val hashtags = MediaUtils.extractHashtags(cleanText)
            val docRef = db.collection("posts").document()
            path = docRef.path
            val payload = mapOf(
                "authorId" to uid,
                "authorName" to author.displayName.ifBlank { "Creator" }.take(60),
                "authorUsername" to author.username.ifBlank { "creator" }.take(40),
                "authorAvatar" to author.avatarData,
                "text" to cleanText,
                "mediaType" to mediaType,
                "mediaData" to mediaData,
                "thumbnailData" to thumbnailData,
                "hashtags" to hashtags,
                "visibility" to if (visibility == "friends") "friends" else "public",
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "sharesCount" to 0L,
                "viewsCount" to 0L,
                "reelPost" to isReel,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            db.collection("public_profiles").document(uid)
                .update(
                    mapOf(
                        "postsCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleReaction(
        post: Post,
        currentReaction: String?,
        newReaction: String,
        actorName: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val likeRef = db.collection("users").document(uid).collection("liked_posts").document(post.id)
        val postRef = db.collection("posts").document(post.id)
        return try {
            if (currentReaction == newReaction) {
                likeRef.delete().await()
                if (post.likesCount > 0L) {
                    postRef.update(
                        mapOf(
                            "likesCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else {
                likeRef.set(
                    mapOf(
                        "postId" to post.id,
                        "reactionType" to newReaction,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                if (currentReaction == null) {
                    postRef.update(
                        mapOf(
                            "likesCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                if (post.authorId != uid) {
                    val notifRef = db.collection("users").document(post.authorId).collection("notifications").document()
                    notifRef.set(
                        mapOf(
                            "userId" to post.authorId,
                            "actorId" to uid,
                            "actorName" to actorName.take(60),
                            "type" to "like",
                            "title" to "New reaction on your ${if (post.reelPost) "Reel" else "post"}",
                            "body" to "$actorName reacted ($newReaction) to your ${if (post.reelPost) "Reel" else "post"}.",
                            "targetId" to post.id,
                            "readStatus" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, postRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleSavePost(postId: String, isCurrentlySaved: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val saveRef = db.collection("users").document(uid).collection("saved_posts").document(postId)
        return try {
            if (isCurrentlySaved) {
                saveRef.delete().await()
            } else {
                saveRef.set(
                    mapOf(
                        "postId" to postId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, saveRef.path)
            Result.failure(e)
        }
    }

    suspend fun addComment(
        post: Post,
        authorName: String,
        authorAvatar: String?,
        text: String,
        parentCommentId: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val commentRef = db.collection("posts").document(post.id).collection("comments").document()
        return try {
            val cleanText = text.trim().take(1000)
            require(cleanText.isNotEmpty()) { "Comment cannot be empty" }
            val payload = mapOf(
                "postId" to post.id,
                "authorId" to uid,
                "authorName" to authorName.take(60),
                "authorAvatar" to authorAvatar,
                "text" to cleanText,
                "parentCommentId" to parentCommentId,
                "postVisibility" to post.visibility,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            commentRef.set(payload).await()
            db.collection("posts").document(post.id)
                .update(
                    mapOf(
                        "commentsCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            if (post.authorId != uid) {
                db.collection("users").document(post.authorId).collection("notifications").document().set(
                    mapOf(
                        "userId" to post.authorId,
                        "actorId" to uid,
                        "actorName" to authorName.take(60),
                        "type" to "comment",
                        "title" to "New comment",
                        "body" to "$authorName commented: ${cleanText.take(80)}",
                        "targetId" to post.id,
                        "readStatus" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            Result.failure(e)
        }
    }

    suspend fun incrementShare(postId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("posts").document(postId)
                .update(
                    mapOf(
                        "sharesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
            Result.failure(e)
        }
    }

    suspend fun recordContentView(post: Post): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        return try {
            db.collection("posts").document(post.id)
                .update(
                    mapOf(
                        "viewsCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            db.collection("public_profiles").document(post.authorId)
                .update(
                    mapOf(
                        "totalViews" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            if (uid != post.authorId) {
                val eventRef = db.collection("monetization_events").document()
                eventRef.set(
                    mapOf(
                        "creatorId" to post.authorId,
                        "viewerId" to uid,
                        "contentId" to post.id,
                        "eventType" to if (post.reelPost) "video_view" else "post_view",
                        "verified" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/${post.id}")
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String, currentPostsCount: Long): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("posts").document(postId).delete().await()
            if (currentPostsCount > 0L) {
                db.collection("public_profiles").document(uid)
                    .update(
                        mapOf(
                            "postsCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "posts/$postId")
            Result.failure(e)
        }
    }

    suspend fun submitReport(targetType: String, targetId: String, reason: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("reports").document()
        return try {
            val cleanReason = reason.trim().take(300)
            require(cleanReason.length >= 3) { "Please provide at least 3 characters for report reason" }
            ref.set(
                mapOf(
                    "reporterId" to uid,
                    "targetType" to targetType,
                    "targetId" to targetId,
                    "reason" to cleanReason,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }
}
