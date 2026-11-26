package com.vidrierapp.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidrierapp.presentation.common.AppViewModelProvider
import com.vidrierapp.presentation.common.NetworkStatusViewModel
import com.vidrierapp.presentation.common.OfflineBannerSlot
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.vidrierapp.R
import com.vidrierapp.presentation.detail.ShopDetailScreen
import com.vidrierapp.presentation.map.MapScreen
import com.vidrierapp.presentation.visit.LogVisitScreen
import com.vidrierapp.presentation.visit.MyVisitsHubScreen
import com.vidrierapp.presentation.auth.AuthViewModel
import com.vidrierapp.presentation.auth.LoginScreen
import com.vidrierapp.presentation.profile.ProfileScreen

private data class TopLevelTab(
    val destination: NavKey,
    val labelRes: Int,
    val icon: ImageVector
)

private val topLevelTabs = listOf(
    TopLevelTab(MapDestination, R.string.nav_explore, Icons.Filled.Map),
    TopLevelTab(VisitsHubDestination, R.string.nav_visits, Icons.AutoMirrored.Filled.MenuBook),
    TopLevelTab(ProfileDestination, R.string.nav_profile, Icons.Filled.Person),
)

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val session by authViewModel.session.collectAsState()
    val loginState by authViewModel.loginState.collectAsState()
    if (session == null) {
        LoginScreen(loginState = loginState, onLogin = authViewModel::login)
        return
    }
    val backStack = rememberNavBackStack(MapDestination as NavKey)
    val currentDestination = backStack.lastOrNull()
    val networkStatusViewModel: NetworkStatusViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val isOffline by networkStatusViewModel.isOffline.collectAsState()
    val showBanner = isOffline && currentDestination != VisitsHubDestination
    val showBottomBar = topLevelTabs.any { it.destination == currentDestination }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentDestination = currentDestination,
                    onTabSelected = { destination ->
                        backStack.clear()
                        backStack.add(destination)
                    },
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
        OfflineBannerSlot(
            visible = showBanner,
            onVisitsClick = {
                backStack.clear()
                backStack.add(VisitsHubDestination)
            },
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (showBanner) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier
                )
        ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                entry<MapDestination> {
                    MapScreen(
                        onShopClick = { shopId ->
                            backStack.add(ShopDetailDestination(shopId))
                        }
                    )
                }
                entry<VisitsHubDestination> {
                    MyVisitsHubScreen(
                        onNewVisitClick = {
                            backStack.add(LogVisitDestination(NO_SHOP_ID))
                        },
                        onSavedShopClick = { shopId ->
                            backStack.add(ShopDetailDestination(shopId))
                        }
                    )
                }
                entry<ProfileDestination> {
                    ProfileScreen(username = session?.username.orEmpty(), onLogout = authViewModel::logout)
                }
                entry<ShopDetailDestination> { destination ->
                    ShopDetailScreen(
                        shopId = destination.shopId,
                        onBackClick = { backStack.removeLastOrNull() },
                        onLogVisitClick = { shopId ->
                            backStack.add(LogVisitDestination(shopId))
                        }
                    )
                }
                entry<LogVisitDestination> { destination ->
                    LogVisitScreen(
                        shopId = destination.shopId,
                        onBackClick = { backStack.removeLastOrNull() }
                    )
                }
            }
        )
        }
        }
    }
}

@Composable
internal fun AppBottomBar(
    currentDestination: NavKey?,
    onTabSelected: (NavKey) -> Unit,
) {
    NavigationBar {
        topLevelTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentDestination == tab.destination,
                onClick = {
                    if (currentDestination != tab.destination) {
                        onTabSelected(tab.destination)
                    }
                },
                icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                label = { Text(text = stringResource(tab.labelRes)) }
            )
        }
    }
}
