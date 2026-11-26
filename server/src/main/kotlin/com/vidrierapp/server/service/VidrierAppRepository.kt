package com.vidrierapp.server.service

import com.vidrierapp.server.model.CategoryMetricResponse
import com.vidrierapp.server.model.FrequencyMetricResponse
import com.vidrierapp.server.model.HourlyMetricResponse
import com.vidrierapp.server.model.ShopDetailResponse
import com.vidrierapp.server.model.ShopSummaryResponse
import com.vidrierapp.server.model.TrialResponse
import com.vidrierapp.server.model.CreateShopRequest
import com.vidrierapp.server.model.ReportRentedResponse
import com.vidrierapp.server.model.ZoneSummaryResponse
import com.vidrierapp.server.model.MockCheckoutResponse
import com.vidrierapp.server.model.UnclassifiedCategoryResponse
import com.vidrierapp.server.auth.AuthUser
import com.vidrierapp.server.auth.UserRole
import com.vidrierapp.server.application.port.PaymentGateway
import com.vidrierapp.server.application.port.PlatformRepository
import java.security.MessageDigest
import java.sql.ResultSet
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sql.DataSource

open class VidrierAppRepository(private val dataSource: DataSource) : PlatformRepository {
    override fun findAuthUser(username: String): AuthUser? = dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT username, password_hash, role FROM app_users WHERE username = ?").use { statement ->
            statement.setString(1, username)
            statement.executeQuery().use { results ->
                if (results.next()) AuthUser(results.getString("username"), results.getString("password_hash"), UserRole.valueOf(results.getString("role"))) else null
            }
        }
    }

    override fun shops(category: String?, bounds: Bounds?): List<ShopSummaryResponse> = dataSource.connection.use { connection ->
        val sql = buildString {
            append("SELECT s.id, s.address_text, s.status, ST_Y(s.location::geometry) latitude, ST_X(s.location::geometry) longitude, s.attributes, COALESCE(vs.vote_count, 0) vote_count, COALESCE(tc.label_es, vs.top_category) top_category FROM shops s LEFT JOIN mv_shop_vote_summary vs ON vs.shop_id = s.id LEFT JOIN categories tc ON tc.slug = vs.top_category WHERE s.status = 'AVAILABLE'")
            if (bounds != null) append(" AND s.location && ST_MakeEnvelope(?, ?, ?, ?, 4326)::geography")
            if (category != null) append(" AND EXISTS (SELECT 1 FROM votes v WHERE v.shop_id = s.id AND v.category = ?)")
            append(" ORDER BY s.id")
        }
        connection.prepareStatement(sql).use { statement ->
            var index = 1
            bounds?.let { statement.setDouble(index++, it.lonMin); statement.setDouble(index++, it.latMin); statement.setDouble(index++, it.lonMax); statement.setDouble(index++, it.latMax) }
            category?.let { statement.setString(index, it) }
            statement.executeQuery().use { results -> generateSequence { if (results.next()) results.toShopSummary() else null }.toList() }
        }
    }

    override fun shopDetail(id: UUID, metricsAllowed: Boolean): ShopDetailResponse = dataSource.connection.use { connection ->
        val shop = connection.prepareStatement("SELECT s.id, s.address_text, s.status, ST_Y(s.location::geometry) latitude, ST_X(s.location::geometry) longitude, s.attributes, COALESCE(vs.vote_count, 0) vote_count, vs.first_vote_at, vs.last_vote_at, COALESCE(tc.label_es, vs.top_category, '') top_category, COALESCE(vs.is_sample_sufficient, false) sufficient FROM shops s LEFT JOIN mv_shop_vote_summary vs ON vs.shop_id = s.id LEFT JOIN categories tc ON tc.slug = vs.top_category WHERE s.id = ?").use { statement ->
            statement.setObject(1, id)
            statement.executeQuery().use { results -> if (!results.next()) throw NoSuchElementException("Shop not found"); results.toShopDetailBase() }
        }
        if (!metricsAllowed || !shop.isSampleSufficient) return shop.copy(metricsAvailable = false)
        val frequency = frequencyMetrics(connection, id)
        val hourly = hourlyMetrics(connection, id)
        shop.copy(
            metricsAvailable = true,
            votes = categoryMetrics(connection, id, shop.totalVotes),
            frequency = frequency,
            hourlyDistribution = hourly,
            frequencyCounts = MetricMapping.frequencyCounts(frequency),
            votesByWeekday = MetricMapping.votesByWeekday(hourly),
            votesByTimeSlot = MetricMapping.votesByTimeSlot(hourly),
        )
    }

    override fun categoryAliases(): Map<String, String> = dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT alias, canonical_slug FROM category_aliases").use { statement ->
            statement.executeQuery().use { results -> buildMap { while (results.next()) put(results.getString("alias"), results.getString("canonical_slug")) } }
        }
    }

    override fun submitVote(shopId: UUID, category: NormalizedCategory, frequency: String?, fingerprint: String, browserToken: String, ipAddress: String): UUID = dataSource.connection.use { connection ->
        connection.autoCommit = false
        try {
            connection.prepareStatement("SELECT status FROM shops WHERE id = ? FOR UPDATE").use { statement -> statement.setObject(1, shopId); statement.executeQuery().use { results -> if (!results.next()) throw NoSuchElementException("Shop not found"); if (results.getString(1) != "AVAILABLE") throw IllegalArgumentException("Shop is not accepting votes") } }
            connection.prepareStatement("SELECT EXISTS (SELECT 1 FROM votes WHERE shop_id = ? AND created_at >= NOW() - INTERVAL '30 days' AND (fingerprint_hash = ? OR browser_token = ?))").use { statement -> statement.setObject(1, shopId); statement.setString(2, fingerprint); statement.setString(3, browserToken); statement.executeQuery().use { if (it.next() && it.getBoolean(1)) throw DuplicateVoteException() } }
            enforceIpRateLimit(connection, ipAddress)
            val voteId = UUID.randomUUID()
            connection.prepareStatement("INSERT INTO votes (id, shop_id, category, raw_category_input, category_status, frequency, day_of_week, hour_bucket, fingerprint_hash, browser_token) VALUES (?, ?, ?, ?, ?::category_status, ?::visit_frequency, EXTRACT(DOW FROM NOW() AT TIME ZONE 'America/Argentina/Buenos_Aires'), FLOOR(EXTRACT(HOUR FROM NOW() AT TIME ZONE 'America/Argentina/Buenos_Aires') / 2) * 2, ?, ?)").use { statement ->
                statement.setObject(1, voteId); statement.setObject(2, shopId); statement.setString(3, category.slug); statement.setString(4, category.rawInput); statement.setString(5, if (category.classified) "CLASSIFIED" else "UNCLASSIFIED"); statement.setString(6, frequency?.uppercase()); statement.setString(7, fingerprint); statement.setString(8, browserToken); statement.executeUpdate()
            }
            connection.commit()
            voteId
        } catch (error: Exception) { connection.rollback(); throw error } finally { connection.autoCommit = true }
    }

    override fun reportRented(id: UUID): ReportRentedResponse = dataSource.connection.use { connection ->
        connection.prepareStatement("UPDATE shops SET rented_report_count = rented_report_count + 1, status = CASE WHEN rented_report_count + 1 >= 3 AND status = 'AVAILABLE' THEN 'UNDER_REVIEW'::shop_status ELSE status END WHERE id = ? RETURNING rented_report_count, status::text").use { statement ->
            statement.setObject(1, id)
            statement.executeQuery().use { results ->
                if (!results.next()) throw NoSuchElementException("Shop not found")
                ReportRentedResponse(id.toString(), results.getInt(1), listingStatusOf(results.getString(2)))
            }
        }
    }

    override fun startTrial(username: String): TrialResponse = dataSource.connection.use { connection ->
        val userId = userId(connection, username)
        connection.prepareStatement("INSERT INTO subscriptions (user_id) VALUES (?) ON CONFLICT (user_id) DO NOTHING").use { it.setObject(1, userId); it.executeUpdate() }
        subscriptionResponse(connection, userId, username)
    }

    override fun checkoutPass(username: String, processor: PaymentGateway, paymentToken: String): MockCheckoutResponse = dataSource.connection.use { connection ->
        val userId = userId(connection, username)
        val decision = processor.charge(paymentToken)
        val paymentId = UUID.randomUUID()
        val reference = processor.providerReference()
        connection.autoCommit = false
        try {
            connection.prepareStatement("INSERT INTO mock_payments (id, user_id, provider_reference, amount_cents, status, failure_reason) VALUES (?, ?, ?, 500000, ?, ?)").use { statement ->
                statement.setObject(1, paymentId); statement.setObject(2, userId); statement.setString(3, reference); statement.setString(4, if (decision.approved) "APPROVED" else "DECLINED"); statement.setString(5, decision.failureReason); statement.executeUpdate()
            }
            val pass = if (decision.approved) activatePass(connection, userId, username) else null
            connection.commit()
            MockCheckoutResponse(paymentId.toString(), reference, if (decision.approved) "APPROVED" else "DECLINED", decision.failureReason, pass)
        } catch (error: Exception) { connection.rollback(); throw error } finally { connection.autoCommit = true }
    }

    override fun subscription(username: String): TrialResponse = dataSource.connection.use { connection ->
        val userId = userId(connection, username)
        connection.prepareStatement("SELECT EXISTS (SELECT 1 FROM subscriptions WHERE user_id = ?)").use { statement ->
            statement.setObject(1, userId)
            val exists = statement.executeQuery().use { it.next(); it.getBoolean(1) }
            if (exists) subscriptionResponse(connection, userId, username) else TrialResponse(username, "NONE", Instant.EPOCH.toString(), false)
        }
    }

    override fun hasActiveAccess(username: String): Boolean = dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT EXISTS (SELECT 1 FROM subscriptions s JOIN app_users u ON u.id = s.user_id WHERE u.username = ? AND COALESCE(s.pass_expires_at, s.trial_expires_at) > NOW())").use { statement -> statement.setString(1, username); statement.executeQuery().use { it.next(); it.getBoolean(1) } }
    }

    override fun createShop(request: CreateShopRequest): ShopSummaryResponse = dataSource.connection.use { connection ->
        require(request.latitude in -90.0..90.0 && request.longitude in -180.0..180.0) { "Invalid coordinates" }
        val attributes = "{\"neighborhood\":\"${jsonEscape(request.neighborhood)}\",\"surface_m2\":${request.surfaceAreaM2},\"rental_price\":\"${jsonEscape(request.rentalPrice)}\",\"agency\":\"${jsonEscape(request.agency)}\",\"phone\":\"${jsonEscape(request.contactPhone)}\",\"email\":\"${jsonEscape(request.contactEmail)}\"}"
        val id = UUID.randomUUID()
        connection.prepareStatement("INSERT INTO shops (id, address_text, place_id, location, location_override_used, attributes, created_by) VALUES (?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?, ?::jsonb, 'admin')").use { statement ->
            statement.setObject(1, id); statement.setString(2, request.address); statement.setString(3, request.placeId); statement.setDouble(4, request.longitude); statement.setDouble(5, request.latitude); statement.setBoolean(6, request.locationOverrideUsed); statement.setString(7, attributes); statement.executeUpdate()
        }
        ShopSummaryResponse(id.toString(), request.address, request.neighborhood, request.latitude, request.longitude, request.surfaceAreaM2, request.rentalPrice, status = "AVAILABLE", listingStatus = "ACTIVE")
    }

    override fun addCategoryAlias(alias: String, canonicalSlug: String) = dataSource.connection.use { connection ->
        require(alias.isNotBlank()) { "alias is required" }
        connection.prepareStatement("INSERT INTO category_aliases (alias, canonical_slug) VALUES (?, ?) ON CONFLICT (alias) DO UPDATE SET canonical_slug = EXCLUDED.canonical_slug").use { statement -> statement.setString(1, alias); statement.setString(2, canonicalSlug); statement.executeUpdate() }
    }

    override fun reclassifyVotes(alias: String, canonicalSlug: String): Int = dataSource.connection.use { connection ->
        connection.autoCommit = false
        try {
            connection.prepareStatement("INSERT INTO category_aliases (alias, canonical_slug) VALUES (?, ?) ON CONFLICT (alias) DO UPDATE SET canonical_slug = EXCLUDED.canonical_slug").use { statement -> statement.setString(1, alias); statement.setString(2, canonicalSlug); statement.executeUpdate() }
            val updated = connection.prepareStatement("UPDATE votes SET category = ?, category_status = 'CLASSIFIED'::category_status WHERE category_status = 'UNCLASSIFIED' AND lower(raw_category_input) = lower(?)").use { statement -> statement.setString(1, canonicalSlug); statement.setString(2, alias); statement.executeUpdate() }
            connection.commit()
            updated
        } catch (error: Exception) { connection.rollback(); throw error } finally { connection.autoCommit = true }
    }

    override fun unclassifiedCategories(): List<UnclassifiedCategoryResponse> = dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT raw_category_input, COUNT(*)::int vote_count FROM votes WHERE category_status = 'UNCLASSIFIED' AND raw_category_input IS NOT NULL GROUP BY raw_category_input ORDER BY vote_count DESC, raw_category_input LIMIT 100").use { statement -> statement.executeQuery().use { results -> buildList { while (results.next()) add(UnclassifiedCategoryResponse(results.getString(1), results.getInt(2))) } } }
    }

    override fun updateShopStatus(id: UUID, status: String) = dataSource.connection.use { connection ->
        require(status in setOf("AVAILABLE", "UNDER_REVIEW", "RENTED")) { "Invalid shop status" }
        connection.prepareStatement("UPDATE shops SET status = ?::shop_status, last_verified_at = CASE WHEN ? = 'AVAILABLE' THEN NOW() ELSE last_verified_at END, updated_by = 'admin' WHERE id = ?").use { statement -> statement.setString(1, status); statement.setString(2, status); statement.setObject(3, id); if (statement.executeUpdate() == 0) throw NoSuchElementException("Shop not found") }
    }

    override fun expireStaleShops(): Int = dataSource.connection.use { connection ->
        connection.prepareStatement("UPDATE shops SET status = 'UNDER_REVIEW', updated_by = 'maintenance' WHERE status = 'AVAILABLE' AND last_verified_at < NOW() - INTERVAL '60 days'").use { it.executeUpdate() }
    }

    override fun expireAccessStatuses(): Int = dataSource.connection.use { connection ->
        connection.prepareStatement("UPDATE subscriptions SET status = CASE WHEN pass_expires_at IS NOT NULL AND pass_expires_at <= NOW() THEN 'PASS_EXPIRED'::trial_status WHEN pass_expires_at IS NULL AND trial_expires_at <= NOW() THEN 'TRIAL_EXPIRED'::trial_status ELSE status END WHERE (pass_expires_at IS NOT NULL AND pass_expires_at <= NOW() AND status <> 'PASS_EXPIRED') OR (pass_expires_at IS NULL AND trial_expires_at <= NOW() AND status <> 'TRIAL_EXPIRED')").use { it.executeUpdate() }
    }

    override fun refreshAggregates() = dataSource.connection.use { connection -> connection.createStatement().use { statement -> statement.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_shop_vote_summary"); statement.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_shop_hourly_distribution") } }

    override fun zoneSummaries(): List<ZoneSummaryResponse> = dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT COALESCE(s.attributes->>'neighborhood', 'Sin barrio') neighborhood, COUNT(v.id)::int total_votes, COUNT(DISTINCT s.id) FILTER (WHERE v.id IS NOT NULL)::int shops_with_votes, MODE() WITHIN GROUP (ORDER BY v.category) FILTER (WHERE v.category_status = 'CLASSIFIED') top_category FROM shops s LEFT JOIN votes v ON v.shop_id = s.id GROUP BY COALESCE(s.attributes->>'neighborhood', 'Sin barrio') ORDER BY total_votes DESC").use { statement -> statement.executeQuery().use { results -> buildList { while (results.next()) add(ZoneSummaryResponse(results.getString(1), results.getInt(2), results.getInt(3), results.getString(4))) } } }
    }

    private fun categoryMetrics(connection: java.sql.Connection, id: UUID, total: Int) = query(connection, "SELECT v.category, COALESCE(c.label_es, v.category) label, COUNT(*) count FROM votes v LEFT JOIN categories c ON c.slug = v.category WHERE v.shop_id = ? AND v.category_status = 'CLASSIFIED' GROUP BY v.category, c.label_es ORDER BY count DESC", id) { CategoryMetricResponse(it.getString(1), it.getString(2), it.getInt(3), it.getInt(3) * 100f / total) }
    private fun frequencyMetrics(connection: java.sql.Connection, id: UUID) = query(connection, "SELECT frequency::text, COUNT(*) count FROM votes WHERE shop_id = ? AND frequency IS NOT NULL GROUP BY frequency ORDER BY count DESC", id) { FrequencyMetricResponse(it.getString(1), it.getInt(2)) }
    private fun hourlyMetrics(connection: java.sql.Connection, id: UUID) = query(connection, "SELECT day_of_week, hour_bucket, vote_count FROM mv_shop_hourly_distribution WHERE shop_id = ? ORDER BY day_of_week, hour_bucket", id) { HourlyMetricResponse(it.getInt(1), it.getInt(2), it.getInt(3)) }
    private fun <T> query(connection: java.sql.Connection, sql: String, id: UUID, mapper: (ResultSet) -> T): List<T> = connection.prepareStatement(sql).use { statement -> statement.setObject(1, id); statement.executeQuery().use { results -> buildList { while (results.next()) add(mapper(results)) } } }
    private fun ResultSet.toShopSummary(): ShopSummaryResponse { val attributes = getString("attributes"); return ShopSummaryResponse(getObject("id").toString(), getString("address_text"), jsonString(attributes, "neighborhood"), getDouble("latitude"), getDouble("longitude"), jsonInt(attributes, "surface_m2"), jsonString(attributes, "rental_price"), getString("top_category")?.takeIf { it.isNotBlank() }, getInt("vote_count"), getString("status"), listingStatusOf(getString("status"))) }
    private fun ResultSet.toShopDetailBase(): ShopDetailResponse { val attributes = getString("attributes"); return ShopDetailResponse(getObject("id").toString(), getString("address_text"), jsonString(attributes, "neighborhood"), getDouble("latitude"), getDouble("longitude"), jsonInt(attributes, "surface_m2"), jsonString(attributes, "rental_price"), getString("top_category").takeIf { it.isNotBlank() }, getInt("vote_count"), getString("status"), getObject("first_vote_at", java.time.OffsetDateTime::class.java)?.toInstant()?.toEpochMilli(), getObject("last_vote_at", java.time.OffsetDateTime::class.java)?.toInstant()?.toEpochMilli(), getBoolean("sufficient"), false, contactAgency = jsonString(attributes, "agency"), contactPhone = jsonString(attributes, "phone"), contactEmail = jsonString(attributes, "email"), listingStatus = listingStatusOf(getString("status"))) }
    private fun listingStatusOf(status: String) = if (status == "AVAILABLE") "ACTIVE" else status
    private fun jsonString(json: String, key: String) = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex().find(json)?.groupValues?.get(1) ?: ""
    private fun jsonInt(json: String, key: String) = "\"$key\"\\s*:\\s*(\\d+)".toRegex().find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    private fun trialResponse(username: String, status: String, expiresAt: Instant) = TrialResponse(username, status, expiresAt.toString(), expiresAt.isAfter(Instant.now()))
    private fun userId(connection: java.sql.Connection, username: String): UUID = connection.prepareStatement("SELECT id FROM app_users WHERE username = ?").use { statement -> statement.setString(1, username); statement.executeQuery().use { results -> if (results.next()) results.getObject(1, UUID::class.java) else throw SecurityException("Authenticated user no longer exists") } }
    private fun subscriptionResponse(connection: java.sql.Connection, userId: UUID, username: String): TrialResponse = connection.prepareStatement("SELECT status::text, COALESCE(pass_expires_at, trial_expires_at) FROM subscriptions WHERE user_id = ?").use { statement -> statement.setObject(1, userId); statement.executeQuery().use { results -> results.next(); trialResponse(username, results.getString(1), results.getObject(2, java.time.OffsetDateTime::class.java).toInstant()) } }
    private fun activatePass(connection: java.sql.Connection, userId: UUID, username: String): TrialResponse {
        connection.prepareStatement("INSERT INTO subscriptions (user_id, status, trial_expires_at, pass_activated_at, pass_expires_at) VALUES (?, 'PASS_ACTIVE', NOW(), NOW(), NOW() + INTERVAL '30 days') ON CONFLICT (user_id) DO UPDATE SET status = 'PASS_ACTIVE', pass_activated_at = NOW(), pass_expires_at = NOW() + INTERVAL '30 days'").use { statement -> statement.setObject(1, userId); statement.executeUpdate() }
        return subscriptionResponse(connection, userId, username)
    }
    private fun enforceIpRateLimit(connection: java.sql.Connection, ipAddress: String) {
        val ipHash = sha256(ipAddress)
        connection.prepareStatement("INSERT INTO vote_rate_limits (ip_hash, window_started_at, request_count) VALUES (?, date_trunc('hour', NOW()), 1) ON CONFLICT (ip_hash, window_started_at) DO UPDATE SET request_count = vote_rate_limits.request_count + 1 RETURNING request_count").use { statement -> statement.setString(1, ipHash); statement.executeQuery().use { results -> results.next(); if (results.getInt(1) > 20) throw RateLimitExceededException() } }
    }
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun jsonEscape(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"")
}

