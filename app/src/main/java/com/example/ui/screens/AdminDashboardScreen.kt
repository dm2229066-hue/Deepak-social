package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppConfig
import com.example.ui.components.StatPill
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.CreatorViewModel
import com.example.util.MediaUtils

@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    creatorViewModel: CreatorViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val isAdmin by adminViewModel.isAdmin.collectAsStateWithLifecycle()
    val profiles by adminViewModel.allProfiles.collectAsStateWithLifecycle()
    val posts by adminViewModel.allPosts.collectAsStateWithLifecycle()
    val reports by adminViewModel.allReports.collectAsStateWithLifecycle()
    val monetizationApps by adminViewModel.allMonetizationApps.collectAsStateWithLifecycle()
    val withdrawals by adminViewModel.allWithdrawals.collectAsStateWithLifecycle()
    val ads by adminViewModel.allAds.collectAsStateWithLifecycle()
    val announcements by adminViewModel.allAnnouncements.collectAsStateWithLifecycle()
    val referrals by adminViewModel.allReferrals.collectAsStateWithLifecycle()
    val unverifiedEvents by adminViewModel.unverifiedEvents.collectAsStateWithLifecycle()
    val appConfig by creatorViewModel.appConfig.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }

    if (!isAdmin) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text("Protected Admin Access Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Only verified administrators may access this panel.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = onBack) { Text("Go Back") }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text("Admin Control Center", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Analytics") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Monetization & Payouts") })
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Users") })
                        Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Content & Reports") })
                        Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Ads & Config") })
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatPill("Users", "${profiles.size}", Modifier.weight(1f))
                                StatPill("Posts/Reels", "${posts.size}", Modifier.weight(1f))
                                StatPill("Open Reports", "${reports.count { it.status == "pending" }}", Modifier.weight(1f))
                            }
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatPill("Pending Apps", "${monetizationApps.count { it.status == "pending" }}", Modifier.weight(1f))
                                StatPill("Pending Payouts", "${withdrawals.count { it.status == "pending" }}", Modifier.weight(1f))
                                StatPill("Active Ads", "${ads.count { it.active }}", Modifier.weight(1f))
                            }
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Verified Monetization Event Settlement", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${unverifiedEvents.size} unverified view events awaiting server/admin verification.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Button(
                                        onClick = { adminViewModel.settleVerifiedEvents(unverifiedEvents) },
                                        enabled = unverifiedEvents.isNotEmpty(),
                                        modifier = Modifier.fillMaxWidth().testTag("settle_events_button")
                                    ) {
                                        Text("Verify & Credit Eligible Creator Views")
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        item {
                            Text("Monetization Applications (${monetizationApps.size})", fontWeight = FontWeight.Bold)
                        }
                        items(monetizationApps, key = { "app_${it.id}" }) { app ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${app.displayName} • ${app.status.uppercase()}", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Followers: ${app.followersCount} | Posts: ${app.postsCount} | Views: ${app.totalViews}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (app.status == "pending") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { adminViewModel.reviewMonetization(app, true, "Approved by Admin") }) {
                                                Text("Approve")
                                            }
                                            OutlinedButton(onClick = { adminViewModel.reviewMonetization(app, false, "Criteria not met") }) {
                                                Text("Reject")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Withdrawal Requests (${withdrawals.size})", fontWeight = FontWeight.Bold)
                        }
                        items(withdrawals, key = { "wd_${it.id}" }) { req ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${req.userName} • ${MediaUtils.formatCurrency(req.amount)} (${req.status.uppercase()})", fontWeight = FontWeight.Bold)
                                    Text("${req.method.uppercase()}: ${req.destination}", style = MaterialTheme.typography.bodySmall)
                                    if (req.status == "pending" || req.status == "approved") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (req.status == "pending") {
                                                OutlinedButton(onClick = { adminViewModel.reviewWithdrawal(req, "approved", "Queued for payout") }) {
                                                    Text("Approve")
                                                }
                                            }
                                            Button(onClick = { adminViewModel.reviewWithdrawal(req, "paid", "Settled") }) {
                                                Text("Mark Paid")
                                            }
                                            OutlinedButton(onClick = { adminViewModel.reviewWithdrawal(req, "rejected", "Verification failed") }) {
                                                Text("Reject")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Pending Referral Verifications (${referrals.count { it.status == "pending" }})", fontWeight = FontWeight.Bold)
                        }
                        items(referrals.filter { it.status == "pending" }, key = { "ref_${it.id}" }) { ref ->
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
                                        Text("Invited: ${ref.referredName}", fontWeight = FontWeight.Bold)
                                        Text("Code: ${ref.referralCode}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Button(onClick = { adminViewModel.approveReferral(ref, appConfig.referralRewardInr) }) {
                                        Text("Credit ₹${appConfig.referralRewardInr.toInt()}")
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        items(profiles, key = { "usr_${it.uid}" }) { user ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${user.displayName} (@${user.username})", fontWeight = FontWeight.Bold)
                                    Text("Status: ${user.accountStatus.uppercase()} • Monetization: ${user.monetizationStatus}", style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = user.accountStatus == "active",
                                            onClick = { adminViewModel.updateUserStatus(user.uid, "active") },
                                            label = { Text("Active") }
                                        )
                                        FilterChip(
                                            selected = user.accountStatus == "suspended",
                                            onClick = { adminViewModel.updateUserStatus(user.uid, "suspended") },
                                            label = { Text("Suspend") }
                                        )
                                        FilterChip(
                                            selected = user.accountStatus == "banned",
                                            onClick = { adminViewModel.updateUserStatus(user.uid, "banned") },
                                            label = { Text("Ban") }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        item { Text("Reports (${reports.size})", fontWeight = FontWeight.Bold) }
                        items(reports, key = { "rep_${it.id}" }) { rep ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${rep.targetType.uppercase()} Report • ${rep.status.uppercase()}", fontWeight = FontWeight.Bold)
                                    Text("Reason: ${rep.reason}", style = MaterialTheme.typography.bodySmall)
                                    if (rep.status == "pending") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { adminViewModel.resolveReport(rep, "resolved", removeTarget = true) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Text("Remove Target")
                                            }
                                            OutlinedButton(onClick = { adminViewModel.resolveReport(rep, "dismissed", removeTarget = false) }) {
                                                Text("Dismiss")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("All Posts & Reels (${posts.size})", fontWeight = FontWeight.Bold)
                        }
                        items(posts.take(30), key = { "adm_post_${it.id}" }) { post ->
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
                                        Text("${post.authorName} (${if (post.reelPost) "Reel" else "Post"})", fontWeight = FontWeight.Bold)
                                        Text(post.text.take(80), style = MaterialTheme.typography.bodySmall)
                                    }
                                    OutlinedButton(onClick = { adminViewModel.deletePost(post.id) }) {
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        item {
                            AdminCreateAdCard(onCreateAd = { title, desc, sponsor, cta, url ->
                                adminViewModel.createAd(title, desc, sponsor, cta, url)
                            })
                        }
                        items(ads, key = { "ad_${it.id}" }) { ad ->
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
                                        Text("${ad.title} (${ad.sponsorName})", fontWeight = FontWeight.Bold)
                                        Text("Impressions: ${ad.impressionsCount} • Clicks: ${ad.clicksCount}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    OutlinedButton(onClick = { adminViewModel.toggleAd(ad.id, !ad.active) }) {
                                        Text(if (ad.active) "Pause" else "Activate")
                                    }
                                }
                            }
                        }
                        item {
                            AdminAnnouncementCard(onPublish = { t, m -> adminViewModel.createAnnouncement(t, m) })
                        }
                        items(announcements, key = { "ann_${it.id}" }) { ann ->
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
                                        Text(ann.title, fontWeight = FontWeight.Bold)
                                        Text(ann.message, style = MaterialTheme.typography.bodySmall)
                                    }
                                    OutlinedButton(onClick = { adminViewModel.toggleAnnouncement(ann.id, !ann.active) }) {
                                        Text(if (ann.active) "Hide" else "Show")
                                    }
                                }
                            }
                        }
                        item {
                            AdminRewardsConfigCard(
                                current = appConfig,
                                onSave = { adminViewModel.updateAppConfig(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCreateAdCard(onCreateAd: (String, String, String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var sponsor by remember { mutableStateOf("") }
    var cta by remember { mutableStateOf("Learn More") }
    var url by remember { mutableStateOf("https://") }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Create Sponsored Ad", fontWeight = FontWeight.Bold)
            OutlinedTextField(value = sponsor, onValueChange = { sponsor = it }, label = { Text("Sponsor Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Ad Headline") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = cta, onValueChange = { cta = it }, label = { Text("CTA Text") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("CTA URL") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(
                onClick = {
                    onCreateAd(title, desc, sponsor, cta, url)
                    title = ""
                    desc = ""
                    sponsor = ""
                },
                enabled = title.isNotBlank() && desc.isNotBlank() && sponsor.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Publish Sponsored Ad")
            }
        }
    }
}

@Composable
private fun AdminAnnouncementCard(onPublish: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Broadcast Platform Announcement", fontWeight = FontWeight.Bold)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    onPublish(title, message)
                    title = ""
                    message = ""
                },
                enabled = title.isNotBlank() && message.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Announcement")
            }
        }
    }
}

@Composable
private fun AdminRewardsConfigCard(
    current: AppConfig,
    onSave: (AppConfig) -> Unit
) {
    var reward by remember(current) { mutableStateOf(current.referralRewardInr.toString()) }
    var minWd by remember(current) { mutableStateOf(current.minWithdrawalInr.toString()) }
    var minFriends by remember(current) { mutableStateOf(current.minFriendsForMonetization.toString()) }
    var minPosts by remember(current) { mutableStateOf(current.minPostsForMonetization.toString()) }
    var minViews by remember(current) { mutableStateOf(current.minViewsForMonetization.toString()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Referral Rewards & Monetization Config", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = reward,
                    onValueChange = { reward = it },
                    label = { Text("Referral Reward (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = minWd,
                    onValueChange = { minWd = it },
                    label = { Text("Min Withdrawal (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minFriends,
                    onValueChange = { minFriends = it },
                    label = { Text("Min Friends") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = minPosts,
                    onValueChange = { minPosts = it },
                    label = { Text("Min Posts") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = minViews,
                    onValueChange = { minViews = it },
                    label = { Text("Min Views") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = {
                    onSave(
                        AppConfig(
                            referralRewardInr = reward.toDoubleOrNull() ?: current.referralRewardInr,
                            minWithdrawalInr = minWd.toDoubleOrNull() ?: current.minWithdrawalInr,
                            minFriendsForMonetization = minFriends.toLongOrNull() ?: current.minFriendsForMonetization,
                            minPostsForMonetization = minPosts.toLongOrNull() ?: current.minPostsForMonetization,
                            minViewsForMonetization = minViews.toLongOrNull() ?: current.minViewsForMonetization
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Platform Config")
            }
        }
    }
}
