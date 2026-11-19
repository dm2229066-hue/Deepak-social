package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.Follow
import com.example.data.model.FriendRequest
import com.example.data.model.PublicProfile
import com.example.data.model.UserNotification
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

class SocialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun pairId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    fun observeFriendRequests(uid: String): Flow<List<FriendRequest>> =
        db.collection("friend_requests")
            .whereArrayContains("participantUids", uid)
            .snapshots()
            .map { it.toObjects(FriendRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "friend_requests")
                }
                throw error
            }

    fun observeFollowing(uid: String): Flow<Set<String>> =
        db.collection("follows")
            .whereEqualTo("followerId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Follow::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .map { it.targetId }
                    .toSet()
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "follows")
                }
                throw error
            }

    fun observeFollowers(uid: String): Flow<Set<String>> =
        db.collection("follows")
            .whereEqualTo("targetId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Follow::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .map { it.followerId }
                    .toSet()
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "follows")
                }
                throw error
            }

    fun observeChats(uid: String): Flow<List<ChatThread>> =
        db.collection("chats")
            .whereArrayContains("participantUids", uid)
            .snapshots()
            .map { it.toObjects(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "chats")
                }
                throw error
            }

    fun observeMessages(chatId: String, uid: String): Flow<List<ChatMessage>> =
        db.collection("chats").document(chatId).collection("messages")
            .whereArrayContains("participantUids", uid)
            .snapshots()
            .map { it.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "chats/$chatId/messages")
                }
                throw error
            }

    fun observeNotifications(uid: String): Flow<List<UserNotification>> =
        db.collection("users").document(uid).collection("notifications")
            .snapshots()
            .map { it.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "users/$uid/notifications")
                }
                throw error
            }

    suspend fun sendFriendRequest(me: PublicProfile, target: PublicProfile): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val reqId = pairId(uid, target.uid)
        val reqRef = db.collection("friend_requests").document(reqId)
        return try {
            val payload = mapOf(
                "fromUid" to uid,
                "toUid" to target.uid,
                "fromName" to me.displayName.ifBlank { "User" }.take(60),
                "fromAvatar" to me.avatarData,
                "toName" to target.displayName.ifBlank { "User" }.take(60),
                "toAvatar" to target.avatarData,
                "participantUids" to listOf(uid, target.uid),
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            reqRef.set(payload).await()
            db.collection("users").document(target.uid).collection("notifications").document().set(
                mapOf(
                    "userId" to target.uid,
                    "actorId" to uid,
                    "actorName" to me.displayName.ifBlank { "User" }.take(60),
                    "type" to "friend_request",
                    "title" to "New Friend Request",
                    "body" to "${me.displayName} sent you a friend request.",
                    "targetId" to reqId,
                    "readStatus" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun respondFriendRequest(
        request: FriendRequest,
        accept: Boolean,
        responderName: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val reqRef = db.collection("friend_requests").document(request.id)
        return try {
            val newStatus = if (accept) "accepted" else "declined"
            reqRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (accept) {
                db.collection("public_profiles").document(request.fromUid)
                    .update(mapOf("friendsCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp()))
                    .await()
                db.collection("public_profiles").document(request.toUid)
                    .update(mapOf("friendsCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp()))
                    .await()
                val otherUid = if (request.fromUid == uid) request.toUid else request.fromUid
                db.collection("users").document(otherUid).collection("notifications").document().set(
                    mapOf(
                        "userId" to otherUid,
                        "actorId" to uid,
                        "actorName" to responderName.take(60),
                        "type" to "friend_accept",
                        "title" to "Friend Request Accepted",
                        "body" to "$responderName accepted your friend request.",
                        "targetId" to request.id,
                        "readStatus" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeFriendship(request: FriendRequest): Result<Unit> {
        val reqRef = db.collection("friend_requests").document(request.id)
        return try {
            val wasAccepted = request.status == "accepted"
            reqRef.delete().await()
            if (wasAccepted) {
                db.collection("public_profiles").document(request.fromUid)
                    .update(mapOf("friendsCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp()))
                    .await()
                db.collection("public_profiles").document(request.toUid)
                    .update(mapOf("friendsCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp()))
                    .await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleFollow(
        me: PublicProfile,
        target: PublicProfile,
        isFollowing: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val followId = "${uid}_${target.uid}"
        val followRef = db.collection("follows").document(followId)
        return try {
            if (isFollowing) {
                followRef.delete().await()
                if (me.followingCount > 0L) {
                    db.collection("public_profiles").document(uid)
                        .update(mapOf("followingCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp()))
                        .await()
                }
                if (target.followersCount > 0L) {
                    db.collection("public_profiles").document(target.uid)
                        .update(mapOf("followersCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp()))
                        .await()
                }
            } else {
                followRef.set(
                    mapOf(
                        "followerId" to uid,
                        "targetId" to target.uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                db.collection("public_profiles").document(uid)
                    .update(mapOf("followingCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp()))
                    .await()
                db.collection("public_profiles").document(target.uid)
                    .update(mapOf("followersCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp()))
                    .await()
                db.collection("users").document(target.uid).collection("notifications").document().set(
                    mapOf(
                        "userId" to target.uid,
                        "actorId" to uid,
                        "actorName" to me.displayName.ifBlank { "Creator" }.take(60),
                        "type" to "follow",
                        "title" to "New Follower",
                        "body" to "${me.displayName} started following you.",
                        "targetId" to uid,
                        "readStatus" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, followRef.path)
            Result.failure(e)
        }
    }

    suspend fun openOrCreateChat(me: PublicProfile, other: PublicProfile): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val chatId = pairId(uid, other.uid)
        val chatRef = db.collection("chats").document(chatId)
        return try {
            val snap = chatRef.get().await()
            if (!snap.exists()) {
                val (u1, u2) = if (me.uid < other.uid) me to other else other to me
                val payload = mapOf(
                    "participantUids" to listOf(u1.uid, u2.uid),
                    "user1Uid" to u1.uid,
                    "user1Name" to u1.displayName.ifBlank { "User" }.take(60),
                    "user1Avatar" to u1.avatarData,
                    "user2Uid" to u2.uid,
                    "user2Name" to u2.displayName.ifBlank { "User" }.take(60),
                    "user2Avatar" to u2.avatarData,
                    "lastMessage" to "",
                    "lastSenderId" to uid,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                chatRef.set(payload).await()
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, chatRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        chat: ChatThread,
        senderName: String,
        text: String,
        mediaType: String = "none",
        mediaData: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val msgRef = db.collection("chats").document(chat.id).collection("messages").document()
        return try {
            val cleanText = text.trim().take(2000)
            require(cleanText.isNotEmpty() || !mediaData.isNullOrBlank()) { "Message cannot be empty" }
            val payload = mapOf(
                "chatId" to chat.id,
                "senderId" to uid,
                "participantUids" to chat.participantUids,
                "text" to cleanText,
                "mediaType" to mediaType,
                "mediaData" to mediaData,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            msgRef.set(payload).await()
            val preview = when {
                cleanText.isNotBlank() -> cleanText.take(120)
                mediaType == "image" -> "Sent a photo"
                mediaType == "video" -> "Sent a video"
                else -> "Attachment"
            }
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "lastMessage" to preview,
                    "lastSenderId" to uid,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val recipientUid = chat.participantUids.firstOrNull { it != uid }
            if (recipientUid != null) {
                db.collection("users").document(recipientUid).collection("notifications").document().set(
                    mapOf(
                        "userId" to recipientUid,
                        "actorId" to uid,
                        "actorName" to senderName.take(60),
                        "type" to "message",
                        "title" to "Message from $senderName",
                        "body" to preview,
                        "targetId" to chat.id,
                        "readStatus" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(uid: String, notificationId: String): Result<Unit> {
        val ref = db.collection("users").document(uid).collection("notifications").document(notificationId)
        return try {
            ref.update(mapOf("readStatus" to true)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(uid: String, unreadIds: List<String>): Result<Unit> {
        return try {
            if (unreadIds.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            unreadIds.take(100).forEach { id ->
                val ref = db.collection("users").document(uid).collection("notifications").document(id)
                batch.update(ref, mapOf("readStatus" to true))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/notifications")
            Result.failure(e)
        }
    }
}
