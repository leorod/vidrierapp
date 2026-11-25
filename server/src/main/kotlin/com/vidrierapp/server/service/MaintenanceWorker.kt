package com.vidrierapp.server.service

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val AGGREGATES_REFRESH_SECONDS = 30L

class MaintenanceWorker(private val repository: VidrierAppRepository) {
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "vidrierapp-maintenance").apply { isDaemon = true }
    }

    fun start() {
        executor.scheduleAtFixedRate(
            { runCatching { repository.expireStaleShops(); repository.expireAccessStatuses() } },
            1,
            60,
            TimeUnit.MINUTES,
        )
        executor.scheduleWithFixedDelay(
            { runCatching { repository.refreshAggregates() } },
            AGGREGATES_REFRESH_SECONDS,
            AGGREGATES_REFRESH_SECONDS,
            TimeUnit.SECONDS,
        )
    }
}
