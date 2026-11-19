package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarData: String? = null,
    val coverData: String? = null,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val friendsCount: Long = 0L,
    val postsCount: Long = 0L,
    val totalViews: Long = 0L,
    val monetizationStatus: String = "none", // none, pending, approved, rejected
    val referralCode: String = "",
    val online: Boolean = false,
    @ServerTimestamp val lastSeenAt: Timestamp? = null,
    val accountStatus: String = "active", // active, suspended, banned
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val phone: String? = null,
    val privacyMode: String = "public", // public, friends
    val blockedUserIds: List<String> = emptyList(),
    val referredByCode: String? = null,
    val upiId: String? = null,
    val bankAccountNumber: String? = null,
    val bankIfsc: String? = null,
    val bankHolderName: String? = null,
    val termsAccepted: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorAvatar: String? = null,
    val text: String = "",
    val mediaType: String = "none", // none, image, video
    val mediaData: String? = null,
    val thumbnailData: String? = null,
    val hashtags: List<String> = emptyList(),
    val visibility: String = "public", // public, friends
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L,
    val viewsCount: Long = 0L,
    val reelPost: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val text: String = "",
    val parentCommentId: String? = null,
    val postVisibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserPostAction(
    @DocumentId val id: String = "",
    val postId: String = "",
    val reactionType: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FriendRequest(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val fromName: String = "",
    val fromAvatar: String? = null,
    val toName: String = "",
    val toAvatar: String? = null,
    val participantUids: List<String> = emptyList(),
    val status: String = "pending", // pending, accepted, declined
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Follow(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val targetId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val user1Uid: String = "",
    val user1Name: String = "",
    val user1Avatar: String? = null,
    val user2Uid: String = "",
    val user2Name: String = "",
    val user2Avatar: String? = null,
    val lastMessage: String = "",
    val lastSenderId: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val participantUids: List<String> = emptyList(),
    val text: String = "",
    val mediaType: String = "none", // none, image, video
    val mediaData: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val type: String = "system",
    val title: String = "",
    val body: String = "",
    val targetId: String? = null,
    val readStatus: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Wallet(
    @DocumentId val id: String = "",
    val uid: String = "",
    val totalEarnings: Double = 0.0,
    val dailyEarnings: Double = 0.0,
    val weeklyEarnings: Double = 0.0,
    val monthlyEarnings: Double = 0.0,
    val lifetimeEarnings: Double = 0.0,
    val adRevenue: Double = 0.0,
    val videoEarnings: Double = 0.0,
    val postEarnings: Double = 0.0,
    val referralEarnings: Double = 0.0,
    val availableBalance: Double = 0.0,
    val pendingBalance: Double = 0.0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "adjustment",
    val amount: Double = 0.0,
    val status: String = "completed",
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MonetizationApplication(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val followersCount: Long = 0L,
    val postsCount: Long = 0L,
    val totalViews: Long = 0L,
    val status: String = "pending", // pending, approved, rejected
    val reviewNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val amount: Double = 0.0,
    val method: String = "upi", // upi, bank
    val destination: String = "",
    val status: String = "pending", // pending, approved, paid, rejected
    val adminNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MonetizationEvent(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val viewerId: String = "",
    val contentId: String = "",
    val eventType: String = "post_view", // ad_impression, video_view, post_view
    val verified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Referral(
    @DocumentId val id: String = "",
    val referrerUid: String = "",
    val referredUid: String = "",
    val referredName: String = "",
    val referralCode: String = "",
    val rewardAmount: Double = 0.0,
    val status: String = "pending", // pending, credited, rejected
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SponsoredAd(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val sponsorName: String = "",
    val ctaText: String = "",
    val ctaUrl: String = "",
    val active: Boolean = true,
    val impressionsCount: Long = 0L,
    val clicksCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val active: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Report(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "post", // post, reel, user
    val targetId: String = "",
    val reason: String = "",
    val status: String = "pending", // pending, resolved, dismissed
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "",
    val referralRewardInr: Double = 50.0,
    val minWithdrawalInr: Double = 100.0,
    val minFriendsForMonetization: Long = 3L,
    val minPostsForMonetization: Long = 2L,
    val minViewsForMonetization: Long = 20L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
