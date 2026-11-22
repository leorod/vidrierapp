package com.vidrierapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.vidrierapp.presentation.detail.ShopDetailScreen
import com.vidrierapp.presentation.map.MapScreen
import com.vidrierapp.presentation.visit.LogVisitScreen

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(MapDestination as NavKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<MapDestination> {
                MapScreen(
                    onShopClick = { shopId ->
                        backStack.add(ShopDetailDestination(shopId))
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
