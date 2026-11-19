package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.PrivateUser
import com.example.data.model.PublicProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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

class UserRepository(
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

    fun observeMyProfile(uid: String): Flow<PublicProfile?> =
        db.collection("public_profiles").document(uid)
            .snapshots()
            .map { it.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, "public_profiles/$uid")
                }
                throw error
            }

    fun observePrivateUser(uid: String): Flow<PrivateUser?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { it.toObject(PrivateUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, "users/$uid")
                }
                throw error
            }

    fun observeActiveProfiles(): Flow<List<PublicProfile>> =
        db.collection("public_profiles")
            .whereEqualTo("accountStatus", "active")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "public_profiles")
                }
                throw error
            }

    suspend fun ensureUserBootstrapped(firebaseUser: FirebaseUser): Result<Unit> {
        val uid = firebaseUser.uid
        return try {
            val publicRef = db.collection("public_profiles").document(uid)
            val privateRef = db.collection("users").document(uid)
            val walletRef = db.collection("wallets").document(uid)

            val publicSnap = publicRef.get().await()
            if (!publicSnap.exists()) {
                val rawName = (firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: "Creator")
                    .take(60)
                val baseUsername = rawName.lowercase(Locale.ROOT)
                    .replace(Regex("[^a-z0-9_]"), "_")
                    .trim('_')
                    .ifBlank { "user" }
                    .take(24)
                val username = "${baseUsername}_${uid.take(5).lowercase(Locale.ROOT)}"
                val referralCode = "DS${uid.take(6).uppercase(Locale.ROOT)}"
                val publicPayload = mapOf(
                    "uid" to uid,
                    "username" to username,
                    "displayName" to rawName,
                    "bio" to "Connecting and creating on Deepak Social.",
                    "avatarData" to firebaseUser.photoUrl?.toString()?.take(400_000),
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "friendsCount" to 0L,
                    "postsCount" to 0L,
                    "totalViews" to 0L,
                    "monetizationStatus" to "none",
                    "referralCode" to referralCode,
                    "online" to true,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "accountStatus" to "active",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                publicRef.set(publicPayload).await()
            }

            val privateSnap = privateRef.get().await()
            if (!privateSnap.exists()) {
                val privatePayload = mapOf(
                    "uid" to uid,
                    "email" to (firebaseUser.email ?: ""),
                    "privacyMode" to "public",
                    "blockedUserIds" to emptyList<String>(),
                    "termsAccepted" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                privateRef.set(privatePayload).await()
            }

            val walletSnap = walletRef.get().await()
            if (!walletSnap.exists()) {
                val walletPayload = mapOf(
                    "uid" to uid,
                    "totalEarnings" to 0.0,
                    "dailyEarnings" to 0.0,
                    "weeklyEarnings" to 0.0,
                    "monthlyEarnings" to 0.0,
                    "lifetimeEarnings" to 0.0,
                    "adRevenue" to 0.0,
                    "videoEarnings" to 0.0,
                    "postEarnings" to 0.0,
                    "referralEarnings" to 0.0,
                    "availableBalance" to 0.0,
                    "pendingBalance" to 0.0,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                walletRef.set(walletPayload).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "public_profiles/$uid")
            Result.failure(e)
        }
    }

    suspend fun updatePresence(uid: String, online: Boolean): Result<Unit> {
        return try {
            if (auth.currentUser?.uid != uid) return Result.success(Unit)
            db.collection("public_profiles").document(uid)
                .update(
                    mapOf(
                        "online" to online,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "public_profiles/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        avatarData: String?,
        coverData: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val cleanName = displayName.trim().take(60)
            val cleanUser = username.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9_.]"), "").take(40)
            require(cleanName.isNotEmpty()) { "Display name cannot be empty" }
            require(cleanUser.length >= 2) { "Username must be at least 2 characters" }
            val updates = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "username" to cleanUser,
                "bio" to bio.trim().take(300),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!avatarData.isNullOrBlank()) updates["avatarData"] = avatarData
            if (!coverData.isNullOrBlank()) updates["coverData"] = coverData
            db.collection("public_profiles").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "public_profiles/$uid")
            Result.failure(e)
        }
    }

    suspend fun updatePrivacyMode(privacyMode: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val mode = if (privacyMode == "friends") "friends" else "public"
            db.collection("users").document(uid)
                .update(mapOf("privacyMode" to mode, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updatePayoutDetails(
        upiId: String,
        bankAccountNumber: String,
        bankIfsc: String,
        bankHolderName: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val updates = mutableMapOf<String, Any>(
                "upiId" to upiId.trim().take(80),
                "bankAccountNumber" to bankAccountNumber.trim().take(40),
                "bankIfsc" to bankIfsc.trim().uppercase(Locale.ROOT).take(20),
                "bankHolderName" to bankHolderName.trim().take(80),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun toggleBlockUser(targetUid: String, currentBlocked: List<String>, block: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val updated = if (block) {
                (currentBlocked + targetUid).distinct().take(200)
            } else {
                currentBlocked.filterNot { it == targetUid }
            }
            db.collection("users").document(uid)
                .update(mapOf("blockedUserIds" to updated, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val uid = user.uid
        return try {
            db.collection("users").document(uid).delete().await()
            db.collection("public_profiles").document(uid).delete().await()
            user.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }
}