data class Bounds(val lonMin: Double, val latMin: Double, val lonMax: Double, val latMax: Double)

class DuplicateVoteException : RuntimeException("A vote was already submitted for this shop")

class RateLimitExceededException : RuntimeException("Too many votes from this network; try again later")

object MetricMapping {
    private val frequencyNames = mapOf("DAILY" to "DIARIA", "WEEKLY" to "SEMANAL", "MONTHLY" to "MENSUAL", "OCCASIONAL" to "OCASIONAL")
    private val weekdayNames = listOf("SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")

    open fun frequencyCounts(items: List<FrequencyMetricResponse>): Map<String, Int> =
        items.associate { (frequencyNames[it.frequency] ?: it.frequency) to it.voteCount }

    open fun votesByWeekday(items: List<HourlyMetricResponse>): Map<String, Int> =
        items.groupBy { weekdayNames[it.dayOfWeek.coerceIn(0, 6)] }.mapValues { entry -> entry.value.sumOf { it.voteCount } }

    open fun votesByTimeSlot(items: List<HourlyMetricResponse>): Map<String, Int> =
        items.groupBy { timeSlot(it.hourBucket) }.mapValues { entry -> entry.value.sumOf { it.voteCount } }

    open fun timeSlot(hour: Int): String = when (hour) {
        in 5..11 -> "MORNING"
        in 12..19 -> "AFTERNOON"
        else -> "NIGHT"
    }
}
