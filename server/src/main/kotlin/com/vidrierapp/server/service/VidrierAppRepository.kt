package com.vidrierapp.server.service

import com.vidrierapp.server.model.CategoryMetricResponse
import com.vidrierapp.server.model.FrequencyMetricResponse
import com.vidrierapp.server.model.HourlyMetricResponse
import com.vidrierapp.server.model.ShopDetailResponse
import com.vidrierapp.server.model.ShopSummaryResponse
import com.vidrierapp.server.model.TrialResponse
import java.sql.ResultSet
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sql.DataSource

class VidrierAppRepository(private val dataSource: DataSource) {
    fun shops(category: String?, bounds: Bounds?): List<ShopSummaryResponse> = dataSource.connection.use { connection ->
        val sql = buildString {
            append("SELECT s.id, s.address_text, s.status, ST_Y(s.location::geometry) latitude, ST_X(s.location::geometry) longitude, s.attributes, COALESCE(vs.vote_count, 0) vote_count, vs.top_category FROM shops s LEFT JOIN mv_shop_vote_summary vs ON vs.shop_id = s.id WHERE s.status = 'AVAILABLE'")
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

    fun shopDetail(id: UUID, metricsAllowed: Boolean): ShopDetailResponse = dataSource.connection.use { connection ->
        val shop = connection.prepareStatement("SELECT s.id, s.address_text, s.status, ST_Y(s.location::geometry) latitude, ST_X(s.location::geometry) longitude, s.attributes, COALESCE(vs.vote_count, 0) vote_count, vs.first_vote_at, vs.last_vote_at, COALESCE(vs.top_category, '') top_category, COALESCE(vs.is_sample_sufficient, false) sufficient FROM shops s LEFT JOIN mv_shop_vote_summary vs ON vs.shop_id = s.id WHERE s.id = ?").use { statement ->
            statement.setObject(1, id)
            statement.executeQuery().use { results -> if (!results.next()) throw NoSuchElementException("Shop not found"); results.toShopDetailBase() }
        }
        if (!metricsAllowed || !shop.isSampleSufficient) return shop.copy(metricsAvailable = false)
        shop.copy(metricsAvailable = true, votes = categoryMetrics(connection, id, shop.totalVotes), frequency = frequencyMetrics(connection, id), hourlyDistribution = hourlyMetrics(connection, id))
    }

    fun categoryAliases(): Map<String, String> = dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT alias, canonical_slug FROM category_aliases").use { statement ->
            statement.executeQuery().use { results -> buildMap { while (results.next()) put(results.getString("alias"), results.getString("canonical_slug")) } }
        }
    }

    fun submitVote(shopId: UUID, category: NormalizedCategory, frequency: String?, fingerprint: String, browserToken: String?): UUID = dataSource.connection.use { connection ->
        connection.autoCommit = false
        try {
            connection.prepareStatement("SELECT status FROM shops WHERE id = ? FOR UPDATE").use { statement -> statement.setObject(1, shopId); statement.executeQuery().use { results -> if (!results.next()) throw NoSuchElementException("Shop not found"); if (results.getString(1) != "AVAILABLE") throw IllegalArgumentException("Shop is not accepting votes") } }
            connection.prepareStatement("SELECT fn_vote_is_duplicate(?, ?)").use { statement -> statement.setString(1, fingerprint); statement.setObject(2, shopId); statement.executeQuery().use { if (it.next() && it.getBoolean(1)) throw IllegalArgumentException("A vote was already submitted for this shop") } }
            val voteId = UUID.randomUUID()
            connection.prepareStatement("INSERT INTO votes (id, shop_id, category, raw_category_input, category_status, frequency, day_of_week, hour_bucket, fingerprint_hash, browser_token) VALUES (?, ?, ?, ?, ?::category_status, ?::visit_frequency, EXTRACT(DOW FROM NOW()), FLOOR(EXTRACT(HOUR FROM NOW()) / 2) * 2, ?, ?)").use { statement ->
                statement.setObject(1, voteId); statement.setObject(2, shopId); statement.setString(3, category.slug); statement.setString(4, category.rawInput); statement.setString(5, if (category.classified) "CLASSIFIED" else "UNCLASSIFIED"); statement.setString(6, frequency?.uppercase()); statement.setString(7, fingerprint); statement.setString(8, browserToken); statement.executeUpdate()
            }
            connection.createStatement().use { it.execute("REFRESH MATERIALIZED VIEW mv_shop_vote_summary"); it.execute("REFRESH MATERIALIZED VIEW mv_shop_hourly_distribution") }
            connection.commit()
            voteId
        } catch (error: Exception) { connection.rollback(); throw error } finally { connection.autoCommit = true }
    }

