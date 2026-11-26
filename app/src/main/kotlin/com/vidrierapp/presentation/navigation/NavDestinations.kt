package com.vidrierapp.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object MapDestination : NavKey

@Serializable
data class ShopDetailDestination(val shopId: String) : NavKey

@Serializable
data class LogVisitDestination(val shopId: String) : NavKey

@Serializable
data object VisitsHubDestination : NavKey

@Serializable
data object ProfileDestination : NavKey

const val NO_SHOP_ID = ""
