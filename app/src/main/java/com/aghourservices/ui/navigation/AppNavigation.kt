package com.aghourservices.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.aghourservices.R
import com.aghourservices.data.remote.isSuperAdmin
import com.aghourservices.ui.account.AccountScreen
import com.aghourservices.ui.account.LoginScreen
import com.aghourservices.ui.account.RegisterScreen
import com.aghourservices.ui.admin.FirmFormScreen
import com.aghourservices.ui.components.appContainer
import com.aghourservices.ui.components.MessageBox
import com.aghourservices.ui.components.ScreenTitle
import com.aghourservices.ui.explore.ExploreScreen
import com.aghourservices.ui.firms.FirmDetailScreen
import com.aghourservices.ui.home.HomeScreen
import com.aghourservices.ui.news.ArticleScreen
import com.aghourservices.ui.news.NewsScreen
import com.aghourservices.ui.owner.ChangePasswordScreen
import com.aghourservices.ui.owner.MyFirmsScreen
import com.aghourservices.ui.owner.NotificationTarget
import com.aghourservices.ui.owner.NotificationsScreen
import com.aghourservices.ui.owner.PaymentsScreen
import com.aghourservices.ui.owner.PayScreen
import com.aghourservices.ui.owner.PlansScreen
import com.aghourservices.ui.settings.SettingsScreen
import com.aghourservices.ui.theme.Aghour
import com.aghourservices.ui.welcome.LaunchScreen
import com.aghourservices.ui.welcome.WelcomeScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// ---- Type-safe routes ----
@Serializable data object LoginRoute
@Serializable data object RegisterRoute

// Bottom-bar tabs
@Serializable data object HomeRoute
@Serializable data object ExploreRoute
@Serializable data object NewsRoute
@Serializable data object AccountRoute

// Pushed screens
@Serializable data class FirmRoute(val firmId: Long)
@Serializable data class ArticleRoute(val articleId: Long)
@Serializable data object PlansRoute
@Serializable data class PayRoute(val planId: Long)
@Serializable data object PaymentsRoute
@Serializable data object NotificationsRoute
@Serializable data object ChangePasswordRoute
@Serializable data object MyFirmsRoute
@Serializable data object SettingsRoute

// Super admin
@Serializable data object NewFirmRoute
@Serializable data class EditFirmRoute(val firmId: Long)

/** Filled icon on the selected tab, outlined on the others; the gold label also marks the selection. */
private data class Tab(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
    val matches: (NavDestination) -> Boolean,
)

private val tabs = listOf(
    Tab(HomeRoute, R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home) { it.hasRoute<HomeRoute>() },
    Tab(ExploreRoute, R.string.tab_explore, Icons.Filled.Search, Icons.Outlined.Search) { it.hasRoute<ExploreRoute>() },
    Tab(NewsRoute, R.string.tab_news, Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List) { it.hasRoute<NewsRoute>() },
    Tab(AccountRoute, R.string.tab_account, Icons.Filled.Person, Icons.Outlined.Person) { it.hasRoute<AccountRoute>() },
)

private enum class Stage { Launch, Welcome, Main }

/** Minimum time the logo stays up after the system splash, so the name can be read. */
private const val LAUNCH_MS = 1200L

@Composable
fun AppNavigation() {
    val container = appContainer()
    val welcomeDone by container.session.welcomeDone.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Launch logo once per app start (not again after rotation or returning from the background).
    var launchDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!launchDone) {
            delay(LAUNCH_MS)
            launchDone = true
        }
    }

    // Welcome is shown once, on top of the app (not part of the back stack, so Home stays the
    // start destination and the bottom tabs always pop back to it correctly).
    var showWelcome by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var openLoginAfterWelcome by rememberSaveable { mutableStateOf(false) }
    if (showWelcome == null) welcomeDone?.let { showWelcome = !it }

    val stage = when {
        !launchDone || showWelcome == null -> Stage.Launch
        showWelcome == true -> Stage.Welcome
        else -> Stage.Main
    }
    // Soft cross-fades: logo -> welcome artwork (first launch) or straight into the app.
    Crossfade(targetState = stage, animationSpec = tween(450), label = "stage") { current ->
        when (current) {
            Stage.Launch -> LaunchScreen()
            Stage.Welcome -> WelcomeScreen(
                onStart = {
                    scope.launch { container.session.setWelcomeDone() }
                    showWelcome = false
                },
                onLogin = {
                    scope.launch { container.session.setWelcomeDone() }
                    openLoginAfterWelcome = true
                    showWelcome = false
                },
            )
            Stage.Main -> MainNavigation(openLogin = openLoginAfterWelcome, onLoginOpened = { openLoginAfterWelcome = false })
        }
    }
}

