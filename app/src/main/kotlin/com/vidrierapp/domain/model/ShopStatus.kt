package com.vidrierapp.domain.model

enum class ShopStatus {
    ACTIVE,
    UNDER_REVIEW,
    RENTED;

    companion object {
        fun fromRaw(raw: String?): ShopStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}
