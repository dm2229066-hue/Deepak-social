package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Announcement
import com.example.data.model.AppConfig
import com.example.data.model.MonetizationApplication
import com.example.data.model.MonetizationEvent
import com.example.data.model.Post
import com.example.data.model.PublicProfile
import com.example.data.model.Referral
import com.example.data.model.Report
import com.example.data.model.SponsoredAd
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.AdminRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin = _isAdmin.asStateFlow()

    init {
        viewModelScope.launch {
            _isAdmin.value = adminRepository.checkIsAdmin()
        }
    }

    val allProfiles: StateFlow<List<PublicProfile>> = adminRepository.observeAllProfiles()
        .catch { handleCatch(it, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allPosts: StateFlow<List<Post>> = adminRepository.observeAllPosts()
        .map { it.sortedByDescending { p -> p.createdAt ?: Timestamp(0, 0) } }
        .catch { handleCatch(it, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allReports: StateFlow<List<Report>> = adminRepository.observeAllReports()
        .map { it.sortedByDescending { r -> r.createdAt ?: Timestamp(0, 0) } }
        .catch { handleCatch(it, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allMonetizationApps: StateFlow<List<MonetizationApplication>> = adminRepository.observeAllMonetizationApps()
        .map { it.sortedByDescending { a -> a.createdAt ?: Timestamp(0, 0) } }
        .catch { handleCatch(it, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allWithdrawals: StateFlow<List<WithdrawalRequest>> = adminRepository.observeAllWithdrawals()
        .map { it.sortedByDescending { w -> w.createdAt ?: Timestamp(0, 0) } }
        .catch { handleCatch(it, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allAds: StateFlow<List<SponsoredAd>> = adminRepository.observeAllAds()
        .map { it.sortedByDescending { a -> a.createdAt ?: Timestamp(0, 0) } }
        .catch { handleCatch(it, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allAnnouncements: StateFlow<List<Announcement>> = adminRepository.observeAllAnnouncements()
        .map { it.sortedByDescending { a -> a.createdAt ?: Timestamp(0, 0) } }
        .catch { handleCatch(it, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allReferrals: StateFlow<List<Referral>> = adminRepository.observeAllReferrals()
        .map { it.sortedByDescending { r -> r.createdAt ?: Timestamp(0, 0) } }
        .catch { handleCatch(it, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val unverifiedEvents: StateFlow<List<MonetizationEvent>> = adminRepository.observeUnverifiedEvents()
        .map { list -> list.filter { !it.verified && it.creatorId != "platform" } }
        .catch { handleCatch(it, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<T>.handleCatch(error: Throwable, fallback: T) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(fallback)
    }

    fun updateUserStatus(uid: String, status: String) {
        viewModelScope.launch {
            adminRepository.updateAccountStatus(uid, status)
                .onSuccess { _events.tryEmit("Account status set to $status") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to update user") }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            adminRepository.deletePostAdmin(postId)
                .onSuccess { _events.tryEmit("Content removed") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to delete content") }
        }
    }

    fun resolveReport(report: Report, newStatus: String, removeTarget: Boolean) {
        viewModelScope.launch {
            adminRepository.resolveReport(report, newStatus, removeTarget)
                .onSuccess { _events.tryEmit("Report $newStatus") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to resolve report") }
        }
    }

    fun reviewMonetization(app: MonetizationApplication, approved: Boolean, note: String) {
        viewModelScope.launch {
            adminRepository.reviewMonetizationApp(app, approved, note)
                .onSuccess { _events.tryEmit(if (approved) "Creator approved for monetization!" else "Application rejected") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Review failed") }
        }
    }

    fun reviewWithdrawal(req: WithdrawalRequest, newStatus: String, note: String) {
        viewModelScope.launch {
            adminRepository.reviewWithdrawal(req, newStatus, note)
                .onSuccess { _events.tryEmit("Withdrawal marked $newStatus") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Withdrawal update failed") }
        }
    }

    fun creditVerifiedCreatorEarnings(creatorId: String, type: String, amount: Double, description: String) {
        viewModelScope.launch {
            adminRepository.creditCreatorEarnings(creatorId, type, amount, description)
                .onSuccess { _events.tryEmit("Credited ₹${amount.toInt()} ($type)") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Credit failed") }
        }
    }

    fun approveReferral(referral: Referral, rewardAmount: Double) {
        viewModelScope.launch {
            adminRepository.approveReferral(referral, rewardAmount)
                .onSuccess { _events.tryEmit("Referral bonus credited!") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to credit referral") }
        }
    }

    fun settleVerifiedEvents(events: List<MonetizationEvent>) {
        viewModelScope.launch {
            adminRepository.verifyAndSettleMonetizationEvents(events)
                .onSuccess { _events.tryEmit("Settled verified view events to creator wallets") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Settlement failed") }
        }
    }

    fun createAd(title: String, description: String, sponsor: String, ctaText: String, ctaUrl: String) {
        viewModelScope.launch {
            adminRepository.createSponsoredAd(title, description, sponsor, ctaText, ctaUrl)
                .onSuccess { _events.tryEmit("Sponsored ad published") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to create ad") }
        }
    }

    fun toggleAd(adId: String, active: Boolean) {
        viewModelScope.launch {
            adminRepository.toggleAdActive(adId, active)
                .onSuccess { _events.tryEmit(if (active) "Ad activated" else "Ad paused") }
        }
    }

    fun createAnnouncement(title: String, message: String) {
        viewModelScope.launch {
            adminRepository.createAnnouncement(title, message)
                .onSuccess { _events.tryEmit("Announcement broadcasted") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to broadcast announcement") }
        }
    }

    fun toggleAnnouncement(id: String, active: Boolean) {
        viewModelScope.launch {
            adminRepository.toggleAnnouncement(id, active)
        }
    }

    fun updateAppConfig(config: AppConfig) {
        viewModelScope.launch {
            adminRepository.updateAppConfig(config)
                .onSuccess { _events.tryEmit("Platform reward & threshold settings updated") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to update config") }
        }
    }
}
