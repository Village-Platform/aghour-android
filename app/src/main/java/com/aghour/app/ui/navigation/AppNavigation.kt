package com.aghour.app.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.aghour.app.R
import com.aghour.app.ui.account.AccountScreen
import com.aghour.app.ui.account.LoginScreen
import com.aghour.app.ui.account.RegisterScreen
import com.aghour.app.ui.firms.FirmDetailScreen
import com.aghour.app.ui.firms.FirmsScreen
import com.aghour.app.ui.home.HomeScreen
import com.aghour.app.ui.news.ArticleScreen
import com.aghour.app.ui.news.NewsScreen
import com.aghour.app.ui.search.SearchScreen
import com.aghour.app.ui.theme.Gold100
import com.aghour.app.ui.theme.Green900
import com.aghour.app.ui.theme.Muted
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

// ---- Type-safe routes ----
@Serializable data object HomeRoute
@Serializable data object NewsRoute
@Serializable data object AccountRoute
@Serializable data object LoginRoute
@Serializable data object RegisterRoute
@Serializable data object SearchRoute
@Serializable data class FirmsRoute(val categoryId: Long, val categoryName: String)
@Serializable data class FirmRoute(val firmId: Long)
@Serializable data class ArticleRoute(val articleId: Long)

private data class Tab(
    val route: Any,
    val labelRes: Int,
    val icon: ImageVector,
    val matches: (NavDestination) -> Boolean,
)

private val tabs = listOf(
    Tab(HomeRoute, R.string.tab_home, Icons.Filled.Home) { it.hasRoute<HomeRoute>() },
    Tab(NewsRoute, R.string.tab_news, Icons.AutoMirrored.Filled.List) { it.hasRoute<NewsRoute>() },
    Tab(AccountRoute, R.string.tab_account, Icons.Filled.Person) { it.hasRoute<AccountRoute>() },
)

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBottomBar = destination != null && tabs.any { it.matches(destination) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = destination != null && tab.matches(destination),
                            onClick = { nav.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Gold100,
                                selectedIconColor = Green900,
                                selectedTextColor = Green900,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = HomeRoute,
            // Inner screens have their own top bars; only keep space for the bottom bar.
            modifier = Modifier
                .padding(bottom = padding.calculateBottomPadding())
                .consumeWindowInsets(PaddingValues(bottom = padding.calculateBottomPadding())),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onCategoryClick = { nav.navigate(FirmsRoute(it.id, it.name)) },
                    onArticleClick = { nav.navigate(ArticleRoute(it.id)) },
                    onSeeAllNews = { nav.navigateToTab(NewsRoute) },
                    onSearchClick = { nav.navigate(SearchRoute) },
                )
            }
            composable<SearchRoute> {
                SearchScreen(onBack = { nav.popBackStack() }, onFirmClick = { nav.navigate(FirmRoute(it.id)) })
            }
            composable<NewsRoute> {
                NewsScreen(onArticleClick = { nav.navigate(ArticleRoute(it.id)) })
            }
            composable<AccountRoute> {
                AccountScreen(onLogin = { nav.navigate(LoginRoute) }, onRegister = { nav.navigate(RegisterRoute) })
            }
            composable<FirmsRoute> { entry ->
                val route = entry.toRoute<FirmsRoute>()
                FirmsScreen(
                    categoryId = route.categoryId,
                    categoryName = route.categoryName,
                    onBack = { nav.popBackStack() },
                    onFirmClick = { nav.navigate(FirmRoute(it.id)) },
                )
            }
            composable<FirmRoute> { entry ->
                FirmDetailScreen(firmId = entry.toRoute<FirmRoute>().firmId, onBack = { nav.popBackStack() })
            }
            composable<ArticleRoute> { entry ->
                ArticleScreen(articleId = entry.toRoute<ArticleRoute>().articleId, onBack = { nav.popBackStack() })
            }
            composable<LoginRoute> {
                LoginScreen(
                    onBack = { nav.popBackStack() },
                    onDone = { nav.popBackStack(AccountRoute, inclusive = false) },
                    onGoToRegister = { nav.navigate(RegisterRoute) { popUpTo<LoginRoute> { inclusive = true } } },
                )
            }
            composable<RegisterRoute> {
                RegisterScreen(
                    onBack = { nav.popBackStack() },
                    onDone = { nav.popBackStack(AccountRoute, inclusive = false) },
                    onGoToLogin = { nav.navigate(LoginRoute) { popUpTo<RegisterRoute> { inclusive = true } } },
                )
            }
        }
    }
}

/** Standard bottom-navigation behaviour: one copy per tab, state restored when switching back. */
private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