@Composable
private fun MainNavigation(openLogin: Boolean, onLoginOpened: () -> Unit) {
    val container = appContainer()
    val user by container.auth.currentUser.collectAsStateWithLifecycle()
    // Server feature flag (dashboard): plans, subscribing and payment history. null = still loading.
    val plansFlag by container.features.plansEnabled.collectAsStateWithLifecycle()
    val plansOn = plansFlag == true
    // Shows the admin controls only; the server's policies still decide every request.
    val isAdmin = user.isSuperAdmin
    val nav = rememberNavController()
    LaunchedEffect(openLogin) {
        if (openLogin) {
            onLoginOpened()
            nav.navigate(LoginRoute)
        }
    }
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val currentTab = destination?.let { d -> tabs.firstOrNull { it.matches(d) } }
    val c = Aghour.colors

    Scaffold(
        containerColor = c.page,
        contentColor = c.ink,
        bottomBar = {
            if (currentTab != null) {
                Column {
                    HorizontalDivider(color = c.line)
                    NavigationBar(containerColor = c.surface, tonalElevation = 0.dp) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = tab == currentTab,
                                onClick = { nav.navigateToTab(tab.route) },
                                icon = { Icon(if (tab == currentTab) tab.selectedIcon else tab.icon, contentDescription = null) },
                                label = { Text(stringResource(tab.labelRes), fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent,
                                    selectedIconColor = c.goldInk,
                                    selectedTextColor = c.goldInk,
                                    unselectedIconColor = c.muted,
                                    unselectedTextColor = c.muted,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
        NavHost(
            navController = nav,
            startDestination = HomeRoute,
            modifier = Modifier.padding(bottom).consumeWindowInsets(bottom),
            // Tabs cross-fade; pushed screens slide in from the reading side (right-to-left aware) and
            // slide back on Back. Short distance + fade = smooth, no full-width jumps.
            enterTransition = { if (isTabSwitch()) fadeIn(tween(TAB_MS)) else pushEnter() },
            exitTransition = { if (isTabSwitch()) fadeOut(tween(TAB_MS)) else pushExit() },
            popEnterTransition = { if (isTabSwitch()) fadeIn(tween(TAB_MS)) else popEnter() },
            popExitTransition = { if (isTabSwitch()) fadeOut(tween(TAB_MS)) else popExit() },
        ) {
            // ---- Auth ----
            composable<LoginRoute> {
                LoginScreen(
                    onBack = { nav.back() },
                    onDone = { nav.afterAuth() },
                    onGoToRegister = { nav.go(RegisterRoute) { popUpTo<LoginRoute> { inclusive = true } } },
                )
            }
            composable<RegisterRoute> {
                RegisterScreen(
                    onBack = { nav.back() },
                    onDone = { nav.afterAuth() },
                    onGoToLogin = { nav.go(LoginRoute) { popUpTo<RegisterRoute> { inclusive = true } } },
                )
            }

            // ---- Tabs ----
            composable<HomeRoute> {
                HomeScreen(
                    onCategoryClick = { category ->
                        container.exploreCategory.value = category.id
                        nav.navigateToTab(ExploreRoute)
                    },
                    onExplore = { nav.navigateToTab(ExploreRoute) },
                    onFirmClick = { nav.go(FirmRoute(it.id)) },
                    onArticleClick = { nav.go(ArticleRoute(it.id)) },
                    onSeeAllNews = { nav.navigateToTab(NewsRoute) },
                    onAddFirm = if (plansOn) ({ nav.go(PlansRoute) }) else null,
                    onNotifications = { nav.go(NotificationsRoute) },
                )
            }
            composable<ExploreRoute> { ExploreScreen(onFirmClick = { nav.go(FirmRoute(it.id)) }) }
            composable<NewsRoute> { NewsScreen(onArticleClick = { nav.go(ArticleRoute(it.id)) }) }
            composable<AccountRoute> {
                AccountScreen(
                    onLogin = { nav.go(LoginRoute) },
                    onRegister = { nav.go(RegisterRoute) },
                    onPlans = if (plansOn) ({ nav.go(PlansRoute) }) else null,
                    onPayments = if (plansOn) ({ nav.go(PaymentsRoute) }) else null,
                    onNotifications = { nav.go(NotificationsRoute) },
                    onChangePassword = { nav.go(ChangePasswordRoute) },
                    onMyFirms = { nav.go(MyFirmsRoute) },
                    onSettings = { nav.go(SettingsRoute) },
                    onAddFirm = if (isAdmin) ({ nav.go(NewFirmRoute) }) else null,
                )
            }

            // ---- Details ----
            composable<FirmRoute> { entry ->
                val firmId = entry.toRoute<FirmRoute>().firmId
                FirmDetailScreen(
                    firmId = firmId,
                    onBack = { nav.back() },
                    onEdit = if (isAdmin) ({ nav.go(EditFirmRoute(firmId)) }) else null,
                )
            }
            composable<ArticleRoute> { entry ->
                ArticleScreen(articleId = entry.toRoute<ArticleRoute>().articleId, onBack = { nav.back() })
            }

            // ---- Firm owners ----
            // Behind the plans flag. Their entry points are hidden while it is off; the gate covers a
            // screen that was already open when the flag was switched off.
            composable<PlansRoute> {
                PlansGate(plansFlag, onBack = { nav.back() }) {
                    PlansScreen(
                        onBack = { nav.back() },
                        // Paying needs an account: guests sign in first, then come back here.
                        onSubscribe = { plan -> if (user == null) nav.go(LoginRoute) else nav.go(PayRoute(plan.id)) },
                    )
                }
            }
            composable<PayRoute> { entry ->
                PlansGate(plansFlag, onBack = { nav.back() }) {
                    PayScreen(
                        planId = entry.toRoute<PayRoute>().planId,
                        onBack = { nav.back() },
                        onDone = { nav.go(PaymentsRoute) { popUpTo<PlansRoute> { inclusive = true } } },
                    )
                }
            }
            composable<PaymentsRoute> {
                PlansGate(plansFlag, onBack = { nav.back() }) { PaymentsScreen(onBack = { nav.back() }) }
            }
            composable<NotificationsRoute> {
                NotificationsScreen(
                    onBack = { nav.back() },
                    isAvailable = { target ->
                        plansOn || (target != NotificationTarget.Plans && target != NotificationTarget.Payments)
                    },
                    onOpen = { target ->
                        when (target) {
                            NotificationTarget.Payments -> nav.go(PaymentsRoute)
                            NotificationTarget.Plans -> nav.go(PlansRoute)
                            NotificationTarget.MyFirms -> nav.go(MyFirmsRoute)
                            NotificationTarget.News -> nav.navigateToTab(NewsRoute)
                        }
                    },
                )
            }
            composable<ChangePasswordRoute> { ChangePasswordScreen(onBack = { nav.back() }) }
            composable<MyFirmsRoute> { MyFirmsScreen(onBack = { nav.back() }, onFirmClick = { nav.go(FirmRoute(it.id)) }) }
            composable<SettingsRoute> { SettingsScreen(onBack = { nav.back() }) }

            // ---- Super admin ----
            composable<NewFirmRoute> {
                FirmFormScreen(
                    firmId = null,
                    onBack = { nav.back() },
                    // Open the new firm in place of the form, so Back returns to the profile.
                    onSaved = { id -> nav.go(FirmRoute(id)) { popUpTo<NewFirmRoute> { inclusive = true } } },
                )
            }
            composable<EditFirmRoute> { entry ->
                FirmFormScreen(firmId = entry.toRoute<EditFirmRoute>().firmId, onBack = { nav.back() }, onSaved = { nav.back() })
            }
        }
    }
}

/**
 * Shows [content] unless the plans flag is known to be off; then a short "not available" page with
 * a back button. A message rather than an automatic pop: popping during the enter transition would
 * be ignored by the safe-navigation helpers below and leave a blank screen.
 */
@Composable
private fun PlansGate(enabled: Boolean?, onBack: () -> Unit, content: @Composable () -> Unit) {
    if (enabled == false) {
        Column(Modifier.fillMaxSize().background(Aghour.colors.page)) {
            ScreenTitle(stringResource(R.string.subscription_and_plans), onBack = onBack)
            MessageBox(stringResource(R.string.feature_unavailable))
        }
    } else {
        content()
    }
}

// ---- Safe navigation ----
//
// Taps that arrive while a screen is still animating in/out (double taps, fast back presses)
// used to push the same screen twice or pop the last screen and leave a blank page.
// Only the screen that is fully visible (RESUMED) may navigate.

private fun NavHostController.isIdle(): Boolean =
    currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

/** Navigate once, ignoring repeated taps during transitions. */
fun NavHostController.go(route: Any, builder: NavOptionsBuilder.() -> Unit = {}) {
    if (isIdle()) navigate(route, builder)
}

/** Back that never empties the stack. */
fun NavHostController.back() {
    if (isIdle() && previousBackStackEntry != null) popBackStack()
}

/** Bottom-bar behaviour: one copy per tab, state restored when switching back, Back returns to Home. */
fun NavHostController.navigateToTab(route: Any) {
    if (!isIdle()) return
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** After login/register: return to the screen that asked for it (Account, Plans, or Home). */
private fun NavHostController.afterAuth() {
    if (previousBackStackEntry != null) {
        popBackStack()
    } else {
        navigate(HomeRoute)
    }
}

// ---- Transitions ----

private const val NAV_MS = 320
private const val TAB_MS = 220
private val navEasing = FastOutSlowInEasing

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    tabs.any { it.matches(initialState.destination) } && tabs.any { it.matches(targetState.destination) }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pushEnter(): EnterTransition =
    slideIntoContainer(SlideDirection.Start, tween(NAV_MS, easing = navEasing)) { it / 4 } + fadeIn(tween(NAV_MS))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pushExit(): ExitTransition =
    slideOutOfContainer(SlideDirection.Start, tween(NAV_MS, easing = navEasing)) { it / 8 } + fadeOut(tween(NAV_MS / 2))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    slideIntoContainer(SlideDirection.End, tween(NAV_MS, easing = navEasing)) { it / 8 } + fadeIn(tween(NAV_MS))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition =
    slideOutOfContainer(SlideDirection.End, tween(NAV_MS, easing = navEasing)) { it / 4 } + fadeOut(tween(NAV_MS / 2))
