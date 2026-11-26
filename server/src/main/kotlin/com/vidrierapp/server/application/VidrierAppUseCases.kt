package com.vidrierapp.server.application

import com.vidrierapp.server.application.port.GeocodingGateway
import com.vidrierapp.server.application.port.PaymentGateway
import com.vidrierapp.server.application.port.PlatformRepository
import com.vidrierapp.server.model.CreateShopRequest
import com.vidrierapp.server.service.Bounds
import com.vidrierapp.server.service.CategoryNormalizer
import java.util.UUID

class VidrierAppUseCases(
    private val repository: PlatformRepository,
    private val categoryNormalizer: CategoryNormalizer,
    private val paymentGateway: PaymentGateway,
    private val geocodingGateway: GeocodingGateway,
) {
    data class SubmittedVote(val id: UUID, val category: String, val classified: Boolean)

    fun shops(category: String?, bounds: Bounds?) = repository.shops(category, bounds)
    fun shopDetail(id: UUID, deviceId: String?) = repository.shopDetail(id, repository.hasActiveAccess(deviceId))
    fun submitVote(shopId: UUID, category: String?, otherCategory: String?, frequency: String?, fingerprint: String, browserToken: String, ipAddress: String): SubmittedVote {
        val normalized = categoryNormalizer.normalize(category, otherCategory)
        val id = repository.submitVote(shopId, normalized, frequency, fingerprint, browserToken, ipAddress)
        return SubmittedVote(id, normalized.slug, normalized.classified)
    }
    fun reportRented(id: UUID) = repository.reportRented(id)
    fun startTrial(deviceId: String) = repository.startTrial(deviceId)
    fun checkoutPass(deviceId: String, paymentToken: String) = repository.checkoutPass(deviceId, paymentGateway, paymentToken)
    fun zoneSummaries() = repository.zoneSummaries()
    fun resolveLocation(placeId: String?, address: String?) = geocodingGateway.resolve(placeId, address)
    fun createShop(request: CreateShopRequest) = repository.createShop(request.also {
        require(geocodingGateway.validate(it.geoResolutionId, it.latitude, it.longitude, it.confirmFarLocation)) {
            "Selected location is more than 150 meters from the resolved address; set confirmFarLocation to override"
        }
    }.copy(locationOverrideUsed = request.locationOverrideUsed || request.confirmFarLocation))
    fun updateShopStatus(id: UUID, status: String) = repository.updateShopStatus(id, status)
    fun runMaintenance() = com.vidrierapp.server.model.MaintenanceRunResponse(repository.expireStaleShops(), repository.expireAccessStatuses(), repository.refreshAggregates())
    fun addCategoryAlias(alias: String, canonicalSlug: String) = repository.addCategoryAlias(alias, canonicalSlug)
    fun unclassifiedCategories() = repository.unclassifiedCategories()
    fun reclassifyVotes(alias: String, canonicalSlug: String): Int {
        val updated = repository.reclassifyVotes(alias, canonicalSlug)
        repository.refreshAggregates()
        return updated
    }
}
