package com.vidrierapp.domain.model

const val SHOP_CACHE_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000

data class CachedShop(
    val shop: Shop,
    val savedAt: Long
)
