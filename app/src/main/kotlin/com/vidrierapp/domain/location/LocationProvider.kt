package com.vidrierapp.domain.location

import com.vidrierapp.domain.model.GeoPoint

interface LocationProvider {
    suspend fun currentLocation(): GeoPoint?
}
