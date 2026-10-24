package com.aghour.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import com.aghour.app.R
import com.aghour.app.ui.account.AccountScreen
import com.aghour.app.ui.account.LoginScreen
import com.aghour.app.ui.account.RegisterScreen
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.explore.ExploreScreen
import com.aghour.app.ui.firms.FirmDetailScreen
import com.aghour.app.ui.home.HomeScreen
import com.aghour.app.ui.news.ArticleScreen
import com.aghour.app.ui.news.NewsScreen
import com.aghour.app.ui.owner.ChangePasswordScreen
import com.aghour.app.ui.owner.MyFirmsScreen
import com.aghour.app.ui.owner.NotificationsScreen
import com.aghour.app.ui.owner.PaymentsScreen
import com.aghour.app.ui.owner.PayScreen
import com.aghour.app.ui.owner.PlansScreen
import com.aghour.app.ui.theme.Cream
import com.aghour.app.ui.theme.Forest
import com.aghour.app.ui.theme.Gold
import com.aghour.app.ui.theme.Sand
import com.aghour.app.ui.welcome.WelcomeScreen
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

private data class Tab(val route: Any, val labelRes: Int, val icon: ImageVector, val matches: (NavDestination) -> Boolean)

private val tabs = listOf(
    Tab(HomeRoute, R.string.tab_home, Icons.Filled.Home) { it.hasRoute<HomeRoute>() },
    Tab(ExploreRoute, R.string.tab_explore, Icons.Filled.Search) { it.hasRoute<ExploreRoute>() },
    Tab(NewsRoute, R.string.tab_news, Icons.AutoMirrored.Filled.List) { it.hasRoute<NewsRoute>() },
    Tab(AccountRoute, R.string.tab_account, Icons.Filled.Person) { it.hasRoute<AccountRoute>() },
)

@Composable
fun AppNavigation() {
    val container = appContainer()
    val welcomeDone by container.session.welcomeDone.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Welcome is shown once, on top of the app (not part of the back stack, so Home stays the
    // start destination and the bottom tabs always pop back to it correctly).
    // The splash screen stays up until the flag is read from disk.
    var showWelcome by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var openLoginAfterWelcome by rememberSaveable { mutableStateOf(false) }
    if (showWelcome == null) {
        val done = welcomeDone ?: return Box(Modifier.fillMaxSize())
        showWelcome = !done
    }
    // Soft cross-fade from the welcome artwork into the app.
    Crossfade(targetState = showWelcome == true, animationSpec = tween(450), label = "welcome") { welcome ->
        if (welcome) {
            WelcomeScreen(
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
        } else {
            MainNavigation(openLogin = openLoginAfterWelcome, onLoginOpened = { openLoginAfterWelcome = false })
        }
    }
}

@Composable
private fun MainNavigation(openLogin: Boolean, onLoginOpened: () -> Unit) {
    val container = appContainer()
    val user by container.auth.currentUser.collectAsStateWithLifecycle()
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

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            if (currentTab != null) {
                Column {
                    HorizontalDivider(color = Sand)
                    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = tab == currentTab,
                                onClick = { nav.navigateToTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(stringResource(tab.labelRes), fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Gold.copy(alpha = 0.15f),
                                    selectedIconColor = Forest,
                                    selectedTextColor = Gold,
                                    unselectedIconColor = Forest.copy(alpha = 0.4f),
                                    unselectedTextColor = Forest.copy(alpha = 0.4f),
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
                    onAddFirm = { nav.go(PlansRoute) },
                )
            }
            composable<ExploreRoute> { ExploreScreen(onFirmClick = { nav.go(FirmRoute(it.id)) }) }
            composable<NewsRoute> { NewsScreen(onArticleClick = { nav.go(ArticleRoute(it.id)) }) }
            composable<AccountRoute> {
                AccountScreen(
                    onLogin = { nav.go(LoginRoute) },
                    onRegister = { nav.go(RegisterRoute) },
                    onPlans = { nav.go(PlansRoute) },
                    onPayments = { nav.go(PaymentsRoute) },
                    onNotifications = { nav.go(NotificationsRoute) },
                    onChangePassword = { nav.go(ChangePasswordRoute) },
                    onMyFirms = { nav.go(MyFirmsRoute) },
                )
            }

            // ---- Details ----
            composable<FirmRoute> { entry ->
                FirmDetailScreen(firmId = entry.toRoute<FirmRoute>().firmId, onBack = { nav.back() })
            }
            composable<ArticleRoute> { entry ->
                ArticleScreen(articleId = entry.toRoute<ArticleRoute>().articleId, onBack = { nav.back() })
            }

            // ---- Firm owners ----
            composable<PlansRoute> {
                PlansScreen(
                    onBack = { nav.back() },
                    // Paying needs an account: guests sign in first, then come back here.
                    onSubscribe = { plan -> if (user == null) nav.go(LoginRoute) else nav.go(PayRoute(plan.id)) },
                )
            }
            composable<PayRoute> { entry ->
                PayScreen(
                    planId = entry.toRoute<PayRoute>().planId,
                    onBack = { nav.back() },
                    onDone = { nav.go(PaymentsRoute) { popUpTo<PlansRoute> { inclusive = true } } },
                )
            }
            composable<PaymentsRoute> { PaymentsScreen(onBack = { nav.back() }) }
            composable<NotificationsRoute> { NotificationsScreen(onBack = { nav.back() }) }
            composable<ChangePasswordRoute> { ChangePasswordScreen(onBack = { nav.back() }) }
            composable<MyFirmsRoute> { MyFirmsScreen(onBack = { nav.back() }, onFirmClick = { nav.go(FirmRoute(it.id)) }) }
        }
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
