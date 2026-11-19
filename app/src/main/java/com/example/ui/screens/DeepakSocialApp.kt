package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOut
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.data.repository.AdminRepository
import com.example.data.repository.CreatorRepository
import com.example.data.repository.FeedRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.UserRepository
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.CreatorViewModel
import com.example.ui.viewmodel.FeedViewModel
import com.example.ui.viewmodel.SocialViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object DeepakRoutes {
    const val HOME = "home"
    const val FRIENDS = "friends"
    const val REELS = "reels"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val USER_PROFILE = "user_profile/{uid}"
    const val CHATS = "chats"
    const val CHAT_THREAD = "chat_thread/{chatId}"
    const val CREATOR_DASHBOARD = "creator_dashboard"
    const val REFERRALS = "referrals"
    const val SETTINGS = "settings"
    const val ADMIN = "admin"

    fun userProfile(uid: String) = "user_profile/$uid"
    fun chatThread(chatId: String) = "chat_thread/$chatId"
}

private data class NavTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val PrimaryTabs = listOf(
    NavTabItem(DeepakRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    NavTabItem(DeepakRoutes.FRIENDS, "Friends", Icons.Filled.Group, Icons.Outlined.Group, "nav_friends"),
    NavTabItem(DeepakRoutes.REELS, "Reels", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle, "nav_reels"),
    NavTabItem(DeepakRoutes.NOTIFICATIONS, "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications, "nav_notifications"),
    NavTabItem(DeepakRoutes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

@Composable
fun DeepakSocialApp(
    themeMode: ThemeMode,
    onChangeThemeMode: (ThemeMode) -> Unit
) {
    var showSplash by remember { mutableStateOf(true) }
    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser
    )

    if (showSplash) {
        DeepakSplashScreen(onSplashFinished = { showSplash = false })
        return
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        androidx.compose.runtime.key(user.uid) {
            AuthenticatedShell(
                firebaseUser = user,
                themeMode = themeMode,
                onChangeThemeMode = onChangeThemeMode
            )
        }
    }
}

@Composable
private fun AuthenticatedShell(
    firebaseUser: FirebaseUser,
    themeMode: ThemeMode,
    onChangeThemeMode: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val uid = firebaseUser.uid
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val userRepository = remember(uid) { UserRepository(appContext) }
    val feedViewModel: FeedViewModel = viewModel(
        key = "feed_$uid",
        factory = viewModelFactory {
            initializer { FeedViewModel(FeedRepository(appContext), uid) }
        }
    )
    val socialViewModel: SocialViewModel = viewModel(
        key = "social_$uid",
        factory = viewModelFactory {
            initializer { SocialViewModel(userRepository, SocialRepository(appContext), uid) }
        }
    )
    val creatorViewModel: CreatorViewModel = viewModel(
        key = "creator_$uid",
        factory = viewModelFactory {
            initializer { CreatorViewModel(CreatorRepository(appContext), uid) }
        }
    )
    val adminViewModel: AdminViewModel = viewModel(
        key = "admin_$uid",
        factory = viewModelFactory {
            initializer { AdminViewModel(AdminRepository(appContext)) }
        }
    )

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uid) {
        userRepository.ensureUserBootstrapped(firebaseUser)
            .onFailure { err ->
                snackbarHostState.showSnackbar(err.localizedMessage ?: "Failed to initialize profile")
            }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val presenceScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    DisposableEffect(lifecycleOwner, uid) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    presenceScope.launch { userRepository.updatePresence(uid, true) }
                }
                Lifecycle.Event.ON_STOP -> {
                    presenceScope.launch(NonCancellable) { userRepository.updatePresence(uid, false) }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            presenceScope.launch(NonCancellable) { userRepository.updatePresence(uid, false) }
        }
    }

    LaunchedEffect(feedViewModel, socialViewModel, creatorViewModel, adminViewModel) {
        launch { feedViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
        launch { socialViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
        launch { creatorViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
        launch { adminViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
    }

    val myProfileState by socialViewModel.myProfileState.collectAsStateWithLifecycle()
    val privateUser by socialViewModel.privateUserState.collectAsStateWithLifecycle()
    val isAdmin by adminViewModel.isAdmin.collectAsStateWithLifecycle()
    val friendRequests by socialViewModel.friendRequests.collectAsStateWithLifecycle()
    val notificationsState by socialViewModel.notificationsState.collectAsStateWithLifecycle()

    val pendingRequestsCount = remember(friendRequests, uid) {
        friendRequests.count { it.status == "pending" && it.toUid == uid }
    }
    val unreadNotificationsCount = remember(notificationsState) {
        (notificationsState as? UiState.Success)?.data?.count { !it.readStatus } ?: 0
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTopLevelNav = currentRoute in PrimaryTabs.map { it.route }

    when (val profileUi = myProfileState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Setting up your Deepak Social profile...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(profileUi.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Button(onClick = { signOut(credentialManager, scope) }) {
                        Text("Sign Out")
                    }
                }
            }
        }
        is UiState.Success -> {
            val currentUserProfile = profileUi.data ?: PublicProfile(
                uid = uid,
                displayName = firebaseUser.displayName ?: "Creator",
                username = "user_${uid.take(5).lowercase()}"
            )

            if (currentUserProfile.accountStatus == "suspended" || currentUserProfile.accountStatus == "banned") {
                Box(
                    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            Icons.Filled.Block,
                            contentDescription = "Account restricted",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Account ${currentUserProfile.accountStatus.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your account has been restricted by an administrator. Contact Deepak Social support for review.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { signOut(credentialManager, scope) }) {
                            Text("Sign Out")
                        }
                    }
                }
                return
            }

            val blockedIds = privateUser?.blockedUserIds.orEmpty()

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (showTopLevelNav && !useNavigationRail) {
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                PrimaryTabs.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    val badgeCount = when (tab.route) {
                                        DeepakRoutes.FRIENDS -> pendingRequestsCount
                                        DeepakRoutes.NOTIFICATIONS -> unreadNotificationsCount
                                        else -> 0
                                    }
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTopLevel(navController, tab.route) },
                                        icon = {
                                            TabIconWithBadge(
                                                icon = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                label = tab.label,
                                                badgeCount = badgeCount
                                            )
                                        },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (showTopLevelNav && useNavigationRail) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Spacer(modifier = Modifier.height(12.dp))
                                PrimaryTabs.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    val badgeCount = when (tab.route) {
                                        DeepakRoutes.FRIENDS -> pendingRequestsCount
                                        DeepakRoutes.NOTIFICATIONS -> unreadNotificationsCount
                                        else -> 0
                                    }
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateToTopLevel(navController, tab.route) },
                                        icon = {
                                            TabIconWithBadge(
                                                icon = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                label = tab.label,
                                                badgeCount = badgeCount
                                            )
                                        },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = DeepakRoutes.HOME,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            composable(DeepakRoutes.HOME) {
                                HomeFeedScreen(
                                    currentUser = currentUserProfile,
                                    blockedUserIds = blockedIds,
                                    feedViewModel = feedViewModel,
                                    socialViewModel = socialViewModel,
                                    creatorViewModel = creatorViewModel,
                                    onOpenChats = { navController.navigate(DeepakRoutes.CHATS) },
                                    onOpenUserProfile = { targetUid ->
                                        if (targetUid == uid) {
                                            navigateToTopLevel(navController, DeepakRoutes.PROFILE)
                                        } else {
                                            navController.navigate(DeepakRoutes.userProfile(targetUid))
                                        }
                                    }
                                )
                            }

                            composable(DeepakRoutes.FRIENDS) {
                                FriendsScreen(
                                    currentUser = currentUserProfile,
                                    blockedUserIds = blockedIds,
                                    socialViewModel = socialViewModel,
                                    onOpenChat = { chatId ->
                                        navController.navigate(DeepakRoutes.chatThread(chatId))
                                    },
                                    onOpenUserProfile = { targetUid ->
                                        navController.navigate(DeepakRoutes.userProfile(targetUid))
                                    }
                                )
                            }

                            composable(DeepakRoutes.REELS) {
                                ReelsScreen(
                                    currentUser = currentUserProfile,
                                    blockedUserIds = blockedIds,
                                    feedViewModel = feedViewModel,
                                    socialViewModel = socialViewModel,
                                    onOpenUserProfile = { targetUid ->
                                        if (targetUid == uid) {
                                            navigateToTopLevel(navController, DeepakRoutes.PROFILE)
                                        } else {
                                            navController.navigate(DeepakRoutes.userProfile(targetUid))
                                        }
                                    }
                                )
                            }

                            composable(DeepakRoutes.NOTIFICATIONS) {
                                NotificationsScreen(
                                    socialViewModel = socialViewModel,
                                    creatorViewModel = creatorViewModel,
                                    onNotificationClick = { notif ->
                                        when (notif.type) {
                                            "message" -> {
                                                val chatId = notif.targetId
                                                if (!chatId.isNullOrBlank()) {
                                                    navController.navigate(DeepakRoutes.chatThread(chatId))
                                                } else {
                                                    navController.navigate(DeepakRoutes.CHATS)
                                                }
                                            }
                                            "friend_request", "friend_accept", "follow" -> {
                                                navigateToTopLevel(navController, DeepakRoutes.FRIENDS)
                                            }
                                            "monetization", "withdrawal", "referral" -> {
                                                navController.navigate(DeepakRoutes.CREATOR_DASHBOARD)
                                            }
                                            else -> {
                                                navigateToTopLevel(navController, DeepakRoutes.HOME)
                                            }
                                        }
                                    }
                                )
                            }

                            composable(DeepakRoutes.PROFILE) {
                                ProfileScreen(
                                    currentUser = currentUserProfile,
                                    viewedUid = null,
                                    isAdmin = isAdmin,
                                    feedViewModel = feedViewModel,
                                    socialViewModel = socialViewModel,
                                    onBack = null,
                                    onOpenCreatorDashboard = { navController.navigate(DeepakRoutes.CREATOR_DASHBOARD) },
                                    onOpenReferrals = { navController.navigate(DeepakRoutes.REFERRALS) },
                                    onOpenChatInbox = { navController.navigate(DeepakRoutes.CHATS) },
                                    onOpenDirectChat = { chatId -> navController.navigate(DeepakRoutes.chatThread(chatId)) },
                                    onOpenSettings = { navController.navigate(DeepakRoutes.SETTINGS) },
                                    onOpenAdminDashboard = { navController.navigate(DeepakRoutes.ADMIN) }
                                )
                            }

                            composable(
                                route = DeepakRoutes.USER_PROFILE,
                                arguments = listOf(navArgument("uid") { type = NavType.StringType })
                            ) { entry ->
                                val targetUid = entry.arguments?.getString("uid")
                                ProfileScreen(
                                    currentUser = currentUserProfile,
                                    viewedUid = targetUid,
                                    isAdmin = isAdmin,
                                    feedViewModel = feedViewModel,
                                    socialViewModel = socialViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenCreatorDashboard = { navController.navigate(DeepakRoutes.CREATOR_DASHBOARD) },
                                    onOpenReferrals = { navController.navigate(DeepakRoutes.REFERRALS) },
                                    onOpenChatInbox = { navController.navigate(DeepakRoutes.CHATS) },
                                    onOpenDirectChat = { chatId -> navController.navigate(DeepakRoutes.chatThread(chatId)) },
                                    onOpenSettings = { navController.navigate(DeepakRoutes.SETTINGS) },
                                    onOpenAdminDashboard = { navController.navigate(DeepakRoutes.ADMIN) }
                                )
                            }

                            composable(DeepakRoutes.CHATS) {
                                ChatInboxScreen(
                                    currentUser = currentUserProfile,
                                    blockedUserIds = blockedIds,
                                    socialViewModel = socialViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenChat = { chatId ->
                                        navController.navigate(DeepakRoutes.chatThread(chatId))
                                    }
                                )
                            }

                            composable(
                                route = DeepakRoutes.CHAT_THREAD,
                                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                            ) { entry ->
                                val chatId = entry.arguments?.getString("chatId").orEmpty()
                                ChatConversationScreen(
                                    chatId = chatId,
                                    currentUser = currentUserProfile,
                                    socialViewModel = socialViewModel,
                                    onBack = {
                                        socialViewModel.setActiveChat(null)
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable(DeepakRoutes.CREATOR_DASHBOARD) {
                                CreatorDashboardScreen(
                                    currentUser = currentUserProfile,
                                    privateUser = privateUser,
                                    creatorViewModel = creatorViewModel,
                                    socialViewModel = socialViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(DeepakRoutes.REFERRALS) {
                                ReferralScreen(
                                    currentUser = currentUserProfile,
                                    creatorViewModel = creatorViewModel,
                                    onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(DeepakRoutes.SETTINGS) {
                                SettingsAndLegalScreen(
                                    themeMode = themeMode,
                                    onChangeThemeMode = onChangeThemeMode,
                                    privateUser = privateUser,
                                    socialViewModel = socialViewModel,
                                    onSignedOut = {},
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(DeepakRoutes.ADMIN) {
                                AdminDashboardScreen(
                                    adminViewModel = adminViewModel,
                                    creatorViewModel = creatorViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    icon: ImageVector,
    label: String,
    badgeCount: Int
) {
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (badgeCount > 99) "99+" else "$badgeCount")
                }
            }
        ) {
            Icon(icon, contentDescription = label)
        }
    } else {
        Icon(icon, contentDescription = label)
    }
}

private fun navigateToTopLevel(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
