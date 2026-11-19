package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.Announcement
import com.example.data.model.AppConfig
import com.example.data.model.MonetizationApplication
import com.example.data.model.MonetizationEvent
import com.example.data.model.Post
import com.example.data.model.PublicProfile
import com.example.data.model.Referral
import com.example.data.model.Report
import com.example.data.model.SponsoredAd
import com.example.data.model.Wallet
import com.example.data.model.WithdrawalRequest
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

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val BOOTSTRAP_ADMIN_EMAIL = "rahul11223344d@gmail.com"
    }

    suspend fun checkIsAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.isEmailVerified && user.email == BOOTSTRAP_ADMIN_EMAIL) return true
        return try {
            db.collection("admins").document(user.uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    fun observeAllProfiles(): Flow<List<PublicProfile>> =
        db.collection("public_profiles")
            .snapshots()
            .map { it.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleAdminFlowError(it, "public_profiles") }

    fun observeAllPosts(): Flow<List<Post>> =
        db.collection("posts")
            .snapshots()
            .map { it.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleAdminFlowError(it, "posts") }

    fun observeAllReports(): Flow<List<Report>> =
        db.collection("reports")
            .snapshots()
            .map { it.toObjects(Report::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleAdminFlowError(it, "reports") }

    fun observeAllMonetizationApps(): Flow<List<MonetizationApplication>> =
        db.collection("monetization_applications")
            .snapshots()
            .map { it.toObjects(MonetizationApplication::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleAdminFlowError(it, "monetization_applications") }

    fun observeAllWithdrawals(): Flow<List<WithdrawalRequest>> =
        db.collection("withdrawal_requests")
            .snapshots()
            .map { it.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleAdminFlowError(it, "withdrawal_requests") }

    fun observeAllAds(): Flow<List<SponsoredAd>> =
        db.collection("ads")
            .snapshots()
            .map { it.toObjects(SponsoredAd::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleAdminFlowError(it, "ads") }

    fun observeAllAnnouncements(): Flow<List<Announcement>> =
        db.collection("announcements")
            .snapshots()
            .map { it.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleAdminFlowError(it, "announcements") }

    fun observeAllReferrals(): Flow<List<Referral>> =
        db.collection("referrals")
            .snapshots()
            .map { it.toObjects(Referral::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleAdminFlowError(it, "referrals") }

    fun observeUnverifiedEvents(): Flow<List<MonetizationEvent>> =
        db.collection("monetization_events")
            .snapshots()
            .map { it.toObjects(MonetizationEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleAdminFlowError(it, "monetization_events") }

    private fun handleAdminFlowError(error: Throwable, path: String): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        if (auth.currentUser != null) {
            handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        }
        throw error
    }

    suspend fun updateAccountStatus(targetUid: String, status: String): Result<Unit> = runCatchingAdmin("public_profiles/$targetUid") {
        db.collection("public_profiles").document(targetUid)
            .update(mapOf("accountStatus" to status, "updatedAt" to FieldValue.serverTimestamp()))
            .await()
    }

    suspend fun deletePostAdmin(postId: String): Result<Unit> = runCatchingAdmin("posts/$postId") {
        db.collection("posts").document(postId).delete().await()
    }

    suspend fun resolveReport(report: Report, newStatus: String, deleteTargetPost: Boolean): Result<Unit> =
        runCatchingAdmin("reports/${report.id}") {
            db.collection("reports").document(report.id)
                .update(mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            if (deleteTargetPost && (report.targetType == "post" || report.targetType == "reel")) {
                db.collection("posts").document(report.targetId).delete().await()
            } else if (deleteTargetPost && report.targetType == "user") {
                db.collection("public_profiles").document(report.targetId)
                    .update(mapOf("accountStatus" to "suspended", "updatedAt" to FieldValue.serverTimestamp()))
                    .await()
            }
        }

    suspend fun reviewMonetizationApp(app: MonetizationApplication, approved: Boolean, note: String): Result<Unit> =
        runCatchingAdmin("monetization_applications/${app.userId}") {
            val status = if (approved) "approved" else "rejected"
            db.collection("monetization_applications").document(app.userId)
                .update(
                    mapOf(
                        "status" to status,
                        "reviewNote" to note.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            db.collection("public_profiles").document(app.userId)
                .update(
                    mapOf(
                        "monetizationStatus" to status,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            val adminUid = auth.currentUser?.uid ?: "admin"
            db.collection("users").document(app.userId).collection("notifications").document().set(
                mapOf(
                    "userId" to app.userId,
                    "actorId" to adminUid,
                    "actorName" to "Deepak Social Creator Team",
                    "type" to "monetization",
                    "title" to if (approved) "Monetization Approved!" else "Monetization Update",
                    "body" to if (approved) "Your account is now approved for Creator Monetization." else "Application status: $status. ${note.take(120)}",
                    "readStatus" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun creditCreatorEarnings(
        creatorId: String,
        type: String, // ad_revenue, video_monetization, post_monetization, referral_bonus
        amount: Double,
        description: String
    ): Result<Unit> = runCatchingAdmin("wallets/$creatorId") {
        require(amount > 0.0) { "Credit amount must be positive" }
        val walletRef = db.collection("wallets").document(creatorId)
        val snap = walletRef.get().await()
        val existing = snap.toObject(Wallet::class.java) ?: Wallet(uid = creatorId)
        val updatedFields = mutableMapOf<String, Any>(
            "uid" to creatorId,
            "totalEarnings" to existing.totalEarnings + amount,
            "dailyEarnings" to existing.dailyEarnings + amount,
            "weeklyEarnings" to existing.weeklyEarnings + amount,
            "monthlyEarnings" to existing.monthlyEarnings + amount,
            "lifetimeEarnings" to existing.lifetimeEarnings + amount,
            "availableBalance" to existing.availableBalance + amount,
            "adRevenue" to existing.adRevenue + if (type == "ad_revenue") amount else 0.0,
            "videoEarnings" to existing.videoEarnings + if (type == "video_monetization") amount else 0.0,
            "postEarnings" to existing.postEarnings + if (type == "post_monetization") amount else 0.0,
            "referralEarnings" to existing.referralEarnings + if (type == "referral_bonus") amount else 0.0,
            "pendingBalance" to existing.pendingBalance,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (snap.exists()) {
            walletRef.update(updatedFields).await()
        } else {
            walletRef.set(updatedFields).await()
        }
        db.collection("users").document(creatorId).collection("transactions").document().set(
            mapOf(
                "userId" to creatorId,
                "type" to type,
                "amount" to amount,
                "status" to "completed",
                "description" to description.take(200),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun reviewWithdrawal(request: WithdrawalRequest, newStatus: String, note: String): Result<Unit> =
        runCatchingAdmin("withdrawal_requests/${request.id}") {
            val reqRef = db.collection("withdrawal_requests").document(request.id)
            val walletRef = db.collection("wallets").document(request.userId)
            val walletSnap = walletRef.get().await()
            val wallet = walletSnap.toObject(Wallet::class.java) ?: Wallet(uid = request.userId)

            if (request.status == "pending" && (newStatus == "approved" || newStatus == "paid")) {
                require(wallet.availableBalance >= request.amount) { "Insufficient available balance in creator wallet" }
                val newAvailable = (wallet.availableBalance - request.amount).coerceAtLeast(0.0)
                val newPending = if (newStatus == "approved") wallet.pendingBalance + request.amount else wallet.pendingBalance
                walletRef.update(
                    mapOf(
                        "availableBalance" to newAvailable,
                        "pendingBalance" to newPending,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                if (newStatus == "paid") {
                    db.collection("users").document(request.userId).collection("transactions").document().set(
                        mapOf(
                            "userId" to request.userId,
                            "type" to "withdrawal",
                            "amount" to -request.amount,
                            "status" to "completed",
                            "description" to "Payout via ${request.method.uppercase()} (${request.destination.take(60)})",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else if (request.status == "approved" && newStatus == "paid") {
                val newPending = (wallet.pendingBalance - request.amount).coerceAtLeast(0.0)
                walletRef.update(
                    mapOf(
                        "pendingBalance" to newPending,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                db.collection("users").document(request.userId).collection("transactions").document().set(
                    mapOf(
                        "userId" to request.userId,
                        "type" to "withdrawal",
                        "amount" to -request.amount,
                        "status" to "completed",
                        "description" to "Payout via ${request.method.uppercase()} (${request.destination.take(60)})",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else if (request.status == "approved" && newStatus == "rejected") {
                val newPending = (wallet.pendingBalance - request.amount).coerceAtLeast(0.0)
                walletRef.update(
                    mapOf(
                        "availableBalance" to wallet.availableBalance + request.amount,
                        "pendingBalance" to newPending,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            reqRef.update(
                mapOf(
                    "status" to newStatus,
                    "adminNote" to note.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val adminUid = auth.currentUser?.uid ?: "admin"
            db.collection("users").document(request.userId).collection("notifications").document().set(
                mapOf(
                    "userId" to request.userId,
                    "actorId" to adminUid,
                    "actorName" to "Deepak Social Payouts",
                    "type" to "withdrawal",
                    "title" to "Withdrawal ${newStatus.replaceFirstChar { it.uppercase() }}",
                    "body" to "Your withdrawal of ₹${request.amount.toInt()} is now $newStatus.",
                    "targetId" to request.id,
                    "readStatus" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun approveReferral(referral: Referral, rewardAmount: Double): Result<Unit> =
        runCatchingAdmin("referrals/${referral.id}") {
            require(referral.status == "pending") { "Referral already processed" }
            db.collection("referrals").document(referral.id)
                .update(
                    mapOf(
                        "status" to "credited",
                        "rewardAmount" to rewardAmount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            creditCreatorEarnings(
                creatorId = referral.referrerUid,
                type = "referral_bonus",
                amount = rewardAmount,
                description = "Verified referral reward for inviting ${referral.referredName}"
            ).getOrThrow()
        }

    suspend fun verifyAndSettleMonetizationEvents(events: List<MonetizationEvent>, ratePerEvent: Double = 0.5): Result<Unit> =
        runCatchingAdmin("monetization_events") {
            val unverified = events.filter { !it.verified && it.creatorId != "platform" }
            require(unverified.isNotEmpty()) { "No unverified creator events to settle" }
            val byCreator = unverified.groupBy { it.creatorId }
            unverified.take(50).forEach { ev ->
                db.collection("monetization_events").document(ev.id)
                    .update(mapOf("verified" to true)).await()
            }
            byCreator.forEach { (creatorId, creatorEvents) ->
                val videoCount = creatorEvents.count { it.eventType == "video_view" }
                val postCount = creatorEvents.count { it.eventType == "post_view" }
                if (videoCount > 0) {
                    creditCreatorEarnings(
                        creatorId = creatorId,
                        type = "video_monetization",
                        amount = videoCount * ratePerEvent,
                        description = "Settled $videoCount verified Reel views"
                    ).getOrThrow()
                }
                if (postCount > 0) {
                    creditCreatorEarnings(
                        creatorId = creatorId,
                        type = "post_monetization",
                        amount = postCount * (ratePerEvent * 0.5),
                        description = "Settled $postCount verified post impressions"
                    ).getOrThrow()
                }
            }
        }

    suspend fun createSponsoredAd(
        title: String,
        description: String,
        sponsorName: String,
        ctaText: String,
        ctaUrl: String
    ): Result<Unit> = runCatchingAdmin("ads") {
        val ref = db.collection("ads").document()
        ref.set(
            mapOf(
                "title" to title.trim().take(100),
                "description" to description.trim().take(300),
                "sponsorName" to sponsorName.trim().take(60),
                "ctaText" to ctaText.trim().take(30),
                "ctaUrl" to ctaUrl.trim().take(300),
                "active" to true,
                "impressionsCount" to 0L,
                "clicksCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun toggleAdActive(adId: String, active: Boolean): Result<Unit> = runCatchingAdmin("ads/$adId") {
        db.collection("ads").document(adId)
            .update(mapOf("active" to active, "updatedAt" to FieldValue.serverTimestamp()))
            .await()
    }

    suspend fun createAnnouncement(title: String, message: String): Result<Unit> = runCatchingAdmin("announcements") {
        val uid = auth.currentUser?.uid ?: "admin"
        db.collection("announcements").document().set(
            mapOf(
                "title" to title.trim().take(120),
                "message" to message.trim().take(500),
                "active" to true,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun toggleAnnouncement(id: String, active: Boolean): Result<Unit> = runCatchingAdmin("announcements/$id") {
        db.collection("announcements").document(id).update(mapOf("active" to active)).await()
    }

    suspend fun updateAppConfig(config: AppConfig): Result<Unit> = runCatchingAdmin("app_config/rewards") {
        db.collection("app_config").document("rewards").set(
            mapOf(
                "referralRewardInr" to config.referralRewardInr.coerceIn(0.0, 100000.0),
                "minWithdrawalInr" to config.minWithdrawalInr.coerceIn(1.0, 100000.0),
                "minFriendsForMonetization" to config.minFriendsForMonetization.coerceAtLeast(0L),
                "minPostsForMonetization" to config.minPostsForMonetization.coerceAtLeast(0L),
                "minViewsForMonetization" to config.minViewsForMonetization.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private suspend inline fun runCatchingAdmin(path: String, block: () -> Unit): Result<Unit> {
        return try {
            block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
