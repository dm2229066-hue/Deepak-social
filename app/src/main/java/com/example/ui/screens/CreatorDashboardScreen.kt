package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.PrivateUser
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.data.model.Wallet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldCreator
import com.example.ui.viewmodel.CreatorViewModel
import com.example.ui.viewmodel.SocialViewModel
import com.example.util.MediaUtils

@Composable
fun CreatorDashboardScreen(
    currentUser: PublicProfile,
    privateUser: PrivateUser?,
    creatorViewModel: CreatorViewModel,
    socialViewModel: SocialViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val walletState by creatorViewModel.walletState.collectAsStateWithLifecycle()
    val transactions by creatorViewModel.transactionsState.collectAsStateWithLifecycle()
    val withdrawals by creatorViewModel.withdrawalsState.collectAsStateWithLifecycle()
    val monetizationApp by creatorViewModel.monetizationAppState.collectAsStateWithLifecycle()
    val appConfig by creatorViewModel.appConfig.collectAsStateWithLifecycle()

    var showPayoutDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }

    val wallet = (walletState as? UiState.Success)?.data ?: Wallet(uid = currentUser.uid)

    if (showPayoutDialog) {
        PayoutDetailsDialog(
            privateUser = privateUser,
            onDismiss = { showPayoutDialog = false },
            onSave = { upi, acc, ifsc, holder ->
                socialViewModel.updatePayoutDetails(upi, acc, ifsc, holder) {
                    showPayoutDialog = false
                }
            }
        )
    }

    if (showWithdrawDialog) {
        WithdrawMoneyDialog(
            availableBalance = wallet.availableBalance,
            minThreshold = appConfig.minWithdrawalInr,
            privateUser = privateUser,
            onDismiss = { showWithdrawDialog = false },
            onSubmit = { amount, method, destination ->
                creatorViewModel.requestWithdrawal(
                    userName = currentUser.displayName,
                    amount = amount,
                    method = method,
                    destination = destination,
                    availableBalance = wallet.availableBalance
                ) {
                    showWithdrawDialog = false
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
        ) {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("creator_dashboard_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Creator Earning Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (walletState is UiState.Loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Studio Hero Banner + Available / Pending Balance
                    item(key = "hero_balance") {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_creator_banner),
                                    contentDescription = "Creator Studio",
                                    modifier = Modifier.fillMaxWidth().height(190.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.85f))
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Verified Creator Ledger",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (currentUser.monetizationStatus == "approved") {
                                                EmeraldSuccess
                                            } else {
                                                GoldCreator
                                            }
                                        ) {
                                            Text(
                                                text = currentUser.monetizationStatus.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column {
                                            Text(
                                                text = "Available Balance",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                            Text(
                                                text = MediaUtils.formatCurrency(wallet.availableBalance),
                                                style = MaterialTheme.typography.displaySmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Pending Balance",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.75f)
                                            )
                                            Text(
                                                text = MediaUtils.formatCurrency(wallet.pendingBalance),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action buttons: Withdraw & Payout Details
                    item(key = "payout_actions") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showWithdrawDialog = true },
                                enabled = wallet.availableBalance >= appConfig.minWithdrawalInr,
                                modifier = Modifier.weight(1f).testTag("withdraw_money_button")
                            ) {
                                Text("Withdraw Money")
                            }
                            OutlinedButton(
                                onClick = { showPayoutDialog = true },
                                modifier = Modifier.weight(1f).testTag("payout_details_button")
                            ) {
                                Text("UPI / Bank Details")
                            }
                        }
                        if (wallet.availableBalance < appConfig.minWithdrawalInr) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Minimum withdrawal threshold: ${MediaUtils.formatCurrency(appConfig.minWithdrawalInr)}. Earnings are credited strictly from verified ad impressions, eligible views, and approved referrals.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Time-period earnings breakdown
                    item(key = "time_earnings") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Earnings Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    EarningMetricTile("Total Earnings", wallet.totalEarnings, Modifier.weight(1f))
                                    EarningMetricTile("Daily", wallet.dailyEarnings, Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    EarningMetricTile("Weekly", wallet.weeklyEarnings, Modifier.weight(1f))
                                    EarningMetricTile("Monthly", wallet.monthlyEarnings, Modifier.weight(1f))
                                }
                                EarningMetricTile("Lifetime Earnings", wallet.lifetimeEarnings, Modifier.fillMaxWidth())
                            }
                        }
                    }

                    // Source breakdown
                    item(key = "source_earnings") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Revenue Streams", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    EarningMetricTile("Ad Revenue", wallet.adRevenue, Modifier.weight(1f))
                                    EarningMetricTile("Video Monetization", wallet.videoEarnings, Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    EarningMetricTile("Post Monetization", wallet.postEarnings, Modifier.weight(1f))
                                    EarningMetricTile("Referral Earnings", wallet.referralEarnings, Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Monetization Eligibility Tracker
                    item(key = "monetization_eligibility") {
                        val reqFriends = appConfig.minFriendsForMonetization.coerceAtLeast(1L)
                        val reqPosts = appConfig.minPostsForMonetization.coerceAtLeast(1L)
                        val reqViews = appConfig.minViewsForMonetization.coerceAtLeast(1L)
                        val eligible = currentUser.friendsCount >= reqFriends &&
                            currentUser.postsCount >= reqPosts &&
                            currentUser.totalViews >= reqViews &&
                            currentUser.accountStatus == "active"

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Monetization Eligibility",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                EligibilityProgressRow("Friends / Connections", currentUser.friendsCount, reqFriends)
                                EligibilityProgressRow("Published Posts & Reels", currentUser.postsCount, reqPosts)
                                EligibilityProgressRow("Verified Content Views", currentUser.totalViews, reqViews)

                                val status = monetizationApp?.status ?: currentUser.monetizationStatus
                                when (status) {
                                    "approved" -> {
                                        Text(
                                            text = "Approved! Your verified content impressions qualify for server/admin revenue settlement.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EmeraldSuccess,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    "pending" -> {
                                        Text(
                                            text = "Application pending Admin review.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GoldCreator,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    else -> {
                                        Button(
                                            onClick = { creatorViewModel.applyForMonetization(currentUser) },
                                            enabled = eligible,
                                            modifier = Modifier.fillMaxWidth().testTag("apply_monetization_button")
                                        ) {
                                            Text(if (eligible) "Apply for Monetization" else "Meet Eligibility Targets to Apply")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Saved Payout Details Summary
                    item(key = "payout_summary") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Configured Payout Methods", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "UPI ID: ${privateUser?.upiId?.takeIf { it.isNotBlank() } ?: "Not configured"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Bank: ${privateUser?.bankAccountNumber?.takeIf { it.isNotBlank() }?.let { "••••${it.takeLast(4)} (${privateUser.bankIfsc})" } ?: "Not configured"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Withdrawal Requests
                    item(key = "withdrawals_header") {
                        Text(
                            text = "Withdrawal Requests (${withdrawals.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (withdrawals.isEmpty()) {
                        item {
                            Text(
                                text = "No withdrawal requests submitted yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(withdrawals, key = { it.id }) { req ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${MediaUtils.formatCurrency(req.amount)} via ${req.method.uppercase()}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${req.destination} • ${MediaUtils.formatRelativeTime(req.createdAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = req.status.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Transaction History
                    item(key = "transactions_header") {
                        Text(
                            text = "Transaction History (${transactions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (transactions.isEmpty()) {
                        item {
                            Text(
                                text = "No verified ledger transactions recorded yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(transactions, key = { it.id }) { tx ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.description, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "${tx.type} • ${MediaUtils.formatRelativeTime(tx.createdAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = (if (tx.amount >= 0) "+" else "") + MediaUtils.formatCurrency(tx.amount),
                                        fontWeight = FontWeight.Bold,
                                        color = if (tx.amount >= 0) EmeraldSuccess else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EarningMetricTile(
    label: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(MediaUtils.formatCurrency(amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EligibilityProgressRow(label: String, current: Long, required: Long) {
    val progress = (current.toFloat() / required.toFloat()).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$current / $required", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun PayoutDetailsDialog(
    privateUser: PrivateUser?,
    onDismiss: () -> Unit,
    onSave: (upi: String, acc: String, ifsc: String, holder: String) -> Unit
) {
    var upi by remember { mutableStateOf(privateUser?.upiId ?: "") }
    var acc by remember { mutableStateOf(privateUser?.bankAccountNumber ?: "") }
    var ifsc by remember { mutableStateOf(privateUser?.bankIfsc ?: "") }
    var holder by remember { mutableStateOf(privateUser?.bankHolderName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("UPI & Bank Account Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = upi,
                    onValueChange = { upi = it.take(80) },
                    label = { Text("UPI ID (e.g. name@upi)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("upi_input")
                )
                OutlinedTextField(
                    value = holder,
                    onValueChange = { holder = it.take(80) },
                    label = { Text("Account Holder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = acc,
                    onValueChange = { acc = it.take(40) },
                    label = { Text("Bank Account Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ifsc,
                    onValueChange = { ifsc = it.take(20) },
                    label = { Text("IFSC Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(upi, acc, ifsc, holder) },
                modifier = Modifier.testTag("save_payout_details_button")
            ) {
                Text("Save Details")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun WithdrawMoneyDialog(
    availableBalance: Double,
    minThreshold: Double,
    privateUser: PrivateUser?,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, method: String, destination: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("upi") }
    val defaultDest = remember(method, privateUser) {
        if (method == "upi") privateUser?.upiId.orEmpty()
        else listOfNotNull(privateUser?.bankHolderName, privateUser?.bankAccountNumber, privateUser?.bankIfsc)
            .filter { it.isNotBlank() }
            .joinToString(" | ")
    }
    var destination by remember(defaultDest) { mutableStateOf(defaultDest) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Withdraw Money") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Available: ${MediaUtils.formatCurrency(availableBalance)} (Min ${MediaUtils.formatCurrency(minThreshold)})",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = method == "upi", onClick = { method = "upi" }, label = { Text("UPI") })
                    FilterChip(selected = method == "bank", onClick = { method = "bank" }, label = { Text("Bank Transfer") })
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (INR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it.take(160) },
                    label = { Text(if (method == "upi") "UPI ID" else "Bank Account / IFSC") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val parsed = amountText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onSubmit(parsed, method, destination) },
                enabled = parsed >= minThreshold && parsed <= availableBalance && destination.trim().length >= 3
            ) {
                Text("Submit Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
