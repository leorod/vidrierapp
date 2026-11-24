package com.vidrierapp.presentation.map

import com.vidrierapp.domain.location.LocationProvider
import com.vidrierapp.domain.model.GeoPoint

class FakeLocationProvider(var location: GeoPoint?) : LocationProvider {
    var calls = 0

    override suspend fun currentLocation(): GeoPoint? {
        calls += 1
        return location
    }
}
