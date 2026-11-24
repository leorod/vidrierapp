package com.vidrierapp.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.automirrored.filled.MenuBook
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

private data class TopLevelTab(
    val destination: NavKey,
    val labelRes: Int,
    val icon: ImageVector
)

private val topLevelTabs = listOf(
    TopLevelTab(MapDestination, R.string.nav_explore, Icons.Filled.Map),
    TopLevelTab(VisitsHubDestination, R.string.nav_visits, Icons.AutoMirrored.Filled.MenuBook)
)

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(MapDestination as NavKey)
    val currentDestination = backStack.lastOrNull()
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
                    }
                )
            }
        }
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
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
                        }
                    )
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

@Composable
private fun AppBottomBar(
    currentDestination: NavKey?,
    onTabSelected: (NavKey) -> Unit
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
