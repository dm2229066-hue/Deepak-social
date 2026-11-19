package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.Announcement
import com.example.data.model.AppConfig
import com.example.data.model.MonetizationApplication
import com.example.data.model.PublicProfile
import com.example.data.model.Referral
import com.example.data.model.SponsoredAd
import com.example.data.model.Wallet
import com.example.data.model.WalletTransaction
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
import java.util.Locale

class CreatorRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeWallet(uid: String): Flow<Wallet> =
        db.collection("wallets").document(uid)
            .snapshots()
            .map { snap ->
                snap.toObject(Wallet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: Wallet(uid = uid)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, "wallets/$uid")
                }
                throw error
            }

    fun observeTransactions(uid: String): Flow<List<WalletTransaction>> =
        db.collection("users").document(uid).collection("transactions")
            .snapshots()
            .map { it.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "users/$uid/transactions")
                }
                throw error
            }

    fun observeMyWithdrawals(uid: String): Flow<List<WithdrawalRequest>> =
        db.collection("withdrawal_requests")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { it.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "withdrawal_requests")
                }
                throw error
            }

    fun observeMonetizationApplication(uid: String): Flow<MonetizationApplication?> =
        db.collection("monetization_applications").document(uid)
            .snapshots()
            .map { it.toObject(MonetizationApplication::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, "monetization_applications/$uid")
                }
                throw error
            }

    fun observeReferrals(uid: String): Flow<List<Referral>> =
        db.collection("referrals")
            .whereEqualTo("referrerUid", uid)
            .snapshots()
            .map { it.toObjects(Referral::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "referrals")
                }
                throw error
            }

    fun observeMyReferralClaim(uid: String): Flow<Referral?> =
        db.collection("referrals").document(uid)
            .snapshots()
            .map { it.toObject(Referral::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, "referrals/$uid")
                }
                throw error
            }

    fun observeAppConfig(): Flow<AppConfig> =
        db.collection("app_config").document("rewards")
            .snapshots()
            .map { snap ->
                snap.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: AppConfig()
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, "app_config/rewards")
                }
                throw error
            }

    fun observeActiveAds(): Flow<List<SponsoredAd>> =
        db.collection("ads")
            .whereEqualTo("active", true)
            .snapshots()
            .map { it.toObjects(SponsoredAd::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "ads")
                }
                throw error
            }

    fun observeActiveAnnouncements(): Flow<List<Announcement>> =
        db.collection("announcements")
            .whereEqualTo("active", true)
            .snapshots()
            .map { it.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "announcements")
                }
                throw error
            }

    suspend fun applyForMonetization(profile: PublicProfile): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val appRef = db.collection("monetization_applications").document(uid)
        return try {
            appRef.set(
                mapOf(
                    "userId" to uid,
                    "displayName" to profile.displayName.ifBlank { "Creator" }.take(60),
                    "followersCount" to profile.followersCount,
                    "postsCount" to profile.postsCount,
                    "totalViews" to profile.totalViews,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, appRef.path)
            Result.failure(e)
        }
    }

    suspend fun requestWithdrawal(
        userName: String,
        amount: Double,
        method: String,
        destination: String,
        availableBalance: Double,
        minThreshold: Double
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("withdrawal_requests").document()
        return try {
            require(amount >= minThreshold) { "Minimum withdrawal is ₹${minThreshold.toInt()}" }
            require(amount <= availableBalance) { "Requested amount exceeds available balance" }
            val cleanDest = destination.trim().take(160)
            require(cleanDest.length >= 3) { "Please enter valid payout destination details" }
            ref.set(
                mapOf(
                    "userId" to uid,
                    "userName" to userName.ifBlank { "Creator" }.take(60),
                    "amount" to amount,
                    "method" to if (method == "bank") "bank" else "upi",
                    "destination" to cleanDest,
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

    suspend fun redeemReferralCode(me: PublicProfile, rawCode: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanCode = rawCode.trim().uppercase(Locale.ROOT)
        val refDoc = db.collection("referrals").document(uid)
        return try {
            require(cleanCode.length >= 4) { "Enter a valid referral code" }
            require(cleanCode != me.referralCode.uppercase(Locale.ROOT)) { "You cannot redeem your own referral code" }
            val existingClaim = refDoc.get().await()
            require(!existingClaim.exists()) { "Referral code already claimed for this account" }

            val matchedProfiles = db.collection("public_profiles")
                .whereEqualTo("accountStatus", "active")
                .whereEqualTo("referralCode", cleanCode)
                .get()
                .await()
            val referrer = matchedProfiles.documents.firstOrNull()
                ?.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalArgumentException("Referral code not found")

            require(referrer.uid != uid) { "Self-referral is not allowed" }

            refDoc.set(
                mapOf(
                    "referrerUid" to referrer.uid,
                    "referredUid" to uid,
                    "referredName" to me.displayName.ifBlank { "User" }.take(60),
                    "referralCode" to cleanCode,
                    "rewardAmount" to 0.0,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("users").document(uid).update(
                mapOf(
                    "referredByCode" to cleanCode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, refDoc.path)
            Result.failure(e)
        }
    }

    suspend fun recordAdInteraction(adId: String, clicked: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        return try {
            val field = if (clicked) "clicksCount" else "impressionsCount"
            db.collection("ads").document(adId)
                .update(
                    mapOf(
                        field to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            db.collection("monetization_events").document().set(
                mapOf(
                    "creatorId" to "platform",
                    "viewerId" to uid,
                    "contentId" to adId,
                    "eventType" to "ad_impression",
                    "verified" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "ads/$adId")
            Result.failure(e)
        }
    }
}
