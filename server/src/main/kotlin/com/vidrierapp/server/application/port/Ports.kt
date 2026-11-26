package com.vidrierapp.server.application.port

import com.vidrierapp.server.auth.AuthUser
import com.vidrierapp.server.model.CreateShopRequest
import com.vidrierapp.server.model.FrequencyMetricResponse
import com.vidrierapp.server.model.HourlyMetricResponse
import com.vidrierapp.server.model.MockCheckoutResponse
import com.vidrierapp.server.model.ReportRentedResponse
import com.vidrierapp.server.model.ShopDetailResponse
import com.vidrierapp.server.model.ShopSummaryResponse
import com.vidrierapp.server.model.TrialResponse
import com.vidrierapp.server.model.UnclassifiedCategoryResponse
import com.vidrierapp.server.model.ZoneSummaryResponse
import com.vidrierapp.server.service.Bounds
import com.vidrierapp.server.service.NormalizedCategory
import java.util.UUID

interface UserAccountRepository {
    fun findAuthUser(username: String): AuthUser?
}

interface CategoryAliasRepository {
    fun categoryAliases(): Map<String, String>
}

object EmptyCategoryAliasRepository : CategoryAliasRepository {
    override fun categoryAliases(): Map<String, String> = emptyMap()
}

interface PaymentGateway {
    fun charge(paymentToken: String): PaymentDecision
    fun providerReference(): String
}

data class PaymentDecision(val approved: Boolean, val failureReason: String? = null)

interface GeocodingGateway {
    fun resolve(placeId: String?, address: String?): com.vidrierapp.server.model.GeoResolveResponse
    fun validate(resolutionId: String?, latitude: Double, longitude: Double, confirmFarLocation: Boolean): Boolean
}

interface PlatformRepository : UserAccountRepository, CategoryAliasRepository {
    fun shops(category: String?, bounds: Bounds?): List<ShopSummaryResponse>
    fun shopDetail(id: UUID, metricsAllowed: Boolean): ShopDetailResponse
    fun submitVote(shopId: UUID, category: NormalizedCategory, frequency: String?, fingerprint: String, browserToken: String, ipAddress: String): UUID
    fun reportRented(id: UUID): ReportRentedResponse
    fun startTrial(username: String): TrialResponse
    fun checkoutPass(username: String, processor: PaymentGateway, paymentToken: String): MockCheckoutResponse
    fun hasActiveAccess(username: String): Boolean
    fun createShop(request: CreateShopRequest): ShopSummaryResponse
    fun addCategoryAlias(alias: String, canonicalSlug: String): Int
    fun reclassifyVotes(alias: String, canonicalSlug: String): Int
    fun unclassifiedCategories(): List<UnclassifiedCategoryResponse>
    fun updateShopStatus(id: UUID, status: String)
    fun expireStaleShops(): Int
    fun expireAccessStatuses(): Int
    fun refreshAggregates(): Boolean
    fun zoneSummaries(): List<ZoneSummaryResponse>
}