    fun reportRented(id: UUID) = dataSource.connection.use { connection ->
        connection.prepareStatement("UPDATE shops SET rented_report_count = rented_report_count + 1, status = CASE WHEN rented_report_count + 1 >= 3 THEN 'UNDER_REVIEW'::shop_status ELSE status END WHERE id = ?").use { statement -> statement.setObject(1, id); if (statement.executeUpdate() == 0) throw NoSuchElementException("Shop not found") }
    }

    fun startTrial(deviceId: String): TrialResponse = dataSource.connection.use { connection ->
        connection.prepareStatement("INSERT INTO device_trials (device_id, trial_expires_at) VALUES (?, NOW() + INTERVAL '3 days') ON CONFLICT (device_id) DO NOTHING").use { it.setString(1, deviceId); it.executeUpdate() }
        connection.prepareStatement("SELECT status::text, COALESCE(pass_expires_at, trial_expires_at) expires_at FROM device_trials WHERE device_id = ?").use { statement -> statement.setString(1, deviceId); statement.executeQuery().use { results -> results.next(); trialResponse(deviceId, results.getString(1), results.getObject(2, Instant::class.java)) } }
    }

    fun hasActiveAccess(deviceId: String?): Boolean {
        if (deviceId.isNullOrBlank()) return false
        return dataSource.connection.use { connection -> connection.prepareStatement("SELECT EXISTS (SELECT 1 FROM device_trials WHERE device_id = ? AND COALESCE(pass_expires_at, trial_expires_at) > NOW())").use { statement -> statement.setString(1, deviceId); statement.executeQuery().use { it.next(); it.getBoolean(1) } } }
    }

    private fun categoryMetrics(connection: java.sql.Connection, id: UUID, total: Int) = query(connection, "SELECT v.category, COALESCE(c.label_es, v.category) label, COUNT(*) count FROM votes v LEFT JOIN categories c ON c.slug = v.category WHERE v.shop_id = ? AND v.category_status = 'CLASSIFIED' GROUP BY v.category, c.label_es ORDER BY count DESC", id) { CategoryMetricResponse(it.getString(1), it.getString(2), it.getInt(3), it.getInt(3) * 100f / total) }
    private fun frequencyMetrics(connection: java.sql.Connection, id: UUID) = query(connection, "SELECT frequency::text, COUNT(*) count FROM votes WHERE shop_id = ? AND frequency IS NOT NULL GROUP BY frequency ORDER BY count DESC", id) { FrequencyMetricResponse(it.getString(1), it.getInt(2)) }
    private fun hourlyMetrics(connection: java.sql.Connection, id: UUID) = query(connection, "SELECT day_of_week, hour_bucket, vote_count FROM mv_shop_hourly_distribution WHERE shop_id = ? ORDER BY day_of_week, hour_bucket", id) { HourlyMetricResponse(it.getInt(1), it.getInt(2), it.getInt(3)) }
    private fun <T> query(connection: java.sql.Connection, sql: String, id: UUID, mapper: (ResultSet) -> T): List<T> = connection.prepareStatement(sql).use { statement -> statement.setObject(1, id); statement.executeQuery().use { results -> buildList { while (results.next()) add(mapper(results)) } } }
    private fun ResultSet.toShopSummary(): ShopSummaryResponse { val attributes = getString("attributes"); return ShopSummaryResponse(getObject("id").toString(), getString("address_text"), jsonString(attributes, "neighborhood"), getDouble("latitude"), getDouble("longitude"), jsonInt(attributes, "surface_m2"), jsonString(attributes, "rental_price"), getString("top_category")?.takeIf { it.isNotBlank() }, getInt("vote_count"), getString("status")) }
    private fun ResultSet.toShopDetailBase(): ShopDetailResponse { val attributes = getString("attributes"); return ShopDetailResponse(getObject("id").toString(), getString("address_text"), jsonString(attributes, "neighborhood"), getDouble("latitude"), getDouble("longitude"), jsonInt(attributes, "surface_m2"), jsonString(attributes, "rental_price"), getString("top_category").takeIf { it.isNotBlank() }, getInt("vote_count"), getString("status"), getObject("first_vote_at", Instant::class.java)?.toString(), getObject("last_vote_at", Instant::class.java)?.toString(), getBoolean("sufficient"), false, contactAgency = jsonString(attributes, "agency")) }
    private fun jsonString(json: String, key: String) = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex().find(json)?.groupValues?.get(1) ?: ""
    private fun jsonInt(json: String, key: String) = "\"$key\"\\s*:\\s*(\\d+)".toRegex().find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    private fun trialResponse(deviceId: String, status: String, expiresAt: Instant) = TrialResponse(deviceId, status, expiresAt.toString(), expiresAt.isAfter(Instant.now()))
}

data class Bounds(val lonMin: Double, val latMin: Double, val lonMax: Double, val latMax: Double)
