package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Announcement
import com.example.data.model.AppConfig
import com.example.data.model.MonetizationApplication
import com.example.data.model.PublicProfile
import com.example.data.model.Referral
import com.example.data.model.SponsoredAd
import com.example.data.model.UiState
import com.example.data.model.Wallet
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.CreatorRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CreatorViewModel(
    private val creatorRepository: CreatorRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    val walletState: StateFlow<UiState<Wallet>> = creatorRepository.observeWallet(currentUserId)
        .map<Wallet, UiState<Wallet>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load wallet"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val transactionsState: StateFlow<List<WalletTransaction>> = creatorRepository.observeTransactions(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val withdrawalsState: StateFlow<List<WithdrawalRequest>> = creatorRepository.observeMyWithdrawals(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val monetizationAppState: StateFlow<MonetizationApplication?> =
        creatorRepository.observeMonetizationApplication(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val referralsState: StateFlow<List<Referral>> = creatorRepository.observeReferrals(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myReferralClaim: StateFlow<Referral?> = creatorRepository.observeMyReferralClaim(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val appConfig: StateFlow<AppConfig> = creatorRepository.observeAppConfig()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(AppConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppConfig())

    val activeAds: StateFlow<List<SponsoredAd>> = creatorRepository.observeActiveAds()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeAnnouncements: StateFlow<List<Announcement>> = creatorRepository.observeActiveAnnouncements()
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun applyForMonetization(profile: PublicProfile) {
        viewModelScope.launch {
            creatorRepository.applyForMonetization(profile)
                .onSuccess { _events.tryEmit("Monetization application submitted for Admin review!") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Application failed") }
        }
    }

    fun requestWithdrawal(
        userName: String,
        amount: Double,
        method: String,
        destination: String,
        availableBalance: Double,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            creatorRepository.requestWithdrawal(
                userName = userName,
                amount = amount,
                method = method,
                destination = destination,
                availableBalance = availableBalance,
                minThreshold = appConfig.value.minWithdrawalInr
            ).onSuccess {
                _events.tryEmit("Withdrawal request submitted!")
                onDone()
            }.onFailure {
                _events.tryEmit(it.localizedMessage ?: "Withdrawal request failed")
            }
        }
    }

    fun redeemReferralCode(me: PublicProfile, code: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            creatorRepository.redeemReferralCode(me, code)
                .onSuccess {
                    _events.tryEmit("Referral code submitted for verification!")
                    onDone()
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Invalid referral code") }
        }
    }

    private val recordedImpressions = mutableSetOf<String>()

    fun recordAdImpressionOnce(adId: String) {
        if (!recordedImpressions.add(adId)) return
        viewModelScope.launch {
            creatorRepository.recordAdInteraction(adId, clicked = false)
        }
    }

    fun recordAdClick(adId: String) {
        viewModelScope.launch {
            creatorRepository.recordAdInteraction(adId, clicked = true)
        }
    }
}
